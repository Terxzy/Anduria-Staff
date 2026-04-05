package me.anduria.staffsystem.code;

import java.util.List;

/**
 * Immutable POJO reprezentujici jeden "code" (kod pro odmen).
 * Naciten z codes/&lt;name&gt;.yml.
 */
public class CodeData {

    private final String name;
    private final String permission;
    private final boolean oneTime;
    private final long cooldownSeconds;
    private final int maxUses;
    private final List<String> commands;

    // Zpravy
    private final String msgSuccess;
    private final String msgNoPermission;
    private final String msgAlreadyUsed;
    private final String msgCooldown;
    private final String msgMaxUses;
    private final String msgNotFound;

    public CodeData(
            String name,
            String permission,
            boolean oneTime,
            long cooldownSeconds,
            int maxUses,
            List<String> commands,
            String msgSuccess,
            String msgNoPermission,
            String msgAlreadyUsed,
            String msgCooldown,
            String msgMaxUses,
            String msgNotFound
    ) {
        this.name            = name;
        this.permission      = permission;
        this.oneTime         = oneTime;
        this.cooldownSeconds = cooldownSeconds;
        this.maxUses         = maxUses;
        this.commands        = commands;
        this.msgSuccess      = msgSuccess;
        this.msgNoPermission = msgNoPermission;
        this.msgAlreadyUsed  = msgAlreadyUsed;
        this.msgCooldown     = msgCooldown;
        this.msgMaxUses      = msgMaxUses;
        this.msgNotFound     = msgNotFound;
    }

    public String       getName()          { return name; }
    public String       getPermission()    { return permission; }
    public boolean      isOneTime()        { return oneTime; }
    public long         getCooldown()      { return cooldownSeconds; }
    public int          getMaxUses()       { return maxUses; }
    public List<String> getCommands()      { return commands; }
    public String       getMsgSuccess()    { return msgSuccess; }
    public String       getMsgNoPermission(){ return msgNoPermission; }
    public String       getMsgAlreadyUsed(){ return msgAlreadyUsed; }
    public String       getMsgCooldown()   { return msgCooldown; }
    public String       getMsgMaxUses()    { return msgMaxUses; }
    public String       getMsgNotFound()   { return msgNotFound; }
}