package com.zachsthings.narwhal.irc;

import org.pircbotx.PircBotX;

/**
 * Our subclass of {@link PircBotX} that fixes an issue where shutdown doesn't correctly
 * stop the output thread
 */
public class NarwhalBot extends PircBotX {
    public void shutdown() {
        super.shutdown();
        if (outputThread.getQueueSize() == 0) {
            outputThread.interrupt();
        }
    }
}
