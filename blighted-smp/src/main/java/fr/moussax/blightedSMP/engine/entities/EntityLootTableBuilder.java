package fr.moussax.blightedSMP.engine.entities;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.loot.LootCondition;
import fr.moussax.blightedSMP.engine.loot.LootEntry;
import fr.moussax.blightedSMP.engine.loot.LootResult;
import fr.moussax.blightedSMP.engine.loot.LootTable;
import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import fr.moussax.blightedSMP.engine.loot.decorators.FeedbackSpecification;
import fr.moussax.blightedSMP.engine.loot.decorators.GenericFeedbackDecorator;
import fr.moussax.blightedSMP.engine.loot.providers.AmountProvider;
import fr.moussax.blightedSMP.engine.loot.results.ItemResult;
import fr.moussax.blightedSMP.engine.loot.results.blightstone.ResonantBlightstoneResult;
import fr.moussax.blightedSMP.engine.loot.strategies.LootingAwareProbabilisticStrategy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent builder for configuring probabilistic entity loot tables with rarity feedback.
 */
public final class EntityLootTableBuilder {

    private static final Function<EntityLootRarity, FeedbackSpecification> ENTITY_FEEDBACK_MAPPER = rarity -> switch (rarity) {
        case RARE ->
                FeedbackSpecification.full(" §f§lRARE DROP! §f| §7You found §f", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.8f);
        case VERY_RARE ->
                FeedbackSpecification.full(" §b§lVERY RARE DROP! §f| §7You found §f", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f);
        case CRAZY ->
                FeedbackSpecification.full(" §d§lCRAZY DROP! §f| §7You found §f", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f);
        case INSANE ->
                FeedbackSpecification.full(" §c§lINSANE DROP! §f| §7You found §f", Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f);
        default -> null;
    };

    private final LootTable.Builder builder = LootTable.builder();
    private int maxDrops = 3;

    /**
     * Sets the maximum number of loot drops allowed per roll.
     *
     * @param maxDrops maximum drop count
     * @return this builder
     */
    public EntityLootTableBuilder maxDrops(int maxDrops) {
        this.maxDrops = maxDrops;
        return this;
    }

