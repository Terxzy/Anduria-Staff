package me.anduria.staffsystem.report;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.storage.StorageProvider;
import me.anduria.staffsystem.utils.ColorUtils;
import me.anduria.staffsystem.webhook.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrator report systemu.
 *
 * <p>Po kazdem reportu:
 * <ol>
 *   <li>Ulozi report do storage (async)</li>
 *   <li>Odesle Discord webhook (async)</li>
 *   <li>Posle zpravy staffum s permission {@code anduria.staffsystem.notify}</li>
 *   <li>Posle zpravu reporterovi (z {@code reports/config.yml} sekce {@code reporter-message})</li>
 * </ol>
 */
public class ReportManager {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final AnduriaStaffSystem plugin;
    private final StorageProvider    storage;
    private final DiscordWebhook     discordWebhook;

    public ReportManager(AnduriaStaffSystem plugin, StorageProvider storage) {
        this.plugin        = plugin;
        this.storage       = storage;
        this.discordWebhook = new DiscordWebhook(plugin);
    }

    // ---- Public API ---------------------------------------------

    /** Vytvor report pro ONLINE target. */
    public void createReport(Player reporter, Player target, String category) {
        createReport(reporter, target.getUniqueId(), target.getName(), category);
    }

    /**
     * Vytvor report (online i offline target).
     *
     * @param reporter   hrac, ktery podava report
     * @param targetUuid UUID nahlasovaneho hrace
     * @param targetName jmeno nahlasovaneho hrace
     * @param category   kategorie reportu
     */
    public void createReport(Player reporter, UUID targetUuid, String targetName, String category) {
        LocalDateTime now  = LocalDateTime.now();
        String        time = now.format(TIME_FMT);

        Report report = new Report(
                reporter.getUniqueId(), reporter.getName(),
                targetUuid, targetName,
                category, now);

        // 1. Uloz + Discord (async — nesmi blokovat main thread)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { storage.saveReport(report); }
            catch (Exception e) {
                plugin.getLogger().severe("[ReportManager] Chyba pri ukladani: " + e.getMessage());
            }
            discordWebhook.send(reporter.getName(), targetName, category, time);
        });

        // 2. Staff notifikace (main thread)
        notifyStaff(reporter.getName(), targetName, category, time);

        // 3. Zprava reporterovi (main thread)
        sendReporterMessage(reporter, targetName, category, time);
    }

    // ---- Privatni metody ----------------------------------------

    /**
     * Posle staff notifikaci vsem online hracum
     * s permission {@code anduria.staffsystem.notify}.
     */
    private void notifyStaff(String reporterName, String targetName, String category, String time) {
        List<String> lines = plugin.getConfigManager().getStaffNotify();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("anduria.staffsystem.notify")) continue;
            for (String line : lines) {
                p.sendMessage(ColorUtils.colorize(applyPlaceholders(line, reporterName, targetName, category, time)));
            }
        }
    }

    /**
     * Posle personalizovanou zpravu reporterovi.
     * Text je nacten z {@code reports/config.yml -> reporter-message}.
     */
    private void sendReporterMessage(Player reporter, String targetName, String category, String time) {
        List<String> lines = plugin.getConfigManager().getReporterMessage();
        if (lines.isEmpty()) return;
        for (String line : lines) {
            reporter.sendMessage(ColorUtils.colorize(
                    applyPlaceholders(line, reporter.getName(), targetName, category, time)));
        }
    }

    /** Nahradi vsechny standardni placeholdery. */
    private String applyPlaceholders(String s, String player, String target, String category, String time) {
        return s.replace("%player%",   player)
                .replace("%target%",   target)
                .replace("%category%", category)
                .replace("%time%",     time);
    }
}