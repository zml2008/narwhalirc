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
package com.zachsthings.narwhal.irc;

import com.google.common.base.Preconditions;
import com.zachsthings.narwhal.irc.chatstyle.IrcStyleHandler;
import com.zachsthings.narwhal.irc.util.NarwhalIRCUtil;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.channel.ChatChannel;
import org.spout.api.command.CommandSource;
import org.spout.api.data.ValueHolder;
import org.spout.api.data.ValueHolderBase;
import org.spout.api.geo.World;
import org.spout.api.lang.Locale;

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

    private final AtomicReference<ChatChannel> activeChannel = new AtomicReference<ChatChannel>(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL);

    public BotCommandSource(NarwhalIRCPlugin plugin, User user, Channel channel, boolean stripColor) {
        this.plugin = plugin;
        this.user = user;
        this.channel = channel;
        this.stripColor = stripColor;
    }

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
    public boolean sendMessage(ChatArguments message) {
        String messageStr = stripColor ? message.getPlainString() : message.asString(IrcStyleHandler.ID);
        if (channel == null) {
            user.getBot().sendMessage(user, messageStr);
        } else {
            user.getBot().sendMessage(channel, "_" + user.getNick().substring(1) + ": " + messageStr);
        }
        return true;
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
    public boolean hasPermission(String s) {
        return hasPermission(null, s);
    }

    @Override
    public boolean hasPermission(World world, String s) {
        return false;
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

    public User getUser() {
        return user;
    }

    public Channel getChannel() {
        return channel;
    }
}
