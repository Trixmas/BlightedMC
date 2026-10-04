package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.commands.PlayerCommand;
import fr.moussax.blightedSMP.engine.recipes.forging.menu.ForgeMenu;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public final class ForgeCommand extends PlayerCommand {
    @Override
    protected boolean execute(Player player, Command command, String label, String[] args) {
        new ForgeMenu(null).open(player);
        return true;
    }
}
