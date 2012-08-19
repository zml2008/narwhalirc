package com.zachsthings.narwhal.irc.util;

import org.spout.api.util.config.Configuration;
import org.spout.api.util.config.ConfigurationNode;
import org.spout.api.util.config.migration.ConfigurationMigrator;
import org.spout.api.util.config.migration.MigrationAction;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Migrator for the configuration handling old formats.
 */
public class FormatConfigurationMigrator extends ConfigurationMigrator {
    public static final Pattern OLD_PATTERN_MATCHER = Pattern.compile("%([^%]+)%");
    public static final String[] FORMAT_A_BASE = new String[] {"format", "irc-to-server"};
    public static final String[] FORMAT_B_BASE = new String[] {"format", "server-to-irc"};

    public FormatConfigurationMigrator(Configuration config) {
        super(config);
    }

    private static class UpdatePlaceholderPatterns implements MigrationAction {
        public static UpdatePlaceholderPatterns INSTANCE = new UpdatePlaceholderPatterns();

        public String[] convertKey(String[] key) {
            return key;
        }

        public Object convertValue(Object value) {
            if (value instanceof String) {
                return OLD_PATTERN_MATCHER.matcher((String) value).replaceAll("{$1}");
            }
            return value;
        }
    }

    private Map<String[], MigrationAction> actions;
    @Override
    protected Map<String[], MigrationAction> getMigrationActions() {
        if (actions == null) {
            actions = new HashMap<String[], MigrationAction>();
            for (ConfigurationNode bot : getConfiguration().getNode("connections").getChildren().values()) {
                for (ConfigurationNode channel : bot.getNode("channels").getChildren().values()) {
                    ConfigurationNode a = channel.getNode(FORMAT_A_BASE);
                    ConfigurationNode b = channel.getNode(FORMAT_B_BASE);
                    if (a.getString() != null && OLD_PATTERN_MATCHER.matcher(a.getString()).find()) {
                        actions.put(a.getPathElements(), UpdatePlaceholderPatterns.INSTANCE);
                    }
                    if (b.getString() != null && OLD_PATTERN_MATCHER.matcher(b.getString()).find()) {
                        actions.put(b.getPathElements(), UpdatePlaceholderPatterns.INSTANCE);
                    }
                }
            }
        }
        return actions;
    }

    @Override
    protected boolean shouldMigrate() {
        return getMigrationActions().size() > 0;
    }
}
