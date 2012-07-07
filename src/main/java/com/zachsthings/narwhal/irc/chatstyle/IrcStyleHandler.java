package com.zachsthings.narwhal.irc.chatstyle;

import org.pircbotx.Colors;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.chat.style.StyleFormatter;
import org.spout.api.chat.style.StyleHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zml2008
 */
public class IrcStyleHandler extends StyleHandler {
    public static final IrcStyleHandler INSTANCE = new IrcStyleHandler();
    private final Map<String, ChatStyle> reverseMapping = new HashMap<String, ChatStyle>();
    public static final int ID = register(INSTANCE);

    public IrcStyleHandler() {
        registerFormatter(ChatStyle.BLACK, new PrefixStyleFormatter(Colors.BLACK));
        registerFormatter(ChatStyle.DARK_BLUE, new PrefixStyleFormatter(Colors.DARK_BLUE));
        registerFormatter(ChatStyle.DARK_GREEN, new PrefixStyleFormatter(Colors.DARK_GREEN));
        registerFormatter(ChatStyle.DARK_CYAN, new PrefixStyleFormatter(Colors.BOLD + Colors.TEAL));
        registerFormatter(ChatStyle.DARK_RED, new PrefixStyleFormatter(Colors.BROWN));
        registerFormatter(ChatStyle.PURPLE, new PrefixStyleFormatter(Colors.PURPLE));
        registerFormatter(ChatStyle.GOLD, new PrefixStyleFormatter(Colors.BOLD + Colors.OLIVE));
        registerFormatter(ChatStyle.GRAY, new PrefixStyleFormatter(Colors.LIGHT_GRAY));
        registerFormatter(ChatStyle.DARK_GRAY, new PrefixStyleFormatter(Colors.DARK_GRAY));
        registerFormatter(ChatStyle.BLUE, new PrefixStyleFormatter(Colors.BLUE));
        registerFormatter(ChatStyle.BRIGHT_GREEN, new PrefixStyleFormatter(Colors.GREEN));
        registerFormatter(ChatStyle.CYAN, new PrefixStyleFormatter(Colors.TEAL));
        registerFormatter(ChatStyle.RED, new PrefixStyleFormatter(Colors.RED));
        registerFormatter(ChatStyle.PINK, new PrefixStyleFormatter(Colors.MAGENTA));
        registerFormatter(ChatStyle.YELLOW, new PrefixStyleFormatter(Colors.OLIVE));
        registerFormatter(ChatStyle.WHITE, new PrefixStyleFormatter(Colors.WHITE));
        registerFormatter(ChatStyle.BOLD, new PrefixStyleFormatter(Colors.BOLD));
        registerFormatter(ChatStyle.RESET, new PrefixStyleFormatter(Colors.NORMAL));
        registerFormatter(ChatStyle.UNDERLINE, new PrefixStyleFormatter(Colors.UNDERLINE));

        reverseMapping.put(Colors.YELLOW, ChatStyle.YELLOW);
        reverseMapping.put(Colors.OLIVE, ChatStyle.GOLD);
        reverseMapping.put(Colors.TEAL, ChatStyle.DARK_CYAN);
        reverseMapping.put(Colors.CYAN, ChatStyle.CYAN);
    }

    @Override
    protected void registerFormatter(ChatStyle style, StyleFormatter formatter) {
        if (formatter instanceof PrefixStyleFormatter) {
            reverseMapping.put(((PrefixStyleFormatter) formatter).getPrefix(), style);
        }
        super.registerFormatter(style, formatter);
    }

    private static final Pattern IRC_COLOR_PATTERN = Pattern.compile("\u0003(\\d\\d)");

    public Object[] fromString(String message) {
        List<Object> items = new ArrayList<Object>();
        addItems(items,  message);
        return items.toArray();
    }

    private void addItems(List<Object> items, String message) {
        int index;
        while ((index = message.indexOf(Colors.BOLD)) != -1) {
            String section = message.substring(0, index);
            addItems(items, section);
            items.add(ChatStyle.BOLD);
            message = message.substring(index);
        }
        while ((index = message.indexOf(Colors.NORMAL)) != -1) {
            String section = message.substring(0, index);
            addItems(items, section);
            items.add(ChatStyle.RESET);
            message = message.substring(index);
        }
        while ((index = message.indexOf(Colors.UNDERLINE)) != -1) {
            String section = message.substring(0, index);
            addItems(items, section);
            items.add(ChatStyle.UNDERLINE);
            message = message.substring(index);
        }
        Matcher match = IRC_COLOR_PATTERN.matcher(message);
        while (match.find()) {
            ChatStyle reverse = reverseMapping.get(match.group(0));
            String section = message.substring(0, match.regionStart());
            addItems(items, section);
            if (reverse != null) {
                items.add(reverse);
            }
            message = message.substring(match.start());
        }
        items.add(message);
    }

    @Override
    public String stripStyle(String message) {
        return Colors.removeFormattingAndColors(message);
    }
}
