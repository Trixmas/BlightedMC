package fr.moussax.blightedSMP.content.rituals;

import fr.moussax.blightedSMP.content.entities.powerful.Illusioner;
import fr.moussax.blightedSMP.engine.entities.rituals.AncientRitual;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;
import org.bukkit.Material;

public class AncientRituals implements RegistryModule<Consumer<AncientRitual>> {

    @Override
    public void register(Consumer<AncientRitual> registry) {

        AncientRitual dummy = AncientRitual.Builder.of(new Illusioner())
                .displayedItem(Material.ENDER_PEARL, builder -> builder.setDisplayName("hello"))
                .addOffering(Material.DIRT, 45)
                .addOffering(Material.EMERALD, 45)
                .blightCost(12)
                .levelCost(3)
                .build();

        registry.accept(dummy);
    }
}
