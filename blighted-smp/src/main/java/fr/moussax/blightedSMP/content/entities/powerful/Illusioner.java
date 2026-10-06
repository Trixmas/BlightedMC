package fr.moussax.blightedSMP.content.entities.powerful;

import fr.moussax.blightedSMP.engine.entities.components.impl.ShieldComponent;
import fr.moussax.blightedSMP.engine.entities.rituals.AncientCreature;
import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

import java.time.Duration;
import java.util.List;

public class Illusioner extends AncientCreature {

    public Illusioner() {
        super("ANCIENT_ILLUSIONER", "Ancient Dummy", EntityType.ILLUSIONER);
        timeAllowance(Duration.ofSeconds(30));

        attributes(attr -> attr
                .attackDamage(12)
        );

        loot(loot -> loot
                .maxDrops(4)
                .drop(Material.SPECTRAL_ARROW, 4, 12, 0.6)
                .drop(Material.GLASS_BOTTLE, 1, 2, 0.4)
                .drop(Material.TOTEM_OF_UNDYING, 0.02, EntityLootRarity.VERY_RARE)
                .drop(Material.OMINOUS_BOTTLE, 0.15)
                .blight(12, 0.25)
                .enchantedBook(
                        List.of(Enchantment.QUICK_CHARGE, Enchantment.PIERCING, Enchantment.POWER),
                        1, 7, 0.07, EntityLootRarity.RARE
                )
        );
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();

        if (getComponent("BLIGHTED_SHIELD") == null) {
            addComponent(new ShieldComponent(120));
        }
    }
}
