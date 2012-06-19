package com.zachsthings.narwhal.irc;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.util.yaml.YAMLNode;
import com.zachsthings.libcomponents.bukkit.YAMLNodeConfigurationNode;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.ConfigurationNode;
import com.zachsthings.libcomponents.config.Setting;
import com.zachsthings.libcomponents.config.SettingBase;
import org.bukkit.ChatColor;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;

/**
* @author zml2008
*/
public class BotSession extends ConfigurationBase {
    private final String server;
    private final PircBotX bot;
    private final IrcBridge component;
    private final Map<String, Map<String, BotCommandSender>> senders = new HashMap<String, Map<String, BotCommandSender>>();
    private final Map<String, ChannelCommandSender> channelSenders = new HashMap<String, ChannelCommandSender>();
    private SSlConfiguration ssl;
    @Setting("nick") private String nick = "NarwhalBot";
    @Setting("channels") private Map<String, Map<String, Object>> rawChannels = createDefaultChannels();
    @Setting("port") private String port = "6667";
    @Setting("nickserv-pass") private String nickServPass;
    @Setting("strip-color") private boolean stripColor;
    @Setting("bind-address") private String bindAddress;

    @SettingBase("ssl")
    private static class SSlConfiguration extends ConfigurationBase {
        @Setting("trust-all-certs") public boolean trustAllCerts;
    }

    private static Map<String, Map<String, Object>> createDefaultChannels() {
        Map<String, Map<String, Object>> defChannel = new HashMap<String, Map<String, Object>>();
        CommandBookUtil.getNestedMap(defChannel, "#zml").put("receive-events",
                Arrays.asList("join", "quit", "kick", "message", "me"));
        return defChannel;
    }

    public BotSession(String server, IrcBridge component) {
        this.server = server;
        this.component = component;
        this.bot = new PircBotX();
        bot.getListenerManager().addListener(new NarwhalBotListener(this, component));
        bot.setMessageDelay(250);
        bot.setLogin("Narwhal");
    }

    @Override
    public void load(ConfigurationNode node) {
        ssl = new SSlConfiguration();
        ssl.load(node);
        super.load(node);
    }

    public boolean connect() {
        bot.startIdentServer();
        bot.setName(nick);
        boolean ssl = false;
        String localPort = port;
        if (localPort.startsWith("+")) {
            localPort = localPort.substring(1);
            ssl = true;
        }
        int port = 6667;
        try {
            port = Integer.parseInt(localPort);
        } catch (NumberFormatException ignore) {
        }

        if (bindAddress != null && bindAddress.length() > 0) {
            try {
                bot.setInetAddress(InetAddress.getByName(bindAddress));
            } catch (UnknownHostException e) {
                CommandBook.logger().warning("NarwhalIRC: Error setting bind address: " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            if (ssl) {
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
        for (Entry<String, Map<String, Object>> entry : rawChannels.entrySet()) {
            ChannelCommandSender sender = new ChannelCommandSender(bot.getChannel(entry.getKey()), stripColor);
            sender.load(new YAMLNodeConfigurationNode(new YAMLNode(entry.getValue(), true)));
            if (sender.getKey() != null) {
                bot.joinChannel(entry.getKey(), sender.getKey());
            } else {
                bot.joinChannel(entry.getKey());
            }
            channelSenders.put(entry.getKey(), sender);
        }
    }

    public void quit(String reason, boolean shutdown) {
        bot.quitServer(reason);
        senders.clear();
        channelSenders.clear();
        if (shutdown) {
            bot.shutdown();
        }
    }

    public String getServer() {
        return server;
    }

    public BotCommandSender getSender(String name, String chan) {
        User user = bot.getUser(name);
        Channel channel = chan == null ? null : bot.getChannel(chan);
        return getSender(user, channel);
    }

    public BotCommandSender getSender(User user, Channel channel) {
        Map<String, BotCommandSender> inChannel = CommandBookUtil.getNestedMap(senders,
                channel == null ? null : channel.getName());
        BotCommandSender sender = inChannel.get(user.getNick());
        if (sender == null) {
            sender = new BotCommandSender(user, channel, stripColor);
            inChannel.put(user.getNick(), sender);
        }
        return sender;
    }

    public void removeSender(User user, Channel channel) {
        Map<String, BotCommandSender> inChannel = CommandBookUtil.getNestedMap(senders, channel.getName());
        if (inChannel != null) {
            inChannel.remove(user.getNick());
        }
    }

    public void removeSender(User user) {
        for (Channel channel : user.getChannels()) {
            removeSender(user, channel);
        }
    }

    public Collection<ChannelCommandSender> getChannels() {
        return channelSenders.values();
    }

    public ChannelCommandSender getChannel(String name) {
        return channelSenders.get(name);
    }

    /**
     * Called on a command from the bot.
     *
     * @param user The User who sent the command
     * @param source The Channel this command came from
     * @param args The arguments for the command
     * @return whether the command could be found.
     */
    public boolean handleCommand(User user, Channel source, String[] args) {
        BotCommandSender sender = getSender(user, source);
        if (!component.getBotCommandsManager().hasCommand(args[0])) return false;
        try {
            component.getBotCommandsManager().execute(args, sender, sender, user.getBot(), source);
        } catch (CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

}
