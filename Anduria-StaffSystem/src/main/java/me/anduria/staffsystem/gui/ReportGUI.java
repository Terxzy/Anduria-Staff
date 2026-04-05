package me.anduria.staffsystem.gui;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.utils.ColorUtils;
import me.anduria.staffsystem.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Buduje a otvira report GUI.
 *
 * <p>Klic zmena v teto verzi:
 * Inventar je vytvoreny s {@link ReportGUIHolder} jako holderem.
 * GUIListener identifikuje nas inventar pomoci
 * {@code holder instanceof ReportGUIHolder} — zadne porovnavani titulku.
 *
 * <p>Holder zaroven drzi slot-&gt;GuiItem mapu pro O(1) vyhledavani pri kliknuti.
 */
public class ReportGUI {

    private final AnduriaStaffSystem plugin;

    public ReportGUI(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    // ---- Otevreni GUI -------------------------------------------

    /**
     * Otevre report GUI pro hrace reportera.
     * Target muze byt offline — identifikujeme ho UUID a jmenem.
     *
     * @param reporter   hrac, ktery otevre GUI
     * @param targetUuid UUID nahlasovaneho hrace
     * @param targetName jmeno nahlasovaneho hrace
     */
    public void open(Player reporter, UUID targetUuid, String targetName) {
        // 1. Vytvor holder se session daty
        ReportGUIHolder holder = new ReportGUIHolder(targetUuid, targetName);

        // 2. Priprav titulek (barvicka + placeholder %target%)
        String rawTitle = plugin.getConfigManager().getMenuTitle();
        String title = ColorUtils.colorize(
                PlaceholderUtils.replace(rawTitle, reporter, targetName, ""));

        // 3. Vytvor inventar s nasim holderem
        int size = plugin.getConfigManager().getMenuSize();
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        // 4. Vloz itemy a registruj je do slot mapy
        Map<String, GuiItem> items = plugin.getConfigManager().getGuiItems();
        for (GuiItem guiItem : items.values()) {
            if (!canView(reporter, guiItem)) continue;
            int slot = guiItem.getSlot();
            if (slot < 0 || slot >= size) continue;

            inv.setItem(slot, buildItemStack(guiItem, reporter, targetName));
            holder.registerItem(slot, guiItem);   // <-- klic: zaregistruj pro GUIListener
        }

        // 5. Uloz session a otevri inventar
        plugin.putSession(reporter.getUniqueId(),
                new AnduriaStaffSystem.TargetSession(targetUuid, targetName));
        reporter.openInventory(inv);
    }

    /**
     * Zkratka pro online target.
     */
    public void open(Player reporter, Player target) {
        open(reporter, target.getUniqueId(), target.getName());
    }

    /**
     * Zkratka pro offline / override target (pouze jmeno — UUID se dopocita).
     */
    public void open(Player reporter, String targetName) {
        // Zkusime najit online hrace
        var onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            open(reporter, onlineTarget.getUniqueId(), targetName);
        } else {
            // Offline fallback: deterministicke UUID
            UUID offlineUuid = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + targetName).getBytes(StandardCharsets.UTF_8));
            open(reporter, offlineUuid, targetName);
        }
    }

    // ---- Item building ------------------------------------------

    private ItemStack buildItemStack(GuiItem guiItem, Player reporter, String targetName) {
        ItemStack stack = guiItem.hasBasehead()
                ? buildSkullFromBase64(guiItem.getBasehead(), guiItem.getAmount())
                : new ItemStack(guiItem.getMaterial(), guiItem.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Nazev
        String name = PlaceholderUtils.replace(
                guiItem.getDisplayName(), reporter, targetName, guiItem.getReportCategory());
        meta.setDisplayName(ColorUtils.colorize(name));

        // Lore
        List<String> lore = new ArrayList<>();
        for (String line : guiItem.getLore()) {
            lore.add(ColorUtils.colorize(
                    PlaceholderUtils.replace(line, reporter, targetName, guiItem.getReportCategory())));
        }
        meta.setLore(lore);

        // Custom model data
        if (guiItem.getCustomModelData() > 0) {
            meta.setCustomModelData(guiItem.getCustomModelData());
        }

        // Glow efekt (fake enchant + hide flag)
        if (guiItem.isGlow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Postavi PLAYER_HEAD s Base64 texturou (DeluxeMenus basehead styl).
     * Pri jakekoli chybe vrati plain PLAYER_HEAD.
     */
    private ItemStack buildSkullFromBase64(String base64, int amount) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, Math.max(1, amount));
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, StandardCharsets.UTF_8);

            if (!json.contains("\"url\":\"")) {
                plugin.getLogger().warning("[Basehead] JSON neobsahuje URL textury.");
                return skull;
            }
            String url = json.split("\"url\":\"")[1].split("\"")[0];

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(url).toURL());
            profile.setTextures(textures);

            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwnerProfile(profile);
                skull.setItemMeta(meta);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Basehead] Chyba pri parsovani: " + e.getMessage());
        }
        return skull;
    }

    // ---- Permission check ---------------------------------------

    private boolean canView(Player player, GuiItem guiItem) {
        if (!guiItem.getPermission().isBlank() && !player.hasPermission(guiItem.getPermission()))
            return false;
        if (!guiItem.getViewRequirement().isBlank() && !player.hasPermission(guiItem.getViewRequirement()))
            return false;
        return true;
    }
}