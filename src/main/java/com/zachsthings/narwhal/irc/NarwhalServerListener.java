package com.zachsthings.narwhal.irc;

import com.zachsthings.narwhal.irc.chatstyle.IrcStyleHandler;
import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.style.ChatStyle;
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

        for (BotSession bot : plugin.getBots()) {
            for (ChannelCommandSource chan : bot.getChannels()) {
                if (chan.receivesEvent(PassedEvent.MESSAGE)) {
                    ChatArguments args = chan.getServerToIrcFormat().getArguments();
                    args.setPlaceHolder(ChannelCommandSource.EVENT,
                            event.getFormat().getArguments()
                                    .setPlaceHolder(PlayerChatEvent.NAME, new ChatArguments(event.getPlayer().getDisplayName()))
                                    .setPlaceHolder(PlayerChatEvent.MESSAGE, event.getMessage()))
                            .setPlaceHolder(ChannelCommandSource.NAME, new ChatArguments(event.getPlayer().getDisplayName()))
                            .setPlaceHolder(ChannelCommandSource.MESSAGE, event.getMessage());
                }
            }
        }
    }

    @EventHandler(order = Order.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (event.getMessage() == null) {
            return;
        }
        addDupeMessage(PassedEvent.JOIN, new ChatArguments(event.getMessage()).asString(IrcStyleHandler.ID));
        ChatArguments items = new ChatArguments();
        items.append("[").append(event.getMessage()).append(ChatStyle.RESET).append("]");
        plugin.broadcastBotMessage(PassedEvent.JOIN, items);
    }

    @EventHandler(order = Order.MONITOR)
    public void onQuit(PlayerLeaveEvent event) {
        if (event.isCancelled() || event.getMessage() == null) {
            return;
        }
        final PassedEvent passedEvent = event instanceof PlayerKickEvent ? PassedEvent.KICK : PassedEvent.QUIT;
        addDupeMessage(passedEvent, new ChatArguments(event.getMessage()).asString(IrcStyleHandler.ID));
        ChatArguments items = new ChatArguments();
        items.append("[").append(event.getMessage()).append(ChatStyle.RESET).append("]");
        plugin.broadcastBotMessage(passedEvent, items);
    }

    @EventHandler(order = Order.EARLIEST)
    public void onGetAllWithNode(PermissionGetAllWithNodeEvent event) {
        boolean blacklisted = false;
        for (String node : event.getNodes()) {
            if (NarwhalIRCPlugin.BLACKLISTED_BOT_PERMS.contains(node)) {
                blacklisted = true;
                break;
            }
        }
        for (BotSession bot : plugin.getBots()) {
            channels: for (ChannelCommandSource channel : bot.getChannels()) {
                if (blacklisted) {
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
