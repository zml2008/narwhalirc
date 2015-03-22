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

import ninja.leaping.narwhalirc.chatstyle.IrcStyleHandler;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

/**
 * Listener for events coming from a NarwhalBot instance
 */
public class NarwhalBotListener extends ListenerAdapter<NarwhalBot> implements Listener<NarwhalBot> {
    private final BotSession session;
    private final NarwhalIRCPlugin plugin;

    public NarwhalBotListener(BotSession session, NarwhalIRCPlugin plugin) {
        this.session = session;
        this.plugin = plugin;
    }

    @Override
    public void onMessage(MessageEvent<NarwhalBot> event) {
        if (plugin.doesChannelCommands()) {
            ChannelCommandSource source = session.getChannel(event.getChannel().getName());
            if (source == null) {
                return;
            }

            if (event.getMessage().startsWith(plugin.getCommandPrefix())) {
                session.handleCommand(event.getUser(), event.getChannel(), event.getMessage().substring(plugin.getCommandPrefix().length()));
            } else if (source.sendsEvent(PassedEvent.MESSAGE)) {
                ChatArguments args = source.getIrcToServerFormat().getArguments();
                if (args.hasPlaceholder(ChannelCommandSource.NAME)) {
                    args.setPlaceHolder(ChannelCommandSource.NAME, new ChatArguments(event.getUser().getNick()));
                }
                if (args.hasPlaceholder(ChannelCommandSource.CHANNEL)) {
                    args.setPlaceHolder(ChannelCommandSource.CHANNEL, new ChatArguments(event.getChannel().getName()));
                }
                if (args.hasPlaceholder(ChannelCommandSource.MESSAGE)) {
                    args.setPlaceHolder(ChannelCommandSource.MESSAGE, ChatArguments.fromString(event.getMessage(), IrcStyleHandler.ID));
                }

                plugin.getServer().broadcastMessage(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL, args);
            }
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent<NarwhalBot> event) {
        if (plugin.doesPrivateCommands()) {
            if (event.getMessage().startsWith(plugin.getCommandPrefix())) {
                session.handleCommand(event.getUser(), null, event.getMessage().substring(plugin.getCommandPrefix().length()));
            } else {
                event.getBot().sendMessage(event.getUser(),
                        "I'm a teapot! Ask the server admin if I am short or stout.");
            }
        }
    }

    @Override
    public void onJoin(JoinEvent<NarwhalBot> event) {
        ChannelCommandSource channel = session.getChannel(event.getChannel().getName());
        if (channel == null) {
            return;
        }
        if (event.getUser().getNick().equals(event.getBot().getNick())) {
            return;
        }
        if (!channel.sendsEvent(PassedEvent.JOIN)) {
            return;
        }

        plugin.getServer().broadcastMessage(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL, ChatStyle.BLUE, event.getUser().getNick(),
                " has joined ", event.getChannel().getName());
    }

    @Override
    public void onPart(PartEvent<NarwhalBot> event) {
        ChannelCommandSource channel = session.getChannel(event.getChannel().getName());
        if (channel == null) {
            return;
        }

        if (event.getUser().getNick().equals(event.getBot().getNick())) {
            return;
        }
        session.removeSender(event.getUser(), event.getChannel());

        if (channel.sendsEvent(PassedEvent.QUIT)) {
            String message;
            if (event.getReason().equals("")) {
                message = "";
            } else {
                message = ": " + event.getReason();
            }
            plugin.getServer().broadcastMessage(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL, ChatStyle.BLUE, event.getUser().getNick(),
                    " has left ", event.getChannel().getName(), message);
        }
    }

    @Override
    public void onQuit(QuitEvent<NarwhalBot> event) {
        boolean broadcastQuit = false;
        boolean contains = false;
        for (ChannelCommandSource source : session.getChannels()) {
            if (source.getChannel().getUsers().contains(event.getUser())) {
                broadcastQuit |= source.sendsEvent(PassedEvent.QUIT);
                contains = true;
            }
        }

        if (!contains) {
            return;
        }

        if (event.getUser().equals(event.getBot().getUserBot())) {
            return;
        }

        session.removeSender(event.getUser());

        if (broadcastQuit) {
            String message;
            if (event.getReason().equals("")) {
                message = "";
            } else {
                message = ": " + event.getReason();
            }
            plugin.getServer().broadcastMessage(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL, ChatStyle.BLUE,
                    event.getUser().getNick(), " has left IRC", message);
        }
    }

    @Override
    public void onKick(final KickEvent<NarwhalBot> event) {
        if (event.getRecipient().getNick().equals(event.getBot().getNick())) {
            plugin.getEngine().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    event.getBot().joinChannel(event.getChannel().getName());
                }
            }, 10000L, TaskPriority.LOW);
        } else {
            ChannelCommandSource source = session.getChannel(event.getChannel().getName());
            if (source != null) {
                session.removeSender(event.getRecipient(), event.getChannel());
                if (source.sendsEvent(PassedEvent.KICK)) {
                    String message;
                    if (event.getReason().equals("")) {
                        message = "";
                    } else {
                        message = ": " + event.getReason();
                    }
                    plugin.getServer().broadcastMessage(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL, ChatStyle.BLUE,
                            event.getRecipient().getNick(), " has has been kicked from",
                            event.getChannel().getName(), " by ", event.getSource().getNick(), message);
                }
            }
        }
    }

    @Override
    public void onAction(ActionEvent<NarwhalBot> event) {
        ChannelCommandSource channel = session.getChannel(event.getChannel().getName());
        if (channel == null || !channel.sendsEvent(PassedEvent.ACTION)) {
            return;
        }
        if (!event.getUser().getNick().equals(event.getBot().getNick())) {
            plugin.getServer().broadcastMessage(NarwhalIRCPlugin.IRC_BROADCAST_CHANNEL, event.getChannel().getName(), ": * ", event.getUser().getNick(), " ", event.getAction());
        }
    }

    @Override
    public void onNickChange(NickChangeEvent<NarwhalBot> event) {
        if (!(event.getOldNick().equals(event.getBot().getNick()) || event.getNewNick().equals(event.getBot().getNick()))) {
            session.removeSender(event.getUser());
        }
    }
}
