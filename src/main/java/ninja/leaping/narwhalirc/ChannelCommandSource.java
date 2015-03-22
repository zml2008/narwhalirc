/**
 * NarwhalIRC
 * Copyright (C) 2013 zml2008
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ninja.leaping.narwhalirc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.narwhalirc.chatstyle.IrcStyleHandler;
import org.pircbotx.Channel;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.text.message.Message;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;

import java.util.*;

/**
 * There is one ChannelCommandSource per channel which handles broad
 */
public class ChannelCommandSource implements CommandSource {
    public static final Placeholder NAME = new Placeholder("NAME"), CHANNEL = new Placeholder("CHANNEL"), MESSAGE = new Placeholder("MESSAGE"), EVENT = new Placeholder("EVENT");

	@Setting("key") private String channelKey;
	@Setting("receive-events") private Set<PassedEvent> receiveEvents = new HashSet<PassedEvent>(Arrays.asList(PassedEvent.values()));
    @Setting("send-events") private Set<PassedEvent> sendEvents = new HashSet<PassedEvent>(Arrays.asList(PassedEvent.values()));
    @Setting({"format", "irc-to-server"}) public ChatTemplate ircToServerFormat = new ChatTemplate(new ChatArguments("<", NAME, "> ", CHANNEL, ": ", MESSAGE));
    @Setting({"format", "server-to-irc"}) private ChatTemplate serverToIrcFormat = new ChatTemplate(new ChatArguments(EVENT));

    private final NarwhalIRCPlugin plugin;
    private final Channel channel;
    private boolean stripColor;
    private final AtomicReference<ChatChannel> activeChannel = new AtomicReference<ChatChannel>(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL);

    public ChannelCommandSource(NarwhalIRCPlugin plugin, Channel channel, boolean stripColor) {
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

    /**
     * This method returns whether a message type (IRC -> game)
     * @param event
     * @return
     */
    public boolean sendsEvent(PassedEvent event) {
        return sendEvents.contains(event);
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
    public void sendMessage(String... strings) {
       for (String msg : strings) {
           channel.send().message(msg);
       }
    }

    @Override
    public void sendMessage(Message... messages) {
        for (Message msg : messages) {
            channel.send().message(msg.toString());
        }

    }

    @Override
    public void sendMessage(Iterable<Message> iterable) {

    }

    @Override
    public String getIdentifier() {
        return getName();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.<CommandSource>of(this);
    }

    @Override
    public SubjectCollection getContainingCollection() {
        return null;
    }

    @Override
    public SubjectData getData() {
        return null;
    }

    @Override
    public SubjectData getTransientData() {
        return null;
    }

    @Override
    public boolean hasPermission(Set<Context> set, String s) {
        return false;
    }

    @Override
    public boolean hasPermission(String s) {
        return false;
    }

    @Override
    public Tristate getPermissionValue(Set<Context> set, String s) {
        return null;
    }

    @Override
    public boolean isChildOf(Subject subject) {
        return false;
    }

    @Override
    public boolean isChildOf(Set<Context> set, Subject subject) {
        return false;
    }

    @Override
    public List<Subject> getParents() {
        return null;
    }

    @Override
    public List<Subject> getParents(Set<Context> set) {
        return null;
    }

    @Override
    public Set<Context> getActiveContexts() {
        return null;
    }
}
