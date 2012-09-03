package com.zachsthings.narwhal.irc;

import com.zachsthings.narwhal.irc.util.ChatTemplateSerializer;
import com.zachsthings.narwhal.irc.util.FormatConfigurationMigrator;
import org.pircbotx.Channel;
import org.spout.api.Server;
import org.spout.api.chat.ChatArguments;
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
import org.spout.api.util.config.migration.MigrationException;
import org.spout.api.util.config.serialization.Serialization;
import org.spout.api.util.config.yaml.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main class for NarwhalIRC
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
                    IRC_BROADCAST_PERMISSION,
                    "spout.chat.receive.*",
                    "-spout.chat.receive.console")));

    private LocalConfiguration config;

    private final Map<String, BotSession> bots = new ConcurrentHashMap<String, BotSession>();


    /**
     * The commands available for bots.
     */
    private RootCommand botCommands;

    private Server server;

    /**
     * The AnnotatedCommandRegistrationFactory
     */
    private final AnnotatedCommandRegistrationFactory commandRegistration =
            new AnnotatedCommandRegistrationFactory(new SimpleInjector(this));

    @Override
    public void onEnable() {
        if (!(getEngine() instanceof Server)) {
            throw new IllegalStateException("NarwhalIRC can only run on servers!");
        }
        this.server = (Server) getEngine();

        botCommands = new RootCommand(getEngine());
        config = new LocalConfiguration(new File(getDataFolder(), "config.yml"));
        Serialization.registerSerializer(new ChatTemplateSerializer());
        try {
            loadConfig();
        } catch (ConfigurationException e) {
            getLogger().log(Level.SEVERE, "Unable to load configuration for plugin: " + e.getMessage(), e);
        }
        DefaultPermissions.addDefaultPermission(IRC_BROADCAST_PERMISSION);
        getEngine().getRootCommand().addSubCommands(this, IRCCommands.class, commandRegistration);
        getEngine().getEventManager().registerEvents(new NarwhalServerListener(this), this);
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

    public Server getServer() {
        return server;
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

    protected void loadConfig() throws ConfigurationException {
        config.load();
        FormatConfigurationMigrator migrator = new FormatConfigurationMigrator(config);
        try {
            migrator.migrate();
        } catch (MigrationException e) {
            getLogger().log(Level.SEVERE, "Could not migrate NarwhalIRC configuration to new format", e);
            return;
        }
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
    public void broadcastBotMessage(PassedEvent type, ChatArguments message) {
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
}
