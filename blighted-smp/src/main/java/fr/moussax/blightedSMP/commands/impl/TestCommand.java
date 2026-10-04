package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.blightedSMP.commands.AdminCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import static fr.moussax.blightedSMP.engine.player.hud.PlayerHudManager.SECTION_HEALTH;

public final class TestCommand extends AdminCommand {
    @Override
    protected boolean executeAdmin(Player player, Command command, String label, String[] args) {
        Actionbar.sendSlotCountdown(player, SECTION_HEALTH, "§e§lIMMUNE! §f%ds", 20);
        return true;
    }
}
