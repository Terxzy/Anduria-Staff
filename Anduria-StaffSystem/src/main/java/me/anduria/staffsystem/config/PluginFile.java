package me.anduria.staffsystem.config;

import me.anduria.staffsystem.AnduriaStaffSystem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Pomocna trida pro praci s extra YAML soubory pluginu.
 * Zajistuje:
 *   1. Zkopiruje vychozi resource soubor z JARu pokud neexistuje.
 *   2. Nacte YAML soubor ze slozky pluginu.
 *   3. Poskytne FileConfiguration pro cteni hodnot.
 */
public class PluginFile {

    private final AnduriaStaffSystem plugin;

    /** Cesta relativni k datafolderu pluginu, napr. "reports/categories.yml" */
    private final String relativePath;

    /** Absolutni File objekt */
    private final File file;

    /** Nactena konfigurace */
    private FileConfiguration config;

    /**
     * @param plugin       instance pluginu
     * @param relativePath cesta relativni k datafolderu (napr. "webhook/config.yml")
     */
    public PluginFile(AnduriaStaffSystem plugin, String relativePath) {
        this.plugin = plugin;
        this.relativePath = relativePath;
        this.file = new File(plugin.getDataFolder(), relativePath);
    }

    /**
     * Zajisti existenci souboru (zkopiruje default z JARu pokud chybi)
     * a nacte YAML konfiguraci.
     */
    public void loadOrCreate() {
        // 1. Vytvor nadrazene adresare
        file.getParentFile().mkdirs();

        // 2. Zkopiruj default ze zdroje v JARu pokud soubor neexistuje
        if (!file.exists()) {
            InputStream resource = plugin.getResource(relativePath.replace('\\', '/'));
            if (resource != null) {
                try {
                    plugin.saveResource(relativePath.replace('\\', '/'), false);
                } catch (IllegalArgumentException e) {
                    // Resource nenalezen v JARu - vytvor prazdny soubor
                    try { file.createNewFile(); } catch (IOException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Nelze vytvorit soubor: " + relativePath, ex);
                    }
                }
            } else {
                try { file.createNewFile(); } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Nelze vytvorit soubor: " + relativePath, ex);
                }
            }
        }

        // 3. Nacti YAML
        reload();
    }

    /**
     * Znovu nacte YAML soubor z disku.
     * Pouzivano pri /anduriastaff reload.
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);

        // Merge defaults z JARu (pridava chybejici klice bez prepisu)
        InputStream resource = plugin.getResource(relativePath.replace('\\', '/'));
        if (resource != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
    }

    /**
     * Ulozi aktualni konfiguraci zpet na disk.
     */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nelze ulozit soubor: " + relativePath, e);
        }
    }

    public FileConfiguration getConfig() { return config; }
    public File getFile()                { return file; }
    public String getRelativePath()      { return relativePath; }
}