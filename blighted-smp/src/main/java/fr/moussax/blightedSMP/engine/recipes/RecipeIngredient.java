package fr.moussax.blightedSMP.engine.recipes;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an ingredient used in a crafting or forging recipe, which can be either
 * a custom item managed by {@link BlightedItem} or a vanilla {@link Material}.
 */
@Getter
public final class RecipeIngredient {

    @Nullable
    private final BlightedItem item;

    @Nullable
    private final Material material;

    private final int amount;

    private final String id;

    public RecipeIngredient(@NonNull BlightedItem item, int amount) {
        this.item = Objects.requireNonNull(item, "item cannot be null");
        this.material = null;
        this.amount = amount;
        this.id = item.getItemId();
    }

    public RecipeIngredient(@NonNull Material material, int amount) {
        this.item = null;
        this.material = Objects.requireNonNull(material, "material cannot be null");
        this.amount = amount;
        this.id = "vanilla:" + material.name();
    }

    public static RecipeIngredient of(@NonNull BlightedItem item, int amount) {
        return new RecipeIngredient(item, amount);
    }

    public static RecipeIngredient of(@NonNull Material material, int amount) {
        return new RecipeIngredient(material, amount);
    }

    public static RecipeIngredient of(@NonNull BlightedItem item) {
        return new RecipeIngredient(item, 1);
    }

    public static RecipeIngredient of(@NonNull Material material) {
        return new RecipeIngredient(material, 1);
    }

    public static RecipeIngredient of(@NonNull String itemId, int amount) {
        return new RecipeIngredient(ItemRegistry.getOrThrow(itemId), amount);
    }

    public static RecipeIngredient of(@NonNull String itemId) {
        return of(itemId, 1);
    }

    public boolean isCustom() {
        return item != null;
    }

    public boolean isVanilla() {
        return material != null;
    }

    /**
     * Creates an ItemStack representing this ingredient for display or inventory operations.
     *
     * @return constructed item stack
     */
    public ItemStack toItemStack() {
        if (item != null) {
            ItemStack stack = item.toItemStack();
            stack.setAmount(amount);
            return stack;
        }
        return new ItemStack(Objects.requireNonNull(material), amount);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof RecipeIngredient other)) return false;
        return amount == other.amount && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount);
    }
}
