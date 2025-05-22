package me.pilkeysek.skyeNetP.commands;

import me.pilkeysek.skyeNetP.menu.CreativeMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeMenuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!player.hasPermission("skyenetp.command.creativemenu")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        CreativeMenu.openMenu(player);
        return true;
    }
}
