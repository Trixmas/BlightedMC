package fr.moussax.bedrock.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.utils.debug.Log;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.*;
import org.bukkit.inventory.meta.components.consumable.ConsumableComponent;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.tag.DamageTypeTags;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent builder for creating and configuring Bukkit {@link ItemStack} instances.
 *
 * <p>Changes are stored internally and applied when {@link #toItemStack()} is called.
 * Supports item metadata such as names, lore, enchantments, attributes, potion effects,
 * banner patterns, armor trims, skull textures, and other Bukkit components.</p>
 *
 * <p>The builder works on a copy of the provided item and is not thread-safe.
 * Some operations are only available for specific item types and are ignored when
 * unsupported by the underlying {@link ItemMeta}.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "UnusedReturnValue"})
public class ItemBuilder {

    private final ItemStack item;
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private final List<PotionEffect> customEffects = new ArrayList<>();
    private ItemMeta itemMeta;
    private boolean unstackable;
    private List<Pattern> bannerPatterns;

    private PlayerProfile skullOwnerProfile;
    private String base64Texture;
    private Color leatherColor;
    private Color potionColor;
    private ArmorTrim armorTrim;

    /**
     * Creates an item builder from a material.
     *
     * @param material the item material
     */
    public ItemBuilder(@NonNull Material material) {
        this(new ItemStack(material));
    }

    /**
     * Creates an item builder from a material with an amount.
     *
     * @param material the item material
     * @param amount   the item amount
     * @throws IllegalArgumentException if the amount exceeds the material stack size
     */
    public ItemBuilder(@NonNull Material material, int amount) {
        this(new ItemStack(material, validateAmount(material, amount)));
    }

    /**
     * Creates an item builder from a material with a display name.
     *
     * @param material    the item material
     * @param displayName the item display name
     */
    public ItemBuilder(@NonNull Material material, @NonNull String displayName) {
        this(new ItemStack(material));
        this.itemMeta.setDisplayName(ColorUtils.colorize(displayName));
    }

    /**
     * Creates an item builder from a material with a display name.
     *
     * @param material    the item material
     * @param displayName the item display name
     */
    public ItemBuilder(@NonNull Material material, int amount, @NonNull String displayName) {
        this(new ItemStack(material, validateAmount(material, amount)));
        this.itemMeta.setDisplayName(ColorUtils.colorize(displayName));
    }

    /**
     * Creates an item builder from an existing item.
     *
     * <p>The provided item is copied and will not be modified directly.</p>
     *
     * @param itemStack the source item
     */
    public ItemBuilder(@NonNull ItemStack itemStack) {
        this.item = itemStack.clone();
        ItemMeta meta = this.item.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException("ItemMeta cannot be null for material: " + itemStack.getType());
        }

        this.itemMeta = meta;
        this.enchantments.putAll(itemStack.getEnchantments());

