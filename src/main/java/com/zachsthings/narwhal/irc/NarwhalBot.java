package com.zachsthings.narwhal.irc;

import org.pircbotx.PircBotX;

/**
 * @author zml2008
 */
public class NarwhalBot extends PircBotX {
    public void shutdown() {
        super.shutdown();
        if (outputThread.getQueueSize() == 0) {
            outputThread.interrupt();
        }
    }
}
