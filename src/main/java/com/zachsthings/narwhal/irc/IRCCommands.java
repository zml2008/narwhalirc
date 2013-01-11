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

import org.pircbotx.Channel;
import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.command.CommandContext;
import org.spout.api.command.CommandSource;
import org.spout.api.command.annotated.Command;
import org.spout.api.command.annotated.CommandPermissions;
import org.spout.api.exception.CommandException;

import java.util.Iterator;

/**
 * Commands to manage NarwhalIRC
 */
public class IRCCommands {
    private final NarwhalIRCPlugin plugin;

    public IRCCommands(NarwhalIRCPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = "irc", desc = "Commands related to NarwhalIRC")
    public class IRCCommand {
        @Command(aliases = {"msg", "tell", "message"}, desc = "Send a message to a user or channel in IRC", usage = "<server>:<user> <message>", min = 2, max = -1)
        @CommandPermissions("narwhal.irc.msg")
        public void msg(CommandContext args, CommandSource sender) throws CommandException {
            CommandSource target;
            String[] split = args.getString(0).split(":", 2);
            if (split.length < 2) {
                throw new CommandException("A server must be specified for this command");
            }

            BotSession bot = plugin.getBot(split[0]);
            if (bot == null) {
                throw new CommandException("No bot for server '" + split[0] + "'!");
            }

            if (split[1].startsWith("#")) {
                target = bot.getChannel(split[1]);
            } else {
                target = bot.getSender(split[1], null);
            }
            target.sendMessage(args.getJoinedString(1));
        }

        @Command(aliases = {"channels", "chans"}, desc = "Lists all the channels this server is connected to")
        @CommandPermissions("narwhal.irc.channels")
        public void channels(CommandContext args, CommandSource sender) throws CommandException {
            for (BotSession bot : plugin.getBots()) {
                ChatArguments builder = new ChatArguments();
                builder.append(ChatStyle.BLUE).append(bot.getServer()).append(": ");
                for (Iterator<ChannelCommandSource> i = bot.getChannels().iterator(); i.hasNext(); ) {
                    final Channel chan = i.next().getChannel();
                    if (chan.getChannelKey() != null) {
                        builder.append("+");
                    }
                    builder.append(chan.getName());
                    if (i.hasNext()) {
                        builder.append(", ");
                    }
                }
                sender.sendMessage(builder);
            }
        }
    }
}
