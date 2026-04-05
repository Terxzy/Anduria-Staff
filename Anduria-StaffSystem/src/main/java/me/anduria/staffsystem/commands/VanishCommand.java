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
 * Handler pro /vanish (a nakonfigurovane aliasy, napr. /v).
 *
 * <p>Pouziti:
 * <pre>
 *   /vanish   — prepne vanish ON / OFF
 * </pre>
 *
 * <p>Vanish skryje hrace vsem online hracum, kteri NEMAJI permission
 * {@code anduria.staffsystem.vanish.see}. Hrace s touto permission
 * vanishnute hrace vzdy vidi.
 *
 * <p>Stav je uchovavan v pameti (reset po restartu serveru).
 *
 * <p>Permission: {@code anduria.staffsystem.vanish} (default: op).
 * Zprava pri odmituti: klic {@code messages.no-vanish-permission} z config.yml.
 */
public class VanishCommand implements CommandExecutor, TabCompleter {

    private final AnduriaStaffSystem plugin;

    public VanishCommand(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Pouze hrac
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cTento prikaz muze pouzit pouze hrac."));
            return true;
        }

        // Permission check — pouziva vlastni zpravu no-vanish-permission
        if (!player.hasPermission("anduria.staffsystem.vanish")) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("no-vanish-permission")));
            return true;
        }

        // Prepni vanish stav
        boolean nowVanished = plugin.toggleVanish(player.getUniqueId());

        if (nowVanished) {
            // Skryj hrace vsem, kteri NEMAJI vanish.see
            for (Player observer : Bukkit.getOnlinePlayers()) {
                if (observer.equals(player)) continue;           // sebe neskryvame
                if (!observer.hasPermission("anduria.staffsystem.vanish.see")) {
                    observer.hidePlayer(plugin, player);
                }
                // Hrace s vanish.see nechame beze zmeny — oni ho stale vidi
            }
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getVanishEnabledMsg()));
        } else {
            // Zobraz hrace vsem
            for (Player observer : Bukkit.getOnlinePlayers()) {
                observer.showPlayer(plugin, player);
            }
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getVanishDisabledMsg()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String label, String[] args) {
        return List.of();
    }
}