package me.anduria.staffsystem;

import me.anduria.staffsystem.code.CodeManager;
import me.anduria.staffsystem.commands.AStaffCommand;
import me.anduria.staffsystem.commands.CodeCommand;
import me.anduria.staffsystem.commands.CreateCodeCommand;
import me.anduria.staffsystem.commands.DynamicCommand;
import me.anduria.staffsystem.commands.NickCommand;
import me.anduria.staffsystem.commands.ReportCommand;
import me.anduria.staffsystem.commands.StaffChatCommand;
import me.anduria.staffsystem.commands.StaffSystemCommand;
import me.anduria.staffsystem.commands.VanishCommand;
import me.anduria.staffsystem.config.ConfigManager;
import me.anduria.staffsystem.database.MySQLStorageProvider;
import me.anduria.staffsystem.listeners.GUIListener;
import me.anduria.staffsystem.listeners.VanishListener;
import me.anduria.staffsystem.report.ReportManager;
import me.anduria.staffsystem.storage.StorageProvider;
import me.anduria.staffsystem.storage.YamlStorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hlavni trida pluginu Anduria-StaffSystem.
 *
 * <p>Staticke commandy (plugin.yml): report, astaff, anduriastaff.
 * Dynamicke commandy (config.yml):  code, create-code, staffchat, vanish, nick.
 *
 * <p>Udrzuje v pameti:
 * <ul>
 *   <li>Session mapu: reporter UUID -&gt; TargetSession (pro GUI)</li>
 *   <li>Vanish mnozinu: UUID vanishnutes hracu</li>
 * </ul>
 */
public final class AnduriaStaffSystem extends JavaPlugin {

    private static AnduriaStaffSystem instance;

    private ConfigManager   configManager;
    private StorageProvider storageProvider;
    private ReportManager   reportManager;
    private CodeManager     codeManager;

    /** GUI session mapa: reporter UUID -&gt; cil session. */
    private final Map<UUID, TargetSession> sessions = new ConcurrentHashMap<>();

    /** Mnozina UUID vanishnutes hracu. Thread-safe Set. */
    private final Set<UUID> vanishedPlayers = Collections.synchronizedSet(new HashSet<>());

    // ---- Lifecycle ----------------------------------------------

    @Override
    public void onEnable() {
        instance = this;

        configManager   = new ConfigManager(this);
        configManager.loadAll();

        storageProvider = initStorage();
        reportManager   = new ReportManager(this, storageProvider);
        codeManager     = new CodeManager(this);
        codeManager.loadAll();

        registerStaticCommands();
        registerDynamicCommands();
        registerListeners();

        getLogger().info("Anduria-StaffSystem byl spusten.");
    }

    @Override
    public void onDisable() {
        if (storageProvider instanceof MySQLStorageProvider mysql) mysql.close();
        sessions.clear();
        vanishedPlayers.clear();
        getLogger().info("Anduria-StaffSystem byl vypnut.");
    }

    // ---- Reload -------------------------------------------------

    public void reload() {
        configManager.loadAll();
        if (storageProvider instanceof MySQLStorageProvider mysql) mysql.close();
        storageProvider = initStorage();
        reportManager   = new ReportManager(this, storageProvider);
        codeManager.loadAll();
        getLogger().info("Anduria-StaffSystem byl reloadnut.");
    }

    // ---- Command registration -----------------------------------

    /** Staticke commandy definovane v plugin.yml. */
    private void registerStaticCommands() {
        ReportCommand reportCmd = new ReportCommand(this);
        getCommand("report").setExecutor(reportCmd);
        getCommand("report").setTabCompleter(reportCmd);

        AStaffCommand astaffCmd = new AStaffCommand(this);
        getCommand("astaff").setExecutor(astaffCmd);
        getCommand("astaff").setTabCompleter(astaffCmd);

        StaffSystemCommand adminCmd = new StaffSystemCommand(this);
        getCommand("anduriastaff").setExecutor(adminCmd);
        getCommand("anduriastaff").setTabCompleter(adminCmd);
    }

