package fr.moussax.blightedSMP.engine.fishing;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.loot.*;
import fr.moussax.blightedSMP.engine.loot.decorators.FeedbackSpecification;
import fr.moussax.blightedSMP.engine.loot.decorators.FishingCatchQuality;
import fr.moussax.blightedSMP.engine.loot.decorators.GenericFeedbackDecorator;
import fr.moussax.blightedSMP.engine.loot.providers.AmountProvider;
import fr.moussax.blightedSMP.engine.loot.results.EntityResult;
import fr.moussax.blightedSMP.engine.loot.results.ItemResult;
import fr.moussax.blightedSMP.engine.loot.results.blightstone.ResonantBlightstoneResult;
import fr.moussax.blightedSMP.engine.loot.strategies.WeightedSelectionStrategy;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Loot table for fishing rolls containing separate entity and item drop pools.
 *
 * <p>The entity pool is evaluated first based on the base roll chance, player Luck of the Sea level,
 * and fishing combo. If no entity is selected, the item pool is evaluated using weighted selection.</p>
 */
public final class FishingLootTable {

    private static final double LUCK_ENTITY_CHANCE_PER_LEVEL = 0.03;

    private final LootTable entityTable;
    private final LootTable itemTable;
    @Getter
    private final double entityRollChance;

    private FishingLootTable(LootTable entityTable, LootTable itemTable, double entityRollChance) {
        this.entityTable = entityTable;
        this.itemTable = itemTable;
        this.entityRollChance = entityRollChance;
    }

    /**
     * Rolls this fishing loot table without Luck of the Sea or combo bonuses.
     *
     * @param player   player performing the roll
     * @param location location where catch occurs
     * @param velocity velocity of fishing hook
     * @return {@code true} if a loot entry was selected and executed
     */
    public boolean roll(@NonNull BlightedPlayer player, @NonNull Location location, @NonNull Vector velocity) {
        return roll(player, location, velocity, 0, 0);
    }

    /**
     * Rolls this fishing loot table using a Luck of the Sea level bonus.
     *
     * @param player         player performing the roll
     * @param location       location where catch occurs
     * @param velocity       velocity of fishing hook
     * @param luckOfSeaLevel player Luck of the Sea level
     * @return {@code true} if a loot entry was selected and executed
     */
    public boolean roll(
            @NonNull BlightedPlayer player,
            @NonNull Location location,
            @NonNull Vector velocity,
            int luckOfSeaLevel
    ) {
        return roll(player, location, velocity, luckOfSeaLevel, 0);
    }

    /**
     * Rolls this fishing loot table using Luck of the Sea and fishing combo bonuses.
     *
     * @param player         player performing the roll
     * @param location       location where catch occurs
     * @param velocity       velocity of fishing hook
     * @param luckOfSeaLevel player Luck of the Sea level
     * @param combo          current fishing combo count
     * @return {@code true} if a loot entry was selected and executed
     */
    public boolean roll(
            @NonNull BlightedPlayer player,
            @NonNull Location location,
            @NonNull Vector velocity,
            int luckOfSeaLevel,
            int combo
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = Objects.requireNonNull(location.getWorld());
        Biome biome = world.getBiome(location);
        LootContext context = new LootContext(player, world, biome, location, random, velocity);

        double totalEntityChance = entityRollChance
                + (luckOfSeaLevel * LUCK_ENTITY_CHANCE_PER_LEVEL)
                + FishingComboTracker.getSeaCreatureChanceBonus(combo);

        if (random.nextDouble() < totalEntityChance) {
            List<LootTable.SelectedLoot> selected = entityTable.roll(context);
            if (!selected.isEmpty()) {
                for (LootTable.SelectedLoot loot : selected) {
                    loot.result().execute(context, loot.amount());
                }
                return true;
            }
        }

        List<LootTable.SelectedLoot> selected = itemTable.roll(context);
        if (!selected.isEmpty()) {
            for (LootTable.SelectedLoot loot : selected) {
                loot.result().execute(context, loot.amount());
            }
            return true;
        }

        return false;
    }

    /**
     * Creates a new builder for configuring a {@link FishingLootTable}.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for constructing {@link FishingLootTable} instances.
     */
    public static final class Builder {

