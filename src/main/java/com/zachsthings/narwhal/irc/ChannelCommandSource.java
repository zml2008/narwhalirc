package com.zachsthings.narwhal.irc;

import org.pircbotx.Channel;
import org.spout.api.ChatColor;
import org.spout.api.Spout;
import org.spout.api.command.CommandSource;
import org.spout.api.data.ValueHolder;
import org.spout.api.data.ValueHolderBase;
import org.spout.api.event.Result;
import org.spout.api.event.server.permissions.PermissionNodeEvent;
import org.spout.api.geo.World;
import org.spout.api.util.config.Configuration;
import org.spout.api.util.config.ConfigurationNodeSource;
import org.spout.api.util.config.annotated.AnnotatedConfiguration;
import org.spout.api.util.config.annotated.Setting;

import java.util.*;

/**
 * There is one ChannelCommandSource per channel which handles broad
 */
public class ChannelCommandSource extends AnnotatedConfiguration implements CommandSource {

	@Setting("key") private String channelKey;
	@Setting("permissions") private Map<String, Boolean> permissions = new HashMap<String, Boolean>();
	@Setting("receive-events") private Set<PassedEvent> receiveEvents = new HashSet<PassedEvent>(Arrays.asList(PassedEvent.values()));
    @Setting({"format", "irc-to-server"}) public String ircToServerFormat = "<%name%> %channel%: %msg%";
    @Setting({"format", "server-to-irc"}) private String serverToIrcFormat = "%event%";

    private final Channel channel;
    private boolean stripColor;

    public ChannelCommandSource(Configuration config, Channel channel, boolean stripColor) {
        super(config);
        this.channel = channel;
        this.stripColor = stripColor;
    }

	public String getKey() {
		return channelKey;
	}

	public boolean receivesEvent(PassedEvent event) {
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

	// -- Spout interface methods

    public boolean sendMessage(String message) {
        return sendRawMessage(stripColor ? ChatColor.strip(message) : IrcColor.replaceColor(message, false));
    }

    public boolean sendRawMessage(String message) {
        channel.getBot().sendMessage(channel, message);
        return true;
    }

    public String getName() {
        return channel.getBot().getServer() + ":" + channel.getName();
    }

    public ValueHolder getData(String s) {
        return new ValueHolderBase(null) {
            @Override
            public Object getValue(Object def) {
                return null;
            }
        };
    }

    public boolean hasPermission(String permission) {
        return hasPermission(null, permission);
    }

    public boolean hasPermission(World world, String permission) {
        PermissionNodeEvent event = new PermissionNodeEvent(world, this, permission);
        for (String perm : event.getNodes()) {
            Boolean val = permissions.get(perm);
            if (val != null) {
                event.setResult(val ? Result.ALLOW : Result.DENY);
                break;
            }
        }
        Spout.getEventManager().callEvent(event);
        return event.getResult() == Result.ALLOW;
    }

    public Boolean getRawPermission(String perm) {
        return permissions.get(perm);
    }

    public boolean isInGroup(String s) {
        return false;
    }

    public String[] getGroups() {
        return new String[0];
    }

    public boolean isGroup() {
        return false;
    }
}
