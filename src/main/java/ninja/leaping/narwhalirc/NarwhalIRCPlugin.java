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

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.narwhalirc.util.ChatTemplateSerializer;
import ninja.leaping.narwhalirc.util.FormatConfigurationMigrator;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.ServiceReference;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.event.Subscribe;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main class for NarwhalIRC
 */
@Plugin(name = PomData.NAME, version = PomData.VERSION, id=PomData.ARTIFACT_ID)
public class NarwhalIRCPlugin {
    /**
     * This is the permission required for users to receive messages sent from IRC
     */
    public static final String IRC_BROADCAST_PERMISSION = "narwhal.irc.broadcast";
    //public static final ChatChannel IRC_BROADCAST_CHANNEL = new PermissionChatChannel("NarwhalIRC", "narwhal.irc.broadcast");
    /**
     * A Set of permissions that no bot has
     */
    private LocalConfiguration config;
    private final Map<String, BotSession> bots = new ConcurrentHashMap<String, BotSession>();
    /**
     * The commands available for bots.
     */
    private RootCommand botCommands;
    @Inject
    private Game game;
    @Inject @DefaultConfig(sharedRoot = true) private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    private ServiceReference<PermissionService> perms;

    @Subscribe
    public void onLoad(PreInitializationEvent event) {
        perms.executeWhenPresent(new Predicate<PermissionService>() {
            @Override
            public boolean apply(PermissionService permissionService) {
                permissionService.getDefaultData().setPermission(SubjectData.GLOBAL_CONTEXT, IRC_BROADCAST_PERMISSION, Tristate.TRUE);
                return true;
            }
        });
    }

    @Subscribe
    public void onEnable(InitializationEvent event) {
        if (!game.getServer().isPresent()) {
            throw new IllegalStateException("NarwhalIRC can only run on servers!");
        }

        this.server = (Server) getEngine();

        botCommands = new RootCommand(getEngine());
        TypeSerializers.registerSerializer(new ChatTemplateSerializer());
        try {
            loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to load configuration for plugin: " + e.getMessage(), e);
        }

        getEngine().getRootCommand().addSubCommands(this, IRCCommands.class, commandRegistration);
        getEngine().getEventManager().registerEvents(new NarwhalServerListener(this), this);
        botCommands.addSubCommands(this, BasicBotCommands.class, commandRegistration);
    }

    public void reload() {
        for (BotSession bot : bots.values()) {
            bot.quit("Reloading NarwhalIRC!");
        }
        bots.clear();
        try {
            loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to load configuration for plugin: " + e.getMessage(), e);
        }
    }

    @Subscribe
    public void onDisable(ServerStoppingEvent event) {
        for (BotSession bot : bots.values()) {
            bot.quit("Disabling");
        }
        bots.clear();
    }

    public Game getGame() {
        return game;
    }

    private class LocalConfiguration {
        @Setting("command-prefix")
        public String commandPrefix = ".";
        @Setting("debug-log")
        public boolean debugLog = false;
        @Setting("channel-commands")
        public boolean channelCommands = true;
        @Setting("private-commands")
        public boolean privateCommands = true;
        @Setting("connections")
        public Map<String, BotSession> serverMap = createServerMap();

        private Map<String, Map<?, ?>> createServerMap() {
            Map<String, Map<?, ?>> ret = new HashMap<String, Map<?, ?>>();
            Map<String, Object> defServer = new HashMap<String, Object>();
            defServer.put("port", "6667");
            ret.put("irc.leaping.ninja", defServer);
            return ret;
        }
    }

    protected void loadConfig() throws IOError {
        config = configLoader.load();
        ConfigurationTransformation migrator = ConfigurationMigrators.format();
        migrator.apply(config);
        for (Map.Entry<String, Map<?, ?>> entry : config.serverMap.entrySet()) {
            BotSession bot = new BotSession(new MapConfiguration(entry.getValue()), entry.getKey(), this);
            bot.load();
            if (bot.connect()) {
                bot.joinChannels();
            } else {
                bot.save();
                continue;
            }
            bots.put(entry.getKey(), bot);
            bot.save();
        }
        configLoader.save(config);
    }

    /**
     * Broadcast a message to all bots that receive the given {@link PassedEvent}
     *
     * @param type    The type of message being passed
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

    public boolean doesDebugLog() {
        return config.debugLog;
    }

    public boolean doesChannelCommands() {
        return config.channelCommands;
    }

    public boolean doesPrivateCommands() {
        return config.privateCommands;
    }

    public BotSession getBot(String server) {
        return bots.get(server);
    }

    public Collection<BotSession> getBots() {
        return Collections.unmodifiableCollection(bots.values());
    }
}
