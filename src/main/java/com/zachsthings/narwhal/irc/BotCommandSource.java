package com.zachsthings.narwhal.irc;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.spout.api.ChatColor;
import org.spout.api.command.CommandSource;
import org.spout.api.data.ValueHolder;
import org.spout.api.data.ValueHolderBase;
import org.spout.api.geo.World;

/**
 * This is a CommandSender implementation for users in an IRC channel.
 * It will not receive broadcasts from server.broadcastMessage() but otherwise should
 * behave like a normal CommandSender
 */
public class BotCommandSource implements CommandSource {
    /**
     * The user in IRC that this CommandSender corresponds to.
     */
    private final User user;

    /**
     * The channel this BotCommandSource exists in
     */
    private final Channel channel;

    private final boolean stripColor;

    public BotCommandSource(User user, Channel channel, boolean stripColor) {
        this.user = user;
        this.channel = channel;
        this.stripColor = stripColor;
    }

    public boolean sendMessage(String message) {
        if (channel == null) {
            return sendRawMessage(stripColor ? ChatColor.strip(message) : IrcColor.replaceColor(message, false));
        } else {
            return sendRawMessage("_" + user.getNick().substring(1) + ": " + (stripColor ? ChatColor.strip(message) : IrcColor.replaceColor(message, false)));
        }
    }

    public boolean sendRawMessage(String message) {
        if (channel == null) {
            user.getBot().sendMessage(user, message);
        } else {
            user.getBot().sendMessage(channel, message);
        }
        return true;
    }

    public String getName() {
        return user.getNick();
    }

    public ValueHolder getData(String s) {
        return new ValueHolderBase(null) {
            @Override
            public Object getValue(Object def) {
                return def;
            }
        };
    }

    public boolean hasPermission(String s) {
        return false;
    }

    public boolean hasPermission(World world, String s) {
        return false;
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

    public User getUser() {
        return user;
    }

    public Channel getChannel() {
        return channel;
    }
}
