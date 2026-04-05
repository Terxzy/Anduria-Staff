package me.anduria.staffsystem.config;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.gui.GuiItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralni spravce vsech konfiguraci pluginu.
 *
 * <p>Soubory:
 * <ul>
 *   <li>{@code config.yml}              – menu, zpravy, GUI itemy, command aliasy</li>
 *   <li>{@code reports/config.yml}      – reporter-message</li>
 *   <li>{@code reports/categories.yml}  – report kategorie</li>
 *   <li>{@code staffchat/config.yml}    – format, usage</li>
 *   <li>{@code vanish/config.yml}       – vanish zpravy</li>
 *   <li>{@code nick/config.yml}         – nick zpravy, max-length</li>
 *   <li>{@code webhook/config.yml}      – Discord webhook</li>
 *   <li>{@code database/config.yml}     – databaze</li>
 * </ul>
 */
public class ConfigManager {

    private final AnduriaStaffSystem plugin;

    private PluginFile reportsConfigFile;
    private PluginFile categoriesFile;
    private PluginFile staffchatFile;
    private PluginFile vanishFile;
    private PluginFile nickFile;
    private PluginFile webhookFile;
    private PluginFile databaseFile;

    private final Map<String, GuiItem> guiItems   = new HashMap<>();
    private final Map<String, String>  categories = new HashMap<>();

    public ConfigManager(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    // ---- Nacitani -----------------------------------------------

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        reportsConfigFile = new PluginFile(plugin, "reports/config.yml");
        categoriesFile    = new PluginFile(plugin, "reports/categories.yml");
        staffchatFile     = new PluginFile(plugin, "staffchat/config.yml");
        vanishFile        = new PluginFile(plugin, "vanish/config.yml");
        nickFile          = new PluginFile(plugin, "nick/config.yml");
        webhookFile       = new PluginFile(plugin, "webhook/config.yml");
        databaseFile      = new PluginFile(plugin, "database/config.yml");

        reportsConfigFile.loadOrCreate();
        categoriesFile.loadOrCreate();
        staffchatFile.loadOrCreate();
        vanishFile.loadOrCreate();
        nickFile.loadOrCreate();
        webhookFile.loadOrCreate();
        databaseFile.loadOrCreate();

        loadGuiItems();
        loadCategories();

        plugin.getLogger().info("[Config] Nacteno: " + guiItems.size() + " GUI item(u), "
                + categories.size() + " kategori(i).");
    }

    // ---- GUI items ----------------------------------------------

