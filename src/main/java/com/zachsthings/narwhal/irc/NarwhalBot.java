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
import org.spout.api.Spout;

/**
 * Our subclass of {@link PircBotX} that fixes an issue where shutdown doesn't correctly
 * stop the output thread
 */
public class NarwhalBot extends PircBotX {
    public NarwhalBot() {
        super();
        if (Spout.debugMode()) {
            setVerbose(true);
        }
    }
    public void shutdown() {
        super.shutdown();
        /*if (outputThread.getQueueSize() == 0) {
            outputThread.interrupt();
        }*/
    }
}
