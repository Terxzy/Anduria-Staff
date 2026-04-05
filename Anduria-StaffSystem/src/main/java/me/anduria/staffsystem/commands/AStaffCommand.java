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
 * Handler pro admin prikaz /astaff <hrac>.
 *
 * <p>Otevre stejne GUI jako {@code /report}, ale:
 * <ul>
 *   <li>Vyzaduje permission {@code anduria.staffsystem.admin} nebo OP.</li>
 *   <li>Automaticky ma override – muze nahlasit offline i sebe sama.</li>
 *   <li>Prikaz {@code /report} zustava nezmeneny.</li>
 * </ul>
 */
public class AStaffCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;
    private final ReportGUI reportGUI;

    public AStaffCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
        this.reportGUI = new ReportGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cTento prikaz muze pouzit pouze hrac."));
            return true;
        }

        // Vyzaduje admin permission nebo OP
        if (!player.isOp() && !player.hasPermission("anduria.staffsystem.admin")) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.colorize("&cPouziti: /astaff <hrac>"));
            return true;
        }

        String targetName = args[0];

        // /astaff vzdy ma override (admin prikaz)
        Player onlineTarget = Bukkit.getPlayerExact(targetName);

        if (onlineTarget != null) {
            plugin.putSession(player.getUniqueId(),
                    new AnduriaStaffSystem.TargetSession(onlineTarget.getUniqueId(), onlineTarget.getName()));
            reportGUI.open(player, onlineTarget.getName());
        } else {
            // Offline player support (admin override)
            UUID targetUuid = resolveOfflineUuid(targetName);
            plugin.putSession(player.getUniqueId(),
                    new AnduriaStaffSystem.TargetSession(targetUuid, targetName));
            reportGUI.open(player, targetName);
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private UUID resolveOfflineUuid(String name) {
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(op.getName())) {
                return op.getUniqueId();
            }
        }
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