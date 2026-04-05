package me.anduria.staffsystem.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Obalova trida pro dynamicky registrovane prikazy pres Bukkit CommandMap.
 *
 * <p>Umoznuje registrovat libovolne jmeno a aliasy bez nutnosti upravovat plugin.yml.
 * Deleguje onCommand a onTabComplete na poskytnuty executor/completer.
 */
public class DynamicCommand extends Command {

    private final CommandExecutor executor;
    private final TabCompleter    completer;

    /**
     * @param name      primarni jmeno prikazu
     * @param desc      popis
     * @param usage     pouziti
     * @param aliases   seznam aliasu (muze byt prazdny)
     * @param executor  handler pro onCommand
     * @param completer handler pro tab complete (muze byt null)
     */
    public DynamicCommand(
            String name,
            String desc,
            String usage,
            List<String> aliases,
            CommandExecutor executor,
            TabCompleter completer
    ) {
        super(name, desc, usage, aliases);
        this.executor  = executor;
        this.completer = completer;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        // Vytvorime dummy Command pro kompatibilitu s CommandExecutor
        return executor.onCommand(sender, this, label, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (completer == null) return List.of();
        List<String> result = completer.onTabComplete(sender, this, alias, args);
        return result != null ? result : List.of();
    }
}