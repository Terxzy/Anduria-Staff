package me.anduria.staffsystem.code;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spravuje nacitani, pouzivani a sledovani kodu (codes/*.yml).
 *
 * <p>Struktura slozky:
 * <pre>
 * codes/
 *   vip.yml        – definice kodu
 *   test.yml
 *   usage.yml      – auto-generovany soubor pro sledovani vyuziti
 * </pre>
 *
 * <p>usage.yml format:
 * <pre>
 * vip:
 *   last-use:
 *     &lt;uuid&gt;: &lt;timestamp-ms&gt;
 *   total-uses: 5
 * </pre>
 */
public class CodeManager {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AnduriaStaffSystem plugin;
    private final File codesDir;
    private final File usageFile;

    /** Nactene kody: lowercase name -> CodeData */
    private final Map<String, CodeData> codes = new HashMap<>();

    /** Usage data z usage.yml: code name -> (uuid -> timestamp posledniho pouziti) */
    private FileConfiguration usageConfig;

    public CodeManager(AnduriaStaffSystem plugin) {
        this.plugin    = plugin;
        this.codesDir  = new File(plugin.getDataFolder(), "codes");
        this.usageFile = new File(codesDir, "usage.yml");
    }

    // ---- Inicializace -------------------------------------------

    public void loadAll() {
        codesDir.mkdirs();

        // Zkopiruj default codes z JARu pokud neexistuji
        for (String defaultCode : List.of("codes/vip.yml", "codes/test.yml")) {
            File target = new File(plugin.getDataFolder(), defaultCode);
            if (!target.exists()) {
                try { plugin.saveResource(defaultCode, false); }
                catch (Exception ignored) {}
            }
        }

        // Nacti usage.yml
        loadUsage();

        // Nacti vsechny *.yml soubory (krome usage.yml)
        codes.clear();
        File[] files = codesDir.listFiles((d, n) -> n.endsWith(".yml") && !n.equals("usage.yml"));
        if (files != null) {
            for (File f : files) {
                CodeData data = loadCodeFile(f);
                if (data != null) {
                    codes.put(data.getName().toLowerCase(), data);
                }
            }
        }
        plugin.getLogger().info("[CodeManager] Nacteno " + codes.size() + " kodu.");
    }

    private CodeData loadCodeFile(File file) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("name", file.getName().replace(".yml", ""));

        return new CodeData(
                name,
                cfg.getString("permission", ""),
                cfg.getBoolean("one-time", false),
                cfg.getLong("cooldown", 0),
                cfg.getInt("max-uses", 0),
                cfg.getStringList("commands"),
                cfg.getString("messages.success",       "&aKod uspesne pouzit."),
                cfg.getString("messages.no-permission", "&cNemas opravneni."),
                cfg.getString("messages.already-used",  "&cJiz jsi pouzil tento kod."),
                cfg.getString("messages.cooldown",      "&cCooldown: %time%."),
                cfg.getString("messages.max-uses",      "&cKod je vyerpan."),
                cfg.getString("messages.not-found",     "&cKod neexistuje.")
        );
    }

    private void loadUsage() {
        if (!usageFile.exists()) {
            try { usageFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().warning("[CodeManager] Nelze vytvorit usage.yml: " + e.getMessage());
            }
        }
        usageConfig = YamlConfiguration.loadConfiguration(usageFile);
    }

    private void saveUsage() {
        try { usageConfig.save(usageFile); }
        catch (IOException e) { plugin.getLogger().warning("[CodeManager] Nelze ulozit usage.yml: " + e.getMessage()); }
    }

    // ---- Pouziti kodu -------------------------------------------

    public enum UseResult { SUCCESS, NOT_FOUND, NO_PERMISSION, ALREADY_USED, COOLDOWN, MAX_USES }

    /**
     * Pokusi se pouzit kod pro daneho hrace.
     * Vsechna logika (permission, one-time, cooldown, max-uses) je zde.
     *
     * @param player hrac, ktery pouziva kod
     * @param codeName jmeno kodu (case-insensitive)
     * @return vysledek pokusu
     */
    public UseResult useCode(Player player, String codeName) {
        CodeData code = codes.get(codeName.toLowerCase());
        if (code == null) return UseResult.NOT_FOUND;

        UUID uuid = player.getUniqueId();
        String uuidStr = uuid.toString();
        String basePath = code.getName() + ".";

        // 1. Permission
        if (!code.getPermission().isBlank() && !player.hasPermission(code.getPermission())) {
            return UseResult.NO_PERMISSION;
        }

        // 2. One-time pouziti
        if (code.isOneTime()) {
            boolean used = usageConfig.getBoolean(basePath + "one-time-used." + uuidStr, false);
            if (used) return UseResult.ALREADY_USED;
        }

        // 3. Cooldown (ignorovano pokud one-time)
        if (!code.isOneTime() && code.getCooldown() > 0) {
            long lastUse = usageConfig.getLong(basePath + "last-use." + uuidStr, 0L);
            long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
            if (elapsed < code.getCooldown()) {
                long remaining = code.getCooldown() - elapsed;
                // Ulozime remaining pro placeholder %time%
                player.setMetadata("anduria_cd_remaining",
                        new org.bukkit.metadata.FixedMetadataValue(plugin, remaining));
                return UseResult.COOLDOWN;
            }
        }

        // 4. Max-uses (celkove)
        if (code.getMaxUses() > 0) {
            int total = usageConfig.getInt(basePath + "total-uses", 0);
            if (total >= code.getMaxUses()) return UseResult.MAX_USES;
        }

        // ==== Kod je platny — proved commandy ====
        String time = LocalDateTime.now().format(TIME_FMT);
        for (String cmd : code.getCommands()) {
            executeCommand(player, cmd, time);
        }

        // Zaznamenej pouziti
        long now = System.currentTimeMillis();
        if (code.isOneTime()) {
            usageConfig.set(basePath + "one-time-used." + uuidStr, true);
        } else {
            usageConfig.set(basePath + "last-use." + uuidStr, now);
        }
        int totalUses = usageConfig.getInt(basePath + "total-uses", 0);
        usageConfig.set(basePath + "total-uses", totalUses + 1);
        saveUsage();

        return UseResult.SUCCESS;
    }

    /**
     * Provede jeden command z kodu.
     * Podporuje: [console], [player], [message].
     */
    private void executeCommand(Player player, String rawCmd, String time) {
        if (rawCmd == null || rawCmd.isBlank()) return;

        String cmd = rawCmd
                .replace("%player%", player.getName())
                .replace("%time%", time);

        if (cmd.startsWith("[console] ")) {
            String c = cmd.substring("[console] ".length());
            Bukkit.getScheduler().runTask(plugin, (Runnable)
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c));

        } else if (cmd.startsWith("[player] ")) {
            String c = cmd.substring("[player] ".length());
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> player.performCommand(c));

        } else if (cmd.startsWith("[message] ")) {
            player.sendMessage(ColorUtils.colorize(cmd.substring("[message] ".length())));
        }
    }

    // ---- Zpravy s placeholdery -----------------------------------

    public String buildCooldownMsg(CodeData code, Player player) {
        long remaining = 0;
        if (player.hasMetadata("anduria_cd_remaining")) {
            remaining = player.getMetadata("anduria_cd_remaining").get(0).asLong();
            player.removeMetadata("anduria_cd_remaining", plugin);
        }
        return code.getMsgCooldown().replace("%time%", formatSeconds(remaining));
    }

    private String formatSeconds(long secs) {
        if (secs < 60)   return secs + "s";
        if (secs < 3600) return (secs / 60) + "m " + (secs % 60) + "s";
        return (secs / 3600) + "h " + ((secs % 3600) / 60) + "m";
    }

    // ---- Vytvoreni noveho kodu ----------------------------------

    /**
     * Vytvori novy kod soubor s default templatem.
     *
     * @param codeName jmeno kodu
     * @return true = soubor byl vytvoren, false = jiz existuje
     */
    public boolean createCode(String codeName) {
        File target = new File(codesDir, codeName.toLowerCase() + ".yml");
        if (target.exists()) return false;

        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("name", codeName.toLowerCase());
        cfg.set("permission", "");
        cfg.set("one-time", false);
        cfg.set("cooldown", 0);
        cfg.set("max-uses", 0);
        cfg.set("commands", List.of("[message] &aKod %player% pouzit."));
        cfg.set("messages.success",       "&aKod uspesne pouzit.");
        cfg.set("messages.no-permission", "&cNemas opravneni.");
        cfg.set("messages.already-used",  "&cJiz pouzito.");
        cfg.set("messages.cooldown",      "&cCooldown: %time%.");
        cfg.set("messages.max-uses",      "&cKod je vyerpan.");
        cfg.set("messages.not-found",     "&cKod neexistuje.");

        try {
            cfg.save(target);
            // Nacti novy kod do pameti
            CodeData data = loadCodeFile(target);
            if (data != null) codes.put(data.getName().toLowerCase(), data);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[CodeManager] Nelze vytvorit kod '" + codeName + "': " + e.getMessage());
            return false;
        }
    }

    // ---- Gettery ------------------------------------------------

    public CodeData getCode(String name) {
        return codes.get(name.toLowerCase());
    }

    public Map<String, CodeData> getAllCodes() { return codes; }
}