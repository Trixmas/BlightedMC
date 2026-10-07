package fr.moussax.blightedSMP.engine.items;

import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityExecutor;
import fr.moussax.blightedSMP.engine.items.abilities.FullSetBonus;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.lore.ItemLoreRenderer;
import fr.moussax.blightedSMP.engine.recipes.RecipePreviewManager;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Custom item definition with persistent identity, rarity, abilities, and restriction rules.
 *
 * <p>Extends {@link ItemBuilder} to provide fluent construction while integrating with the
 * BlightedMC item system (ability execution, set bonuses, restriction rules, and recipe previews).
 * Built stacks are marked with persistent metadata keys {@link #BLIGHTED_ID_KEY} and
 * {@link #BLIGHTED_RARITY_KEY}.</p>
 */
public class BlightedItem extends ItemBuilder implements Supplier<ItemStack> {

    public static final NamespacedKey BLIGHTED_ID_KEY = BlightedSMP.getInstance() != null
            ? new NamespacedKey(BlightedSMP.getInstance(), "blighted_id")
            : NamespacedKey.fromString("blightedsmp:blighted_id");
    public static final NamespacedKey BLIGHTED_RARITY_KEY = BlightedSMP.getInstance() != null
            ? new NamespacedKey(BlightedSMP.getInstance(), "blighted_rarity")
            : NamespacedKey.fromString("blightedsmp:blighted_rarity");
    public static final NamespacedKey BLIGHTED_SOULBOUND_KEY = BlightedSMP.getInstance() != null
            ? new NamespacedKey(BlightedSMP.getInstance(), "blighted_soulbound")
            : NamespacedKey.fromString("blightedsmp:blighted_soulbound");

    @Getter
    private final String itemId;
    @Getter
    private final ItemRarity itemRarity;
    @Getter
    private final ItemType itemType;
    @Getter
    private FullSetBonus fullSetBonus;
    @Getter
    private final List<ItemAbility<? extends Event>> abilities = new ArrayList<>();
    @Getter
    private final Set<ItemRestriction> restrictions = EnumSet.noneOf(ItemRestriction.class);
    @Getter
    private final List<String> description = new ArrayList<>();
    @Getter
    private final List<String> customLoreLines = new ArrayList<>();
    @Getter
    private boolean recipePreviewEnabled = false;
    @Getter
    private boolean spacedRarity = true;
    @Getter
    private boolean soulbound = false;
    @Getter
    private ItemConsumeHandler consumeHandler;

    /**
     * Constructs a custom item definition from a base material.
     *
     * @param itemId   unique item identifier
     * @param type     item category
     * @param rarity   item rarity
     * @param material base material
     */
    public BlightedItem(@NonNull String itemId, @NonNull ItemType type, @NonNull ItemRarity rarity, @NonNull Material material) {
        super(material);
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.itemType = Objects.requireNonNull(type, "itemType cannot be null");
        this.itemRarity = Objects.requireNonNull(rarity, "itemRarity cannot be null");
    }

    /**
     * Constructs a custom item definition from an existing item stack.
     *
     * @param itemId    unique item identifier
     * @param type      item category
     * @param rarity    item rarity
     * @param itemStack base item stack
     */
    public BlightedItem(@NonNull String itemId, @NonNull ItemType type, @NonNull ItemRarity rarity, @NonNull ItemStack itemStack) {
        super(itemStack);
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.itemType = Objects.requireNonNull(type, "itemType cannot be null");
        this.itemRarity = Objects.requireNonNull(rarity, "itemRarity cannot be null");
    }

    /**
     * Sets description lines describing this item.
     *
     * @param lines description lines
     * @return this item instance for method chaining
     */
    public BlightedItem description(String... lines) {
        this.description.clear();
        Collections.addAll(this.description, lines);
        return this;
    }

    /**
     * Adds an ability to this item.
     *
     * @param ability ability to add
     * @return this item instance for method chaining
     */
    public BlightedItem addAbility(ItemAbility<? extends Event> ability) {
        if (ability != null) {
            this.abilities.add(ability);
        }
        return this;
    }

    @SafeVarargs
    public final BlightedItem addAbilities(ItemAbility<? extends Event>... abilities) {
        for (ItemAbility<? extends Event> ability : abilities) {
            addAbility(ability);
        }
        return this;
    }

    /**
     * Sets the full-set bonus for this item.
     *
     * @param fullSetBonus set bonus to assign, or {@code null} to remove
     * @return this item instance for method chaining
     */
    public BlightedItem setFullSetBonus(FullSetBonus fullSetBonus) {
        this.fullSetBonus = fullSetBonus;
        return this;
    }

    /**
     * Registers a behavioral restriction flag for this item.
     *
     * @param restriction restriction to add
     * @return this item instance for method chaining
     */
    public BlightedItem addRestriction(@NonNull ItemRestriction restriction) {
        this.restrictions.add(Objects.requireNonNull(restriction, "restriction cannot be null"));
        return this;
    }

    /**
     * Checks if this item has the given restriction flag.
     *
     * @param restriction restriction to check
     * @return {@code true} if restricted, {@code false} otherwise
     */
    public boolean hasRestriction(@NonNull ItemRestriction restriction) {
        return this.restrictions.contains(restriction);
    }

    public BlightedItem preventPlacement() {
        return addRestriction(ItemRestriction.PREVENT_PLACEMENT);
    }

    public BlightedItem preventConsume() {
        return addRestriction(ItemRestriction.PREVENT_CONSUMPTION);
    }

    public BlightedItem preventProjectileLaunch() {
        return addRestriction(ItemRestriction.PREVENT_PROJECTILE_LAUNCH);
    }

    public BlightedItem preventBucketInteractions() {
        return addRestriction(ItemRestriction.PREVENT_BUCKET_INTERACTIONS);
    }

    public BlightedItem preventDrop() {
        return addRestriction(ItemRestriction.PREVENT_DROP);
    }

    public BlightedItem preventInteraction() {
        return addRestriction(ItemRestriction.PREVENT_INTERACTION);
    }

    /**
     * Marks this item as soulbound, preventing it from being dropped and preserving it across player deaths.
     *
     * @return this item instance for method chaining
     */
    public BlightedItem soulbound() {
        return soulbound(true);
    }

    /**
     * Configures whether this item is soulbound.
     *
     * @param soulbound {@code true} if soulbound, {@code false} otherwise
     * @return this item instance for method chaining
     */
    public BlightedItem soulbound(boolean soulbound) {
        this.soulbound = soulbound;
        if (soulbound) {
            preventDrop();
        } else {
            this.restrictions.remove(ItemRestriction.PREVENT_DROP);
        }
        return this;
    }

    /**
     * Appends custom lore lines to this item.
     *
     * @param lines lore lines
     * @return this item instance for method chaining
     */
    @Override
    public BlightedItem addLore(String... lines) {
        Collections.addAll(this.customLoreLines, lines);
        return this;
    }

    /**
     * Configures the item lore footer to sit flush against the preceding line without an empty spacer line.
     *
     * @return this item instance for method chaining
     */
    public BlightedItem flushRarity() {
        this.spacedRarity = false;
        return this;
    }

    /**
     * Configures the item lore footer to include an empty spacer line before the rarity tag.
     *
     * @return this item instance for method chaining
     */
    public BlightedItem spacedRarity() {
        this.spacedRarity = true;
        return this;
    }

    /**
     * Configures whether an empty spacer line precedes the rarity footer.
     *
     * @param spaced {@code true} to include an empty line spacer, {@code false} to sit flush
     * @return this item instance for method chaining
     */
    public BlightedItem padRarity(boolean spaced) {
        this.spacedRarity = spaced;
        return this;
    }

    /**
     * Enables recipe preview support for this item.
     */
    public void enableRecipePreview() {
        this.recipePreviewEnabled = true;
    }

    /**
     * Opens the recipe preview interface for a player.
     *
     * @param player     player viewing the preview
     * @param parentMenu parent menu to return to, or {@code null} for no parent
     * @return {@code true} if the preview opened successfully, {@code false} otherwise
     */
    public boolean openRecipePreview(@NonNull Player player, @Nullable Menu parentMenu) {
        return RecipePreviewManager.openPreview(player, this, parentMenu);
    }

    /**
     * Opens the recipe preview interface for a player without a parent menu.
     *
     * @param player player viewing the preview
     * @return {@code true} if the preview opened successfully, {@code false} otherwise
     */
    public boolean openRecipePreview(@NonNull Player player) {
        return openRecipePreview(player, null);
    }

    /**
     * Sets the item display name, prefixing it with the rarity color code.
     *
     * @param displayName new display name
     * @return this item builder instance
     */
    @Override
    public BlightedItem setDisplayName(@NonNull String displayName) {
        super.setDisplayName(itemRarity.getColorPrefix() + displayName);
        return this;
    }

    /**
     * Resolves the registered custom item definition corresponding to an item stack safely.
     *
     * @param itemStack item stack to inspect
     * @return registered custom item definition, or {@code null} if stack is non-custom or unregistered
     */
    @Nullable
    public static BlightedItem fromItemStack(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return null;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return null;

        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        String itemId = container.get(BLIGHTED_ID_KEY, PersistentDataType.STRING);
        if (itemId == null) return null;

        return ItemRegistry.get(itemId);
    }

    /**
     * Triggers active abilities on this item that match a triggering event.
     *
     * @param blightedPlayer player context executing the ability
     * @param event          triggering event
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void triggerAbilities(BlightedPlayer blightedPlayer, Event event) {
        for (ItemAbility<? extends Event> ability : abilities) {
            if (ability.getTrigger().matches(event)) {
                AbilityExecutor.execute((ItemAbility) ability, blightedPlayer, event);
            }
        }
    }

    /**
     * Registers an item consume handler receiving both the player and item stack.
     *
     * @param consumeHandler consume handler callback
     * @return this item instance for method chaining
     */
    public BlightedItem onConsume(ItemConsumeHandler consumeHandler) {
        this.consumeHandler = consumeHandler;
        return this;
    }

    /**
     * Registers a simple consume handler receiving the consuming player.
     *
     * @param consumeHandler consume callback receiving the player
     */
    public void onConsume(Consumer<Player> consumeHandler) {
        this.consumeHandler = (player, _) -> consumeHandler.accept(player);
    }

    /**
     * Evaluates whether this item is classified as equippable gear (weapon, armor, or tool).
     *
     * @return {@code true} if equippable equipment, {@code false} otherwise
     */
    public boolean isEquipment() {
        if (itemType == null || itemType.getCategory() == null) return false;
        return switch (itemType.getCategory()) {
            case ARMOR, MELEE_WEAPON, RANGE_WEAPON, TOOLS -> true;
            default -> false;
        };
    }

    /**
     * Checks if the given item stack is marked as soulbound.
     *
     * @param itemStack item stack to check
     * @return {@code true} if marked as soulbound, {@code false} otherwise
     */
     public static boolean isSoulbound(@Nullable ItemStack itemStack) {
         if (itemStack == null || itemStack.getType().isAir()) return false;
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta == null) return false;
         PersistentDataContainer container = itemMeta.getPersistentDataContainer();
         if (container.has(BLIGHTED_SOULBOUND_KEY, PersistentDataType.BOOLEAN)) {
             return Boolean.TRUE.equals(container.get(BLIGHTED_SOULBOUND_KEY, PersistentDataType.BOOLEAN));
         }
         if (container.has(BLIGHTED_SOULBOUND_KEY, PersistentDataType.BYTE)) {
             Byte byteValue = container.get(BLIGHTED_SOULBOUND_KEY, PersistentDataType.BYTE);
             return byteValue != null && byteValue == 1;
         }
         BlightedItem blighted = fromItemStack(itemStack);
         return blighted != null && blighted.isSoulbound();
     }

    /**
     * Builds the item stack, applies metadata, renders canonical lore, and returns an independent clone.
     *
     * @return configured, pristine item stack
     */
    @Override
    public ItemStack toItemStack() {
        setPersistentData(BLIGHTED_ID_KEY, PersistentDataType.STRING, itemId);
        setPersistentData(BLIGHTED_RARITY_KEY, PersistentDataType.STRING, itemRarity.name());
        if (soulbound) {
            setPersistentData(BLIGHTED_SOULBOUND_KEY, PersistentDataType.BOOLEAN, true);
        }

        List<String> renderedLore = ItemLoreRenderer.render(this);
        super.setLore(renderedLore);

        return super.toItemStack().clone();
    }

    /**
     * Creates an item stack instance by delegating to {@link #toItemStack()}.
     *
     * @return newly built item stack
     */
    @Override
    public ItemStack get() {
        return this.toItemStack();
    }
}
