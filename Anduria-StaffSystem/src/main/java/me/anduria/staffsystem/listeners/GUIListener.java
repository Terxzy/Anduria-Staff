package me.anduria.staffsystem.listeners;

import me.anduria.staffsystem.AnduriaStaffSystem;
import me.anduria.staffsystem.gui.GuiItem;
import me.anduria.staffsystem.gui.ReportGUIHolder;
import me.anduria.staffsystem.report.ReportManager;
import me.anduria.staffsystem.utils.ColorUtils;
import me.anduria.staffsystem.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Spravuje vsechny interakce s report GUI inventarem.
 *
 * <h2>Identifikace GUI</h2>
 * <p>Inventar rozpoznavame pomoci {@link ReportGUIHolder}:
 * {@code event.getInventory().getHolder() instanceof ReportGUIHolder}.
 * Neni treba porovnavat titulek — holder je vzdy pritomen, nezavisi na locale
 * ani na placeholderech v titulku.
 *
 * <h2>Zamknutí inventare</h2>
 * <p>Vsechny typy kliknuti jsou okamzite cancelovany:
 * levy, pravy, shift, double-click, number-key (hotbar), drop, drag atd.
 * Hrac nemuze vzit, presunout ani vyhazovat zadny item.
 *
 * <h2>Click commandy</h2>
 * <p>Po cancelovani eventu zkontrolujeme, zda existuje GuiItem na danem slotu
 * (vyhledani pres slot mapu holderu — O(1)).
 * Pokud item ma commandy pro dany typ kliknuti, provedeme je v poradi.
 * Placeholdery %player%, %target%, %category%, %time% jsou nahrazeny pred provedenim.
 */
public class GUIListener implements Listener {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final AnduriaStaffSystem plugin;

