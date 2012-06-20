package com.zachsthings.narwhal.irc;

import org.bukkit.ChatColor;
import org.pircbotx.Colors;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class exists to aid in the conversion of color codes from mIRC colors to
 * Minecraft ChatColors. Mapping is not perfect, but most colors should come across fairly well.
 */
public class IrcColor {
    private static final Pattern MC_CHAT_COLOR_REGEX = Pattern.compile("(?i)\u00A7([A-FK0-9])");
    private static Map<ChatColor, String> colorMapping = new HashMap<ChatColor, String>(ChatColor.values().length);
    private static Map<String, ChatColor> reverseMapping = new HashMap<String, ChatColor>(colorMapping.size());
    static {
        colorMapping.put(ChatColor.BLACK, Colors.BLACK);
        colorMapping.put(ChatColor.DARK_BLUE, Colors.DARK_BLUE);
        colorMapping.put(ChatColor.DARK_GREEN, Colors.DARK_GREEN);
        colorMapping.put(ChatColor.DARK_AQUA, Colors.BOLD + Colors.TEAL);
        colorMapping.put(ChatColor.DARK_RED, Colors.BROWN);
        colorMapping.put(ChatColor.DARK_PURPLE, Colors.PURPLE);
        colorMapping.put(ChatColor.GOLD, Colors.BOLD + Colors.OLIVE);
        colorMapping.put(ChatColor.GRAY, Colors.LIGHT_GRAY);
        colorMapping.put(ChatColor.DARK_GRAY, Colors.DARK_GRAY);
        colorMapping.put(ChatColor.BLUE, Colors.BLUE);
        colorMapping.put(ChatColor.GREEN, Colors.GREEN);
        colorMapping.put(ChatColor.AQUA, Colors.TEAL);
        colorMapping.put(ChatColor.RED, Colors.RED);
        colorMapping.put(ChatColor.LIGHT_PURPLE, Colors.MAGENTA);
        colorMapping.put(ChatColor.YELLOW, Colors.OLIVE);
        colorMapping.put(ChatColor.WHITE, Colors.NORMAL);
        for (Map.Entry<ChatColor, String> entry : colorMapping.entrySet()) {
            reverseMapping.put(entry.getValue(), entry.getKey());
        }
        reverseMapping.put(Colors.YELLOW, ChatColor.YELLOW);
        reverseMapping.put(Colors.TEAL, ChatColor.DARK_AQUA);
        reverseMapping.put(Colors.CYAN, ChatColor.AQUA);
    }

    public static String replaceColor(String orig, boolean fromIrc) {

        if (fromIrc) {
            String result = orig;
            for (Map.Entry<String, ChatColor> entry : reverseMapping.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue().toString());
            }
            result = result.replace("\3", ChatColor.WHITE.toString());
            result = Colors.removeFormatting(result);
            return result;
        } else {
            Matcher matcher = MC_CHAT_COLOR_REGEX.matcher(orig);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                ChatColor color = ChatColor.getByChar(matcher.group(1).toLowerCase());
                if (color != null) {
                    matcher.appendReplacement(result, colorMapping.get(color));
                } else {
                    matcher.appendReplacement(result, matcher.group());
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }
}
