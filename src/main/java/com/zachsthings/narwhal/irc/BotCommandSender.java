package com.zachsthings.narwhal.irc;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.pircbotx.Channel;
import org.pircbotx.User;

import java.util.Set;

/**
 * This is a CommandSender implementation for users in an IRC channel.
 * It will not receive broadcasts from server.broadcastMessage() but otherwise should
 * behave like a normal CommandSender
 */
public class BotCommandSender implements CommandSender {
    /**
     * The user in IRC that this CommandSender corresponds to.
     */
    private User user;

    /**
     * The PermissibleBase that provides a simple permissions implementation
     */
    private final PermissibleBase perm;

    /**
     * The channel this BotCommandSender exists in
     */
    private final Channel channel;

    private boolean stripColor;

    public BotCommandSender(User user, Channel channel, boolean stripColor) {
        this.user = user;
        this.channel = channel;
        this.stripColor = stripColor;
        perm = new PermissibleBase(this);
        final PermissionAttachment attach = perm.addAttachment(CommandBook.inst());
        attach.setPermission(Server.BROADCAST_CHANNEL_USERS, false);
        attach.setPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE, false);
        attach.setPermission(IrcBridge.IRC_BROADCAST_PERMISSION, false);
    }

    public void sendMessage(String message) {
        if (channel == null) {
            user.getBot().sendMessage(user, stripColor ? ChatColor.stripColor(message) : IrcColor.replaceColor(message, false));
        } else {
            user.getBot().sendMessage(channel, "_" + user.getNick().substring(1) + ": " + (stripColor ? ChatColor.stripColor(message) : IrcColor.replaceColor(message, false)));
        }
    }

    public void sendMessage(String[] strings) {
        for (String line : strings) {
            sendMessage(line);
        }
    }

    public Server getServer() {
        return Bukkit.getServer();
    }

    public String getName() {
        return user.getNick();
    }

    public boolean isPermissionSet(String s) {
        return perm.isPermissionSet(s);
    }

    public boolean isPermissionSet(Permission permission) {
        return perm.isPermissionSet(permission);
    }

    public boolean hasPermission(String s) {
        return perm.hasPermission(s);
    }

    public boolean hasPermission(Permission permission) {
        return perm.hasPermission(permission);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String permission, boolean value) {
        return perm.addAttachment(plugin, permission, value);
    }

    public PermissionAttachment addAttachment(Plugin plugin) {
        return perm.addAttachment(plugin);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String permission, boolean value, int ticks) {
        return perm.addAttachment(plugin, permission, value, ticks);
    }

    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return perm.addAttachment(plugin, ticks);
    }

    public void removeAttachment(PermissionAttachment permissionAttachment) {
        perm.removeAttachment(permissionAttachment);
    }

    public void recalculatePermissions() {
        perm.recalculatePermissions();
        getServer().getPluginManager().unsubscribeFromPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE, this);
        getServer().getPluginManager().unsubscribeFromPermission(Server.BROADCAST_CHANNEL_USERS, this);
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return perm.getEffectivePermissions();
    }

    public boolean isOp() {
        return channel != null && channel.isOp(user);
    }

    public void setOp(boolean value) {
        if (channel != null) {
            if (value) {
                channel.op(user);
            } else {
                channel.deOp(user);
            }
            recalculatePermissions();
        }
    }
}
