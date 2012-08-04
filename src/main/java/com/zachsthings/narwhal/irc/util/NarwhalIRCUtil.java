package com.zachsthings.narwhal.irc.util;

import org.spout.api.Spout;
import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.ChatTemplate;
import org.spout.api.chat.Placeholder;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.chat.style.fallback.DefaultStyleHandler;
import org.spout.api.command.Command;
import org.spout.api.command.CommandSource;
import org.spout.api.command.RootCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for NarwhalIRC
 */
public class NarwhalIRCUtil {
    public static boolean handleCommand(CommandSource source, String rawArgs, int styleHandlerId) {
        return handleCommand(source, rawArgs, styleHandlerId, Spout.getEngine().getRootCommand());
    }
    public static boolean handleCommand(CommandSource source, String rawArgs, int styleHandlerId, RootCommand root) {
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

    public static boolean handleCommand(CommandSource source, String cmdName, ChatArguments args, RootCommand root) {
        Command cmd = root.getChild(cmdName);
        if (cmd != null) {
            cmd.process(source, cmdName, args, false);
            return true;
        } else {
            source.sendMessage(ChatStyle.RED, "Unknown command: ", cmdName);
            return false;
        }
    }

    public static ChatTemplate fromString(String pattern) {
        return fromString(pattern, DefaultStyleHandler.ID);
    }

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("([^\\{]+)(\\{[^{].*\\})?");
    public static ChatTemplate fromString(String pattern, int styleHandlerId) {
        ChatArguments args = new ChatArguments();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(pattern);
        while (matcher.find()) {
            args.append(ChatArguments.fromString(matcher.group(1), styleHandlerId));
            if (matcher.group(2) != null) {
                args.append(new Placeholder(matcher.group(2).toUpperCase()));
            }
        }
        return new ChatTemplate(args);
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
