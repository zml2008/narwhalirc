package com.zachsthings.narwhal.irc;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.server.ServerCommandEvent;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;

/**
 * @author zml2008
 */
public class BasicBotCommnds {
    @Command(aliases = {"players", "ply"},
            desc = "Return the online players")
    public static void onlinePlayers(CommandContext args, CommandSender sender, PircBotX bot, Channel source) throws CommandException {
        StringBuilder build = new StringBuilder();
        Player[] onlinePlayers = CommandBook.server().getOnlinePlayers();
        if (onlinePlayers.length > 0) {
            for (Player player : onlinePlayers) {
                if (build.length() == 0) {
                    build.append("Online players: (")
                            .append(onlinePlayers.length).append("/")
                            .append(Bukkit.getServer().getMaxPlayers())
                            .append("): ");
                } else {
                    build.append(", ");
                }
                build.append(player.getDisplayName()).append(ChatColor.WHITE);
            }
        } else {
            build.append("No players online.");
        }
        sender.sendMessage(build.toString());
    }

    @Command(aliases = {"echo"},
            desc = "Echo what is sent in", usage = "<message>",
            min = 1)
    public static void echo(CommandContext args, CommandSender sender, PircBotX bot, Channel source) throws CommandException {
        sender.sendMessage(args.getJoinedStrings(0));
    }

    @Command(aliases = {"exec"}, anyFlags = true,
            desc = "Executes a command on the server. ",
            usage = "<command>")
    public static void exec(CommandContext args, CommandSender sender, PircBotX bot, Channel source) throws CommandException {
        //ServerCommandEvent event = CommandBook.callEvent(new ServerCommandEvent(sender, args.getJoinedStrings(0)));
        //if (event.getCommand() != null) {
            if (!CommandBook.server().dispatchCommand(sender, args.getJoinedStrings(0))) {
                throw new CommandException("Unknown command: " + args.getString(0) + "!");
            }
        //}
    }

    @Command(aliases = "broadcastadmin", desc = "Broadcasts a message with BROADCAST_CHANNEL_ADMIN", min = 1)
    public static void broadcastAdmin(CommandContext args, CommandSender sender, PircBotX bot, Channel source) throws CommandException {
        CommandBook.server().broadcast(args.getJoinedStrings(0), Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    @Command(aliases = "msg", desc = "Sends a message to a player on the server", usage = "<player> <message>", min = 2)
    public static void msg(CommandContext args, CommandSender sender, PircBotX bot, Channel source) throws CommandException {
        CommandSender target = PlayerUtil.matchPlayerOrConsole(sender, args.getString(0));
        target.sendMessage(ChatColor.GRAY + "IRC-PM from " +  bot.getServer() +
                ":" + sender.getName() + ": " + ChatColor.WHITE + args.getJoinedStrings(1));
        sender.sendMessage("Message sent to " + PlayerUtil.toName(target));
    }
}
