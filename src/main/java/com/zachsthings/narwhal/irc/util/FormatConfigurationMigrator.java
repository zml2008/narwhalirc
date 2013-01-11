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
