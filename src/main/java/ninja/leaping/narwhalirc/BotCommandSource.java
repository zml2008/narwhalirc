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
import ninja.leaping.narwhalirc.chatstyle.IrcStyleHandler;
import ninja.leaping.narwhalirc.util.NarwhalIRCUtil;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.text.message.Message;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a CommandSender implementation for users in an IRC channel.
 * It will not receive broadcasts from server.broadcastMessage() but otherwise should
 * behave like a normal CommandSender
 */
public class BotCommandSource implements CommandSource {
    private final NarwhalIRCPlugin plugin;
    /**
     * The user in IRC that this CommandSender corresponds to.
     */
    private final User user;

    /**
     * The channel this BotCommandSource exists in
     */
    private final Channel channel;

    private final boolean stripColor;

    public BotCommandSource(NarwhalIRCPlugin plugin, User user, Channel channel, boolean stripColor) {
        this.plugin = plugin;
        this.user = user;
        this.channel = channel;
        this.stripColor = stripColor;
    }

    @Override
    public void processCommand(String cmdName, ChatArguments args) {
        NarwhalIRCUtil.handleCommand(this, cmdName, args);
        /*
        Command cmd = plugin.getBotCommands().getChild(cmdName);
        if (cmd != null) {
            cmd.process(this, cmdName, args, false);
        } else {
            sendMessage(ChatStyle.RED, "Unknown command: " + cmdName);
        }*/
    }

    @Override
    public boolean sendRawMessage(Object... message) {
        return sendRawMessage(new ChatArguments(message));
    }

    @Override
    public boolean sendRawMessage(ChatArguments message) {
        if (channel == null) {
            user.getBot().sendMessage(user, message.asString(IrcStyleHandler.ID));
        } else {
            user.getBot().sendMessage(channel, message.asString(IrcStyleHandler.ID));
        }
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
        return user.getNick();
    }

    @Override
    public void sendMessage(String... strings) {

    }

    @Override
    public void sendMessage(Message... messages) {

    }

    @Override
    public void sendMessage(Iterable<Message> iterable) {

    }

    public User getUser() {
        return user;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return null;
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
