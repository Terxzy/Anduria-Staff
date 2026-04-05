package me.anduria.staffsystem.commands;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Handler pro prikaz /anduriastaff.
 * Subcommand: reload – reloaduje config, GUI, webhook, databazi a staffchat.
 */
public class StaffSystemCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;

    public StaffSystemCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("anduria.staffsystem.reload")) {
            sender.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtils.colorize("&cPouziti: /anduriastaff reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("reload-success")));
        } else {
            sender.sendMessage(ColorUtils.colorize(
                    "&cNeznamy subcommand. Pouziti: /anduriastaff reload"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}