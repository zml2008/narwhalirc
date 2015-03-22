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
package ninja.leaping.narwhalirc.chatstyle;

import org.pircbotx.Colors;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zml2008
 */
public class IrcStyleHandler extends StyleHandler {
    public static final IrcStyleHandler INSTANCE = new IrcStyleHandler();
    public static final String ITALIC_CODE = "\29";
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

    public ChatArguments extractArguments(String message) {
        List<Object> items = new ArrayList<Object>();
        addItems(items,  message);
        return new ChatArguments(items);
    }

    private String handleDirectString(List<Object> items, String origStyle, ChatStyle replaceStyle, String message) {
        int index;
        while ((index = message.indexOf(origStyle)) != -1) {
            String section = message.substring(0, index);
            addItems(items, section);
            if (index + origStyle.length() == message.length()) {
                return "";
            }
            items.add(replaceStyle);
            message = message.substring(index + origStyle.length());
        }
        return message;
    }

    private void addItems(List<Object> items, String message) {
        message = handleDirectString(items, Colors.BOLD, ChatStyle.BOLD, message);
        message = handleDirectString(items, Colors.NORMAL, ChatStyle.RESET, message);
        message = handleDirectString(items, Colors.UNDERLINE, ChatStyle.UNDERLINE, message);
        message = handleDirectString(items, ITALIC_CODE, ChatStyle.ITALIC, message);

        Matcher match = IRC_COLOR_PATTERN.matcher(message);
        while (match.find()) {
            ChatStyle reverse = reverseMapping.get(match.group(0));
            String section = message.substring(0, match.start());
            addItems(items, section);
            if (reverse != null) {
                items.add(reverse);
            }
            message = message.substring(match.end());
        }
        items.add(message.replace("\03", ""));
    }

    @Override
    public String stripStyle(String message) {
        return Colors.removeFormattingAndColors(message);
    }
}
