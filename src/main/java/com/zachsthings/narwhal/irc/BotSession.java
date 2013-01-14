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
package com.zachsthings.narwhal.irc;

import com.zachsthings.narwhal.irc.chatstyle.IrcStyleHandler;
import com.zachsthings.narwhal.irc.util.NarwhalIRCUtil;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.spout.api.exception.ConfigurationException;
import org.spout.api.util.config.Configuration;
import org.spout.api.util.config.MapConfiguration;
import org.spout.api.util.config.annotated.AnnotatedSubclassConfiguration;
import org.spout.api.util.config.annotated.Setting;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import static com.zachsthings.narwhal.irc.util.NarwhalIRCUtil.getNestedMap;

/**
* @author zml2008
*/
public class BotSession extends AnnotatedSubclassConfiguration {
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

    private static class SSlConfiguration extends AnnotatedSubclassConfiguration {
        @Setting("trust-all-certs") public boolean trustAllCerts;
        @Setting("enabled") public boolean enabled;

        public SSlConfiguration(Configuration config) {
            super(config);
        }
    }

    private static Map<String, Map<?, ?>> createDefaultChannels() {
        Map<String, Map<?, ?>> defChannel = new HashMap<String, Map<?, ?>>();
        Map<String, Object> child = new HashMap<String, Object>();
        child.put("receive-events", Arrays.asList(PassedEvent.values()));
        defChannel.put("#zml", child);
        return defChannel;
    }

    public BotSession(Configuration config, String server, NarwhalIRCPlugin plugin) {
        super(config);
        this.server = server;
        this.plugin = plugin;
        this.bot = new NarwhalBot(plugin.doesDebugLog());
        bot.getListenerManager().addListener(new NarwhalBotListener(this, plugin));
        bot.setMessageDelay(250);
        bot.setLogin("Narwhal");
    }

    public boolean connect() {
        boolean success = true;
        bot.startIdentServer();
        bot.setName(nick);

        if (bindAddress != null && bindAddress.length() > 0) {
            try {
                bot.setInetAddress(InetAddress.getByName(bindAddress));
            } catch (UnknownHostException e) {
                plugin.getLogger().log(Level.WARNING, "Error setting bind address: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }

        try {
            if (ssl.enabled) {
                UtilSSLSocketFactory factory = new UtilSSLSocketFactory();
                if (this.ssl.trustAllCerts) {
                    factory.trustAllCertificates();
                }
                bot.connect(server, port, password, factory);
            } else {
                bot.connect(server, port, password);
            }
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
            if (this.nickServPass != null && this.nickServPass.length() > 0) {
                //bot.sendMessage(bot.getUser("NickServ"), "IDENTIFY " + bot.getName() + " " + this.nickServPass);
                bot.identify(nickServPass);
            }
        } else {
            if (bot.isConnected()) {
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
                bot.joinChannel(entry.getKey(), source.getKey());
            } else {
                bot.joinChannel(entry.getKey());
            }
            channelSenders.put(entry.getKey(), source);
            try {
                source.save();
            } catch (ConfigurationException ignore) {
            }
        }
    }

    public void quit(String reason, boolean shutdown) {
        if (shutdown) {
            bot.shutdown(reason);
        } else {
            bot.quitServer(reason);
        }
        senders.clear();
        channelSenders.clear();
    }

    public String getServer() {
        return server;
    }

    public BotCommandSource getSender(String name, String chan) {
        User user = bot.getUser(name);
        Channel channel = chan == null ? null : bot.getChannel(chan);
        return getCommandSource(user, channel);
    }

    public BotCommandSource getCommandSource(User user, Channel channel) {
        Map<String, BotCommandSource> inChannel = getNestedMap(senders,
                channel == null ? null : channel.getName());
        BotCommandSource source = inChannel.get(user.getNick());
        if (source == null) {
            source = new BotCommandSource(plugin, user, channel, stripColor);
            inChannel.put(user.getNick(), source);
        }
        return source;
    }

    public void removeSender(User user, Channel channel) {
        Map<String, BotCommandSource> inChannel = getNestedMap(senders, channel.getName());
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
