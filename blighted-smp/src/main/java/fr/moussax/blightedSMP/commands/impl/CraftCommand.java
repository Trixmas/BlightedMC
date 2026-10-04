package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.commands.PlayerCommand;
import fr.moussax.blightedSMP.engine.recipes.crafting.menu.CraftingTableMenu;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public final class CraftCommand extends PlayerCommand {
    @Override
    protected boolean execute(Player player, Command command, String label, String[] args) {
        new CraftingTableMenu().open(player);
        return true;
    }
}
