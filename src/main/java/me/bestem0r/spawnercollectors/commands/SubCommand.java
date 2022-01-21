package me.bestem0r.spawnercollectors.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    List<String> getCompletion(int index, String[] args);

    void run(CommandSender sender, String[] args);

    String getDescription();
}