        private static final Function<FishingCatchQuality, FeedbackSpecification> FISHING_FEEDBACK_MAPPER = quality -> switch (quality) {
            case GOOD_CATCH ->
                    FeedbackSpecification.full(" §5§lGOOD CATCH! §f| §7You found §f", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f);
            case GREAT_CATCH ->
                    FeedbackSpecification.full(" §6§lGREAT CATCH! §f| §7You found §f", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
            case OUTSTANDING_CATCH ->
                    FeedbackSpecification.full(" §d§lOUTSTANDING CATCH! §f| §7You found §f", Sound.ENTITY_PLAYER_LEVELUP, 1.5f);
            default -> null;
        };

        private static final Function<FishingCatchQuality, FeedbackSpecification> FISHING_SOUND_MAPPER = quality -> switch (quality) {
            case GOOD_CATCH -> FeedbackSpecification.soundOnly(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f);
            case GREAT_CATCH -> FeedbackSpecification.soundOnly(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
            case OUTSTANDING_CATCH -> FeedbackSpecification.soundOnly(Sound.ENTITY_PLAYER_LEVELUP, 1.5f);
            default -> null;
        };

        private final LootTable.Builder entityTableBuilder = LootTable.builder();
        private final LootTable.Builder itemTableBuilder = LootTable.builder();
        private double entityRollChance = 0.15;

        /**
         * Sets the base probability of selecting from the entity pool instead of items.
         *
         * @param chance entity selection probability (clamped between 0.0 and 1.0)
         * @return this builder
         */
        public Builder entityRollChance(double chance) {
            this.entityRollChance = Math.clamp(chance, 0.0, 1.0);
            return this;
        }

        /**
         * Adds a custom entry to the entity loot pool.
         *
         * @param entry entity loot entry
         * @return this builder
         */
        public Builder entity(@NonNull LootEntry entry) {
            Objects.requireNonNull(entry, "entry cannot be null");
            entityTableBuilder.addEntry(entry);
            return this;
        }

        /**
         * Adds custom entries to the entity loot pool.
         *
         * @param entries entity loot entries
         * @return this builder
         */
        public Builder entity(@NonNull LootEntry... entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            entityTableBuilder.addEntries(entries);
            return this;
        }

        /**
         * Adds a custom entry to the item loot pool.
         *
         * @param entry item loot entry
         * @return this builder
         */
        public Builder item(@NonNull LootEntry entry) {
            Objects.requireNonNull(entry, "entry cannot be null");
            itemTableBuilder.addEntry(entry);
            return this;
        }

        /**
         * Adds custom entries to the item loot pool.
         *
         * @param entries item loot entries
         * @return this builder
         */
        public Builder item(@NonNull LootEntry... entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            itemTableBuilder.addEntries(entries);
            return this;
        }

        /**
         * Adds an item drop with default quantity (1) and common catch quality.
         *
         * @param material item material
         * @param weight   selection weight
         * @return this builder
         */
        public Builder item(@NonNull Material material, double weight) {
            return item(material, 1, 1, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds an item drop with default quantity (1) and custom catch quality feedback.
         *
         * @param material item material
         * @param weight   selection weight
         * @param quality  catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull Material material, double weight, @NonNull FishingCatchQuality quality) {
            return item(material, 1, 1, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds an item drop with default quantity (1), custom catch quality feedback, and selection condition.
         *
         * @param material  item material
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(material, 1, 1, weight, quality, condition);
        }

        /**
         * Adds an item drop with fixed quantity and common catch quality.
         *
         * @param material item material
         * @param count    fixed drop quantity
         * @param weight   selection weight
         * @return this builder
         */
        public Builder item(@NonNull Material material, int count, double weight) {
            return item(material, count, count, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds an item drop with fixed quantity and custom catch quality feedback.
         *
         * @param material item material
         * @param count    fixed drop quantity
         * @param weight   selection weight
         * @param quality  catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull Material material, int count, double weight, @NonNull FishingCatchQuality quality) {
            return item(material, count, count, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds an item drop with fixed quantity, custom catch quality feedback, and selection condition.
         *
         * @param material  item material
         * @param count     fixed drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                int count,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(material, count, count, weight, quality, condition);
        }

        /**
         * Adds an item drop with variable quantity range and common catch quality.
         *
         * @param material  item material
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @return this builder
         */
        public Builder item(@NonNull Material material, int minAmount, int maxAmount, double weight) {
            return item(material, minAmount, maxAmount, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds an item drop with variable quantity range and custom catch quality feedback.
         *
         * @param material  item material
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return item(material, minAmount, maxAmount, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds an item drop with variable quantity range, custom catch quality feedback, and selection condition.
         *
         * @param material  item material
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(material, "material cannot be null");
            return registerItemEntry(ItemResult.of(material), minAmount, maxAmount, weight, quality, condition);
        }

        /**
         * Adds a modified item drop with default quantity (1) and common catch quality.
         *
         * @param material item material
         * @param modifier item builder modifier callback
         * @param weight   selection weight
         * @return this builder
         */
        public Builder item(@NonNull Material material, @NonNull Consumer<ItemBuilder> modifier, double weight) {
            return item(material, modifier, 1, 1, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified item drop with default quantity (1) and custom catch quality feedback.
         *
         * @param material item material
         * @param modifier item builder modifier callback
         * @param weight   selection weight
         * @param quality  catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return item(material, modifier, 1, 1, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified item drop with default quantity (1), custom catch quality feedback, and selection condition.
         *
         * @param material  item material
         * @param modifier  item builder modifier callback
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(material, modifier, 1, 1, weight, quality, condition);
        }

        /**
         * Adds a modified item drop with fixed quantity and common catch quality.
         *
         * @param material item material
         * @param modifier item builder modifier callback
         * @param count    fixed drop quantity
         * @param weight   selection weight
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                int count,
                double weight
        ) {
            return item(material, modifier, count, count, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified item drop with fixed quantity and custom catch quality feedback.
         *
         * @param material item material
         * @param modifier item builder modifier callback
         * @param count    fixed drop quantity
         * @param weight   selection weight
         * @param quality  catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                int count,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return item(material, modifier, count, count, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified item drop with fixed quantity, custom catch quality feedback, and selection condition.
         *
         * @param material  item material
         * @param modifier  item builder modifier callback
         * @param count     fixed drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                int count,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(material, modifier, count, count, weight, quality, condition);
        }

        /**
         * Adds a modified item drop with variable quantity range and common catch quality.
         *
         * @param material  item material
         * @param modifier  item builder modifier callback
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                int minAmount,
                int maxAmount,
                double weight
        ) {
            return item(material, modifier, minAmount, maxAmount, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified item drop with variable quantity range and custom catch quality feedback.
         *
         * @param material  item material
         * @param modifier  item builder modifier callback
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return item(material, modifier, minAmount, maxAmount, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified item drop with variable quantity range, custom catch quality feedback, and selection condition.
         *
         * @param material  item material
         * @param modifier  item builder modifier callback
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull Material material,
                @NonNull Consumer<ItemBuilder> modifier,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(material, "material cannot be null");
            Objects.requireNonNull(modifier, "modifier cannot be null");
            return registerItemEntry(ItemResult.of(material, modifier), minAmount, maxAmount, weight, quality, condition);
        }

        /**
         * Adds a registered item drop with default quantity (1) and common catch quality.
         *
         * @param itemId registered item identifier
         * @param weight selection weight
         * @return this builder
         */
        public Builder item(@NonNull String itemId, double weight) {
            return item(itemId, 1, 1, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a registered item drop with default quantity (1) and custom catch quality feedback.
         *
         * @param itemId  registered item identifier
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull String itemId, double weight, @NonNull FishingCatchQuality quality) {
            return item(itemId, 1, 1, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a registered item drop with default quantity (1), custom catch quality feedback, and selection condition.
         *
         * @param itemId    registered item identifier
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull String itemId,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(itemId, 1, 1, weight, quality, condition);
        }

        /**
         * Adds a registered item drop with fixed quantity and common catch quality.
         *
         * @param itemId registered item identifier
         * @param count  fixed drop quantity
         * @param weight selection weight
         * @return this builder
         */
        public Builder item(@NonNull String itemId, int count, double weight) {
            return item(itemId, count, count, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a registered item drop with fixed quantity and custom catch quality feedback.
         *
         * @param itemId  registered item identifier
         * @param count   fixed drop quantity
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull String itemId, int count, double weight, @NonNull FishingCatchQuality quality) {
            return item(itemId, count, count, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a registered item drop with fixed quantity, custom catch quality feedback, and selection condition.
         *
         * @param itemId    registered item identifier
         * @param count     fixed drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull String itemId,
                int count,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(itemId, count, count, weight, quality, condition);
        }

        /**
         * Adds a registered item drop with variable quantity range and common catch quality.
         *
         * @param itemId    registered item identifier
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @return this builder
         */
        public Builder item(@NonNull String itemId, int minAmount, int maxAmount, double weight) {
            return item(itemId, minAmount, maxAmount, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a registered item drop with variable quantity range and custom catch quality feedback.
         *
         * @param itemId    registered item identifier
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull String itemId,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return item(itemId, minAmount, maxAmount, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a registered item drop with variable quantity range, custom catch quality feedback, and selection condition.
         *
         * @param itemId    registered item identifier
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull String itemId,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(itemId, "itemId cannot be null");
            return registerItemEntry(ItemResult.of(itemId), minAmount, maxAmount, weight, quality, condition);
        }

        /**
         * Adds a Blighted item drop with default quantity (1) and common catch quality.
         *
         * @param item   Blighted item definition
         * @param weight selection weight
         * @return this builder
         */
        public Builder item(@NonNull BlightedItem item, double weight) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), weight);
        }

        /**
         * Adds a Blighted item drop with default quantity (1) and custom catch quality feedback.
         *
         * @param item    Blighted item definition
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull BlightedItem item, double weight, @NonNull FishingCatchQuality quality) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), weight, quality);
        }

        /**
         * Adds a Blighted item drop with default quantity (1), custom catch quality feedback, and selection condition.
         *
         * @param item      Blighted item definition
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull BlightedItem item,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), weight, quality, condition);
        }

        /**
         * Adds a Blighted item drop with fixed quantity and common catch quality.
         *
         * @param item   Blighted item definition
         * @param count  fixed drop quantity
         * @param weight selection weight
         * @return this builder
         */
        public Builder item(@NonNull BlightedItem item, int count, double weight) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), count, weight);
        }

        /**
         * Adds a Blighted item drop with fixed quantity and custom catch quality feedback.
         *
         * @param item    Blighted item definition
         * @param count   fixed drop quantity
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull BlightedItem item, int count, double weight, @NonNull FishingCatchQuality quality) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), count, weight, quality);
        }

        /**
         * Adds a Blighted item drop with fixed quantity, custom catch quality feedback, and selection condition.
         *
         * @param item      Blighted item definition
         * @param count     fixed drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull BlightedItem item,
                int count,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), count, weight, quality, condition);
        }

        /**
         * Adds a Blighted item drop with variable quantity range and common catch quality.
         *
         * @param item      Blighted item definition
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @return this builder
         */
        public Builder item(@NonNull BlightedItem item, int minAmount, int maxAmount, double weight) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), minAmount, maxAmount, weight);
        }

        /**
         * Adds a Blighted item drop with variable quantity range and custom catch quality feedback.
         *
         * @param item      Blighted item definition
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull BlightedItem item,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), minAmount, maxAmount, weight, quality);
        }

        /**
         * Adds a Blighted item drop with variable quantity range, custom catch quality feedback, and selection condition.
         *
         * @param item      Blighted item definition
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull BlightedItem item,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(item, "item cannot be null");
            return item(item.getItemId(), minAmount, maxAmount, weight, quality, condition);
        }

        /**
         * Adds a custom loot result item drop with default quantity (1) and common catch quality.
         *
         * @param result custom loot result
         * @param weight selection weight
         * @return this builder
         */
        public Builder item(@NonNull LootResult result, double weight) {
            return item(result, 1, 1, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result item drop with default quantity (1) and custom catch quality feedback.
         *
         * @param result  custom loot result
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull LootResult result, double weight, @NonNull FishingCatchQuality quality) {
            return item(result, 1, 1, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result item drop with default quantity (1), custom catch quality feedback, and selection condition.
         *
         * @param result    custom loot result
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull LootResult result,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(result, 1, 1, weight, quality, condition);
        }

        /**
         * Adds a custom loot result item drop with fixed quantity and common catch quality.
         *
         * @param result custom loot result
         * @param count  fixed drop quantity
         * @param weight selection weight
         * @return this builder
         */
        public Builder item(@NonNull LootResult result, int count, double weight) {
            return item(result, count, count, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result item drop with fixed quantity and custom catch quality feedback.
         *
         * @param result  custom loot result
         * @param count   fixed drop quantity
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder item(@NonNull LootResult result, int count, double weight, @NonNull FishingCatchQuality quality) {
            return item(result, count, count, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result item drop with fixed quantity, custom catch quality feedback, and selection condition.
         *
         * @param result    custom loot result
         * @param count     fixed drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull LootResult result,
                int count,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return item(result, count, count, weight, quality, condition);
        }

        /**
         * Adds a custom loot result item drop with variable quantity range and common catch quality.
         *
         * @param result    custom loot result
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @return this builder
         */
        public Builder item(@NonNull LootResult result, int minAmount, int maxAmount, double weight) {
            return item(result, minAmount, maxAmount, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result item drop with variable quantity range and custom catch quality feedback.
         *
         * @param result    custom loot result
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @return this builder
         */
        public Builder item(
                @NonNull LootResult result,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return item(result, minAmount, maxAmount, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result item drop with variable quantity range, custom catch quality feedback, and selection condition.
         *
         * @param result    custom loot result
         * @param minAmount minimum drop quantity
         * @param maxAmount maximum drop quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder item(
                @NonNull LootResult result,
                int minAmount,
                int maxAmount,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(result, "result cannot be null");
            return registerItemEntry(result, minAmount, maxAmount, weight, quality, condition);
        }

        /**
         * Adds a fixed Blight reward drop with common catch quality.
         *
         * @param blight Blight quantity
         * @param weight selection weight
         * @return this builder
         */
        public Builder blight(int blight, double weight) {
            return blight(blight, blight, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a fixed Blight reward drop with custom catch quality feedback.
         *
         * @param blight  Blight quantity
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder blight(int blight, double weight, @NonNull FishingCatchQuality quality) {
            return blight(blight, blight, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a variable Blight reward drop with common catch quality.
         *
         * @param minBlight minimum Blight quantity
         * @param maxBlight maximum Blight quantity
         * @param weight    selection weight
         * @return this builder
         */
        public Builder blight(int minBlight, int maxBlight, double weight) {
            return blight(minBlight, maxBlight, weight, FishingCatchQuality.COMMON, LootCondition.alwaysTrue());
        }

        /**
         * Adds a variable Blight reward drop with custom catch quality feedback.
         *
         * @param minBlight minimum Blight quantity
         * @param maxBlight maximum Blight quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @return this builder
         */
        public Builder blight(int minBlight, int maxBlight, double weight, @NonNull FishingCatchQuality quality) {
            return blight(minBlight, maxBlight, weight, quality, LootCondition.alwaysTrue());
        }

        /**
         * Adds a variable Blight reward drop with custom catch quality feedback and selection condition.
         *
         * @param minBlight minimum Blight quantity
         * @param maxBlight maximum Blight quantity
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder blight(
                int minBlight,
                int maxBlight,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(quality, "quality cannot be null");
            Objects.requireNonNull(condition, "condition cannot be null");
            return registerItemEntry(new ResonantBlightstoneResult(), minBlight, maxBlight, weight, quality, condition);
        }

        /**
         * Adds a damaged item drop with randomized durability and common catch quality.
         *
         * @param material      item material
         * @param minPercentage minimum durability ratio remaining
         * @param maxPercentage maximum durability ratio remaining
         * @param weight        selection weight
         * @return this builder
         */
        public Builder damagedItem(
                @NonNull Material material,
                double minPercentage,
                double maxPercentage,
                double weight
        ) {
            return damagedItem(material, minPercentage, maxPercentage, weight, FishingCatchQuality.COMMON);
        }

        /**
         * Adds a damaged item drop with randomized durability and custom catch quality feedback.
         *
         * @param material      item material
         * @param minPercentage minimum durability ratio remaining
         * @param maxPercentage maximum durability ratio remaining
         * @param weight        selection weight
         * @param quality       catch quality tier for feedback
         * @return this builder
         */
        public Builder damagedItem(
                @NonNull Material material,
                double minPercentage,
                double maxPercentage,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            Objects.requireNonNull(material, "material cannot be null");
            Objects.requireNonNull(quality, "quality cannot be null");
            return registerItemEntry(
                    ItemResult.randomDurability(material, minPercentage, maxPercentage),
                    1, 1, weight, quality, LootCondition.alwaysTrue()
            );
        }

        /**
         * Adds a randomized enchanted book drop with common catch quality.
         *
         * @param enchantments list of eligible enchantments
         * @param minLevel     minimum enchantment level
         * @param maxLevel     maximum enchantment level
         * @param weight       selection weight
         * @return this builder
         */
        public Builder enchantedBook(
                @NonNull List<Enchantment> enchantments,
                int minLevel,
                int maxLevel,
                double weight
        ) {
            return enchantedBook(enchantments, minLevel, maxLevel, weight, FishingCatchQuality.COMMON);
        }

        /**
         * Adds a randomized enchanted book drop with custom catch quality feedback.
         *
         * @param enchantments list of eligible enchantments
         * @param minLevel     minimum enchantment level
         * @param maxLevel     maximum enchantment level
         * @param weight       selection weight
         * @param quality      catch quality tier for feedback
         * @return this builder
         */
        public Builder enchantedBook(
                @NonNull List<Enchantment> enchantments,
                int minLevel,
                int maxLevel,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            Objects.requireNonNull(enchantments, "enchantments cannot be null");
            Objects.requireNonNull(quality, "quality cannot be null");
            return registerItemEntry(
                    ItemResult.randomEnchantedBook(enchantments, minLevel, maxLevel),
                    1, 1, weight, quality, LootCondition.alwaysTrue()
            );
        }

        /**
         * Adds an enchanted book drop from a weighted enchantment pool with common catch quality.
         *
         * @param enchantmentPool map of enchantments and their levels
         * @param weight          selection weight
         * @return this builder
         */
        public Builder enchantedBook(
                @NonNull Map<Enchantment, Integer> enchantmentPool,
                double weight
        ) {
            return enchantedBook(enchantmentPool, weight, FishingCatchQuality.COMMON);
        }

        /**
         * Adds an enchanted book drop from a weighted enchantment pool with custom catch quality feedback.
         *
         * @param enchantmentPool map of enchantments and their levels
         * @param weight          selection weight
         * @param quality         catch quality tier for feedback
         * @return this builder
         */
        public Builder enchantedBook(
                @NonNull Map<Enchantment, Integer> enchantmentPool,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            Objects.requireNonNull(enchantmentPool, "enchantmentPool cannot be null");
            Objects.requireNonNull(quality, "quality cannot be null");
            return registerItemEntry(
                    ItemResult.randomEnchantedBook(enchantmentPool),
                    1, 1, weight, quality, LootCondition.alwaysTrue()
            );
        }

        /**
         * Adds an entity drop with common catch quality.
         *
         * @param type   entity type
         * @param weight selection weight
         * @return this builder
         */
        public Builder entity(@NonNull EntityType type, double weight) {
            return entity(type, weight, FishingCatchQuality.COMMON, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds an entity drop with custom catch quality feedback.
         *
         * @param type    entity type
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder entity(@NonNull EntityType type, double weight, @NonNull FishingCatchQuality quality) {
            return entity(type, weight, quality, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds an entity drop with custom catch quality feedback and selection condition.
         *
         * @param type      entity type
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return entity(type, weight, quality, null, false, condition);
        }

        /**
         * Adds an entity drop with custom catch quality feedback and catch message.
         *
         * @param type         entity type
         * @param weight       selection weight
         * @param quality      catch quality tier for feedback
         * @param catchMessage custom catch message
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage
        ) {
            return entity(type, weight, quality, catchMessage, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds an entity drop with full customization options.
         *
         * @param type                 entity type
         * @param weight               selection weight
         * @param quality              catch quality tier for feedback
         * @param catchMessage         custom catch message
         * @param suppressStandardText whether to suppress standard catch header prefix
         * @param condition            condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage,
                boolean suppressStandardText,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(type, "type cannot be null");
            return registerEntityEntry(
                    EntityResult.vanilla(type),
                    weight,
                    quality,
                    catchMessage,
                    suppressStandardText,
                    condition
            );
        }

        /**
         * Adds a modified entity drop with common catch quality.
         *
         * @param type     entity type
         * @param modifier spawned entity modifier callback
         * @param weight   selection weight
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                @NonNull Consumer<LivingEntity> modifier,
                double weight
        ) {
            return entity(type, modifier, weight, FishingCatchQuality.COMMON, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified entity drop with custom catch quality feedback.
         *
         * @param type     entity type
         * @param modifier spawned entity modifier callback
         * @param weight   selection weight
         * @param quality  catch quality tier for feedback
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                @NonNull Consumer<LivingEntity> modifier,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return entity(type, modifier, weight, quality, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified entity drop with custom catch quality feedback and selection condition.
         *
         * @param type      entity type
         * @param modifier  spawned entity modifier callback
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                @NonNull Consumer<LivingEntity> modifier,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return entity(type, modifier, weight, quality, null, false, condition);
        }

        /**
         * Adds a modified entity drop with custom catch quality feedback and catch message.
         *
         * @param type         entity type
         * @param modifier     spawned entity modifier callback
         * @param weight       selection weight
         * @param quality      catch quality tier for feedback
         * @param catchMessage custom catch message
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                @NonNull Consumer<LivingEntity> modifier,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage
        ) {
            return entity(type, modifier, weight, quality, catchMessage, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a modified entity drop with full customization options.
         *
         * @param type                 entity type
         * @param modifier             spawned entity modifier callback
         * @param weight               selection weight
         * @param quality              catch quality tier for feedback
         * @param catchMessage         custom catch message
         * @param suppressStandardText whether to suppress standard catch header prefix
         * @param condition            condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull EntityType type,
                @NonNull Consumer<LivingEntity> modifier,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage,
                boolean suppressStandardText,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(modifier, "modifier cannot be null");
            return registerEntityEntry(
                    EntityResult.vanilla(type, modifier),
                    weight,
                    quality,
                    catchMessage,
                    suppressStandardText,
                    condition
            );
        }

        /**
         * Adds a Blighted entity drop with common catch quality.
         *
         * @param entity Blighted entity definition
         * @param weight selection weight
         * @return this builder
         */
        public Builder entity(@NonNull BlightedEntity entity, double weight) {
            return entity(entity, weight, FishingCatchQuality.COMMON, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a Blighted entity drop with custom catch quality feedback.
         *
         * @param entity  Blighted entity definition
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder entity(@NonNull BlightedEntity entity, double weight, @NonNull FishingCatchQuality quality) {
            return entity(entity, weight, quality, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a Blighted entity drop with custom catch quality feedback and selection condition.
         *
         * @param entity    Blighted entity definition
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull BlightedEntity entity,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return entity(entity, weight, quality, null, false, condition);
        }

        /**
         * Adds a Blighted entity drop with custom catch quality feedback and catch message.
         *
         * @param entity       Blighted entity definition
         * @param weight       selection weight
         * @param quality      catch quality tier for feedback
         * @param catchMessage custom catch message
         * @return this builder
         */
        public Builder entity(
                @NonNull BlightedEntity entity,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage
        ) {
            return entity(entity, weight, quality, catchMessage, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a Blighted entity drop with full customization options.
         *
         * @param entity               Blighted entity definition
         * @param weight               selection weight
         * @param quality              catch quality tier for feedback
         * @param catchMessage         custom catch message
         * @param suppressStandardText whether to suppress standard catch header prefix
         * @param condition            condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull BlightedEntity entity,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage,
                boolean suppressStandardText,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(entity, "entity cannot be null");
            return registerEntityEntry(
                    EntityResult.blighted(entity),
                    weight,
                    quality,
                    catchMessage,
                    suppressStandardText,
                    condition
            );
        }

        /**
         * Adds a custom spawner entity drop with common catch quality.
         *
         * @param spawner custom spawner function
         * @param weight  selection weight
         * @return this builder
         */
        public Builder entity(@NonNull Function<Location, LivingEntity> spawner, double weight) {
            return entity(spawner, weight, FishingCatchQuality.COMMON, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom spawner entity drop with custom catch quality feedback.
         *
         * @param spawner custom spawner function
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder entity(
                @NonNull Function<Location, LivingEntity> spawner,
                double weight,
                @NonNull FishingCatchQuality quality
        ) {
            return entity(spawner, weight, quality, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom spawner entity drop with custom catch quality feedback and selection condition.
         *
         * @param spawner   custom spawner function
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull Function<Location, LivingEntity> spawner,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return entity(spawner, weight, quality, null, false, condition);
        }

        /**
         * Adds a custom spawner entity drop with custom catch quality feedback and catch message.
         *
         * @param spawner      custom spawner function
         * @param weight       selection weight
         * @param quality      catch quality tier for feedback
         * @param catchMessage custom catch message
         * @return this builder
         */
        public Builder entity(
                @NonNull Function<Location, LivingEntity> spawner,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage
        ) {
            return entity(spawner, weight, quality, catchMessage, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom spawner entity drop with full customization options.
         *
         * @param spawner              custom spawner function
         * @param weight               selection weight
         * @param quality              catch quality tier for feedback
         * @param catchMessage         custom catch message
         * @param suppressStandardText whether to suppress standard catch header prefix
         * @param condition            condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull Function<Location, LivingEntity> spawner,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage,
                boolean suppressStandardText,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(spawner, "spawner cannot be null");
            return registerEntityEntry(
                    EntityResult.custom(spawner),
                    weight,
                    quality,
                    catchMessage,
                    suppressStandardText,
                    condition
            );
        }

        /**
         * Adds a custom loot result entity drop with common catch quality.
         *
         * @param result custom loot result
         * @param weight selection weight
         * @return this builder
         */
        public Builder entity(@NonNull LootResult result, double weight) {
            return entity(result, weight, FishingCatchQuality.COMMON, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result entity drop with custom catch quality feedback.
         *
         * @param result  custom loot result
         * @param weight  selection weight
         * @param quality catch quality tier for feedback
         * @return this builder
         */
        public Builder entity(@NonNull LootResult result, double weight, @NonNull FishingCatchQuality quality) {
            return entity(result, weight, quality, null, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result entity drop with custom catch quality feedback and selection condition.
         *
         * @param result    custom loot result
         * @param weight    selection weight
         * @param quality   catch quality tier for feedback
         * @param condition condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull LootResult result,
                double weight,
                @NonNull FishingCatchQuality quality,
                @NonNull LootCondition condition
        ) {
            return entity(result, weight, quality, null, false, condition);
        }

        /**
         * Adds a custom loot result entity drop with custom catch quality feedback and catch message.
         *
         * @param result       custom loot result
         * @param weight       selection weight
         * @param quality      catch quality tier for feedback
         * @param catchMessage custom catch message
         * @return this builder
         */
        public Builder entity(
                @NonNull LootResult result,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage
        ) {
            return entity(result, weight, quality, catchMessage, false, LootCondition.alwaysTrue());
        }

        /**
         * Adds a custom loot result entity drop with full customization options.
         *
         * @param result               custom loot result
         * @param weight               selection weight
         * @param quality              catch quality tier for feedback
         * @param catchMessage         custom catch message
         * @param suppressStandardText whether to suppress standard catch header prefix
         * @param condition            condition required for eligibility
         * @return this builder
         */
        public Builder entity(
                @NonNull LootResult result,
                double weight,
                @NonNull FishingCatchQuality quality,
                @Nullable String catchMessage,
                boolean suppressStandardText,
                @NonNull LootCondition condition
        ) {
            Objects.requireNonNull(result, "result cannot be null");
            return registerEntityEntry(result, weight, quality, catchMessage, suppressStandardText, condition);
        }

        private Builder registerItemEntry(
                LootResult result,
                int minAmount,
                int maxAmount,
                double weight,
                FishingCatchQuality quality,
                LootCondition condition
        ) {
            Objects.requireNonNull(quality, "quality cannot be null");
            Objects.requireNonNull(condition, "condition cannot be null");

            AmountProvider amountProvider = (minAmount == maxAmount)
                    ? AmountProvider.fixed(minAmount)
                    : AmountProvider.range(minAmount, maxAmount);

            itemTableBuilder.addEntry(
                    LootEntry.weighted(
                            new GenericFeedbackDecorator<>(result, quality, FISHING_FEEDBACK_MAPPER),
                            weight,
                            amountProvider,
                            condition
                    )
            );
            return this;
        }

        private Builder registerEntityEntry(
                LootResult result,
                double weight,
                FishingCatchQuality quality,
                String catchMessage,
                boolean suppressStandardText,
                LootCondition condition
        ) {
            Objects.requireNonNull(quality, "quality cannot be null");
            Objects.requireNonNull(condition, "condition cannot be null");

            Function<FishingCatchQuality, FeedbackSpecification> mapper;
            if (catchMessage != null || suppressStandardText) {
                mapper = catchQuality -> {
                    FeedbackSpecification base = suppressStandardText
                            ? FISHING_SOUND_MAPPER.apply(catchQuality)
                            : FISHING_FEEDBACK_MAPPER.apply(catchQuality);
                    return new FeedbackSpecification(
                            catchMessage,
                            suppressStandardText ? null : (base != null ? base.messagePrefix() : null),
                            base != null ? base.sound() : null,
                            base != null ? base.pitch() : 1.0f
                    );
                };
            } else {
                mapper = FISHING_FEEDBACK_MAPPER;
            }

            entityTableBuilder.addEntry(
                    LootEntry.weighted(
                            new GenericFeedbackDecorator<>(result, quality, mapper),
                            weight,
                            AmountProvider.fixed(1),
                            condition
                    )
            );
            return this;
        }

        /**
         * Constructs the configured {@link FishingLootTable}.
         *
         * @return new fishing loot table
         */
        @NonNull
        public FishingLootTable build() {
            return new FishingLootTable(
                    entityTableBuilder
                            .selectionStrategy(new WeightedSelectionStrategy())
                            .rollChance(1.0)
                            .build(),
                    itemTableBuilder
                            .selectionStrategy(new WeightedSelectionStrategy())
                            .rollChance(1.0)
                            .build(),
                    entityRollChance
            );
        }
    }
}
