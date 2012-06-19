package com.zachsthings.narwhal.irc;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.util.yaml.YAMLNode;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.bukkit.YAMLNodeConfigurationNode;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.pircbotx.Channel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * @author zml2008
 */
@ComponentInformation(friendlyName = "NarwhalIRC",
        desc = "A lightweight irc bridge to connect a Minecraft server to one or more IRC channels")
public class IrcBridge extends BukkitComponent implements Listener {
    private LocalConfiguration config;

    /**
     * This is the permission required for users to receive messages sent from IRC
     */
    public static String IRC_BROADCAST_PERMISSION = "narwhal.irc.broadcast";

    private final Map<String, BotSession> bots = new ConcurrentHashMap<String, BotSession>();

    /**
     * The commands available for bots.
     */
    private CommandsManager<CommandSender> botCommands = new CommandsManager<CommandSender>() {
        @Override
        public boolean hasPermission(CommandSender sender, String permission) {
            return true;
        }
    };

    @Override
    public void enable() {
        loadConfig();
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
        botCommands.register(BasicBotCommnds.class);
        if (Bukkit.getServer().getPluginManager().getPermission(IRC_BROADCAST_PERMISSION) == null) {
            Bukkit.getServer().getPluginManager().addPermission(new Permission(IRC_BROADCAST_PERMISSION, "Allows a user to hear messages sent from IRC", PermissionDefault.TRUE));
        }
    }

    @Override
    public void reload() {
        super.reload();
        for (BotSession bot : bots.values()) {
            bot.quit("Reloading the component!", true);
        }
        bots.clear();
        loadConfig();
    }

    @Override
    public void disable() {
        for (BotSession bot : bots.values()) {
            bot.quit("Disabling", true);
        }
        bots.clear();
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("command-prefix") public String commandPrefix = ".";
        @Setting("connections") public Map<String, Map<String, Object>> serverMap = createServerMap();

        private Map<String, Map<String, Object>> createServerMap() {
            Map<String, Map<String, Object>> ret = new HashMap<String, Map<String, Object>>();
            Map<String, Object> defServer = new HashMap<String, Object>();
            defServer.put("port", "6667");
            ret.put("zachsthings.com", defServer);
            return ret;
        }
    }

    protected void loadConfig() {
        config = configure(new LocalConfiguration());
        for (Map.Entry<String, Map<String, Object>> entry : config.serverMap.entrySet()) {
            BotSession bot = new BotSession(entry.getKey(), this);
            bot.load(new YAMLNodeConfigurationNode(new YAMLNode(entry.getValue(), true)));
            bots.put(entry.getKey(), bot);
            bot.connect();
            bot.joinChannels();
        }


    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for (BotSession bot : bots.values()) {
            for (ChannelCommandSender chan : bot.getChannels()) {
                if (chan.receivesEvent("message")) {
                    chan.sendMessage(chan.getServerToIrcFormat().
                            replaceAll("%event%", Matcher.quoteReplacement(String.format(event.getFormat(),
                                    event.getPlayer().getDisplayName(), event.getMessage())))
                            .replaceAll("%name%", Matcher.quoteReplacement(event.getPlayer().getDisplayName()))
                            .replaceAll("%msg%", Matcher.quoteReplacement(event.getMessage())));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (event.getJoinMessage() == null) {
            return;
        }
        broadcastBotMessage("join", "[" + event.getJoinMessage() + ChatColor.WHITE + "]");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (event.getQuitMessage() == null) {
            return;
        }

        broadcastBotMessage("quit", "[" + event.getQuitMessage() + ChatColor.WHITE + "]");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        if (event.isCancelled() || event.getLeaveMessage() == null) {
            return;
        }
        broadcastBotMessage("kick", "[" + event.getLeaveMessage() + ChatColor.WHITE + "]");
    }

    public void broadcastBotMessage(String type, String message) {
        for (BotSession bot : bots.values()) {
			for (ChannelCommandSender chan : bot.getChannels()) {
				if (chan.receivesEvent(type)) {
					chan.sendMessage(message);
				}
			}
        }
    }

    public CommandsManager<CommandSender> getBotCommandsManager() {
        return botCommands;
    }

    public String getCommandPrefix() {
        return config.commandPrefix;
    }

    public BotSession getBot(String server) {
        return bots.get(server);
    }

    public class Commands {
        @Command(aliases = "irc", desc = "Commands related to NarwhalIRC")
        @NestedCommand(IrcCommands.class)
        public void irc() {}

    }

    public class IrcCommands {
        @Command(aliases = {"msg", "tell", "message"}, desc = "Send a message to a user or channel in IRC", usage = "<server>:<user> <message>", min = 2, max = -1)
        @CommandPermissions("narwhal.irc.msg")
        public void msg(CommandContext args, CommandSender sender) throws CommandException {
            CommandSender target;
            String[] split = args.getString(0).split(":", 2);
            if (split.length < 2) {
                throw new CommandException("A server must be specified for this command");
            }

            BotSession bot = getBot(split[0]);
            if (bot == null) {
                throw new CommandException("No bot for server '" + split[0] + "'!");
            }

            if (split[1].startsWith("#")) {
               target = bot.getChannel(split[1]);
            } else {
                target = bot.getSender(split[1], null);
            }
            target.sendMessage(args.getJoinedStrings(1));
        }

        @Command(aliases = {"channels", "chans"}, desc = "Lists all the channels this server is connected to")
        @CommandPermissions("narwhal.irc.channels")
        public void channels(CommandContext args, CommandSender sender) throws CommandException {
            for (BotSession bot : bots.values()) {
                StringBuilder builder = new StringBuilder();
                builder.append(bot.getServer()).append(": ");
                for (Iterator<ChannelCommandSender> i = bot.getChannels().iterator(); i.hasNext(); ) {
                    final Channel chan = i.next().getChannel();
                    builder.append((chan.getChannelKey() == null ? "" : "+") + chan.getName());
                    if (i.hasNext()) {
                        builder.append(", ");
                    }
                }
                sender.sendMessage(ChatColor.BLUE + builder.toString());
            }
        }
    }

}
