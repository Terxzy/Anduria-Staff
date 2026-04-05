package me.anduria.staffsystem.commands;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handler pro /nick (a nakonfigurovane aliasy).
 *
 * <p>Pouziti:
 * <pre>
 *   /nick &lt;jmeno&gt;   — zmeni display name a player list name
 *   /nick reset     — vrati puvodni jmeno
 * </pre>
 *
 * <p>Nick podporuje & barvy a hex &#RRGGBB / #RRGGBB.
 * Maximalni delka viditelneho textu je nastavitelna v nick/config.yml.
 *
 * <p>Permission: {@code anduria.staffsystem.nick} (default: op).
 * Zprava pri odmituti: klic {@code messages.no-nick-permission} z config.yml.
 */
public class NickCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;

    public NickCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Pouze hrac
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cTento prikaz muze pouzit pouze hrac."));
            return true;
        }

        // Permission check — pouziva vlastni zpravu no-nick-permission
        if (!player.hasPermission("anduria.staffsystem.nick")) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-nick-permission")));
            return true;
        }

        // Bez argumentu — zobraz usage
        if (args.length == 0) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getCommandUsage("nick")));
            return true;
        }

        // /nick reset
        if (args[0].equalsIgnoreCase("reset")) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getNickResetMsg()));
            return true;
        }

        // /nick <jmeno>
        String rawNick     = String.join(" ", args);
        String coloredNick = ColorUtils.colorize(rawNick);
        String plainNick   = ColorUtils.toPlain(coloredNick);

        // Validace delky viditelneho textu
        int maxLen = plugin.getConfigManager().getNickMaxLength();
        if (plainNick.isEmpty() || plainNick.length() > maxLen) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getNickInvalidMsg()));
            return true;
        }

        // Nastav nick
        player.setDisplayName(coloredNick);
        player.setPlayerListName(coloredNick);

        String msg = plugin.getConfigManager().getNickChangedMsg()
                .replace("%nick%", coloredNick);
        player.sendMessage(ColorUtils.colorize(msg));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String label, String[] args) {
        if (args.length == 1) return List.of("reset");
        return List.of();
    }
}