package me.anduria.staffsystem.gui;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO reprezentujici jeden item v GUI menu.
 * Naciten dynamicky z config.yml sekce 'items'.
 *
 * <p>Nova pole v teto verzi:
 * <ul>
 *   <li>{@code basehead} - Base64 kodovana textura pro PLAYER_HEAD (DeluxeMenus styl)</li>
 * </ul>
 */
public class GuiItem {

    private String key;
    private Material material = Material.STONE;
    private int amount = 1;
    private int slot = 0;
    private String displayName = " ";
    private List<String> lore = new ArrayList<>();
    private boolean glow = false;
    private int customModelData = 0;
    private String permission = "";
    private String viewRequirement = "";
    private String reportCategory = "";
    private boolean close = false;
    private List<String> leftClickCommands = new ArrayList<>();
    private List<String> rightClickCommands = new ArrayList<>();

    /**
     * Base64 textura pro PLAYER_HEAD itemy (stejne jako DeluxeMenus basehead).
     * Pokud je nastaveny, plugin nastavi tuto texturu misto defaultni hlavy.
     * Format: base64-kodovany JSON {"textures":{"SKIN":{"url":"..."}}}.
     * Prazdny retezec = neni basehead.
     */
    private String basehead = "";

    // ---- Gettery a settery ----------------------------------------

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(1, Math.min(64, amount)); }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore != null ? lore : new ArrayList<>(); }

    public boolean isGlow() { return glow; }
    public void setGlow(boolean glow) { this.glow = glow; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission != null ? permission : ""; }

    public String getViewRequirement() { return viewRequirement; }
    public void setViewRequirement(String viewRequirement) { this.viewRequirement = viewRequirement != null ? viewRequirement : ""; }

    public String getReportCategory() { return reportCategory; }
    public void setReportCategory(String reportCategory) { this.reportCategory = reportCategory != null ? reportCategory : ""; }

    public boolean isClose() { return close; }
    public void setClose(boolean close) { this.close = close; }

    public List<String> getLeftClickCommands() { return leftClickCommands; }
    public void setLeftClickCommands(List<String> cmds) { this.leftClickCommands = cmds != null ? cmds : new ArrayList<>(); }

    public List<String> getRightClickCommands() { return rightClickCommands; }
    public void setRightClickCommands(List<String> cmds) { this.rightClickCommands = cmds != null ? cmds : new ArrayList<>(); }

    public String getBasehead() { return basehead; }
    public void setBasehead(String basehead) { this.basehead = basehead != null ? basehead.trim() : ""; }

    /** Vrati true pokud je item report item (ma nastavenou kategorii). */
    public boolean isReportItem() {
        return reportCategory != null && !reportCategory.isBlank();
    }

    /** Vrati true pokud ma item nastavenou basehead texturu. */
    public boolean hasBasehead() {
        return basehead != null && !basehead.isBlank();
    }
}