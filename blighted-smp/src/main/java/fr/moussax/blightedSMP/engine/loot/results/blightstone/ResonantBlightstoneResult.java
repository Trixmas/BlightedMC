package fr.moussax.blightedSMP.engine.loot.results.blightstone;

import fr.moussax.blightedSMP.engine.loot.LootContext;
import fr.moussax.blightedSMP.engine.loot.LootResult;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/**
 * A {@link LootResult} that drops Resonant Blightstones at the loot origin.
 */
public final class ResonantBlightstoneResult implements LootResult {

    /**
     * Drops the specified quantity of blightstones at the loot origin.
     *
     * @param context loot context
     * @param amount  quantity of Blight to drop
     */
    @Override
    public void execute(LootContext context, int amount) {
        ItemStack blightstone = new ResonantBlightstoneItem(amount).get();
        if (context.origin().getWorld() == null) return;

        Item droppedItem = context.origin().getWorld().dropItem(context.origin(), blightstone);

        if (context.velocity() != null) {
            droppedItem.setVelocity(context.velocity());
        }
    }

    /**
     * Returns the display name for the blightstone drop.
     *
     * @param amount amount of Blight
     * @return formatted display name
     */
    @Override
    public String displayName(int amount) {
        return amount > 1
                ? "§3Resonant Blightstone §8(§3" + amount + "❖ Blight§8)"
                : "§3Resonant Blightstone";
    }
}
