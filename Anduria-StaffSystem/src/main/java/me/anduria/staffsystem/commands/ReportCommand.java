package me.anduria.staffsystem.commands;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.gui.ReportGUI;
import me.anduria.staffsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handler pro prikaz /report <hrac>.
 *
 * <p>Pravidla validace:
 * <ul>
 *   <li>Hrac musi byt online, POKUD reporter NEMA override permission.</li>
 *   <li>Reporter nemuze reportnout sam sebe, POKUD nema override.</li>
 *   <li>S override ({@code anduria.staffsystem.override} nebo OP) je mozne:
 *       <ul>
 *           <li>Nahlasit offline hrace</li>
 *           <li>Nahlasit hrace, ktery nikdy nebyl online</li>
 *           <li>Nahlasit sebe sama (pro testovani)</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public class ReportCommand implements CommandExecutor, TabCompleter {

    private static final String OVERRIDE_PERM = "anduria.staffsystem.override";

    private final AnduriaStaffSystem plugin;
    private final ReportGUI reportGUI;

    public ReportCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
        this.reportGUI = new ReportGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cTento prikaz muze pouzit pouze hrac."));
            return true;
        }

        if (!player.hasPermission("anduria.staffsystem.report")) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.colorize("&cPouziti: /report <hrac>"));
            return true;
        }

        String targetName = args[0];
        boolean hasOverride = player.isOp() || player.hasPermission(OVERRIDE_PERM);

        // --- Kontrola self-report ---
        if (player.getName().equalsIgnoreCase(targetName) && !hasOverride) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("cannot-report-self")));
            return true;
        }

        // --- Hledame hrace ---
        Player onlineTarget = Bukkit.getPlayerExact(targetName);

        if (onlineTarget != null) {
            // Target je online - bezny prutok
            // Self-report override: GUI se otevre, target == reporter
            reportGUI.open(player, onlineTarget.getName());
            // Ulozime session pro GUIListener
            plugin.putSession(player.getUniqueId(),
                    new AnduriaStaffSystem.TargetSession(onlineTarget.getUniqueId(), onlineTarget.getName()));
        } else if (hasOverride) {
            // Target neni online, ale hrac ma override -> hledame offline hrace
            UUID targetUuid = resolveOfflineUuid(targetName);
            plugin.putSession(player.getUniqueId(),
                    new AnduriaStaffSystem.TargetSession(targetUuid, targetName));
            reportGUI.open(player, targetName);
        } else {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("player-not-found")));
        }

        return true;
    }

    /**
     * Zjisti UUID offline hrace.
     * Poradi:
     * 1. Bukkit cachovane offline hrace
     * 2. Offline UUID vygenerovane ze jmena (stejny algoritmus jako Mojang offline mode)
     */
    @SuppressWarnings("deprecation")
    private UUID resolveOfflineUuid(String name) {
        // Zkus najit v Bukkit offline cache
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(op.getName())) {
                return op.getUniqueId();
            }
        }
        // Fallback: vygeneruj deterministicke UUID ze jmena (offline mode)
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(partial)) {
                    suggestions.add(online.getName());
                }
            }
        }
        return suggestions;
    }
}