package fr.moussax.blightedSMP.engine.recipes.forging;

import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for resolving the fuel value of items used by the forge.
 *
 * <p>Fuel values are defined per item and multiplied by the item's stack
 * amount when calculating the total fuel provided by a stack.</p>
 */
public final class ForgeFuel {
    private static final Map<String, Integer> FORGE_FUELS = new HashMap<>();

    static {
        FORGE_FUELS.put("vanilla:COAL", 10);
        FORGE_FUELS.put("vanilla:MAGMA_BLOCK", 40);
        FORGE_FUELS.put("vanilla:BLAZE_ROD", 200);
        FORGE_FUELS.put("vanilla:LAVA_BUCKET", 1_000);
        FORGE_FUELS.put("ENCHANTED_COAL", 3_000);
        FORGE_FUELS.put("ENCHANTED_LAVA_BUCKET", 10_000);
        FORGE_FUELS.put("MAGMA_BUCKET", 20_000);
        FORGE_FUELS.put("PLASMA_BUCKET", 50_000);
    }

    private ForgeFuel() {
    }

    /**
     * Returns the total amount of forge fuel provided by an item stack.
     *
     * <p>The returned value is the fuel value of a single item multiplied by
     * the stack amount. Air and unregistered items provide no fuel.</p>
     *
     * @param item the item stack to evaluate
     * @return the total forge fuel provided by the item stack
     */
    public static int getFuelAmount(@NonNull ItemStack item) {
        if (item.getType() == Material.AIR) return 0;

        String id = Utilities.resolveItemId(item, "");
        return FORGE_FUELS.getOrDefault(id, 0) * item.getAmount();
    }

    /**
     * Returns the forge fuel value provided by a single item.
     *
     * <p>Unlike {@link #getFuelAmount(ItemStack)}, this method does not
     * account for the item's stack amount.</p>
     *
     * @param item the item stack to evaluate
     * @return the forge fuel value per item, or {@code 0} if the item is not fuel
     */
    public static int getFuelPerItem(@NonNull ItemStack item) {
        if (item.getType() == Material.AIR) return 0;

        String id = Utilities.resolveItemId(item, "");
        return FORGE_FUELS.getOrDefault(id, 0);
    }

    /**
     * Determines whether an item can be used as forge fuel.
     *
     * @param item the item stack to evaluate
     * @return {@code true} if the item provides forge fuel; {@code false} otherwise
     */
    public static boolean isFuel(ItemStack item) {
        return getFuelPerItem(item) > 0;
    }
}
