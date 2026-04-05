package me.anduria.staffsystem.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vlastni InventoryHolder pro report GUI.
 *
 * <p>Slouzi jako spolehlivy zpusob identifikace naseho inventare:
 * misto porovnavani titulku (ktery se meni podle targetu) staci zkontrolovat
 * {@code holder instanceof ReportGUIHolder}.
 *
 * <p>Drzí take:
 * <ul>
 *   <li>UUID a jmeno ciloveho hrace (target)</li>
 *   <li>Mapu slot -&gt; {@link GuiItem} pro okamzite vyhledani bez iterace</li>
 * </ul>
 */
public class ReportGUIHolder implements InventoryHolder {

    /** UUID nahlasovaneho hrace. */
    private final UUID   targetUuid;

    /** Jmeno nahlasovaneho hrace. */
    private final String targetName;

    /**
     * Mapa slot -&gt; GuiItem.
     * Plnena v {@link ReportGUI#open} pred otevrenem inventare.
     * Umoznuje O(1) vyhledani itemu v GUIListeneru.
     */
    private final Map<Integer, GuiItem> slotItemMap = new HashMap<>();

    /** Reference na Bukkit inventar (nastavena po Bukkit.createInventory). */
    private Inventory inventory;

    public ReportGUIHolder(UUID targetUuid, String targetName) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    // ---- InventoryHolder API ------------------------------------

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Nastavuje referenci na inventar (volano hned po Bukkit.createInventory). */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    // ---- Slot mapa ---------------------------------------------

    /**
     * Registruje item na danem slotu.
     * Vola se pri budovani GUI pred otevrenem.
     */
    public void registerItem(int slot, GuiItem item) {
        slotItemMap.put(slot, item);
    }

    /**
     * Vraci GuiItem na danem slotu, nebo {@code null} pokud slot neni registrovan.
     */
    public GuiItem getItem(int slot) {
        return slotItemMap.get(slot);
    }

    /** Vrati nemodifikovatelnou kopii slot mapy (pro debugovani). */
    public Map<Integer, GuiItem> getSlotItemMap() {
        return Collections.unmodifiableMap(slotItemMap);
    }

    // ---- Target info -------------------------------------------

    public UUID   getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
}