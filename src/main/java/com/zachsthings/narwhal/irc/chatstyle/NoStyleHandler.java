package com.zachsthings.narwhal.irc.chatstyle;

import org.spout.api.chat.style.ChatStyle;
import org.spout.api.chat.style.StyleFormatter;
import org.spout.api.chat.style.StyleHandler;
import org.spout.api.chat.style.fallback.DefaultStyleFormatter;

/**
 * An implementation of StyleHandler that adds no formatting.
 */
public class NoStyleHandler extends StyleHandler {
    public static final NoStyleHandler INSTANCE = new NoStyleHandler();
    public static final int ID = register(INSTANCE);

    @Override
    public StyleFormatter getFormatter(ChatStyle style) {
        return DefaultStyleFormatter.INSTANCE;
    }

    @Override
    public String stripStyle(String s) {
        return s;
    }
}
