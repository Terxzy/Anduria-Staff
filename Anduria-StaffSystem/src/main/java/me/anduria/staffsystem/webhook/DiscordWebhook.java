package me.anduria.staffsystem.webhook;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.config.ConfigManager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Odesila Discord webhook zpravu po kazdem reportu.
 *
 * <p>Vsechny hodnoty jsou nacitany z {@code webhook/config.yml} pres ConfigManager.
 * Pokud webhook selze nebo neni nakonfigurovan, chyba je zalogovat – plugin nespadne.
 */
public class DiscordWebhook {

    private final AnduriaStaffSystem plugin;

    public DiscordWebhook(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Odesle webhook. Vola se asynchronne z ReportManageru.
     *
     * @param reporter jmeno reportera
     * @param target   jmeno targetu
     * @param category kategorie reportu
     * @param time     formatovany cas
     */
    public void send(String reporter, String target, String category, String time) {
        ConfigManager cfg = plugin.getConfigManager();

        if (!cfg.isWebhookEnabled()) return;

        String url = cfg.getWebhookUrl();
        if (url == null || url.isBlank() || url.contains("YOUR_WEBHOOK_URL")) {
            plugin.getLogger().warning("[Webhook] URL neni nastavena v webhook/config.yml!");
            return;
        }

        try {
            String payload = buildPayload(cfg, reporter, target, category, time);
            sendPost(url, payload);
        } catch (Exception e) {
            plugin.getLogger().warning("[Webhook] Chyba pri odesilani: " + e.getMessage());
        }
    }

    // ---- JSON builder -------------------------------------------

    private String buildPayload(ConfigManager cfg,
                                String reporter, String target,
                                String category, String time) {
        StringBuilder json = new StringBuilder("{");

        // Username + avatar
        json.append("\"username\":\"").append(esc(cfg.getWebhookUsername())).append("\",");
        String avatar = cfg.getWebhookAvatarUrl();
        if (!avatar.isBlank()) {
            json.append("\"avatar_url\":\"").append(esc(avatar)).append("\",");
        }

        // Content
        String content = ph(cfg.getWebhookContent(), reporter, target, category, time);
        json.append("\"content\":\"").append(esc(content)).append("\"");

        // Embed
        if (cfg.isEmbedEnabled()) {
            json.append(",\"embeds\":[{");

            json.append("\"title\":\"").append(esc(ph(cfg.getEmbedTitle(), reporter, target, category, time))).append("\",");

            String desc = ph(cfg.getEmbedDescription(), reporter, target, category, time);
            json.append("\"description\":\"").append(esc(desc)).append("\",");

            json.append("\"color\":").append(cfg.getEmbedColor()).append(",");

            String thumb = ph(cfg.getEmbedThumbnail(), reporter, target, category, time);
            if (!thumb.isBlank()) {
                json.append("\"thumbnail\":{\"url\":\"").append(esc(thumb)).append("\"},");
            }

            String footerText = ph(cfg.getEmbedFooterText(), reporter, target, category, time);
            String footerIcon = cfg.getEmbedFooterIcon();
            if (!footerText.isBlank()) {
                json.append("\"footer\":{\"text\":\"").append(esc(footerText)).append("\"");
                if (!footerIcon.isBlank()) {
                    json.append(",\"icon_url\":\"").append(esc(footerIcon)).append("\"");
                }
                json.append("},");
            }

            if (cfg.isEmbedTimestamp()) {
                json.append("\"timestamp\":\"").append(Instant.now()).append("\"");
            } else {
                // Odeber trailing carku
                if (json.charAt(json.length() - 1) == ',') {
                    json.deleteCharAt(json.length() - 1);
                }
            }

            json.append("}]");
        }

        json.append("}");
        return json.toString();
    }

    private void sendPost(String webhookUrl, String payload) throws Exception {
        URL url = URI.create(webhookUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "Anduria-StaffSystem/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            plugin.getLogger().info("[Webhook] Report odeslan na Discord (HTTP " + code + ").");
        } else {
            plugin.getLogger().warning("[Webhook] Discord vratil HTTP " + code);
        }
        conn.disconnect();
    }

    // ---- Helpers ------------------------------------------------

    /** Nahradi placeholdery. */
    private String ph(String s, String reporter, String target, String category, String time) {
        if (s == null) return "";
        return s.replace("%player%",   reporter)
                .replace("%target%",   target)
                .replace("%category%", category)
                .replace("%time%",     time);
    }

    /** Escapuje specialni JSON znaky. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}