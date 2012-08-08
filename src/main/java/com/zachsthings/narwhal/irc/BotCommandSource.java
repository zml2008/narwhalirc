package com.zachsthings.narwhal.irc;

import com.zachsthings.narwhal.irc.chatstyle.IrcStyleHandler;
import com.zachsthings.narwhal.irc.util.NarwhalIRCUtil;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.command.Command;
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

    public boolean sendMessage(Object... message) {
        return sendMessage(new ChatArguments(message));
    }

    public void sendCommand(String cmd, ChatArguments args) {
        if (cmd.equalsIgnoreCase("say")) {
            sendMessage(args);
        } else {
            processCommand(cmd, args);
        }
    }

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

    public boolean sendMessage(ChatArguments message) {
        String messageStr = stripColor ? message.getPlainString() : message.asString(IrcStyleHandler.ID);
        if (channel == null) {
            user.getBot().sendMessage(user, messageStr);
        } else {
            user.getBot().sendMessage(channel, "_" + user.getNick().substring(1) + ": " + messageStr);
        }
        return true;
    }

    public boolean sendRawMessage(Object... message) {
        return sendRawMessage(new ChatArguments(message));
    }

    public boolean sendRawMessage(ChatArguments message) {
        if (channel == null) {
            user.getBot().sendMessage(user, message.asString(IrcStyleHandler.ID));
        } else {
            user.getBot().sendMessage(channel, message.asString(IrcStyleHandler.ID));
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
