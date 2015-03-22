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
package ninja.leaping.narwhalirc.util;

import org.spongepowered.api.util.command.CommandSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for NarwhalIRC
 */
public class NarwhalIRCUtil {

    public static boolean handleCommand(CommandSource source, String rawArgs, int styleHandlerId) {
        return handleCommand(source, rawArgs, styleHandlerId, Spout.getEngine().getRootCommand());
    }
    public static boolean handleCommand(CommandSource source, String rawArgs, int styleHandlerId, CommandDispatcher root) {
        String cmd;
        ChatArguments arguments;
        int spaceIndex = rawArgs.indexOf(" ");
        if (spaceIndex == -1) {
            cmd = rawArgs;
            arguments = new ChatArguments();
        } else {
            cmd = rawArgs.substring(0, spaceIndex);
            arguments = ChatArguments.fromString(rawArgs.substring(spaceIndex + 1), styleHandlerId);
        }
        return handleCommand(source, cmd, arguments, root);
    }

    public static boolean handleCommand(CommandSource source, String cmdName, ChatArguments args) {
        return handleCommand(source, cmdName, args, Spout.getEngine().getRootCommand());
    }

    public static boolean handleCommand(CommandSource source, String cmdName, String args, RootCommand root) {
        Command cmd = root.getChild(cmdName);
        if (cmd != null) {
            cmd.process(source, cmdName, args, false);
            return true;
        } else {
            source.sendMessage(ChatStyle.RED, "Unknown command: ", cmdName);
            return false;
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
