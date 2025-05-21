package me.pilkeysek.skyeNetP.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;


public class DatapackListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("datapacklist")) {
            if (sender.hasPermission("skyenet.command.datapack.list")) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "datapack list");
                sender.sendMessage("Listing all datapacks...");
            } else {
                sender.sendMessage("You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }
}
