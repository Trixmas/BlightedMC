package fr.moussax.blightedSMP.content.items.abilities.tools;

import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.event.block.BlockBreakEvent;

public class HammerAbility implements ItemAbility<BlockBreakEvent> {
    @Override
    public String getName() {
        return "Hammer";
    }

    @Override
    public AbilityTrigger getTrigger() {
        return AbilityTrigger.BLOCK_BREAK;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Mines in a wide 3x3 area."
        };
    }
    @Override
    public boolean triggerAbility(BlockBreakEvent event) {
        return false;
    }

    @Override
    public boolean canTrigger(BlightedPlayer player) {
        return false;
    }
}
