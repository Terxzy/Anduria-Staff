package me.anduria.staffsystem.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilitni trida pro nahrazovani placeholderu v retezci.
 *
 * <p>Podporovane placeholdery:
 * <ul>
 *   <li>{@code %player%}   - jmeno hrace, ktery provadi akci (reporter)</li>
 *   <li>{@code %target%}   - jmeno nahlasovaneho hrace</li>
 *   <li>{@code %category%} - kategorie reportu</li>
 *   <li>{@code %time%}     - aktualni cas ve formatu dd.MM.yyyy HH:mm:ss</li>
 * </ul>
 *
 * <p>Pokud je nainstalovan PlaceholderAPI, prochazi pres nej vsechny placeholdery.
 */
public final class PlaceholderUtils {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // Utilitni trida - privatni konstruktor
    private PlaceholderUtils() {}

    /**
     * Nahradi vsechny standardni placeholdery pluginu.
     * Targetem muze byt null (napr. pro offline hrace), pak se pouzije targetName.
     *
     * @param s          vstupni retezec
     * @param reporter   hrac, ktery provadi report
     * @param targetName jmeno nahlasovaneho hrace
     * @param category   kategorie reportu
     * @return retezec s nahrazenymi placeholdery
     */
    public static String replace(String s, Player reporter, String targetName, String category) {
        if (s == null) return "";

        String time = LocalDateTime.now().format(TIME_FMT);

        String result = s
                .replace("%player%", reporter.getName())
                .replace("%target%", targetName != null ? targetName : "unknown")
                .replace("%category%", category != null ? category : "")
                .replace("%time%", time);

        return applyPapi(result, reporter);
    }

    /**
     * Overload pro zpetnou kompatibilitu: prijima Player objekt jako target.
     */
    public static String replace(String s, Player reporter, Player target, String category) {
        return replace(s, reporter, target != null ? target.getName() : "unknown", category);
    }

    /**
     * Pokusi se aplikovat PlaceholderAPI placeholdery.
     * Pokud PAPI neni nainstalovan nebo dojde k chybe, vraci vstupni retezec.
     *
     * @param s      retezec s PAPI placeholdery
     * @param player hrac, pro ktereho se placeholdery nahrazuji
     * @return retezec s nahrazenymi PAPI placeholdery
     */
    public static String applyPapi(String s, Player player) {
        if (s == null) return "";
        if (player == null) return s;

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, s);
            } catch (Exception ignored) {
                // PAPI selhalo - pokracuj bez nej
            }
        }
        return s;
    }
}