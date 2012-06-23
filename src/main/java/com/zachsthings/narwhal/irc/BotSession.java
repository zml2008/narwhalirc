package com.zachsthings.narwhal.irc;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;
import org.spout.api.ChatColor;
import org.spout.api.exception.CommandException;
import org.spout.api.exception.CommandUsageException;
import org.spout.api.exception.ConfigurationException;
import org.spout.api.exception.WrappedCommandException;
import org.spout.api.util.config.Configuration;
import org.spout.api.util.config.MapConfiguration;
import org.spout.api.util.config.annotated.AnnotatedConfiguration;
import org.spout.api.util.config.annotated.Setting;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import static com.zachsthings.narwhal.irc.NarwhalIRCPlugin.getNestedMap;

/**
* @author zml2008
*/
public class BotSession extends AnnotatedConfiguration {
    private final String server;
    private final PircBotX bot;
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

    private static class SSlConfiguration extends AnnotatedConfiguration {
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
        this.bot = new PircBotX();
        bot.getListenerManager().addListener(new NarwhalBotListener(this, plugin));
        bot.setMessageDelay(250);
        bot.setLogin("Narwhal");
        bot.useShutdownHook(true);
    }

    public boolean connect() {
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
                bot.connect(server, port, factory);
            } else {
                bot.connect(server, port);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (IrcException e) {
            e.printStackTrace();
            return false;
        }
        if (this.nickServPass != null && this.nickServPass.length() > 0) {
            //bot.sendMessage(bot.getUser("NickServ"), "IDENTIFY " + bot.getName() + " " + this.nickServPass);
            bot.identify(nickServPass);
        }
        return true;
    }

    public void joinChannels() {
        for (Entry<String, Map<?, ?>> entry : rawChannels.entrySet()) {
            ChannelCommandSource source = new ChannelCommandSource(new MapConfiguration(entry.getValue()),
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

    public void quit(String reason) {
        if (bot.isConnected()) {
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
            source = new BotCommandSource(user, channel, stripColor);
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
     * @param args The arguments for the command
     * @return whether the command could be found.
     */
    public boolean handleCommand(User user, Channel channel, String[] args) {
        BotCommandSource source = getCommandSource(user, channel);
        if (!plugin.getBotCommands().hasChild(args[0])) return false;
        try {
            plugin.getBotCommands().execute(source, args, -1, false);
        } catch (CommandUsageException e) {
            source.sendMessage(ChatColor.RED + e.getMessage());
            source.sendMessage(ChatColor.RED + e.getUsage());
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                source.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                source.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (CommandException e) {
            source.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

}
