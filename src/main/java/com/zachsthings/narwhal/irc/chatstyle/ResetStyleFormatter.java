package com.zachsthings.narwhal.irc.chatstyle;

import org.pircbotx.Colors;
import org.spout.api.chat.style.StyleFormatter;

/**
 * Handler the Reset ChatStyle
 */
public class ResetStyleFormatter implements StyleFormatter {
    public String format(String text) {
        return Colors.NORMAL + text;
    }
}
