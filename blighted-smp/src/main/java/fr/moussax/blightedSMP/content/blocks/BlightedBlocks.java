package fr.moussax.blightedSMP.content.blocks;

import fr.moussax.blightedSMP.engine.blocks.BlightedBlock;
import fr.moussax.blightedSMP.registry.RegistryModule;

import java.util.function.Consumer;

public class BlightedBlocks implements RegistryModule<Consumer<BlightedBlock>> {

    @Override
    public void register(Consumer<BlightedBlock> registry) {
        registry.accept(new BlightedWorkbench());
        registry.accept(new BlightedForge());
    }
}
