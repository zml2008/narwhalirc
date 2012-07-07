package com.zachsthings.narwhal.irc.chatstyle;

import org.pircbotx.Colors;
import org.spout.api.chat.style.StyleFormatter;

/**
 * @author zml2008
 */
public class BoldStyleFormatter implements StyleFormatter {
    public String format(String text) {
        return Colors.BOLD + text;
    }
}
