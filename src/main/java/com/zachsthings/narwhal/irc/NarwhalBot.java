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
package com.zachsthings.narwhal.irc;

import org.pircbotx.PircBotX;

/**
 * Our subclass of {@link PircBotX} that fixes an issue where shutdown doesn't correctly
 * stop the output thread
 */
public class NarwhalBot extends PircBotX {
    public NarwhalBot(boolean debugLog) {
        super();
        if (debugLog) {
            setVerbose(true);
        }
    }

    public void cleanUp() {
        if (isConnected()) {
            throw new IllegalStateException("Cannot clean up threads while still connected");
        }
        if (inputThread != null) {
            inputThread.interrupt();
            inputThread = null;
        }

        if (outputThread != null) {
            outputThread.interrupt();
            outputThread = null;
        }
    }

    public void shutdown(String message) {
        if (isConnected()) {
            sendRawLine("QUIT :" + message);
        }
        shutdown();
    }

    /*protected NarwhalOutputThread createOutputThread(BufferedWriter bwriter) {
        NarwhalOutputThread output = new NarwhalOutputThread(this, bwriter);
        output.setName("bot" + botCount + "-output");
        return output;
    }

    private static class NarwhalOutputThread extends OutputThread {

        /**
         * Constructs an OutputThread for the underlying PircBotX.  All messages
         * sent to the IRC server are sent by this OutputThread to avoid hammering
         * the server.  Messages are sent immediately if possible.  If there are
         * multiple messages queued, then there is a delay imposed.
         *
         * @param bot The underlying PircBotX instance.
         *
        protected NarwhalOutputThread(PircBotX bot, BufferedWriter bwriter) {
            super(bot, bwriter);
        }

        @SuppressWarnings("deprecation")
        public void shutdown() {
            interrupt();
            Thread.dumpStack();
            String line;
            while ((line = queue.poll()) != null) {
                sendRawLineNow(line);
            }
            stop(); // Stop and shut up
        }

        /**
         * Overridden run method
         *
        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    String line = queue.take();
                    failIfNotConnected();
                    if (line != null && bot.isConnected())
                        sendRawLineNow(line);

                    //Small delay to prevent spamming of the channel
                    Thread.sleep(bot.getMessageDelay());
                }
            } catch (InterruptedException e) {
                // Just let the method return naturally...
            }
        }
    }*/
}
