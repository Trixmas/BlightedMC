package fr.moussax.blightedSMP.engine.entities.rituals;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Defines an ancient ritual used to summon an {@link AncientCreature}.
 *
 * <p>A ritual specifies the offerings required for the summoning, the item
 * displayed by its user interface, and the resource costs required to perform
 * it.</p>
 */
@Getter
public final class AncientRitual {

    private final AncientCreature summonedCreature;
    private final List<RecipeIngredient> offerings;
    private final ItemStack displayedItem;
    private final int gemsCost;
    private final int levelCost;

    private AncientRitual(
            AncientCreature summonedCreature,
            List<RecipeIngredient> offerings,
            ItemStack displayedItem,
            int gemsCost,
            int levelCost
    ) {
        this.summonedCreature = summonedCreature;
        this.offerings = List.copyOf(offerings);
        this.displayedItem = displayedItem;
        this.gemsCost = gemsCost;
        this.levelCost = levelCost;
    }

    /**
     * Fluent builder for constructing an {@link AncientRitual}.
     *
     * <p>The builder collects the creature, required offerings, displayed
     * item, and resource costs before producing an immutable ritual.</p>
     */
    public static final class Builder {

        private final AncientCreature summonedCreature;
        private final List<RecipeIngredient> offerings = new ArrayList<>();
        private ItemStack displayedItem;
        private int gemsCost;
        private int levelCost;

        private Builder(AncientCreature summonedCreature) {
            if (summonedCreature == null) {
                throw new IllegalArgumentException("Summoned creature cannot be null");
            }
            this.summonedCreature = summonedCreature;
        }

        /**
         * Creates a builder for a ritual that summons the specified creature.
         *
         * @param creature creature summoned by the ritual
         * @return a new ritual builder
         */
        public static Builder of(AncientCreature creature) {
            return new Builder(creature);
        }

        /**
         * Configures the item displayed for this ritual.
         *
         * @param material  base material of the displayed item
         * @param configure configuration applied to the item builder
         * @return this builder
         */
        public Builder displayedItem(
                Material material,
                Consumer<ItemBuilder> configure
        ) {
            ItemBuilder builder = new ItemBuilder(material);
            configure.accept(builder);
            this.displayedItem = builder.toItemStack();
            return this;
        }

        /**
         * Adds an offering required by the ritual.
         *
         * @param offering required offering
         * @return this builder
         */
        public Builder addOffering(RecipeIngredient offering) {
            this.offerings.add(offering);
            return this;
        }

        /**
         * Adds a vanilla material as a required offering.
         *
         * @param material required material
         * @param amount   required amount
         * @return this builder
         */
        public Builder addOffering(Material material, int amount) {
            return addOffering(RecipeIngredient.of(material, amount));
        }

        /**
         * Adds a custom item as a required offering.
         *
         * @param item   required custom item
         * @param amount required amount
         * @return this builder
         */
        public Builder addOffering(BlightedItem item, int amount) {
            return addOffering(RecipeIngredient.of(item, amount));
        }

        /**
         * Adds a registered custom item as a required offering.
         *
         * @param itemId registered item identifier
         * @param amount required amount
         * @return this builder
         */
        public Builder addOffering(String itemId, int amount) {
            return addOffering(RecipeIngredient.of(itemId, amount));
        }

        /**
         * Adds multiple offerings to the ritual.
         *
         * @param offerings offerings required by the ritual
         * @return this builder
         */
        public Builder offerings(RecipeIngredient... offerings) {
            this.offerings.addAll(List.of(offerings));
            return this;
        }

        /**
         * Sets the number of gems required to perform the ritual.
         *
         * @param amount required gem amount
         * @return this builder
         * @throws IllegalArgumentException if the amount is negative
         */
        public Builder gemsCost(int amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("Gems cost cannot be negative");
            }
            this.gemsCost = amount;
            return this;
        }

        /**
         * Sets the player level required to perform the ritual.
         *
         * @param level required player level
         * @return this builder
         * @throws IllegalArgumentException if the level is negative
         */
        public Builder levelCost(int level) {
            if (level < 0) {
                throw new IllegalArgumentException("Level cost cannot be negative");
            }
            this.levelCost = level;
            return this;
        }

        /**
         * Builds the configured ritual.
         *
         * @return a new immutable ancient ritual
         * @throws IllegalStateException if no displayed item has been configured
         */
        public AncientRitual build() {
            if (displayedItem == null) {
                throw new IllegalStateException("Displayed item cannot be null");
            }

            return new AncientRitual(
                    summonedCreature,
                    offerings,
                    displayedItem,
                    gemsCost,
                    levelCost
            );
        }
    }
}
