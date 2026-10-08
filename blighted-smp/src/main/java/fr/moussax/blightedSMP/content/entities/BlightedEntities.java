package fr.moussax.blightedSMP.content.entities;

import fr.moussax.blightedSMP.content.bosses.CorruptedChampion;
import fr.moussax.blightedSMP.content.entities.powerful.Endersent;
import fr.moussax.blightedSMP.content.entities.powerful.Illusioner;
import fr.moussax.blightedSMP.content.entities.powerful.Watchling;
import fr.moussax.blightedSMP.content.factions.blightsworn.*;
import fr.moussax.blightedSMP.content.factions.celestial.*;
import fr.moussax.blightedSMP.content.factions.twisted.*;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.registry.RegistryModule;

import java.util.function.Consumer;

public class BlightedEntities implements RegistryModule<Consumer<BlightedEntity>> {

    @Override
    public void register(Consumer<BlightedEntity> registry) {
        registry.accept(new BlightswornBogged());
        registry.accept(new BlightswornDrowned());
        registry.accept(new BlightswornHusk());
        registry.accept(new BlightswornParched());
        registry.accept(new BlightswornPiglin());
        registry.accept(new BlightswornSkeleton());
        registry.accept(new BlightswornStray());
        registry.accept(new BlightswornWitherSkeleton());
        registry.accept(new BlightswornZombie());
        registry.accept(new BlightswornZombifiedPiglin());

        registry.accept(new TwistedChicken());
        registry.accept(new TwistedCow());
        registry.accept(new TwistedPig());
        registry.accept(new TwistedSheep());
        registry.accept(new TwistedSkeleton());
        registry.accept(new TwistedSpider());
        registry.accept(new TwistedWolf());
        registry.accept(new TwistedZombie());

        registry.accept(new CorruptedChampion());
        registry.accept(new Endersent());
        registry.accept(new Watchling());
        registry.accept(new Illusioner());
    }
}
