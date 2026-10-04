package fr.moussax.blightedSMP.content.items.armors;

import fr.moussax.blightedSMP.content.items.abilities.weave.EmberWeaveSetBonus;
import fr.moussax.blightedSMP.content.items.abilities.weave.MagmaweaveSetBonus;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.items.abilities.FullSetBonus;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.function.Consumer;

public final class FishingArmors implements RegistryModule<Consumer<BlightedItem>> {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void register(Consumer<BlightedItem> registry) {

        String[] anglerArmorDescription = new String[]{
                "A simple garb from a simple",
                "craft. Though beneath the",
                "surface, nothing is ever",
                "quite so simple."
        };

        BlightedItem anglerHelmet = new BlightedItem("ANGLER_HELMET", ItemType.HELMET, ItemRarity.COMMON, Material.COPPER_HELMET);
        anglerHelmet.setDisplayName("Angler Helmet");
        anglerHelmet.addAttributeModifier(Attribute.ARMOR, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
        anglerHelmet.addAttributeModifier(Attribute.OXYGEN_BONUS, 0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
        anglerHelmet.addItemFlag(ItemFlag.HIDE_ATTRIBUTES);
        anglerHelmet.description(anglerArmorDescription);
        anglerHelmet.setMaxDurability(121);

        BlightedItem anglerChestplate = new BlightedItem("ANGLER_CHESTPLATE", ItemType.CHESTPLATE, ItemRarity.COMMON, Material.LEATHER_CHESTPLATE);
        anglerChestplate.setDisplayName("Angler Chestplate");
        anglerChestplate.addAttributeModifier(Attribute.ARMOR, 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
        anglerChestplate.addAttributeModifier(Attribute.OXYGEN_BONUS, 0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
        anglerChestplate.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
        anglerChestplate.description(anglerArmorDescription);
        anglerChestplate.setLeatherColor("#2B457A");
        anglerChestplate.setMaxDurability(176);

        BlightedItem anglerLeggings = new BlightedItem("ANGLER_LEGGINGS", ItemType.LEGGINGS, ItemRarity.COMMON, Material.LEATHER_LEGGINGS);
        anglerLeggings.setDisplayName("Angler Leggings");
        anglerLeggings.addAttributeModifier(Attribute.ARMOR, 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
        anglerLeggings.addAttributeModifier(Attribute.OXYGEN_BONUS, 0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
        anglerLeggings.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
        anglerLeggings.description(anglerArmorDescription);
        anglerLeggings.setLeatherColor("#2B457A");
        anglerLeggings.setMaxDurability(165);

        BlightedItem anglerBoots = new BlightedItem("ANGLER_BOOTS", ItemType.BOOTS, ItemRarity.COMMON, Material.LEATHER_BOOTS);
        anglerBoots.setDisplayName("Angler Boots");
        anglerBoots.addAttributeModifier(Attribute.ARMOR, 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
        anglerBoots.addAttributeModifier(Attribute.OXYGEN_BONUS, 0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
        anglerBoots.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
        anglerBoots.description(anglerArmorDescription);
        anglerBoots.setLeatherColor("#2B457A");

        registry.accept(anglerHelmet);
        registry.accept(anglerChestplate);
        registry.accept(anglerLeggings);
        registry.accept(anglerBoots);

        FullSetBonus emberWeaveSetBonus = new EmberWeaveSetBonus();

        BlightedItem emberWeaveHelmet = new BlightedItem("EMBERWEAVE_HELMET", ItemType.HELMET, ItemRarity.RARE, Material.PLAYER_HEAD);
        emberWeaveHelmet.setDisplayName("Emberweave Helmet");
        emberWeaveHelmet.addAttributeModifier(Attribute.ARMOR, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
        emberWeaveHelmet.preventPlacement();
        emberWeaveHelmet.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjY2M2RkY2JhZjdjNDQ4YTc2ODk4MWFiMDhkYTBmYWMyMzQ4MTU1N2M0ZTVhNTg2YWJmZTc1OWRmMTI3MWNhIn19fQ==");
        emberWeaveHelmet.editEquippable(equippable -> equippable.setSlot(EquipmentSlot.HEAD));
        emberWeaveHelmet.setFireResistant(true);
        emberWeaveHelmet.setFullSetBonus(emberWeaveSetBonus);

        BlightedItem emberWeaveChestplate = new BlightedItem("EMBERWEAVE_CHESTPLATE", ItemType.CHESTPLATE, ItemRarity.RARE, Material.LEATHER_CHESTPLATE);
        emberWeaveChestplate.setDisplayName("Emberweave Chestplate");
        emberWeaveChestplate.addAttributeModifier(Attribute.ARMOR, 6.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
        emberWeaveChestplate.addItemFlag(ItemFlag.HIDE_DYE);
        emberWeaveChestplate.setLeatherColor("#7A2724");
        emberWeaveChestplate.setFireResistant(true);
        emberWeaveChestplate.setMaxDurability(240);
        emberWeaveChestplate.setFullSetBonus(emberWeaveSetBonus);

        BlightedItem emberWeaveLeggins = new BlightedItem("EMBERWEAVE_LEGGINS", ItemType.LEGGINGS, ItemRarity.RARE, Material.LEATHER_LEGGINGS);
        emberWeaveLeggins.setDisplayName("Emberweave Leggings");
        emberWeaveLeggins.addAttributeModifier(Attribute.ARMOR, 5.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
        emberWeaveLeggins.addItemFlag(ItemFlag.HIDE_DYE);
        emberWeaveLeggins.setLeatherColor("#7A2724");
        emberWeaveLeggins.setFireResistant(true);
        emberWeaveLeggins.setMaxDurability(225);
        emberWeaveLeggins.setFullSetBonus(emberWeaveSetBonus);

        BlightedItem emberWeaveBoots = new BlightedItem("EMBERWEAVE_BOOTS", ItemType.BOOTS, ItemRarity.RARE, Material.LEATHER_BOOTS);
        emberWeaveBoots.setDisplayName("Emberweave Boots");
        emberWeaveBoots.addAttributeModifier(Attribute.ARMOR, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
        emberWeaveBoots.addItemFlag(ItemFlag.HIDE_DYE);
        emberWeaveBoots.setLeatherColor("#7A2724");
        emberWeaveBoots.setFireResistant(true);
        emberWeaveBoots.setMaxDurability(195);
        emberWeaveBoots.setFullSetBonus(emberWeaveSetBonus);

        FullSetBonus magmaWeaveSetBonus = new MagmaweaveSetBonus();

        BlightedItem ashfangHelmet = new BlightedItem("ASHFANG_HELMET", ItemType.HELMET, ItemRarity.EPIC, Material.PLAYER_HEAD);
        ashfangHelmet.setDisplayName("Ashfang Helmet");
        ashfangHelmet.addAttributeModifier(Attribute.ARMOR, 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
        ashfangHelmet.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
        ashfangHelmet.editEquippable(equippable -> equippable.setSlot(EquipmentSlot.HEAD));
        ashfangHelmet.preventPlacement();
        ashfangHelmet.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNlNTkyNTBjOTY4ZTUwODUzOWJmMmU4NDkyZjU2YTJmNjY0ZWZiMzA5ZjQ5NWEwN2RjM2E1NGM4YjZhMjQ5ZSJ9fX0=");
        ashfangHelmet.setFireResistant(true);
        ashfangHelmet.setFullSetBonus(magmaWeaveSetBonus);

        BlightedItem ashfangChestplate = new BlightedItem("ASHFANG_CHESTPLATE", ItemType.CHESTPLATE, ItemRarity.EPIC, Material.LEATHER_CHESTPLATE);
        ashfangChestplate.setDisplayName("Ashfang Chestplate");
        ashfangChestplate.setLeatherColor("#420905");
        ashfangChestplate.addAttributeModifier(Attribute.ARMOR, 8.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
        ashfangChestplate.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
        ashfangChestplate.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);
        ashfangChestplate.setArmorTrim(TrimMaterial.RESIN, TrimPattern.FLOW);
        ashfangChestplate.setFireResistant(true);
        ashfangChestplate.setMaxDurability(528);
        ashfangChestplate.setFullSetBonus(magmaWeaveSetBonus);

        BlightedItem ashfangLeggins = new BlightedItem("ASHFANG_LEGGINS", ItemType.LEGGINGS, ItemRarity.EPIC, Material.LEATHER_LEGGINGS);
        ashfangLeggins.setDisplayName("Ashfang Leggings");
        ashfangLeggins.addAttributeModifier(Attribute.ARMOR, 6.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
        ashfangLeggins.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
        ashfangLeggins.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);
        ashfangLeggins.setArmorTrim(TrimMaterial.RESIN, TrimPattern.FLOW);
        ashfangLeggins.setLeatherColor("#420905");
        ashfangLeggins.setFireResistant(true);
        ashfangLeggins.setMaxDurability(495);
        ashfangLeggins.setFullSetBonus(magmaWeaveSetBonus);

        BlightedItem ashfangBoots = new BlightedItem("ASHFANG_BOOTS", ItemType.BOOTS, ItemRarity.EPIC, Material.LEATHER_BOOTS);
        ashfangBoots.setDisplayName("Ashfang Boots");
        ashfangBoots.addAttributeModifier(Attribute.ARMOR, 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
        ashfangBoots.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
        ashfangBoots.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);
        ashfangBoots.setArmorTrim(TrimMaterial.RESIN, TrimPattern.FLOW);
        ashfangBoots.setLeatherColor("#420905");
        ashfangBoots.setFireResistant(true);
        ashfangBoots.setMaxDurability(429);
        ashfangBoots.setFullSetBonus(magmaWeaveSetBonus);

        registry.accept(emberWeaveHelmet);
        registry.accept(emberWeaveChestplate);
        registry.accept(emberWeaveLeggins);
        registry.accept(emberWeaveBoots);
        registry.accept(ashfangHelmet);
        registry.accept(ashfangChestplate);
        registry.accept(ashfangLeggins);
        registry.accept(ashfangBoots);
    }
}
