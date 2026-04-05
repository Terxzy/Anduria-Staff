package me.anduria.staffsystem.listeners;

import me.anduria.staffsystem.AnduriaStaffSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener pro spravny vanish efekt pri joinovani / odchodu hracu.
 *
 * <p>Pri joinu noveho hrace: skryje mu vanishnute hrace (pokud nema vanish.see).
 * Pri joinu vanishnute hrace: skryje ho vsem bez vanish.see.
 * Pri odchodu: odebere ze stavu vanish (volitelne — je to v pameti, restart cisti).
 */
public class VanishListener implements Listener {

    private final AnduriaStaffSystem plugin;

    public VanishListener(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Kdyz novy hrac joinne:
     * 1. Pokud on sam nema vanish.see, skryjeme mu vsechny vanishnute hrace.
     * 2. Pokud on sam je vanished (navrat po reconnectu — neni v pameti po restartu),
     *    nic dalsiho neresime (vanish se resetuje).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();

        // Pro vsechny online vanishnute hrace:
        for (Player vanished : plugin.getVanishedPlayers()) {
            if (vanished.equals(joining)) continue;

            // Novy hrac bez vanish.see vanishnute hrace nevidi
            if (!joining.hasPermission("anduria.staffsystem.vanish.see")) {
                joining.hidePlayer(plugin, vanished);
            }
        }
    }

    /**
     * Kdyz hrac odejde a byl vanished:
     * Odebere ho z evidencni mnoziny (uz neni online).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Pokud odejde vanished hrac, odeber ze stavu
        // (pri priste se servery restartuje, state se ztraci stejne)
        plugin.removeVanish(event.getPlayer().getUniqueId());
    }
}