    public GUIListener(AnduriaStaffSystem plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  InventoryClickEvent — HIGHEST priority
    // ================================================================

    /**
     * Zachyta veskerou interakci s report GUI.
     *
     * <p>Priority = HIGHEST zajistuje spusteni po anti-spam / economy pluginech
     * (LOWEST), ale pred defaultnim Bukkit zpracovanim.
     * ignoreCancelled = false: GUI cancelujeme vzdy, i kdyz jiny plugin event
     * uz zrusil — chceme byt jisti.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        // 1. Zkontroluj, zda je to nas inventar
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ReportGUIHolder guiHolder)) return;

        // 2. VZDY cancel — bez vyjimky, bez ohledu na typ kliknuti
        event.setCancelled(true);

        // 3. Pouze hrac muze klikat
        if (!(event.getWhoClicked() instanceof Player clicker)) return;

        // 4. Kliknuti musi byt ve vrchnim inventari (nase GUI).
        //    Shift-click z player inventare se taky canceluje (uz reseno vyse),
        //    ale commandy spoustime pouze pro kliknuti primo v GUI.
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        // 5. Slot a GuiItem
        int slot = event.getSlot();
        GuiItem guiItem = guiHolder.getItem(slot);

        // Klik na prazdny slot (AIR) nebo filler bez commandu — ignoruj
        if (guiItem == null) return;

        ClickType click = event.getClick();

        // 6. Urcime, ktere commandy spustit
        List<String> commands = resolveCommands(click, guiItem);
        if (commands.isEmpty()) return;

        // 7. Ziskame target ze session holderu
        String targetName = guiHolder.getTargetName();
        UUID   targetUuid = guiHolder.getTargetUuid();

        // 8. Vytvor report (pokud je to report item)
        if (guiItem.isReportItem()) {
            plugin.getReportManager().createReport(
                    clicker, targetUuid, targetName, guiItem.getReportCategory());
        }

        // 9. Proved commandy v poradi
        String time = LocalDateTime.now().format(TIME_FMT);
        executeCommands(clicker, targetName, guiItem.getReportCategory(), time, commands);

        // 10. Zavreni GUI (pokud je close: true nebo [close] v commandu)
        if (guiItem.isClose()) {
            scheduleClose(clicker);
            plugin.removeSession(clicker.getUniqueId());
        }
    }

    // ================================================================
    //  InventoryDragEvent — blokuje tazeni itemu pres GUI
    // ================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ReportGUIHolder)) return;
        // Drag je vzdy cancelovan — hrac nemuze presouva nic
        event.setCancelled(true);
    }

    // ================================================================
    //  Pomocne metody
    // ================================================================

    /**
     * Vrati seznam commandu pro dany typ kliknuti.
     *
     * <p>Levy klik (LEFT, SHIFT_LEFT, DOUBLE_CLICK, WINDOW_BORDER_LEFT)
     * &rarr; left_click_commands.
     *
     * <p>Pravy klik (RIGHT, SHIFT_RIGHT, WINDOW_BORDER_RIGHT)
     * &rarr; right_click_commands; pokud jsou prazdne, pouzije left_click_commands.
     *
     * <p>Ostatni typy (NUMBER_KEY, DROP, CONTROL_DROP atd.)
     * &rarr; left_click_commands jako fallback.
     */
    private List<String> resolveCommands(ClickType click, GuiItem guiItem) {
        boolean isRight = (click == ClickType.RIGHT
                        || click == ClickType.SHIFT_RIGHT
                        || click == ClickType.WINDOW_BORDER_RIGHT);

        if (isRight) {
            List<String> right = guiItem.getRightClickCommands();
            // Pokud right commandy existuji, pouzij je; jinak fallback na left
            if (!right.isEmpty()) return right;
        }

        // Left click, shift-left, number key, drop, atd. — vzdy left commandy
        return guiItem.getLeftClickCommands();
    }

    /**
     * Provede vsechny commandy ze seznamu v poradi.
     *
     * <p>Pred provedenim jsou nahrazeny placeholdery:
     * %player%, %target%, %category%, %time%.
     *
     * <p>Podporovane prefixy:
     * <ul>
     *   <li>{@code [console] &lt;cmd&gt;} — spusti jako konzole (async-safe, main thread)</li>
     *   <li>{@code [player] &lt;cmd&gt;}  — spusti jako hrac</li>
     *   <li>{@code [message] &lt;text&gt;} — posle barevnou zpravu hracovi</li>
     *   <li>{@code [close]}              — zavre inventar na pristi tick</li>
     * </ul>
     */
    private void executeCommands(Player clicker,
                                 String targetName,
                                 String category,
                                 String time,
                                 List<String> commands) {
        for (String rawCmd : commands) {
            if (rawCmd == null || rawCmd.isBlank()) continue;

            // Nahrad placeholdery
            String cmd = rawCmd
                    .replace("%player%",   clicker.getName())
                    .replace("%target%",   targetName != null ? targetName : "unknown")
                    .replace("%category%", category   != null ? category   : "")
                    .replace("%time%",     time);

            // PlaceholderAPI (pokud je dostupne)
            cmd = PlaceholderUtils.applyPapi(cmd, clicker);

            // Dispatch podle prefiksu
            if (cmd.startsWith("[console] ")) {
                final String consoleCmd = cmd.substring("[console] ".length()).trim();
                // Bukkit API musi bezet na main threadu
                Bukkit.getScheduler().runTask(plugin, (Runnable)
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd));

            } else if (cmd.startsWith("[player] ")) {
                final String playerCmd = cmd.substring("[player] ".length()).trim();
                Bukkit.getScheduler().runTask(plugin, (Runnable)
                        () -> clicker.performCommand(playerCmd));

            } else if (cmd.startsWith("[message] ")) {
                String message = cmd.substring("[message] ".length());
                clicker.sendMessage(ColorUtils.colorize(message));

            } else if (cmd.trim().equalsIgnoreCase("[close]")) {
                scheduleClose(clicker);

            }
            // Neznamy prefik — tichy skip (bezpecne)
        }
    }

    /**
     * Naplanova zavreni inventare na pristi tick.
     * Musi byt asynchronni vuci eventu, jinak Bukkit hazi vyjimku.
     */
    private void scheduleClose(Player player) {
        Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
    }
}