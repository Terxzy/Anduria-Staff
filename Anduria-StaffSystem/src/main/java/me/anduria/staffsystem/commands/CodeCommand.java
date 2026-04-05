package me.anduria.staffsystem.commands;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.code.CodeData;
import me.anduria.staffsystem.code.CodeManager;
import me.anduria.staffsystem.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler pro /code a jeho aliasy (napr. /kod).
 * Prikazy jsou registrovany dynamicky z config.yml.
 *
 * <p>Pouziti:
 * <pre>
 *   /code &lt;kod&gt;   – pouzij kod
 * </pre>
 */
public class CodeCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;

    public CodeCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cTento prikaz muze pouzit pouze hrac."));
            return true;
        }

        // Zkontroluj zakladni permission pro pouzivani kodu
        String basePerm = plugin.getConfigManager().getCommandPermission("code");
        if (!basePerm.isBlank() && !player.hasPermission(basePerm)) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            String usage = plugin.getConfigManager().getCommandUsage("code");
            player.sendMessage(ColorUtils.colorize("&cPouziti: " + (usage.isBlank() ? "/code <kod>" : usage)));
            return true;
        }

        String codeName = args[0];
        CodeManager cm = plugin.getCodeManager();
        CodeManager.UseResult result = cm.useCode(player, codeName);
        CodeData code = cm.getCode(codeName);

        switch (result) {
            case SUCCESS -> {
                if (code != null) {
                    player.sendMessage(ColorUtils.colorize(code.getMsgSuccess()));
                } else {
                    player.sendMessage(ColorUtils.colorize("&aKod uspesne pouzit."));
                }
            }
            case NOT_FOUND -> {
                // Pouzij zpravu z prvniho dostupneho kodu nebo generickou
                player.sendMessage(ColorUtils.colorize(
                        "&cKod '" + codeName + "' neexistuje."));
            }
            case NO_PERMISSION -> {
                if (code != null) player.sendMessage(ColorUtils.colorize(code.getMsgNoPermission()));
            }
            case ALREADY_USED -> {
                if (code != null) player.sendMessage(ColorUtils.colorize(code.getMsgAlreadyUsed()));
            }
            case COOLDOWN -> {
                if (code != null) {
                    player.sendMessage(ColorUtils.colorize(cm.buildCooldownMsg(code, player)));
                }
            }
            case MAX_USES -> {
                if (code != null) player.sendMessage(ColorUtils.colorize(code.getMsgMaxUses()));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Neukazujeme jmena kodu v tab complete (bezpecnost)
        return List.of();
    }
}