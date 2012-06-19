package com.zachsthings.narwhal.irc;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.ConfigurationNode;
import com.zachsthings.libcomponents.config.Setting;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * There is one ChannelCommandSender per channel which handles broad
 */
public class ChannelCommandSender extends ConfigurationBase implements CommandSender {

	@Setting("key") private String channelKey;
	@Setting("permissions") private Map<String, Boolean> permissions;
	@Setting("receive-events") private Set<String> receiveEvents = new HashSet<String>(Arrays.asList("join", "quit", "kick", "message", "me"));
    @Setting("format.irc-to-server") public String ircToServerFormat = "<%name%> %channel%: %msg%";
    @Setting("format.server-to-irc") private String serverToIrcFormat = "%event%";

    private final Channel channel;
    private final PermissibleBase perms;
    private boolean stripColor;

    public ChannelCommandSender(Channel channel, boolean stripColor) {
        this.channel = channel;
        perms = new PermissibleBase(this);
        this.stripColor = stripColor;
    }

	@Override
	public void load(ConfigurationNode node) {
		super.load(node);
        PermissionAttachment attach = addAttachment(CommandBook.inst());
        attach.setPermission(IrcBridge.IRC_BROADCAST_PERMISSION, false);
		if (permissions != null) {
			for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
				attach.setPermission(entry.getKey(), entry.getValue());
			}
		}
	}

	public String getKey() {
		return channelKey;
	}

	public boolean receivesEvent(String event) {
		return receiveEvents.contains(event);
	}

    public Channel getChannel() {
        return channel;
    }

    public String getIrcToServerFormat() {
        return ircToServerFormat;
    }

    public String getServerToIrcFormat() {
        return serverToIrcFormat;
    }

	// -- Bukkit interface methods

    public void sendMessage(String message) {
        channel.getBot().sendMessage(channel, stripColor ? ChatColor.stripColor(message) : IrcColor.replaceColor(message, false));
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
        return channel.getBot().getServer() + ":" + channel.getName();
    }

    public boolean isPermissionSet(String permission) {
        return perms.isPermissionSet(permission);
    }

    public boolean isPermissionSet(Permission permission) {
        return perms.isPermissionSet(permission);
    }

    public boolean hasPermission(String permission) {
        return perms.hasPermission(permission);
    }

    public boolean hasPermission(Permission permission) {
        return perms.hasPermission(permission);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String permission, boolean value) {
        return perms.addAttachment(plugin, permission, value);
    }

    public PermissionAttachment addAttachment(Plugin plugin) {
        return perms.addAttachment(plugin);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String permission, boolean value, int ticks) {
        return perms.addAttachment(plugin, permission, value, ticks);
    }

    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return perms.addAttachment(plugin, ticks);
    }

    public void removeAttachment(PermissionAttachment permissionAttachment) {
        perms.removeAttachment(permissionAttachment);
    }

    public void recalculatePermissions() {
        perms.recalculatePermissions();
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return perms.getEffectivePermissions();
    }

    public boolean isOp() {
        return channel.isOp(channel.getBot().getUserBot());
    }

    public void setOp(boolean value) {
        if (value) {
            channel.op(channel.getBot().getUserBot());
        } else {
            channel.deOp(channel.getBot().getUserBot());
        }
    }

}
