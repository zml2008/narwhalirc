package com.zachsthings.narwhal.irc;

import org.spout.api.ChatColor;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.event.Order;
import org.spout.api.event.Result;
import org.spout.api.event.player.PlayerChatEvent;
import org.spout.api.event.player.PlayerJoinEvent;
import org.spout.api.event.player.PlayerKickEvent;
import org.spout.api.event.player.PlayerLeaveEvent;
import org.spout.api.event.server.permissions.PermissionGetAllWithNodeEvent;

import java.util.regex.Matcher;

public class NarwhalServerListener implements Listener {
    private final NarwhalIRCPlugin plugin;

    public NarwhalServerListener(NarwhalIRCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(order = Order.MONITOR)
    public void onChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for (BotSession bot : plugin.getBots()) {
            for (ChannelCommandSource chan : bot.getChannels()) {
                if (chan.receivesEvent(PassedEvent.MESSAGE)) {
                    chan.sendMessage(chan.getServerToIrcFormat().
                            replaceAll("%event%", Matcher.quoteReplacement(String.format(event.getFormat(),
                                    event.getPlayer().getDisplayName(), event.getMessage())))
                            .replaceAll("%name%", Matcher.quoteReplacement(event.getPlayer().getDisplayName()))
                            .replaceAll("%msg%", Matcher.quoteReplacement(event.getMessage())));
                }
            }
        }
    }

    @EventHandler(order = Order.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (event.getMessage() == null) {
            return;
        }
        plugin.broadcastBotMessage(PassedEvent.JOIN, "[" + event.getMessage() + ChatColor.WHITE + "]");
    }

    @EventHandler(order = Order.MONITOR)
    public void onQuit(PlayerLeaveEvent event) {
        if (event.getMessage() == null) {
            return;
        }

        plugin.broadcastBotMessage(event instanceof PlayerKickEvent ? PassedEvent.KICK : PassedEvent.QUIT,
                "[" + event.getMessage() + ChatColor.WHITE + "]");
    }

    @EventHandler(order = Order.EARLIEST)
    public void onGetAllWithNode(PermissionGetAllWithNodeEvent event) {
        for (BotSession bot : plugin.getBots()) {
            channels: for (ChannelCommandSource channel : bot.getChannels()) {
                if (NarwhalIRCPlugin.BLACKLISTED_BOT_PERMS.contains(event.getNode())) {
                    event.getReceivers().put(channel, Result.DENY);
                    continue;
                }

                for (String node : event.getNodes()) {
                    Boolean perm = channel.getRawPermission(node);
                    if (perm != null) {
                        event.getReceivers().put(channel, perm ? Result.ALLOW : Result.DENY);
                        continue channels;
                    }
                }
                event.getReceivers().put(channel, Result.DEFAULT);
            }
        }
    }
}