        if (this.itemMeta instanceof EnchantmentStorageMeta storageMeta) {
            this.enchantments.putAll(storageMeta.getStoredEnchants());
        }
    }

    private static int validateAmount(@NonNull Material material, int amount) {
        if (amount < 1 || amount > 99) {
            throw new IllegalArgumentException("Amount must be between 1 and 99, but was " + amount);
        }
        return amount;
    }

    private static void applyCustomTexture(SkullMeta meta, String textureInput) {
        String url;
        if (textureInput.startsWith("http://") || textureInput.startsWith("https://")) {
            url = textureInput;
        } else if (textureInput.matches("^[a-fA-F0-9]{64}$")) {
            url = "http://textures.minecraft.net/texture/" + textureInput;
        } else {
            String json = new String(Base64.getDecoder().decode(textureInput), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json)
                    .getAsJsonObject().getAsJsonObject("textures")
                    .getAsJsonObject("SKIN");
            url = object.get("url").getAsString();
        }

        UUID id = UUID.randomUUID();
        PlayerProfile profile = Bukkit.createPlayerProfile(id, id.toString().substring(0, 16));
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URI(url).toURL());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid texture URL: " + url, exception);
        }

        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
    }

    private static boolean isLeatherDyeable(Material material) {
        return switch (material) {
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS,
                 LEATHER_BOOTS, LEATHER_HORSE_ARMOR -> true;
            default -> false;
        };
    }

    /**
     * Sets the item name.
     *
     * @param name the item name
     * @return this builder
     */
    public ItemBuilder setItemName(@NonNull String name) {
        this.itemMeta.setItemName(ColorUtils.colorize(name));
        return this;
    }

    /**
     * Sets the item display name.
     *
     * @param displayName the display name
     * @return this builder
     */
    public ItemBuilder setDisplayName(@NonNull String displayName) {
        this.itemMeta.setDisplayName(ColorUtils.colorize(displayName));
        return this;
    }

    /**
     * Adds multiple lore lines to the item.
     *
     * @param lines the lore lines to add
     * @return this builder
     */
    public ItemBuilder addLore(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) return this;
        List<String> lore = itemMeta.getLore() != null ? new ArrayList<>(itemMeta.getLore()) : new ArrayList<>();
        lore.addAll(ColorUtils.colorize(lines));
        itemMeta.setLore(lore);
        return this;
    }

    /**
     * Adds one or more lore lines to the item.
     *
     * @param lines the lore lines to add
     * @return this builder
     */
    public ItemBuilder addLore(String... lines) {
        return lines != null ? addLore(Arrays.asList(lines)) : this;
    }

    /**
     * Sets the item lore, replacing any existing lore.
     *
     * @param lines the lore lines
     * @return this builder
     */
    public ItemBuilder setLore(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            itemMeta.setLore(null);
            return this;
        }
        itemMeta.setLore(ColorUtils.colorize(lines));
        return this;
    }

    /**
     * Sets the item lore, replacing any existing lore.
     *
     * @param lines the lore lines
     * @return this builder
     */
    public ItemBuilder setLore(String... lines) {
        return setLore(lines != null ? Arrays.asList(lines) : Collections.emptyList());
    }

    /**
     * Adds an empty line to the item lore.
     *
     * @return this builder
     */
    public ItemBuilder addEmptyLore() {
        return addLore("");
    }

    /**
     * Clears all lore from the item.
     *
     * @return this builder
     */
    public ItemBuilder clearLore() {
        itemMeta.setLore(null);
        return this;
    }

    /**
     * Replaces a lore line at the given index.
     *
     * @param index the lore line index
     * @param line  the replacement line
     * @return this builder
     * @throws IndexOutOfBoundsException if the index does not exist
     */
    public ItemBuilder setLore(int index, String line) {
        List<String> lore = itemMeta.getLore();
        if (lore == null || index < 0 || index >= lore.size()) {
            throw new IndexOutOfBoundsException("Invalid lore index: " + index);
        }
        lore.set(index, ColorUtils.colorize(line));
        itemMeta.setLore(lore);
        return this;
    }

    /**
     * Sets the item amount.
     *
     * @param amount the item amount
     * @return this builder
     * @throws IllegalArgumentException if the amount exceeds the maximum stack size
     */
    public ItemBuilder setAmount(int amount) {
        int effectiveMax = itemMeta.hasMaxStackSize() ? itemMeta.getMaxStackSize() : 99;
        if (amount < 1 || amount > effectiveMax) {
            throw new IllegalArgumentException("Amount must be between 1 and " + effectiveMax);
        }
        item.setAmount(amount);
        return this;
    }

    /**
     * Sets the maximum stack size of the item.
     *
     * @param size the maximum stack size
     * @return this builder
     */
    public ItemBuilder setMaxStackSize(int size) {
        itemMeta.setMaxStackSize(size);
        return this;
    }

    /**
     * Sets the durability damage.
     *
     * <p>Ignored if the item does not support durability.</p>
     *
     * @param damage the durability damage
     * @return this builder
     */
    public ItemBuilder setDurabilityDamage(int damage) {
        if (itemMeta instanceof Damageable damageable) {
            damageable.setDamage(damage);
        }
        return this;
    }

    /**
     * Sets the maximum durability of the item.
     *
     * <p>Ignored if the item does not support durability.</p>
     *
     * @param maxDurability the maximum durability
     * @return this builder
     */
    public ItemBuilder setMaxDurability(int maxDurability) {
        if (itemMeta instanceof Damageable damageable) {
            damageable.setMaxDamage(maxDurability);
        }
        return this;
    }

    /**
     * Sets the item's durability percentage.
     *
     * <p>Ignored if the item does not support durability.</p>
     *
     * @param percent the remaining durability percentage
     * @return this builder
     */
    public ItemBuilder setDurabilityPercent(double percent) {
        if (!(itemMeta instanceof Damageable damageable)) return this;
        int maxDurability = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        int damage = (int) Math.round(maxDurability * (1.0 - percent));
        damageable.setDamage(Math.min(damage, maxDurability - 1));
        return this;
    }

    /**
     * Sets whether the item is unbreakable.
     *
     * @param unbreakable whether the item should ignore durability loss
     * @return this builder
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        itemMeta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder unbreakable() {
        return setUnbreakable(true);
    }

    public ItemBuilder setUnstackable(boolean unstackable) {
        this.unstackable = unstackable;
        return this;
    }

    public ItemBuilder unstackable() {
        return setUnstackable(true);
    }

    /**
     * Sets whether the item can be placed in lava or fire without being destroyed.
     *
     * @param fireResistant whether the item should resist fire damage
     * @return this builder
     */
    public ItemBuilder setFireResistant(boolean fireResistant) {
        itemMeta.setDamageResistant(fireResistant ? DamageTypeTags.IS_FIRE : null);
        return this;
    }

    public ItemBuilder fireResistant() {
        return setFireResistant(true);
    }

    /**
     * Sets the damage resistance tag of the item.
     *
     * @param tag the damage type tag that the item resists
     * @return this builder
     */
    public ItemBuilder setDamageResistant(@Nullable Tag<DamageType> tag) {
        itemMeta.setDamageResistant(tag);
        return this;
    }

    /**
     * Sets whether the item has gliding behavior.
     *
     * @param glider whether the item should behave as a glider
     * @return this builder
     */
    public ItemBuilder setGlider(boolean glider) {
        itemMeta.setGlider(glider);
        return this;
    }

    public ItemBuilder glider() {
        return setGlider(true);
    }

    /**
     * Sets whether the item tooltip is hidden.
     *
     * @param hideTooltip whether the tooltip should be hidden
     * @return this builder
     */
    public ItemBuilder setHideTooltip(boolean hideTooltip) {
        itemMeta.setHideTooltip(hideTooltip);
        return this;
    }

    public ItemBuilder hideTooltip() {
        return setHideTooltip(true);
    }

    /**
     * Sets the rarity of the item.
     *
     * @param rarity the item rarity
     * @return this builder
     */
    public ItemBuilder setRarity(ItemRarity rarity) {
        itemMeta.setRarity(rarity);
        return this;
    }

    /**
     * Sets the item's custom model identifier, or {@code null} to clear it.
     *
     * @param itemModel the model namespace key, or {@code null}
     * @return this builder
     */
    public ItemBuilder setItemModel(@Nullable NamespacedKey itemModel) {
        itemMeta.setItemModel(itemModel);
        return this;
    }

    /**
     * Sets the item's custom model data, or {@code null} to remove it.
     *
     * @param data the custom model data value, or {@code null} to remove it
     * @return this builder
     */
    public ItemBuilder setCustomModelData(@Nullable Integer data) {
        if (data == null) {
            itemMeta.setCustomModelDataComponent(null);
            return this;
        }

        CustomModelDataComponent component = itemMeta.getCustomModelDataComponent();
        component.setFloats(List.of((float) data));
        itemMeta.setCustomModelDataComponent(component);
        return this;
    }

    /**
     * Sets the tooltip style of the item, or {@code null} to clear it.
     *
     * @param tooltipStyle the tooltip style namespace key, or {@code null}
     * @return this builder
     */
    public ItemBuilder setTooltipStyle(@Nullable NamespacedKey tooltipStyle) {
        itemMeta.setTooltipStyle(tooltipStyle);
        return this;
    }

    /**
     * Adds or replaces persistent data on the item.
     *
     * @param key   the persistent data key
     * @param type  the persistent data type
     * @param value the value to store
     * @param <T>   the primitive data type
     * @param <Z>   the complex data type
     * @return this builder
     */
    public <T, Z> ItemBuilder setPersistentData(@NonNull NamespacedKey key, @NonNull PersistentDataType<T, Z> type, @NonNull Z value) {
        itemMeta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    /**
     * Removes persistent data from the item.
     *
     * @param key the persistent data key
     * @return this builder
     */
    public ItemBuilder removePersistentData(@NonNull NamespacedKey key) {
        itemMeta.getPersistentDataContainer().remove(key);
        return this;
    }

    /**
     * Adds an enchantment to the item.
     *
     * @param enchantment the enchantment
     * @param level       the enchantment level
     * @return this builder
     */
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
        return this;
    }

    /**
     * Adds multiple enchantments to the item.
     *
     * @param enchantments the enchantments to add
     * @return this builder
     */
    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        this.enchantments.putAll(enchantments);
        return this;
    }

    /**
     * Removes an enchantment from the item.
     *
     * @param enchantment the enchantment to remove
     * @return this builder
     */
    public ItemBuilder removeEnchantment(Enchantment enchantment) {
        enchantments.remove(enchantment);
        return this;
    }

    /**
     * Sets whether the item displays an enchantment glint override.
     *
     * @param glint whether the enchantment glint should be displayed
     * @return this builder
     */
    public ItemBuilder setEnchantmentGlint(boolean glint) {
        itemMeta.setEnchantmentGlintOverride(glint);
        return this;
    }

    /**
     * Displays an enchantment glint on the item without enchantments.
     *
     * @return this builder
     */
    public ItemBuilder glow() {
        return setEnchantmentGlint(true);
    }

    /**
     * Adds item flags to hide or alter item information.
     *
     * @param flags the item flags to add
     * @return this builder
     */
    public ItemBuilder addItemFlag(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }

    /**
     * Removes item flags from the item.
     *
     * @param flags the item flags to remove
     * @return this builder
     */
    public ItemBuilder removeItemFlag(ItemFlag... flags) {
        itemMeta.removeItemFlags(flags);
        return this;
    }

    /**
     * Adds all item flags to hide default vanilla item tooltips.
     *
     * @return this builder
     */
    public ItemBuilder hideAllFlags() {
        return addItemFlag(ItemFlag.values());
    }

    /**
     * Adds or replaces an attribute modifier on the item.
     *
     * <p>If a modifier with the same {@link NamespacedKey} already exists for this attribute,
     * it will be replaced.</p>
     *
     * @param attribute the attribute affected by the modifier
     * @param modifier  the attribute modifier
     * @return this builder
     */
    public ItemBuilder addAttributeModifier(Attribute attribute, AttributeModifier modifier) {
        if (itemMeta.getAttributeModifiers() != null) {
            Collection<AttributeModifier> existing = itemMeta.getAttributeModifiers().get(attribute);
            for (AttributeModifier old : new ArrayList<>(existing)) {
                if (old.getKey().equals(modifier.getKey())) {
                    itemMeta.removeAttributeModifier(attribute, old);
                }
            }
        }
        itemMeta.addAttributeModifier(attribute, modifier);
        return this;
    }

    /**
     * Adds an attribute modifier using the provided values and a deterministic namespaced key.
     *
     * @param attribute the attribute affected by the modifier
     * @param amount    the modifier amount
     * @param operation the modifier operation
     * @param slotGroup the equipment slots affected by the modifier
     * @return this builder
     */
    public ItemBuilder addAttributeModifier(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            EquipmentSlotGroup slotGroup
    ) {
        String keyName = attribute.getKeyOrThrow().getKey() + "_" + slotGroup.toString().toLowerCase(Locale.ROOT);
        NamespacedKey key = new NamespacedKey(PluginContext.get(), keyName);
        AttributeModifier modifier = new AttributeModifier(key, amount, operation, slotGroup);
        return addAttributeModifier(attribute, modifier);
    }

    /**
     * Sets the item's base attack damage attribute modifier in the main hand.
     *
     * <p><b>Note:</b> In Minecraft's Data Component model, entity attack damage is managed via
     * this attribute modifier rather than {@link #editWeapon(Consumer)}.</p>
     *
     * @param damage the attack damage value
     * @return this builder
     */
    public ItemBuilder setAttackDamage(double damage) {
        return addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                damage,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
    }

    /**
     * Sets the item's attack speed attribute modifier in the main hand.
     *
     * @param speed the attack speed modifier value
     * @return this builder
     */
    public ItemBuilder setAttackSpeed(double speed) {
        return addAttributeModifier(
                Attribute.ATTACK_SPEED,
                speed,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
    }

    /**
     * Modifies the item's equippable component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editEquippable(@NonNull Consumer<EquippableComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        EquippableComponent equippable = itemMeta.getEquippable();
        consumer.accept(equippable);
        itemMeta.setEquippable(equippable);
        return this;
    }

    /**
     * Prevents the item from being equipped through normal equippable behavior.
     *
     * <p>The item is assigned to the hand slot and marked as non-dispensable.</p>
     *
     * @return this builder
     */
    public ItemBuilder preventEquipping() {
        return editEquippable(equippable -> {
            equippable.setSlot(EquipmentSlot.HAND);
            equippable.setDispensable(false);
        });
    }

    /**
     * Modifies the item's food component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editFood(@NonNull Consumer<FoodComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        FoodComponent food = itemMeta.getFood();
        consumer.accept(food);
        itemMeta.setFood(food);
        return this;
    }

    /**
     * Modifies the item's tool component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editTool(@NonNull Consumer<ToolComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        ToolComponent tool = itemMeta.getTool();
        consumer.accept(tool);
        itemMeta.setTool(tool);
        return this;
    }

    /**
     * Modifies the item's weapon component.
     *
     * <p><b>Important:</b> In Minecraft's Data Component model, {@link WeaponComponent} controls
     * durability damage sustained by the weapon per attack ({@link WeaponComponent#setItemDamagePerAttack(int)})
     * and shield disable duration ({@link WeaponComponent#setDisableBlockingForSeconds(float)}).
     * It does <b>not</b> define the attack damage dealt to entities. To configure entity attack damage,
     * use {@link #setAttackDamage(double)}.</p>
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editWeapon(@NonNull Consumer<WeaponComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        WeaponComponent weapon = itemMeta.getWeapon();
        consumer.accept(weapon);
        itemMeta.setWeapon(weapon);
        return this;
    }

    /**
     * Modifies the item's piercing weapon component (spears, projectiles).
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editPiercingWeapon(@NonNull Consumer<PiercingWeaponComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        PiercingWeaponComponent piercing = itemMeta.getPiercingWeapon();
        consumer.accept(piercing);
        itemMeta.setPiercingWeapon(piercing);
        return this;
    }

    /**
     * Modifies the item's kinetic weapon component (maces, charge weapons).
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editKineticWeapon(@NonNull Consumer<KineticWeaponComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        KineticWeaponComponent kinetic = itemMeta.getKineticWeapon();
        consumer.accept(kinetic);
        itemMeta.setKineticWeapon(kinetic);
        return this;
    }

    /**
     * Modifies the item's attack range component (reach distance and margins).
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editAttackRange(@NonNull Consumer<AttackRangeComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        AttackRangeComponent attackRange = itemMeta.getAttackRange();
        consumer.accept(attackRange);
        itemMeta.setAttackRange(attackRange);
        return this;
    }

    /**
     * Modifies the item's attack blocking component (shields, defensive items).
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editBlocksAttacks(@NonNull Consumer<BlocksAttacksComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        BlocksAttacksComponent blocksAttacks = itemMeta.getBlocksAttacks();
        consumer.accept(blocksAttacks);
        itemMeta.setBlocksAttacks(blocksAttacks);
        return this;
    }

    /**
     * Modifies the item's swing animation component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editSwingAnimation(@NonNull Consumer<SwingAnimationComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        SwingAnimationComponent swing = itemMeta.getSwingAnimation();
        consumer.accept(swing);
        itemMeta.setSwingAnimation(swing);
        return this;
    }

    /**
     * Modifies the item's use effects component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editUseEffects(@NonNull Consumer<UseEffectsComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        UseEffectsComponent useEffects = itemMeta.getUseEffects();
        consumer.accept(useEffects);
        itemMeta.setUseEffects(useEffects);
        return this;
    }

    /**
     * Modifies the item's custom model data component (floats, strings, flags, colors).
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editCustomModelData(@NonNull Consumer<CustomModelDataComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        CustomModelDataComponent component = itemMeta.getCustomModelDataComponent();
        consumer.accept(component);
        itemMeta.setCustomModelDataComponent(component);
        return this;
    }

    /**
     * Sets the minimum attack charge required to strike with this item.
     *
     * @param charge minimum charge fraction (between 0.0 and 1.0), or null to clear
     * @return this builder
     */
    public ItemBuilder setMinimumAttackCharge(@Nullable Float charge) {
        itemMeta.setMinimumAttackCharge(charge);
        return this;
    }

    /**
     * Sets the break sound played when the item runs out of durability.
     *
     * @param sound the break sound, or null to clear
     * @return this builder
     */
    public ItemBuilder setBreakSound(@Nullable Sound sound) {
        itemMeta.setBreakSound(sound);
        return this;
    }

    /**
     * Sets the custom damage type dealt by this item.
     *
     * @param damageType the damage type, or null to clear
     * @return this builder
     */
    public ItemBuilder setDamageType(@Nullable DamageType damageType) {
        itemMeta.setDamageType(damageType);
        return this;
    }

    /**
     * Modifies the item's jukebox playable component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editJukeboxPlayable(@NonNull Consumer<JukeboxPlayableComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        JukeboxPlayableComponent jukebox = itemMeta.getJukeboxPlayable();
        consumer.accept(jukebox);
        itemMeta.setJukeboxPlayable(jukebox);
        return this;
    }

    /**
     * Modifies the item's use cooldown component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editUseCooldown(@NonNull Consumer<UseCooldownComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        UseCooldownComponent cooldown = itemMeta.getUseCooldown();
        consumer.accept(cooldown);
        itemMeta.setUseCooldown(cooldown);
        return this;
    }

    /**
     * Modifies the item's consumable component.
     *
     * @param consumer the component modifier
     * @return this builder
     */
    public ItemBuilder editConsumable(@NonNull Consumer<ConsumableComponent> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        ConsumableComponent consumable = itemMeta.getConsumable();
        consumer.accept(consumable);
        itemMeta.setConsumable(consumable);
        return this;
    }

    /**
     * Sets the item returned after consuming or using this item.
     *
     * @param remainder the remaining item
     * @return this builder
     */
    public ItemBuilder setUseRemainder(@Nullable ItemStack remainder) {
        itemMeta.setUseRemainder(remainder);
        return this;
    }

    /**
     * Sets the item's enchantability value.
     *
     * @param value the enchantability value
     * @return this builder
     */
    public ItemBuilder setEnchantable(@Nullable Integer value) {
        itemMeta.setEnchantable(value);
        return this;
    }

    /**
     * Adds items to a bundle item.
     *
     * <p>Ignored if the item is not a bundle.</p>
     *
     * @param items the items to add
     * @return this builder
     */
    public ItemBuilder addBundleItems(ItemStack... items) {
        if (this.itemMeta instanceof BundleMeta bundleMeta) {
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    bundleMeta.addItem(item);
                }
            }
        }
        return this;
    }

    /**
     * Adds a custom potion effect.
     *
     * @param effect the potion effect
     * @return this builder
     */
    public ItemBuilder addPotionEffect(PotionEffect effect) {
        this.customEffects.add(effect);
        return this;
    }

    /**
     * Sets the potion display color.
     *
     * @param hex the hexadecimal color value
     * @return this builder
     */
    public ItemBuilder setPotionColor(@NonNull String hex) {
        this.potionColor = ColorUtils.fromHex(hex);
        if (this.itemMeta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(this.potionColor);
        }
        return this;
    }

    /**
     * Sets the base potion type.
     *
     * <p>Ignored if the item is not a potion.</p>
     *
     * @param type the potion type
     * @return this builder
     */
    public ItemBuilder setBasePotionType(@NonNull PotionType type) {
        if (this.itemMeta instanceof PotionMeta potionMeta) {
            potionMeta.setBasePotionType(type);
        }
        return this;
    }

    /**
     * Sets the entity type spawned by a spawn egg.
     *
     * <p>Ignored if the item is not a spawn egg.</p>
     *
     * @param type the entity type
     * @return this builder
     */
    public ItemBuilder setSpawnedType(@NonNull EntityType type) {
        if (this.itemMeta instanceof SpawnEggMeta eggMeta) {
            try {
                String nbtString = "{id:\"" + type.getKeyOrThrow() + "\"}";
                EntitySnapshot snapshot = Bukkit.getEntityFactory().createEntitySnapshot(nbtString);
                eggMeta.setSpawnedEntity(snapshot);
            } catch (IllegalArgumentException _) {
                Log.warn("ItemBuilder", "Failed to create EntitySnapshot for type: " + type.name());
            }
        }
        return this;
    }

    /**
     * Sets the leather armor color.
     *
     * <p>Ignored if the item is not a dyeable leather item.</p>
     *
     * @param hex the hexadecimal color value
     * @return this builder
     */
    public ItemBuilder setLeatherColor(@NonNull String hex) {
        this.leatherColor = ColorUtils.fromHex(hex);
        if (isLeatherDyeable(item.getType()) && itemMeta instanceof LeatherArmorMeta leatherArmorMeta) {
            leatherArmorMeta.setColor(this.leatherColor);
        }
        return this;
    }

    /**
     * Sets the armor trim applied to the item.
     *
     * <p>Ignored if the item is not armor.</p>
     *
     * @param material the trim material
     * @param pattern  the trim pattern
     * @return this builder
     */
    public ItemBuilder setArmorTrim(@NonNull TrimMaterial material, @NonNull TrimPattern pattern) {
        this.armorTrim = new ArmorTrim(material, pattern);
        if (itemMeta instanceof ArmorMeta armorMeta) {
            armorMeta.setTrim(this.armorTrim);
        }
        return this;
    }

    /**
     * Sets the banner patterns applied to the item.
     *
     * <p>Ignored if the item is not a banner.</p>
     *
     * @param patterns the banner patterns
     * @return this builder
     */
    public ItemBuilder addBannerPatterns(List<Pattern> patterns) {
        this.bannerPatterns = patterns;
        if (itemMeta instanceof BannerMeta bannerMeta) {
            bannerMeta.setPatterns(patterns);
        }
        return this;
    }

    /**
     * Sets the owner of a player head.
     *
     * <p>The item type is changed to {@link Material#PLAYER_HEAD}.</p>
     *
     * @param playerId the owner's UUID
     * @return this builder
     */
    public ItemBuilder setSkullOwner(@NonNull UUID playerId) {
        ensureSkullMeta();
        this.skullOwnerProfile = Bukkit.createPlayerProfile(playerId);
        this.base64Texture = null;
        return this;
    }

    /**
     * Sets the owner of a player head by player name.
     *
     * <p>The item type is changed to {@link Material#PLAYER_HEAD}.</p>
     *
     * @param playerName the owner's player name
     * @return this builder
     */
    public ItemBuilder setSkullOwner(@NonNull String playerName) {
        ensureSkullMeta();
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        this.skullOwnerProfile = onlinePlayer != null ? onlinePlayer.getPlayerProfile() : Bukkit.createPlayerProfile(playerName);
        this.base64Texture = null;
        return this;
    }

    /**
     * Sets the skull texture using a Base64 texture value, texture URL, or skin hash.
     *
     * <p>The item type is changed to {@link Material#PLAYER_HEAD}.</p>
     *
     * @param base64Texture the Base64 encoded texture data, URL, or hash
     * @return this builder
     */
    public ItemBuilder setCustomSkullTexture(@NonNull String base64Texture) {
        ensureSkullMeta();
        this.base64Texture = base64Texture;
        this.skullOwnerProfile = null;
        return this;
    }

    /**
     * Converts the item into an enchanted book.
     *
     * @return this builder
     */
    public ItemBuilder enchantedBook() {
        if (item.getType() != Material.ENCHANTED_BOOK) {
            item.setType(Material.ENCHANTED_BOOK);
            ItemMeta newMeta = Bukkit.getItemFactory().asMetaFor(itemMeta, Material.ENCHANTED_BOOK);
            if (newMeta != null) {
                this.itemMeta = newMeta;
            }
        }
        return this;
    }

    /**
     * Builds the configured item.
     *
     * <p>Applies all deferred modifications before returning the result.</p>
     *
     * @return the configured item stack
     */
    public ItemStack toItemStack() {
        applyEnchantments();
        applyBannerPatterns();
        applyArmorTrim();
        applyLeatherColor();
        applyPotionProperties();
        applySkullProperties();

        if (unstackable) {
            itemMeta.setMaxStackSize(1);
        }

        item.setItemMeta(itemMeta);
        return item;
    }

    /**
     * Returns a copy of the current item.
     *
     * @return a cloned item stack
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /**
     * Returns a copy of the current item metadata.
     *
     * @return cloned item metadata
     */
    public ItemMeta getItemMeta() {
        return itemMeta.clone();
    }

    /**
     * Applies modifications directly to the item metadata.
     *
     * @param consumer the metadata modifier
     * @return this builder
     */
    public ItemBuilder setItemMeta(Consumer<ItemMeta> consumer) {
        consumer.accept(this.itemMeta);
        return this;
    }

    /**
     * Replaces the item's metadata.
     *
     * @param itemMeta the new item metadata
     * @return this builder
     */
    public ItemBuilder setItemMeta(ItemMeta itemMeta) {
        this.itemMeta = itemMeta;
        return this;
    }

    /**
     * Gets the item's display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return itemMeta.getDisplayName();
    }

    private void applyEnchantments() {
        if (enchantments.isEmpty()) return;
        if (itemMeta instanceof EnchantmentStorageMeta storageMeta) {
            enchantments.forEach((enchantment, level) -> storageMeta.addStoredEnchant(enchantment, level, true));
            return;
        }
        enchantments.forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
    }

    private void applyBannerPatterns() {
        if (bannerPatterns != null && itemMeta instanceof BannerMeta bannerMeta) {
            bannerMeta.setPatterns(bannerPatterns);
            this.itemMeta = bannerMeta;
        }
    }

    private void applyArmorTrim() {
        if (armorTrim != null && itemMeta instanceof ArmorMeta armorMeta) {
            armorMeta.setTrim(armorTrim);
        }
    }

    private void applyLeatherColor() {
        if (leatherColor != null && isLeatherDyeable(item.getType()) && itemMeta instanceof LeatherArmorMeta lam) {
            lam.setColor(leatherColor);
        }
    }

    private void applyPotionProperties() {
        if (itemMeta instanceof PotionMeta potionMeta) {
            if (potionColor != null) {
                potionMeta.setColor(potionColor);
            }
            for (PotionEffect effect : customEffects) {
                potionMeta.addCustomEffect(effect, true);
            }
        }
    }

    private void applySkullProperties() {
        if (item.getType() == Material.PLAYER_HEAD && itemMeta instanceof SkullMeta skullMeta) {
            if (skullOwnerProfile != null) {
                skullMeta.setOwnerProfile(skullOwnerProfile);
            } else if (base64Texture != null) {
                applyCustomTexture(skullMeta, base64Texture);
            }
        }
    }

    private void ensureSkullMeta() {
        if (item.getType() != Material.PLAYER_HEAD) {
            item.setType(Material.PLAYER_HEAD);
            ItemMeta newMeta = Bukkit.getItemFactory().asMetaFor(itemMeta, Material.PLAYER_HEAD);
            if (newMeta != null) {
                this.itemMeta = newMeta;
            }
        }
    }
}