    private void loadGuiItems() {
        guiItems.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("items");
        if (sec == null) { plugin.getLogger().warning("[Config] Chybi sekce 'items'!"); return; }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection item = sec.getConfigurationSection(key);
            if (item != null) guiItems.put(key, parseGuiItem(key, item));
        }
    }

    private GuiItem parseGuiItem(String key, ConfigurationSection sec) {
        GuiItem item = new GuiItem();
        item.setKey(key);
        String matName = sec.getString("material", "STONE").toUpperCase();
        Material mat = Material.matchMaterial(matName);
        item.setMaterial(mat != null ? mat : Material.STONE);
        item.setAmount(sec.getInt("amount", 1));
        item.setSlot(sec.getInt("slot", 0));
        item.setDisplayName(sec.getString("display_name", " "));
        item.setLore(sec.getStringList("lore"));
        item.setGlow(sec.getBoolean("glow", false));
        item.setCustomModelData(sec.getInt("data", 0));
        item.setPermission(sec.getString("permission", ""));
        item.setViewRequirement(sec.getString("view_requirement", ""));
        item.setReportCategory(sec.getString("report_category", ""));
        item.setClose(sec.getBoolean("close", false));
        item.setLeftClickCommands(sec.getStringList("left_click_commands"));
        item.setRightClickCommands(sec.getStringList("right_click_commands"));
        item.setBasehead(sec.getString("basehead", ""));
        return item;
    }

    // ---- Categories ---------------------------------------------

    private void loadCategories() {
        categories.clear();
        if (categoriesFile == null) return;
        ConfigurationSection sec = categoriesFile.getConfig().getConfigurationSection("categories");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            categories.put(key.toUpperCase(), sec.getString(key + ".display", key));
        }
    }

    // ---- Gettery — main config ----------------------------------

    public String getMenuTitle()  { return plugin.getConfig().getString("menu.title", "&8Report"); }

    public int getMenuSize() {
        int size = plugin.getConfig().getInt("menu.size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) return 27;
        return size;
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "&cZprava '" + key + "' nenalezena.");
    }

    public List<String> getStaffNotify() {
        return plugin.getConfig().getStringList("staff-notify");
    }

    public Map<String, GuiItem> getGuiItems()   { return guiItems; }
    public Map<String, String>  getCategories() { return categories; }

    // ---- Command aliasy -----------------------------------------

    public List<String> getCommandAliases(String key) {
        return plugin.getConfig().getStringList("commands." + key + ".aliases");
    }
    public String getCommandDescription(String key) {
        return plugin.getConfig().getString("commands." + key + ".description", "");
    }
    public String getCommandUsage(String key) {
        return plugin.getConfig().getString("commands." + key + ".usage", "");
    }
    public String getCommandPermission(String key) {
        return plugin.getConfig().getString("commands." + key + ".permission", "");
    }

    // ---- Reports config -----------------------------------------

    /**
     * Vraci seznam radku zpravy odeslane reporterovi po vytvoreni reportu.
     * Nacteno z {@code reports/config.yml}.
     */
    public List<String> getReporterMessage() {
        if (reportsConfigFile == null) return List.of();
        return reportsConfigFile.getConfig().getStringList("reporter-message");
    }

    // ---- StaffChat config ---------------------------------------

    private FileConfiguration sc() { return staffchatFile != null ? staffchatFile.getConfig() : null; }

    public boolean isStaffChatEnabled() {
        FileConfiguration c = sc();
        return c == null || c.getBoolean("staffchat.enabled", true);
    }
    public String getStaffChatFormat() {
        FileConfiguration c = sc();
        return c != null ? c.getString("staffchat.format",
                "&8[&bStaffChat&8] &7%player%: &f%message%") : "&8[&bStaffChat&8] &7%player%: &f%message%";
    }
    public String getStaffChatUsage() {
        FileConfiguration c = sc();
        return c != null ? c.getString("staffchat.usage",
                "&cPouziti: /staffchat <zprava>") : "&cPouziti: /staffchat <zprava>";
    }

    // ---- Vanish config ------------------------------------------

    private FileConfiguration vc() { return vanishFile != null ? vanishFile.getConfig() : null; }

    public String getVanishEnabledMsg() {
        FileConfiguration c = vc();
        return c != null ? c.getString("vanish.enabled-message",  "&aVanish zapnut.")  : "&aVanish zapnut.";
    }
    public String getVanishDisabledMsg() {
        FileConfiguration c = vc();
        return c != null ? c.getString("vanish.disabled-message", "&cVanish vypnut.") : "&cVanish vypnut.";
    }

    // ---- Nick config --------------------------------------------

    private FileConfiguration nc() { return nickFile != null ? nickFile.getConfig() : null; }

    public String getNickChangedMsg() {
        FileConfiguration c = nc();
        return c != null ? c.getString("nick.changed", "&aTvuj nick byl zmenen na &f%nick%&a.") : "&aTvuj nick byl zmenen na &f%nick%&a.";
    }
    public String getNickResetMsg() {
        FileConfiguration c = nc();
        return c != null ? c.getString("nick.reset",   "&cTvuj nick byl resetnut.") : "&cTvuj nick byl resetnut.";
    }
    public String getNickInvalidMsg() {
        FileConfiguration c = nc();
        return c != null ? c.getString("nick.invalid",  "&cNeplatny nick.") : "&cNeplatny nick.";
    }
    public int getNickMaxLength() {
        FileConfiguration c = nc();
        return c != null ? c.getInt("nick.max-length", 16) : 16;
    }

    // ---- Webhook config -----------------------------------------

    private FileConfiguration wh() { return webhookFile != null ? webhookFile.getConfig() : null; }

    public boolean isWebhookEnabled()    { FileConfiguration c=wh(); return c!=null && c.getBoolean("discord-webhook.enabled",false); }
    public String  getWebhookUrl()       { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.url","") : ""; }
    public String  getWebhookUsername()  { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.username","Anduria Reports") : "Anduria Reports"; }
    public String  getWebhookAvatarUrl() { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.avatar-url","") : ""; }
    public String  getWebhookContent()   { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.content","") : ""; }
    public boolean isEmbedEnabled()      { FileConfiguration c=wh(); return c!=null && c.getBoolean("discord-webhook.embed.enabled",true); }
    public String  getEmbedTitle()       { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.embed.title","Novy report") : "Novy report"; }
    public String  getEmbedDescription() { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.embed.description","") : ""; }
    public int     getEmbedColor()       { FileConfiguration c=wh(); return c!=null ? c.getInt("discord-webhook.embed.color",16711680) : 16711680; }
    public String  getEmbedThumbnail()   { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.embed.thumbnail-url","") : ""; }
    public String  getEmbedFooterText()  { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.embed.footer.text","") : ""; }
    public String  getEmbedFooterIcon()  { FileConfiguration c=wh(); return c!=null ? c.getString("discord-webhook.embed.footer.icon-url","") : ""; }
    public boolean isEmbedTimestamp()    { FileConfiguration c=wh(); return c!=null && c.getBoolean("discord-webhook.embed.timestamp",true); }

    // ---- Database config ----------------------------------------

    private FileConfiguration db() { return databaseFile != null ? databaseFile.getConfig() : null; }

    public boolean isDatabaseEnabled() { FileConfiguration c=db(); return c!=null && c.getBoolean("database.enabled",false); }
    public String  getDbHost()         { FileConfiguration c=db(); return c!=null ? c.getString("database.host","localhost") : "localhost"; }
    public int     getDbPort()         { FileConfiguration c=db(); return c!=null ? c.getInt("database.port",3306) : 3306; }
    public String  getDbName()         { FileConfiguration c=db(); return c!=null ? c.getString("database.database","anduria_staff") : "anduria_staff"; }
    public String  getDbUser()         { FileConfiguration c=db(); return c!=null ? c.getString("database.username","root") : "root"; }
    public String  getDbPass()         { FileConfiguration c=db(); return c!=null ? c.getString("database.password","") : ""; }
    public String  getDbTable()        { FileConfiguration c=db(); return c!=null ? c.getString("database.table","reports") : "reports"; }
}