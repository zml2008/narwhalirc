package com.zachsthings.narwhal.irc;

import org.spout.api.chat.ChatArguments;
import org.spout.api.chat.style.ChatStyle;
import org.spout.api.command.CommandContext;
import org.spout.api.command.CommandSource;
import org.spout.api.command.annotated.Command;
import org.spout.api.exception.CommandException;
import org.spout.api.player.Player;

import java.util.Collection;

/**
 * @author zml2008
 */
public class BasicBotCommands {
    private final NarwhalIRCPlugin plugin;

    public BasicBotCommands(NarwhalIRCPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"players", "ply"},
            desc = "Return the online players")
    public void onlinePlayers(CommandContext args, BotCommandSource sender) throws CommandException {
        ChatArguments build = new ChatArguments();
        Player[] onlinePlayers = plugin.getEngine().getOnlinePlayers();
        if (onlinePlayers.length > 0) {
            for (Player player : onlinePlayers) {
                if (build.length() == 0) {
                    build.append("Online players: (")
                    .append(onlinePlayers.length)
                    .append("/")
                    .append(plugin.getEngine().getMaxPlayers())
                    .append("): ");
                } else {
                    build.append(", ");
                }
                build.append(player.getDisplayName()).append(ChatStyle.RESET);
            }
        } else {
            build.append("No players online.");
        }
        sender.sendMessage(build);
    }

    @Command(aliases = {"echo"},
            desc = "Echo what is sent in", usage = "<message>",
            min = 1)
    public void echo(CommandContext args, BotCommandSource sender) throws CommandException {
        sender.sendMessage(args.getJoinedString(0));
    }

    @Command(aliases = {"exec"}, //anyFlags = true,
            desc = "Executes a command on the server. ",
            usage = "<command...>", min = 1, max = -1)
    public void exec(CommandContext args, BotCommandSource sender) throws CommandException {
        if (args.length() == 1) {
            sender.processCommand(args.getString(0), new ChatArguments());
        } else {
            sender.processCommand(args.getString(0), args.getJoinedString(1));
        }
    }

    /*@Command(aliases = "broadcastadmin", desc = "Broadcasts a message with narwhalirc.broadcast.admin", min = 1)
    public static void broadcastAdmin(CommandContext args, CommandSource sender, PircBotX bot, Channel source) throws CommandException {
        Spout.getEngine().broadcastMessage(args.getJoinedString(0), Server.BROADCAST_CHANNEL_ADMIN);
    }*/

    @Command(aliases = "msg", desc = "Sends a message to a player on the server", usage = "<player> <message>", min = 2)
    public void msg(CommandContext args, BotCommandSource sender) throws CommandException {
        CommandSource target = matchSinglePlayer(args.getString(0));
        target.sendMessage(ChatStyle.GRAY, "IRC-PM from ", sender.getUser().getServer(),
                ":", sender.getName(), ": ", ChatStyle.RESET, args.getJoinedString(1));
        sender.sendMessage("Message sent to ", target.getName());
    }

    public CommandSource matchSinglePlayer(String name) throws CommandException {
        Collection<Player> players = plugin.getEngine().matchPlayer(name);
        if (players.size() == 0) {
            throw new CommandException("No players matched " + name + "!");
        }

        if (players.size() > 1) {
            throw new CommandException("More than one player matched " + name + "!");
        }
        return players.iterator().next();
    }
}
