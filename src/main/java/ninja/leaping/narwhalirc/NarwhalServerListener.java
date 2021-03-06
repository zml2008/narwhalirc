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
import org.spongepowered.api.event.entity.living.player.PlayerChatEvent;
import org.spongepowered.api.event.entity.living.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.living.player.PlayerQuitEvent;
import org.spongepowered.api.util.event.Order;
import org.spongepowered.api.util.event.Subscribe;

import java.util.HashMap;
import java.util.Map;

public class NarwhalServerListener {
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

    private void addDupeMessage(String message) {
        int chanCount = 0;
        for (BotSession bot : plugin.getBots()) {
            /*for (ChannelCommandSource channel : bot.getChannels()) {
                if (channel.receivesEvent(event)) {
                    ++chanCount;
                }
            }*/
            chanCount += bot.getChannels().size();
        }
        dupeMessages.put(message, chanCount);
    }

    @Subscribe(order = Order.MONITOR)
    public void onChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for (BotSession bot : plugin.getBots()) {
            for (ChannelCommandSource chan : bot.getChannels()) {
                if (chan.receivesEvent(PassedEvent.MESSAGE)) {
                    ChatArguments args = chan.getServerToIrcFormat().getArguments();
                    if (args.hasPlaceholder(ChannelCommandSource.EVENT)) args.setPlaceHolder(ChannelCommandSource.EVENT,
                            event.getFormat().getArguments()
                                    .setPlaceHolder(PlayerChatEvent.NAME, new ChatArguments(event.getPlayer().getDisplayName()))
                                    .setPlaceHolder(PlayerChatEvent.MESSAGE, event.getMessage()));
                    if (args.hasPlaceholder(ChannelCommandSource.NAME)) args.setPlaceHolder(ChannelCommandSource.NAME, new ChatArguments(event.getPlayer().getDisplayName()));
                    if (args.hasPlaceholder(ChannelCommandSource.MESSAGE)) args.setPlaceHolder(ChannelCommandSource.MESSAGE, event.getMessage());
                    chan.sendMessage(args);
                }
            }
        }
    }

    @Subscribe(order = Order.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (event.getMessage() == null) {
            return;
        }
        addDupeMessage(new ChatArguments(event.getMessage()).asString(IrcStyleHandler.ID));
        ChatArguments items = new ChatArguments();
        items.append("[").append(event.getMessage()).append(ChatStyle.RESET).append("]");
        plugin.broadcastBotMessage(PassedEvent.JOIN, items);
    }

    @Subscribe(order = Order.POST)
    public void onQuit(PlayerQuitEvent event) {
        if (event.isCancelled() || event.getMessage() == null) {
            return;
        }
        final PassedEvent passedEvent = event instanceof PlayerKickEvent ? PassedEvent.KICK : PassedEvent.QUIT;
        addDupeMessage(new ChatArguments(event.getMessage()).asString(IrcStyleHandler.ID));
        ChatArguments items = new ChatArguments();
        items.append("[").append(event.getMessage()).append(ChatStyle.RESET).append("]");
        plugin.broadcastBotMessage(passedEvent, items);
    }
}
