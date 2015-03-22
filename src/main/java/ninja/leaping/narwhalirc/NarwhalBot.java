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
package ninja.leaping.narwhalirc;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;

/**
 * Our subclass of {@link PircBotX} that fixes an issue where shutdown doesn't correctly
 * stop the output thread
 */
public class NarwhalBot extends PircBotX {
    public NarwhalBot(Configuration<NarwhalBot> config, boolean debugLog) {
        super(config);
        if (debugLog) {
            //setVerbose(true);
        }
    }
}
