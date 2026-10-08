package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Twisted Zombie — baseline melee mob.
 *
 * <p>Persistent player pursuit, slightly faster movement, equipped with dark purple leather chestplate,
 * establishing the foundational combat language of the Twisted faction.</p>
 */
public final class TwistedZombie extends TwistedCreature {

    public TwistedZombie() {
        super("TWISTED_ZOMBIE", "Twisted Zombie", EntityType.ZOMBIE, 24, 4);
        setDroppedExp(6);

        attributes(attr -> attr
                .movementSpeed(0.26)
                .followRange(35)
        );

        setupTwistedArmor();

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.ROTTEN_FLESH, 1, 2, 1.0)
                .drop(Material.IRON_INGOT, 0.02, EntityLootRarity.RARE)
                .drop(Material.CARROT, 0.025)
                .drop(Material.POTATO, 0.025)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    private void setupTwistedArmor() {
        ItemStack helmet = new ItemBuilder(Material.LEATHER_HELMET)
                .setLeatherColor(CORRUPTION_HEX)
                .unbreakable()
                .toItemStack();
        ItemStack chestplate = new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherColor(CORRUPTION_HEX)
                .unbreakable()
                .toItemStack();
        setArmor(helmet, chestplate, null, null);
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        super.onConfigureAI(spawned);
        applyTwistedHostileGoals(spawned, 1.25D);
    }
}
