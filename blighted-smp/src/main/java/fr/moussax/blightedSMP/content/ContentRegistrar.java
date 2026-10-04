package fr.moussax.blightedSMP.content;

import fr.moussax.blightedSMP.content.entities.BlightedEntities;
import fr.moussax.blightedSMP.content.fishing.EndFishing;
import fr.moussax.blightedSMP.content.fishing.NetherFishing;
import fr.moussax.blightedSMP.content.fishing.OverworldFishing;
import fr.moussax.blightedSMP.content.fishing.OverworldLavaFishing;
import fr.moussax.blightedSMP.content.items.BlightedItems;
import fr.moussax.blightedSMP.content.items.BlightedTools;
import fr.moussax.blightedSMP.content.items.Bonemerang;
import fr.moussax.blightedSMP.content.items.GlimmeringEye;
import fr.moussax.blightedSMP.content.items.Hyperion;
import fr.moussax.blightedSMP.content.items.KnightsSword;
import fr.moussax.blightedSMP.content.items.ThermalFuels;
import fr.moussax.blightedSMP.content.items.armors.FishingArmors;
import fr.moussax.blightedSMP.content.items.armors.RocketBoots;
import fr.moussax.blightedSMP.content.blocks.BlightedBlockItems;
import fr.moussax.blightedSMP.content.blocks.BlightedBlocks;
import fr.moussax.blightedSMP.content.items.materials.BlightedMaterials;
import fr.moussax.blightedSMP.content.items.materials.EndMaterials;
import fr.moussax.blightedSMP.content.items.materials.FishingMaterials;
import fr.moussax.blightedSMP.content.items.materials.NetherMaterials;
import fr.moussax.blightedSMP.content.recipes.*;
import fr.moussax.blightedSMP.content.rituals.AncientRituals;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.rituals.AncientRitual;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingRegistryHandler;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.blocks.BlightedBlock;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.blightedSMP.registry.RegistryModule;

import java.util.List;
import java.util.function.Consumer;

/**
 * Central registry provider for all content modules in BlightedMC.
 *
 * <p>Decouples core engine registries from concrete content implementations.</p>
 */
public final class ContentRegistrar {

    private ContentRegistrar() {
    }

    public static final List<RegistryModule<Consumer<BlightedItem>>> ITEM_MODULES = List.of(
            new BlightedMaterials(),
            new Bonemerang(),
            new GlimmeringEye(),
            new KnightsSword(),
            new RocketBoots(),
            new ThermalFuels(),
            new FishingArmors(),
            new BlightedItems(),
            new FishingMaterials(),
            new NetherMaterials(),
            new EndMaterials(),
            new Hyperion(),
            new BlightedTools(),
            new BlightedBlockItems()
    );

    public static final List<RegistryModule<Consumer<BlightedBlock>>> BLOCK_MODULES = List.of(
            new BlightedBlocks()
    );

    public static final List<RegistryModule<Consumer<BlightedRecipe>>> RECIPE_MODULES = List.of(
            new MaterialRecipes(),
            new NetherMaterialRecipes(),
            new EndRecipes(),
            new EquipmentRecipes(),
            new FishingArmorsRecipes()
    );

    public static final List<RegistryModule<Consumer<ForgeRecipe>>> FORGE_MODULES = List.of(
            new ForgeRecipes()
    );

    public static final List<RegistryModule<Consumer<BlightedEntity>>> ENTITY_MODULES = List.of(
            new BlightedEntities()
    );

    public static final List<RegistryModule<Consumer<AncientRitual>>> RITUAL_MODULES = List.of(
            new AncientRituals()
    );

    public static final List<RegistryModule<FishingRegistryHandler>> FISHING_MODULES = List.of(
            new NetherFishing(),
            new OverworldLavaFishing(),
            new OverworldFishing(),
            new EndFishing()
    );
}
