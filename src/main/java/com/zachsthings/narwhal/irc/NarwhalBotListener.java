package com.zachsthings.narwhal.irc;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;
import org.spout.api.ChatColor;
import org.spout.api.Spout;
import org.spout.api.scheduler.TaskPriority;

import java.util.regex.Matcher;

/**
 * @author zml2008
 */
public class NarwhalBotListener extends ListenerAdapter<PircBotX> implements Listener<PircBotX> {
    private final BotSession session;
    private final NarwhalIRCPlugin plugin;

    public NarwhalBotListener(BotSession session, NarwhalIRCPlugin plugin) {
        this.session = session;
        this.plugin = plugin;
    }

    @Override
    public void onMessage(MessageEvent<PircBotX> event) {
        if (event.getMessage().startsWith(plugin.getCommandPrefix())) {
            String[] commandSplit = event.getMessage().split(" ");
            commandSplit[0] = commandSplit[0].substring(plugin.getCommandPrefix().length());
            session.handleCommand(event.getUser(), event.getChannel(), commandSplit);
        } else {
            ChannelCommandSource source = session.getChannel(event.getChannel().getName());
            if (source != null) {
                Spout.getEngine().broadcastMessage(source.getIrcToServerFormat()
                        .replaceAll("%name%", Matcher.quoteReplacement(event.getUser().getNick()))
                        .replaceAll("%channel%", Matcher.quoteReplacement(event.getChannel().getName()))
                        .replaceAll("%msg%", Matcher.quoteReplacement(IrcColor.replaceColor(event.getMessage(), true)))
                        , NarwhalIRCPlugin.IRC_BROADCAST_PERMISSION);
            }
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
        if (event.getMessage().startsWith(plugin.getCommandPrefix())) {
            String[] commandSplit = event.getMessage().split(" ");
            commandSplit[0] = commandSplit[0].substring(plugin.getCommandPrefix().length());
            session.handleCommand(event.getUser(), null, commandSplit);
        } else {
            event.getBot().sendMessage(event.getUser(),
                    "I'm a teapot! Ask the server admin if I am short or stout.");
        }
    }

    @Override
    public void onJoin(JoinEvent<PircBotX> event) {
        if (event.getUser().getNick().equals(event.getBot().getNick())) return;
        Spout.getEngine().broadcastMessage(ChatColor.BLUE + event.getUser().getNick()
                + " has joined " + event.getChannel().getName(), NarwhalIRCPlugin.IRC_BROADCAST_PERMISSION);
    }


    @Override
    public void onPart(PartEvent<PircBotX> event) {
        if (event.getUser().getNick().equals(event.getBot().getNick())) return;
        session.removeSender(event.getUser(), event.getChannel());
        String message;
        if (event.getReason().equals("")) {
            message = "";
        } else {
            message = ": " + event.getReason();
        }
        Spout.getEngine().broadcastMessage(ChatColor.BLUE + event.getUser().getNick()
                + " has left " + event.getChannel().getName()
                + message, NarwhalIRCPlugin.IRC_BROADCAST_PERMISSION);
    }

    @Override
    public void onQuit(QuitEvent<PircBotX> event) {
        if (event.getUser().equals(event.getBot().getUserBot())) {
            return;
        }
        session.removeSender(event.getUser());
        String message;
        if (event.getReason().equals("")) {
            message = "";
        } else {
            message = ": " + event.getReason();
        }
        Spout.getEngine().broadcastMessage(ChatColor.BLUE + event.getUser().getNick()
                + " has left IRC" + message, NarwhalIRCPlugin.IRC_BROADCAST_PERMISSION);
    }

    @Override
    public void onKick(final KickEvent<PircBotX> event) {
        if (event.getRecipient().getNick().equals(event.getBot().getNick())) {
            Spout.getEngine().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    event.getBot().joinChannel(event.getChannel().getName());
                }
            }, 10000L, TaskPriority.LOW);
        } else {
            // TODO: Broadcast kick to server
        }
    }

    @Override
    public void onAction(ActionEvent<PircBotX> event) {
        if (!event.getUser().getNick().equals(event.getBot().getNick())) {
            Spout.getEngine().broadcastMessage(event.getChannel().getName() + ": * " + event.getUser().getNick() + " " + event.getAction(), NarwhalIRCPlugin.IRC_BROADCAST_PERMISSION);
        }
    }

    @Override
    public void onNickChange(NickChangeEvent<PircBotX> event) {
        if (!(event.getOldNick().equals(event.getBot().getNick()) || event.getNewNick().equals(event.getBot().getNick()))) {
            session.removeSender(event.getUser());
        }
    }
}