    /**
     * Dynamicke commandy registrovane pres Bukkit CommandMap.
     * Jmena a aliasy jsou nacteny z config.yml sekce 'commands'.
     */
    private void registerDynamicCommands() {
        CommandMap commandMap;
        try {
            commandMap = (CommandMap) getServer().getClass()
                    .getMethod("getCommandMap").invoke(getServer());
        } catch (Exception e) {
            getLogger().severe("[Commands] Nelze ziskat CommandMap: " + e.getMessage());
            return;
        }

        registerDynamic(commandMap, "code",        new CodeCommand(this));
        registerDynamic(commandMap, "create-code", new CreateCodeCommand(this));
        registerDynamic(commandMap, "staffchat",   new StaffChatCommand(this));
        registerDynamic(commandMap, "vanish",       new VanishCommand(this));
        registerDynamic(commandMap, "nick",         new NickCommand(this));
    }

    /**
     * Pomocna metoda: zaregistruje jeden dynamicky prikaz.
     * CommandExecutor musi implementovat take TabCompleter.
     */
    private <T extends org.bukkit.command.CommandExecutor & org.bukkit.command.TabCompleter>
    void registerDynamic(CommandMap commandMap, String configKey, T executor) {
        List<String> aliasesCopy = new ArrayList<>(configManager.getCommandAliases(configKey));
        if (aliasesCopy.isEmpty()) {
            getLogger().warning("[Commands] Zadne aliasy pro '" + configKey + "' v config.yml!");
            return;
        }
        String main = aliasesCopy.remove(0);
        commandMap.register("anduria", new DynamicCommand(
                main,
                configManager.getCommandDescription(configKey),
                configManager.getCommandUsage(configKey),
                aliasesCopy,
                executor,
                executor
        ));
        getLogger().info("[Commands] Registrovan '/" + main + "' (aliasy: " + aliasesCopy + ")");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GUIListener(this),     this);
        getServer().getPluginManager().registerEvents(new VanishListener(this),  this);
        // StaffChatListener neni registrovan — toggle mod byl odebran
    }

    // ---- Storage init ------------------------------------------

    private StorageProvider initStorage() {
        if (configManager.isDatabaseEnabled()) {
            try {
                MySQLStorageProvider mysql = new MySQLStorageProvider(this);
                mysql.initialize();
                getLogger().info("[Storage] MySQL.");
                return mysql;
            } catch (Exception e) {
                getLogger().severe("[Storage] MySQL selhalo: " + e.getMessage());
                getLogger().warning("[Storage] Fallback na YAML.");
            }
        }
        YamlStorageProvider yaml = new YamlStorageProvider(this);
        yaml.initialize();
        getLogger().info("[Storage] YAML (reports/reports.yml).");
        return yaml;
    }

    // ---- Session API -------------------------------------------

    public void putSession(UUID uid, TargetSession session) { sessions.put(uid, session); }
    public TargetSession getSession(UUID uid)               { return sessions.get(uid); }
    public void removeSession(UUID uid)                     { sessions.remove(uid); }

    // ---- Vanish API --------------------------------------------

    /**
     * Prepne vanish stav hrace.
     * @return true = hrac je ted vanished, false = hrac je ted viditelny
     */
    public boolean toggleVanish(UUID uuid) {
        if (vanishedPlayers.remove(uuid)) return false;
        vanishedPlayers.add(uuid);
        return true;
    }

    /** Odsrani hrace z vanish evidence (napr. pri odchodu). */
    public void removeVanish(UUID uuid) { vanishedPlayers.remove(uuid); }

    /** @return true pokud je hrac aktualne vanished */
    public boolean isVanished(UUID uuid) { return vanishedPlayers.contains(uuid); }

    /**
     * Vrati seznam aktualne online vanishnutes hracu.
     * Pouzivano v VanishListeneru pri joinu noveho hrace.
     */
    public List<Player> getVanishedPlayers() {
        return vanishedPlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toList());
    }

    // ---- Gettery -----------------------------------------------

    public static AnduriaStaffSystem getInstance() { return instance; }
    public ConfigManager   getConfigManager()      { return configManager; }
    public StorageProvider getStorageProvider()    { return storageProvider; }
    public ReportManager   getReportManager()      { return reportManager; }
    public CodeManager     getCodeManager()        { return codeManager; }

    // ---- Vnorene typy ------------------------------------------

    /**
     * Immutable zaznam o cilove hrace pro dane GUI session.
     * Pouzivano v GUIListeneru a ReportGUI pro identifikaci targetu.
     */
    public record TargetSession(UUID targetUuid, String targetName) {}
}