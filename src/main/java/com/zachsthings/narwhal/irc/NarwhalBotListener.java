package com.zachsthings.narwhal.irc;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import java.util.regex.Matcher;

/**
 * @author zml2008
 */
public class NarwhalBotListener extends ListenerAdapter<PircBotX> implements Listener<PircBotX> {
    private final BotSession session;
    private final IrcBridge component;

    public NarwhalBotListener(BotSession session, IrcBridge component) {
        this.session = session;
        this.component = component;
    }

    @Override
    public void onMessage(MessageEvent<PircBotX> event) {
        if (event.getMessage().startsWith(component.getCommandPrefix())) {
            String[] commandSplit = event.getMessage().split(" ");
            commandSplit[0] = commandSplit[0].substring(component.getCommandPrefix().length());
            session.handleCommand(event.getUser(), event.getChannel(), commandSplit);
        } else {
            ChannelCommandSender sender = session.getChannel(event.getChannel().getName());
            if (sender != null) {
                Bukkit.getServer().broadcast(sender.getIrcToServerFormat()
                        .replaceAll("%name%", Matcher.quoteReplacement(event.getUser().getNick()))
                        .replaceAll("%channel%", Matcher.quoteReplacement(event.getChannel().getName()))
                        .replaceAll("%msg%", Matcher.quoteReplacement(IrcColor.replaceColor(event.getMessage(), true)))
                        , IrcBridge.IRC_BROADCAST_PERMISSION);
            }
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
        if (event.getMessage().startsWith(component.getCommandPrefix())) {
            String[] commandSplit = event.getMessage().split(" ");
            commandSplit[0] = commandSplit[0].substring(component.getCommandPrefix().length());
            session.handleCommand(event.getUser(), null, commandSplit);
        } else {
            event.getBot().sendMessage(event.getUser(), "I'm a teapot!");
        }
    }

    @Override
    public void onJoin(JoinEvent<PircBotX> event) {
        if (event.getUser().getNick().equals(event.getBot().getNick())) return;
        Bukkit.getServer().broadcast(ChatColor.BLUE + event.getUser().getNick()
                + " has joined " + event.getChannel().getName(), IrcBridge.IRC_BROADCAST_PERMISSION);
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
        Bukkit.getServer().broadcast(ChatColor.BLUE + event.getUser().getNick()
                + " has left " + event.getChannel().getName()
                + message, IrcBridge.IRC_BROADCAST_PERMISSION);
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
        Bukkit.getServer().broadcast(ChatColor.BLUE + event.getUser().getNick()
                + " has left IRC" + message, IrcBridge.IRC_BROADCAST_PERMISSION);
    }

    @Override
    public void onKick(final KickEvent<PircBotX> event) {
        if (event.getRecipient().getNick().equals(event.getBot().getNick())) {
            CommandBook.server().getScheduler().scheduleSyncDelayedTask(CommandBook.inst(), new Runnable() {
                public void run() {
                    event.getBot().joinChannel(event.getChannel().getName());
                }
            }, 20 * 10L);
        } else {
            // TODO: Broadcast kick to server
        }
    }

    @Override
    public void onAction(ActionEvent<PircBotX> event) {
        if (!event.getUser().getNick().equals(event.getBot().getNick())) {
            Bukkit.getServer().broadcast(event.getChannel().getName() + ": * " + event.getUser().getNick() + " " + event.getAction(), IrcBridge.IRC_BROADCAST_PERMISSION);
        }
    }

    @Override
    public void onNickChange(NickChangeEvent<PircBotX> event) {
        if (!(event.getOldNick().equals(event.getBot().getNick()) || event.getNewNick().equals(event.getBot().getNick()))) {
            session.removeSender(event.getUser());
        }
    }
}
