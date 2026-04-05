package me.anduria.staffsystem.storage;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.report.Report;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * YAML implementace StorageProvideru.
 * Uklada reporty do {@code reports/reports.yml}.
 */
public class YamlStorageProvider implements StorageProvider {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final AnduriaStaffSystem plugin;
    private File reportsFile;
    private FileConfiguration reportsConfig;
    private int nextId = 1;

    public YamlStorageProvider(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // Slozka reports/
        File reportsDir = new File(plugin.getDataFolder(), "reports");
        reportsDir.mkdirs();

        reportsFile = new File(reportsDir, "reports.yml");
        if (!reportsFile.exists()) {
            try {
                reportsFile.createNewFile();
                plugin.getLogger().info("[YamlStorage] Vytvoren soubor reports/reports.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("[YamlStorage] Nelze vytvorit reports.yml: " + e.getMessage());
            }
        }

        reportsConfig = YamlConfiguration.loadConfiguration(reportsFile);

        // Nastav nextId podle existujicich zaznamu
        if (reportsConfig.contains("reports")) {
            var sec = reportsConfig.getConfigurationSection("reports");
            if (sec != null) nextId = sec.getKeys(false).size() + 1;
        }
        plugin.getLogger().info("[YamlStorage] Nacteno reportu: " + (nextId - 1));
    }

    @Override
    public synchronized void saveReport(Report report) {
        String path = "reports." + nextId;
        reportsConfig.set(path + ".reporter_uuid", report.getReporterUuid().toString());
        reportsConfig.set(path + ".reporter_name", report.getReporterName());
        reportsConfig.set(path + ".target_uuid",   report.getTargetUuid().toString());
        reportsConfig.set(path + ".target_name",   report.getTargetName());
        reportsConfig.set(path + ".category",      report.getCategory());
        reportsConfig.set(path + ".created_at",    report.getCreatedAt().format(FORMATTER));

        try {
            reportsConfig.save(reportsFile);
            plugin.getLogger().info("[YamlStorage] Report #" + nextId + " ulozen.");
            nextId++;
        } catch (IOException e) {
            plugin.getLogger().severe("[YamlStorage] Chyba pri ukladani: " + e.getMessage());
        }
    }
}