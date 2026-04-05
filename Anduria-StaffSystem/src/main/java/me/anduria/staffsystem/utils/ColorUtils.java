package me.anduria.staffsystem.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitni trida pro praci s Minecraft barvami.
 *
 * <p>Podporovane formaty:
 * <ul>
 *   <li>{@code &c}, {@code &l} … – klasicke legacy Minecraft & kody</li>
 *   <li>{@code &#RRGGBB}         – hex barva, alternativni format</li>
 *   <li>{@code #RRGGBB}          – kratky hex format (bez &)</li>
 * </ul>
 *
 * <p>Parser prevede vsechny formaty na Bukkit/BungeeCord ChatColor sekvence
 * kompatibilni s Paper 1.21.x.
 */
public final class ColorUtils {

    /**
     * Regex pro &#RRGGBB format (nejcasteji pouzivany v plugin configu).
     * Napr: {@code &#FF0000Cervena}
     */
    private static final Pattern HEX_PATTERN_AMP =
            Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Regex pro #RRGGBB format bez & prefixu.
     * Napr: {@code #FF0000Cervena}
     */
    private static final Pattern HEX_PATTERN_PLAIN =
            Pattern.compile("#([A-Fa-f0-9]{6})");

    // Utilitni trida – privatni konstruktor
    private ColorUtils() {}

    /**
     * Zpracuje retezec a prevede vsechny podporovane color kody.
     * Poradi zpracovani: &#HEX → #HEX → &legacy
     *
     * @param s vstupni retezec (muze byt null)
     * @return obarveny retezec pripraveny k odeslani hracum
     */
    public static String colorize(String s) {
        if (s == null) return "";

        // 1. Nahrad &#RRGGBB format
        s = applyHexPattern(s, HEX_PATTERN_AMP);

        // 2. Nahrad #RRGGBB format (ktery jiz neni soucast hex patternu)
        //    Pouze pokud neni jiz zpracovan v predchozim kroku
        s = applyHexPattern(s, HEX_PATTERN_PLAIN);

        // 3. Nahrad klasicke & kody
        s = ChatColor.translateAlternateColorCodes('&', s);

        return s;
    }

    /**
     * Aplikuje regex pattern pro hex barvy a prevede je na BungeeCord format.
     * BungeeCord hex format: §x§R§R§G§G§B§B (kazda hex cislice jako samostatny §-kod).
     *
     * @param s       vstupni retezec
     * @param pattern regex pattern (musi mit group(1) = 6-misty hex)
     * @return retezec s nahrazenymi hex barvami
     */
    private static String applyHexPattern(String s, Pattern pattern) {
        Matcher matcher = pattern.matcher(s);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1).toUpperCase();
            // §x§R§R§G§G§B§B – format ktery Bukkit/BungeeCord rozumi
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(result, replacement.toString());
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Odstrani vsechny ChatColor/§ kody z retezce.
     * Pouzivano pro porovnavani titulku GUI.
     *
     * @param s retezec s barvami
     * @return ciry text bez barev
     */
    public static String stripColor(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s);
    }

    /**
     * Zkratka: colorize + stripColor.
     * Pouzivano pro plain-text porovnavani.
     *
     * @param s vstupni retezec s & nebo hex kody
     * @return plain text bez barevnych kodu
     */
    public static String toPlain(String s) {
        return stripColor(colorize(s));
    }
}