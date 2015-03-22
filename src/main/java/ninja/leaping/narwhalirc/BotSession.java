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

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.narwhalirc.chatstyle.IrcStyleHandler;
import ninja.leaping.narwhalirc.util.NarwhalIRCUtil;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.IdentServer;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.DisconnectEvent;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
* @author zml2008
*/
public class BotSession {
    private final String server;
    private final NarwhalBot bot;
    private final NarwhalIRCPlugin plugin;
    private final Map<String, Map<String, BotCommandSource>> senders = new HashMap<String, Map<String, BotCommandSource>>();
    private final Map<String, ChannelCommandSource> channelSenders = new HashMap<String, ChannelCommandSource>();
    @Setting("ssl") private SSlConfiguration ssl;
    @Setting("nick") private String nick = "NarwhalBot";
    @Setting("channels") private Map<String, Map<?, ?>> rawChannels = createDefaultChannels();
    @Setting("port") private int port = 6667;
    @Setting("nickserv-pass") private String nickServPass;
    @Setting("strip-color") private boolean stripColor;
    @Setting("bind-address") private String bindAddress;
    @Setting("password") private String password;
    @Setting("connect-timeout") private int connectTimeout = 10000;

    @ConfigSerializable
    private static class SSlConfiguration {
        @Setting("trust-all-certs") public boolean trustAllCerts;
        @Setting("enabled") public boolean enabled;
    }

    private static Map<String, Map<?, ?>> createDefaultChannels() {
        Map<String, Map<?, ?>> defChannel = new HashMap<String, Map<?, ?>>();
        defChannel.put("#zml", new HashMap<Object, Object>());
        return defChannel;
    }

    public BotSession(ConfigurationNode config, String server, NarwhalIRCPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
        Configuration.Builder<NarwhalBot> botConfig = new Configuration.Builder<NarwhalBot>()
                .setMessageDelay(250)
                .setSocketTimeout(connectTimeout)
                .setLogin("Narwhal")
                .addListener(new NarwhalBotListener(this, plugin))
                .setServer(server, port)
                .setServerPassword(password)
                .setNickservPassword(nickServPass)
                .setIdentServerEnabled(true);
        if (bindAddress != null && !bindAddress.isEmpty()) {
            try {
                botConfig.setLocalAddress(InetAddress.getByName(bindAddress));
            } catch (UnknownHostException e) {
                plugin.getLogger().log(Level.WARNING, "Error setting bind address: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }
        botConfig.setServer(server, port);
        botConfig.setServerPassword(password);
        if (ssl.enabled) {
            UtilSSLSocketFactory factory = new UtilSSLSocketFactory();
            if (ssl.trustAllCerts) {
                factory.trustAllCertificates();
            }
            botConfig.setSocketFactory(factory);
        }


        this.bot = new NarwhalBot(botConfig.buildConfiguration(), plugin.doesDebugLog());
    }

    public boolean connect() {
        boolean success = true;
        IdentServer.startServer();
        bot.sendIRC().changeNick(nick);

        if (bindAddress != null && bindAddress.length() > 0) {
        }

        try {
            bot.startBot();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error connecting to IRC server " + server + ":" + port, e);
            success = false;
        } catch (IrcException e) {
            plugin.getLogger().log(Level.SEVERE, "Error connecting to IRC server " + server + ":" + port, e);
            success = false;
        } catch (Exception e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                plugin.getLogger().log(Level.SEVERE, "Error connecting to IRC server " + server + ":" + port + ", invalid SSL cert ", e.getCause());
            }
            success = false;
        }

        if (success) {
        } else {
            if (bot.isConnected()) {
                bot.stopBotReconnect();
                bot.getListenerManager().addListener(new ListenerAdapter() {
                    @Override
                    public void onDisconnect(DisconnectEvent event) throws Exception {
                        bot.cleanUp();
                        bot.getListenerManager().removeListener(this);
                    }
                });
                bot.disconnect();
            } else {
                bot.cleanUp();
            }
        }

        return success;
    }

    public void joinChannels() {
        for (Entry<String, Map<?, ?>> entry : rawChannels.entrySet()) {
            ChannelCommandSource source = new ChannelCommandSource(plugin, new MapConfiguration(entry.getValue()),
                    bot.getChannel(entry.getKey()), stripColor);
            try {
                source.load();
            } catch (ConfigurationException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading channel config data!", e);
            }
            if (source.getKey() != null) {
                bot.sendIRC().joinChannel(entry.getKey(), source.getKey());
            } else {
                bot.sendIRC().joinChannel(entry.getKey());
            }
            channelSenders.put(entry.getKey(), source);
            try {
                source.save();
            } catch (ConfigurationException ignore) {
            }
        }
    }

    public void quit(String reason) {
        bot.sendIRC().quitServer(reason);
        senders.clear();
        channelSenders.clear();
    }

    public String getServer() {
        return server;
    }

    public BotCommandSource getSender(String name, String chan) {
        User user = bot.getUserChannelDao().getUser(name);
        Channel channel = chan == null ? null : bot.getUserChannelDao().getChannel(chan);
        return getCommandSource(user, channel);
    }

    public BotCommandSource getCommandSource(User user, Channel channel) {
        Map<String, BotCommandSource> inChannel = NarwhalIRCUtil.getNestedMap(senders,
                channel == null ? null : channel.getName());
        BotCommandSource source = inChannel.get(user.getNick());
        if (source == null) {
            source = new BotCommandSource(plugin, user, channel, stripColor);
            inChannel.put(user.getNick(), source);
        }
        return source;
    }

    public void removeSender(User user, Channel channel) {
        Map<String, BotCommandSource> inChannel = NarwhalIRCUtil.getNestedMap(senders, channel.getName());
        if (inChannel != null) {
            inChannel.remove(user.getNick());
        }
    }

    public void removeSender(User user) {
        for (Channel channel : user.getChannels()) {
            removeSender(user, channel);
        }
    }

    public Collection<ChannelCommandSource> getChannels() {
        return channelSenders.values();
    }

    public ChannelCommandSource getChannel(String name) {
        return channelSenders.get(name);
    }

    /**
     * Called on a command from the bot.
     *
     * @param user The User who sent the command
     * @param channel The Channel this command came from
     * @param rawCmd The arguments for the command
     * @return whether the command could be found.
     */
    public boolean handleCommand(User user, Channel channel, String rawCmd) {
        BotCommandSource source = getCommandSource(user, channel);
        return NarwhalIRCUtil.handleCommand(source, rawCmd, IrcStyleHandler.ID, plugin.getBotCommands());
    }

}
