package com.zachsthings.narwhal.irc;

import org.pircbotx.Channel;
import org.spout.api.Spout;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.command.CommandContext;
import org.spout.api.command.CommandSource;
import org.spout.api.command.RootCommand;
import org.spout.api.command.annotated.*;
import org.spout.api.exception.CommandException;
import org.spout.api.exception.ConfigurationException;
import org.spout.api.permissions.DefaultPermissions;
import org.spout.api.plugin.CommonPlugin;
import org.spout.api.util.config.MapConfiguration;
import org.spout.api.util.config.annotated.AnnotatedConfiguration;
import org.spout.api.util.config.annotated.Setting;
import org.spout.api.util.config.yaml.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author zml2008
 */
public class NarwhalIRCPlugin extends CommonPlugin {

    /**
     * This is the permission required for users to receive messages sent from IRC
     */
    public static String IRC_BROADCAST_PERMISSION = "narwhal.irc.broadcast";

    /**
     * A Set of permissions that no bot has
     */
    public static final Set<String> BLACKLISTED_BOT_PERMS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    IRC_BROADCAST_PERMISSION)));

    private LocalConfiguration config;

    private final Map<String, BotSession> bots = new ConcurrentHashMap<String, BotSession>();


    /**
     * The commands available for bots.
     */
    private RootCommand botCommands = new RootCommand(Spout.getEngine());

    /**
     * The AnnotatedCommandRegistrationFactory
     */
    private final AnnotatedCommandRegistrationFactory commandRegistration =
            new AnnotatedCommandRegistrationFactory(new SimpleInjector(this),
            new SimpleAnnotatedCommandExecutorFactory());

    @Override
    public void onEnable() {
        try {
            loadConfig();
        } catch (ConfigurationException e) {
            getLogger().log(Level.SEVERE, "Unable to load configuration for plugin: " + e.getMessage(), e);
        }
        DefaultPermissions.addDefaultPermission(IRC_BROADCAST_PERMISSION);
        Spout.getEngine().getRootCommand().addSubCommands(this, Commands.class, commandRegistration);
        Spout.getEventManager().registerEvents(new NarwhalServerListener(this), this);
        botCommands.addSubCommands(this, BasicBotCommands.class, commandRegistration);
    }


    @Override
    public void onReload() {
        super.onReload();
        for (BotSession bot : bots.values()) {
            bot.quit("Reloading NarwhalIRC!", true);
        }
        bots.clear();
        try {
            loadConfig();
        } catch (ConfigurationException e) {
            getLogger().log(Level.SEVERE, "Unable to load configuration for plugin: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisable() {
        for (BotSession bot : bots.values()) {
            bot.quit("Disabling", true);
        }
        bots.clear();
    }

    private class LocalConfiguration extends AnnotatedConfiguration {
        @Setting("command-prefix") public String commandPrefix = ".";
        @Setting("connections") public Map<String, Map<?, ?>> serverMap = createServerMap();

        public LocalConfiguration(File file) {
            super(new YamlConfiguration(file));
        }

        private Map<String, Map<?, ?>> createServerMap() {
            Map<String, Map<?, ?>> ret = new HashMap<String, Map<?, ?>>();
            Map<String, Object> defServer = new HashMap<String, Object>();
            defServer.put("port", "6667");
            ret.put("zachsthings.com", defServer);
            return ret;
        }
    }

    protected void loadConfig() throws ConfigurationException{
        config = new LocalConfiguration(new File(getDataFolder(), "config.yml"));
        config.load();
        for (Map.Entry<String, Map<?, ?>> entry : config.serverMap.entrySet()) {
            BotSession bot = new BotSession(new MapConfiguration(entry.getValue()), entry.getKey(), this);
            bot.load();
            bot.connect();
            bot.joinChannels();
            bots.put(entry.getKey(), bot);
            bot.save();
        }
        config.save();
    }

    /**
     * Broadcast a message to all bots that receive the given {@link PassedEvent}
     * @param type The type of message being passed
     * @param message The message to broadcast
     */
    public void broadcastBotMessage(PassedEvent type, Object... message) {
        for (BotSession bot : bots.values()) {
			for (ChannelCommandSource chan : bot.getChannels()) {
				if (chan.receivesEvent(type)) {
					chan.sendMessage(message);
				}
			}
        }
    }

    public RootCommand getBotCommands() {
        return botCommands;
    }

    public String getCommandPrefix() {
        return config.commandPrefix;
    }

    public BotSession getBot(String server) {
        return bots.get(server);
    }

    public Collection<BotSession> getBots() {
        return Collections.unmodifiableCollection(bots.values());
    }

    public class Commands {
        @Command(aliases = "irc", desc = "Commands related to NarwhalIRC")
        @NestedCommand(IrcCommands.class)
        public void irc() {}

    }

    public class IrcCommands {
        @Command(aliases = {"msg", "tell", "message"}, desc = "Send a message to a user or channel in IRC", usage = "<server>:<user> <message>", min = 2, max = -1)
        @CommandPermissions("narwhal.irc.msg")
        public void msg(CommandContext args, CommandSource sender) throws CommandException {
            CommandSource target;
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
            target.sendMessage(args.getJoinedString(1));
        }

        @Command(aliases = {"channels", "chans"}, desc = "Lists all the channels this server is connected to")
        @CommandPermissions("narwhal.irc.channels")
        public void channels(CommandContext args, CommandSource sender) throws CommandException {
            for (BotSession bot : bots.values()) {
                List<Object> builder = new ArrayList<Object>();
                builder.add(ChatStyle.BLUE);
                builder.add(bot.getServer());
                builder.add(": ");
                for (Iterator<ChannelCommandSource> i = bot.getChannels().iterator(); i.hasNext(); ) {
                    final Channel chan = i.next().getChannel();
                    builder.add(chan.getChannelKey() == null ? "" : "+");
                    builder.add(chan.getName());
                    if (i.hasNext()) {
                        builder.add(", ");
                    }
                }
                sender.sendMessage(builder);
            }
        }
    }

    public static <T, K, V> Map<K, V> getNestedMap(Map<T, Map<K, V>> collection, T key) {
        Map<K, V> map = collection.get(key);
        if (map == null) {
            map = new HashMap<K, V>();
            collection.put(key, map);
        }
        return map;
    }

}
