package me.anduria.staffsystem.commands;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handler pro /staffchat (a nakonfigurovane aliasy, napr. /sc).
 *
 * <p>Pouziti:
 * <pre>
 *   /staffchat &lt;zprava&gt;   — odesle zpravu do staffchatu
 *   /sc        &lt;zprava&gt;   — alias
 * </pre>
 *
 * <p>Bez argumentu zobrazi usage zpravu z configu.
 * Toggle mod byl odebran — staffchat slouzi pouze jako prikaz pro odeslani zpravy.
 *
 * <p>Zpravu vidi vsichni hrace s permission {@code anduria.staffsystem.staffchat}.
 */
public class StaffChatCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;

    public StaffChatCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission
        if (!sender.hasPermission("anduria.staffsystem.staffchat")) {
            sender.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        // Staffchat musi byt povolen
        if (!plugin.getConfigManager().isStaffChatEnabled()) {
            sender.sendMessage(ColorUtils.colorize("&cStaffChat je vypnuty."));
            return true;
        }

        // Pokud neni zadna zprava, zobraz usage
        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getStaffChatUsage()));
            return true;
        }

        // Spoj argumenty do jedne zpravy
        String message = String.join(" ", args);

        // Urcime jmeno odesilatele
        String playerName = (sender instanceof Player p) ? p.getName() : "Console";

        // Odesli zpravu vsem s permission staffchat
        broadcastStaffChat(playerName, message);
        return true;
    }

    /**
     * Odesle formatovanou staffchat zpravu vsem opravnenym hracum.
     * Volanom take z jinych casti pluginu pokud je potreba.
     *
     * @param senderName jmeno odesilatele
     * @param message    obsah zpravy
     */
    public void broadcastStaffChat(String senderName, String message) {
        String format = plugin.getConfigManager().getStaffChatFormat();
        String formatted = ColorUtils.colorize(
                format.replace("%player%",  senderName)
                      .replace("%message%", message));

        // Odesli vsem opravnenym online hracum
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("anduria.staffsystem.staffchat")) {
                p.sendMessage(formatted);
            }
        }

        // Log do konzole
        plugin.getLogger().info("[StaffChat] " + senderName + ": " + message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String label, String[] args) {
        // Staffchat je volny text — zadny tab complete
        return List.of();
    }
}