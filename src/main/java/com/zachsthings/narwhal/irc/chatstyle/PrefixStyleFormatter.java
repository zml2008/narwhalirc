package com.zachsthings.narwhal.irc.chatstyle;

import org.spout.api.chat.style.StyleFormatter;

/**
 * @author zml2008
 */
public class PrefixStyleFormatter implements StyleFormatter {
    private final String prefix;

    public PrefixStyleFormatter(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String format(String text) {
        return prefix + text;
    }
}