    public EntityLootTableBuilder drop(@NonNull Material material, double dropChance) {
        return drop(material, 1, 1, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(material, 1, 1, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, int count, double dropChance) {
        return drop(material, count, count, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, int count, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(material, count, count, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, int minAmount, int maxAmount, double dropChance) {
        return drop(material, minAmount, maxAmount, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, int minAmount, int maxAmount, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(material, minAmount, maxAmount, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(
            @NonNull Material material,
            int minAmount,
            int maxAmount,
            double dropChance,
            @NonNull EntityLootRarity rarity,
            @NonNull LootCondition condition
    ) {
        Objects.requireNonNull(material, "material cannot be null");
        return registerEntry(ItemResult.of(material), minAmount, maxAmount, dropChance, rarity, condition);
    }

    public EntityLootTableBuilder drop(@NonNull Material material, @NonNull Consumer<ItemBuilder> modifier, double dropChance) {
        return drop(material, modifier, 1, 1, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, @NonNull Consumer<ItemBuilder> modifier, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(material, modifier, 1, 1, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, @NonNull Consumer<ItemBuilder> modifier, int minAmount, int maxAmount, double dropChance) {
        return drop(material, modifier, minAmount, maxAmount, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull Material material, @NonNull Consumer<ItemBuilder> modifier, int minAmount, int maxAmount, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(material, modifier, minAmount, maxAmount, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(
            @NonNull Material material,
            @NonNull Consumer<ItemBuilder> modifier,
            int minAmount,
            int maxAmount,
            double dropChance,
            @NonNull EntityLootRarity rarity,
            @NonNull LootCondition condition
    ) {
        Objects.requireNonNull(material, "material cannot be null");
        Objects.requireNonNull(modifier, "modifier cannot be null");
        return registerEntry(ItemResult.of(material, modifier), minAmount, maxAmount, dropChance, rarity, condition);
    }

    public EntityLootTableBuilder drop(@NonNull String itemId, double dropChance) {
        return drop(itemId, 1, 1, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull String itemId, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(itemId, 1, 1, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull String itemId, int count, double dropChance) {
        return drop(itemId, count, count, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull String itemId, int count, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(itemId, count, count, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull String itemId, int minAmount, int maxAmount, double dropChance) {
        return drop(itemId, minAmount, maxAmount, dropChance, EntityLootRarity.COMMON, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(@NonNull String itemId, int minAmount, int maxAmount, double dropChance, @NonNull EntityLootRarity rarity) {
        return drop(itemId, minAmount, maxAmount, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder drop(
            @NonNull String itemId,
            int minAmount,
            int maxAmount,
            double dropChance,
            @NonNull EntityLootRarity rarity,
            @NonNull LootCondition condition
    ) {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        return registerEntry(ItemResult.of(itemId), minAmount, maxAmount, dropChance, rarity, condition);
    }

    public EntityLootTableBuilder drop(@NonNull BlightedItem item, double dropChance) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), dropChance);
    }

    public EntityLootTableBuilder drop(@NonNull BlightedItem item, double dropChance, @NonNull EntityLootRarity rarity) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), dropChance, rarity);
    }

    public EntityLootTableBuilder drop(@NonNull BlightedItem item, int count, double dropChance) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), count, dropChance);
    }

    public EntityLootTableBuilder drop(@NonNull BlightedItem item, int count, double dropChance, @NonNull EntityLootRarity rarity) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), count, dropChance, rarity);
    }

    public EntityLootTableBuilder drop(@NonNull BlightedItem item, int minAmount, int maxAmount, double dropChance) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), minAmount, maxAmount, dropChance);
    }

    public EntityLootTableBuilder drop(@NonNull BlightedItem item, int minAmount, int maxAmount, double dropChance, @NonNull EntityLootRarity rarity) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), minAmount, maxAmount, dropChance, rarity);
    }

    public EntityLootTableBuilder drop(
            @NonNull BlightedItem item,
            int minAmount,
            int maxAmount,
            double dropChance,
            @NonNull EntityLootRarity rarity,
            @NonNull LootCondition condition
    ) {
        Objects.requireNonNull(item, "item cannot be null");
        return drop(item.getItemId(), minAmount, maxAmount, dropChance, rarity, condition);
    }

    public EntityLootTableBuilder blight(int blight, double dropChance) {
        return blight(blight, dropChance, EntityLootRarity.COMMON);
    }

    public EntityLootTableBuilder blight(int blight, double dropChance, @NonNull EntityLootRarity rarity) {
        Objects.requireNonNull(rarity, "rarity cannot be null");
        return registerEntry(new ResonantBlightstoneResult(), blight, blight, dropChance, rarity, LootCondition.alwaysTrue());
    }

    public EntityLootTableBuilder blightstone(int amount, double dropChance) {
        return blight(amount, dropChance);
    }

    public EntityLootTableBuilder blightstone(int amount, double dropChance, @NonNull EntityLootRarity rarity) {
        return blight(amount, dropChance, rarity);
    }

    public EntityLootTableBuilder damagedItem(
            @NonNull Material material,
            double minPercentage,
            double maxPercentage,
            double dropChance
    ) {
        return damagedItem(material, minPercentage, maxPercentage, dropChance, EntityLootRarity.COMMON);
    }

    public EntityLootTableBuilder damagedItem(
            @NonNull Material material,
            double minPercentage,
            double maxPercentage,
            double dropChance,
            @NonNull EntityLootRarity rarity
    ) {
        Objects.requireNonNull(material, "material cannot be null");
        Objects.requireNonNull(rarity, "rarity cannot be null");
        return registerEntry(
                ItemResult.randomDurability(material, minPercentage, maxPercentage),
                1, 1, dropChance, rarity, LootCondition.alwaysTrue()
        );
    }

    public EntityLootTableBuilder enchantedBook(
            @NonNull List<Enchantment> enchantments,
            int minLevel,
            int maxLevel,
            double dropChance
    ) {
        return enchantedBook(enchantments, minLevel, maxLevel, dropChance, EntityLootRarity.COMMON);
    }

    public EntityLootTableBuilder enchantedBook(
            @NonNull List<Enchantment> enchantments,
            int minLevel,
            int maxLevel,
            double dropChance,
            @NonNull EntityLootRarity rarity
    ) {
        Objects.requireNonNull(enchantments, "enchantments cannot be null");
        Objects.requireNonNull(rarity, "rarity cannot be null");
        return registerEntry(
                ItemResult.randomEnchantedBook(enchantments, minLevel, maxLevel),
                1, 1, dropChance, rarity, LootCondition.alwaysTrue()
        );
    }

    public EntityLootTableBuilder enchantedBook(
            @NonNull Map<Enchantment, Integer> enchantmentPool,
            double dropChance
    ) {
        return enchantedBook(enchantmentPool, dropChance, EntityLootRarity.COMMON);
    }

    public EntityLootTableBuilder enchantedBook(
            @NonNull Map<Enchantment, Integer> enchantmentPool,
            double dropChance,
            @NonNull EntityLootRarity rarity
    ) {
        Objects.requireNonNull(enchantmentPool, "enchantmentPool cannot be null");
        Objects.requireNonNull(rarity, "rarity cannot be null");
        return registerEntry(
                ItemResult.randomEnchantedBook(enchantmentPool),
                1, 1, dropChance, rarity, LootCondition.alwaysTrue()
        );
    }

    private EntityLootTableBuilder registerEntry(
            LootResult result,
            int minAmount,
            int maxAmount,
            double dropChance,
            EntityLootRarity rarity,
            LootCondition condition
    ) {
        AmountProvider amountProvider = (minAmount == maxAmount)
                ? AmountProvider.fixed(minAmount)
                : AmountProvider.range(minAmount, maxAmount);

        builder.addEntry(
                LootEntry.probabilistic(
                        new GenericFeedbackDecorator<>(result, rarity, ENTITY_FEEDBACK_MAPPER),
                        dropChance,
                        amountProvider,
                        condition
                )
        );
        return this;
    }

    /**
     * Constructs configured {@link LootTable} instance.
     *
     * @return new entity loot table
     */
    @NonNull
    public LootTable build() {
        return builder
                .selectionStrategy(new LootingAwareProbabilisticStrategy(maxDrops))
                .rollChance(1.0)
                .build();
    }
}
