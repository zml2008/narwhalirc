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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class NarwhalServerListener implements Listener {
    private static final Map<String, Integer> dupeMessages = new HashMap<String, Integer>();
    private final NarwhalIRCPlugin plugin;

    public NarwhalServerListener(NarwhalIRCPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean checkDupeMessage(String message) {
        Integer val = dupeMessages.get(message);
        if (val == null) {
            return true;
        } else if (val == 0) {
            dupeMessages.remove(message);
            return true;
        } else {
            dupeMessages.put(message, --val);
            return false;
        }
    }

    private void addDupeMessage(PassedEvent event, String message) {
        int chanCount = 0;
        for (BotSession bot : plugin.getBots()) {
            for (ChannelCommandSource channel : bot.getChannels()) {
                if (channel.receivesEvent(event)) {
                    ++chanCount;
                }
            }
        }
        dupeMessages.put(message, chanCount);
    }

    @EventHandler(order = Order.MONITOR)
    public void onChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        addDupeMessage(PassedEvent.MESSAGE, String.format(event.getFormat(), event.getPlayer().getDisplayName(),  event.getMessage()));

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
        addDupeMessage(PassedEvent.JOIN, event.getMessage());
        plugin.broadcastBotMessage(PassedEvent.JOIN, "[" + event.getMessage() + ChatColor.WHITE + "]");
    }

    @EventHandler(order = Order.MONITOR)
    public void onQuit(PlayerLeaveEvent event) {
        if (event.getMessage() == null) {
            return;
        }
        final PassedEvent passedEvent = event instanceof PlayerKickEvent ? PassedEvent.KICK : PassedEvent.QUIT;
        addDupeMessage(passedEvent, event.getMessage());

        plugin.broadcastBotMessage(passedEvent,
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
