package com.zachsthings.narwhal.irc;

import com.google.common.base.Preconditions;
import com.zachsthings.narwhal.irc.chatstyle.IrcStyleHandler;
import org.pircbotx.Channel;
import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.ChatTemplate;
import org.spout.api.chat.Placeholder;
import org.spout.api.chat.channel.ChatChannel;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.command.Command;
import org.spout.api.command.CommandSource;
import org.spout.api.data.ValueHolder;
import org.spout.api.data.ValueHolderBase;
import org.spout.api.event.Result;
import org.spout.api.event.server.permissions.PermissionNodeEvent;
import org.spout.api.geo.World;
import org.spout.api.lang.Locale;
import org.spout.api.util.config.Configuration;
import org.spout.api.util.config.annotated.AnnotatedSubclassConfiguration;
import org.spout.api.util.config.annotated.Setting;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * There is one ChannelCommandSource per channel which handles broad
 */
public class ChannelCommandSource extends AnnotatedSubclassConfiguration implements CommandSource {
    public static final Placeholder NAME = new Placeholder("NAME"), CHANNEL = new Placeholder("CHANNEL"), MESSAGE = new Placeholder("MESSAGE"), EVENT = new Placeholder("EVENT");

	@Setting("key") private String channelKey;
	@Setting("permissions") private Map<String, Boolean> permissions = new HashMap<String, Boolean>();
	@Setting("receive-events") private Set<PassedEvent> receiveEvents = new HashSet<PassedEvent>(Arrays.asList(PassedEvent.values()));
    @Setting({"format", "irc-to-server"}) public ChatTemplate ircToServerFormat = new ChatTemplate(new ChatArguments("<", NAME, "> ", CHANNEL, ": ", MESSAGE));
    @Setting({"format", "server-to-irc"}) private ChatTemplate serverToIrcFormat = new ChatTemplate(new ChatArguments(EVENT));

    private final NarwhalIRCPlugin plugin;
    private final Channel channel;
    private boolean stripColor;
    private final AtomicReference<ChatChannel> activeChannel = new AtomicReference<ChatChannel>(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL);

    public ChannelCommandSource(NarwhalIRCPlugin plugin, Configuration config, Channel channel, boolean stripColor) {
        super(config);
        this.plugin = plugin;
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

    public ChatTemplate getIrcToServerFormat() {
        return ircToServerFormat;
    }

    public ChatTemplate getServerToIrcFormat() {
        return serverToIrcFormat;
    }

	// -- Spout interface methods

    @Override
    public boolean sendMessage(Object... message) {
        return sendMessage(new ChatArguments(message));
    }

    @Override
    public void sendCommand(String cmd, ChatArguments args) {
        if (cmd.equalsIgnoreCase("say")) {
            sendMessage(args);
        } else {
            processCommand(cmd, args);
        }
    }

    @Override
    public void processCommand(String cmdName, ChatArguments args) {
        Command cmd = plugin.getBotCommands().getChild(cmdName);
        if (cmd != null) {
            cmd.process(this, cmdName, args, false);
        } else {
            sendMessage(ChatStyle.RED, "Unknown command: " + cmdName);
        }
    }

    @Override
    public boolean sendMessage(ChatArguments message) {
        String messageStr = message.asString(IrcStyleHandler.ID);
        if (!NarwhalServerListener.checkDupeMessage(messageStr)) {
            return false;
        }
        channel.getBot().sendMessage(channel, stripColor ? message.getPlainString() : messageStr);
        return true;
    }

    @Override
    public boolean sendRawMessage(Object... message) {
        return sendRawMessage(new ChatArguments(message));
    }

    @Override
    public boolean sendRawMessage(ChatArguments message) {
        channel.getBot().sendMessage(channel, message.asString(IrcStyleHandler.ID));
        return true;
    }

    @Override
    public Locale getPreferredLocale() {
        return Locale.ENGLISH_US;
    }

    @Override
    public ChatChannel getActiveChannel() {
        return activeChannel.get();
    }

    @Override
    public void setActiveChannel(ChatChannel chatChannel) {
        Preconditions.checkNotNull(chatChannel);
        chatChannel.onAttachTo(this);
        activeChannel.getAndSet(chatChannel).onDetachedFrom(this);
    }

    @Override
    public String getName() {
        return channel.getBot().getServer() + ":" + channel.getName();
    }

    @Override
    public ValueHolder getData(String s) {
        return getData(null, s);
    }

    @Override
    public ValueHolder getData(World world, String s) {
        return new ValueHolderBase.NullHolder();
    }

    @Override
    public boolean hasData(String s) {
        return hasData(null, s);
    }

    @Override
    public boolean hasData(World world, String s) {
        return false;
    }

    @Override
    public boolean hasPermission(String permission) {
        return hasPermission(null, permission);
    }

    @Override
    public boolean hasPermission(World world, String permission) {
        PermissionNodeEvent event = new PermissionNodeEvent(world, this, permission);
        for (String perm : event.getNodes()) {
            Boolean val = permissions.get(perm);
            if (val != null) {
                event.setResult(val ? Result.ALLOW : Result.DENY);
                break;
            }
        }
        plugin.getEngine().getEventManager().callEvent(event);
        return event.getResult() == Result.ALLOW;
    }

    public Boolean getRawPermission(String perm) {
        return permissions.get(perm);
    }

    @Override
    public boolean isInGroup(String s) {
        return isInGroup(null, s);
    }

    @Override
    public boolean isInGroup(World world, String s) {
        return false;
    }

    @Override
    public String[] getGroups() {
        return getGroups(null);
    }

    @Override
    public String[] getGroups(World world) {
        return new String[0];
    }
}
