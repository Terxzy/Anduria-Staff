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
 * Handler pro /create-code a jeho aliasy.
 * Vytvori novy code soubor s default templatem v codes/ slozce.
 *
 * <p>Permission: {@code anduria.staffsystem.code.create} (nastavitelna v config.yml).
 */
public class CreateCodeCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;

    public CreateCodeCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission ze config.yml
        String perm = plugin.getConfigManager().getCommandPermission("create-code");
        if (perm.isBlank()) perm = "anduria.staffsystem.code.create";

        if (!sender.hasPermission(perm)) {
            sender.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            String usage = plugin.getConfigManager().getCommandUsage("create-code");
            sender.sendMessage(ColorUtils.colorize("&cPouziti: " + (usage.isBlank() ? "/create-code <kod>" : usage)));
            return true;
        }

        String codeName = args[0].toLowerCase();

        // Validace jmena (pouze pismena, cislice, pomlcky, podtrzitka)
        if (!codeName.matches("[a-z0-9_\\-]+")) {
            sender.sendMessage(ColorUtils.colorize(
                    "&cNeplatne jmeno kodu. Pouzivej pouze: a-z, 0-9, -, _"));
            return true;
        }

        boolean created = plugin.getCodeManager().createCode(codeName);
        if (created) {
            sender.sendMessage(ColorUtils.colorize(
                    "&#00FF88Kod &e" + codeName + "&#00FF88 byl vytvoren."));
            sender.sendMessage(ColorUtils.colorize(
                    "&7Soubor: &fcodes/" + codeName + ".yml"));
        } else {
            sender.sendMessage(ColorUtils.colorize(
                    "&cKod &e" + codeName + "&c jiz existuje."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("muj-kod", "vip", "event");
        }
        return List.of();
    }
}