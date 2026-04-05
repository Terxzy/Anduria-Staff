package me.anduria.staffsystem.database;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.config.ConfigManager;
import me.anduria.staffsystem.report.Report;
import me.anduria.staffsystem.storage.StorageProvider;

import java.sql.*;
import java.time.format.DateTimeFormatter;

/**
 * MySQL implementace StorageProvideru.
 * Pripojuje se k MySQL databazi a uklada reporty do SQL tabulky.
 *
 * <p>Pokud pripojeni selze, plugin automaticky fallbackne na YamlStorageProvider.
 * (Fallback je reseni v AnduriaStaffSystem#initStorage)
 */
public class MySQLStorageProvider implements StorageProvider {

    private static final DateTimeFormatter SQL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnduriaStaffSystem plugin;
    private Connection connection;
    private String tableName;

    public MySQLStorageProvider(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        ConfigManager cfg = plugin.getConfigManager();
        this.tableName = cfg.getDbTable();

        String url = "jdbc:mysql://" + cfg.getDbHost() + ":" + cfg.getDbPort()
                + "/" + cfg.getDbName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";

        try {
            // Explicitni nacteni driveru (potrebne po shadingu)
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, cfg.getDbUser(), cfg.getDbPass());
            createTableIfNotExists();
            plugin.getLogger().info("[MySQL] Uspesne pripojeno k databazi '" + cfg.getDbName() + "'.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver nebyl nalezen: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Chyba pri pripojovani k MySQL: " + e.getMessage(), e);
        }
    }

    /**
     * Vytvori tabulku pro reporty, pokud jeste neexistuje.
     */
    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "  `id`            INT AUTO_INCREMENT PRIMARY KEY," +
                "  `reporter_uuid` VARCHAR(36) NOT NULL," +
                "  `reporter_name` VARCHAR(16)  NOT NULL," +
                "  `target_uuid`   VARCHAR(36) NOT NULL," +
                "  `target_name`   VARCHAR(16)  NOT NULL," +
                "  `category`      VARCHAR(64)  NOT NULL," +
                "  `created_at`    DATETIME     NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
        plugin.getLogger().info("[MySQL] Tabulka '" + tableName + "' je pripravena.");
    }

    @Override
    public synchronized void saveReport(Report report) {
        // Obnov pripojeni pokud bylo preruseno
        ensureConnection();

        String sql = "INSERT INTO `" + tableName + "` "
                + "(reporter_uuid, reporter_name, target_uuid, target_name, category, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, report.getReporterUuid().toString());
            ps.setString(2, report.getReporterName());
            ps.setString(3, report.getTargetUuid().toString());
            ps.setString(4, report.getTargetName());
            ps.setString(5, report.getCategory());
            ps.setString(6, report.getCreatedAt().format(SQL_FORMATTER));
            ps.executeUpdate();
            plugin.getLogger().info("[MySQL] Report ulozen do databaze.");
        } catch (SQLException e) {
            plugin.getLogger().severe("[MySQL] Chyba pri ukladani reportu: " + e.getMessage());
        }
    }

    /**
     * Pokusi se znovu pripojit, pokud bylo pripojeni ztraceno.
     */
    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                plugin.getLogger().warning("[MySQL] Pripojeni bylo ztraceno, pokus o reconnect...");
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MySQL] Chyba pri overeni pripojeni: " + e.getMessage());
        }
    }

    /**
     * Uzavre databazove pripojeni.
     * Volano pri onDisable pluginu.
     */
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("[MySQL] Pripojeni bylo uzavreno.");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[MySQL] Chyba pri uzavirani pripojeni: " + e.getMessage());
            }
        }
    }
}