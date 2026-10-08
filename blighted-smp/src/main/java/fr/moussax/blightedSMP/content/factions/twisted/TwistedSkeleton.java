package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Twisted Skeleton — ranged pressure mob.
 *
 * <p>Represents the ranged threat of the Twisted faction. Attempts to maintain line of sight and
 * combat distance while using ranged bow attacks to pressure the player.</p>
 */
public final class TwistedSkeleton extends TwistedCreature {

    public TwistedSkeleton() {
        super("TWISTED_SKELETON", "Twisted Skeleton", EntityType.SKELETON, 20, 3);
        setDroppedExp(6);

        attributes(attr -> attr
                .movementSpeed(0.25)
                .followRange(35)
        );

        setupTwistedEquipment();

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.BONE, 1, 2, 1.0)
                .drop(Material.ARROW, 1, 2, 1.0)
                .damagedItem(Material.BOW, 0.1, 0.6, 0.085, EntityLootRarity.RARE)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    private void setupTwistedEquipment() {
        equipment(equipment -> equipment.mainHand(Material.BOW));
        ItemStack helmet = new ItemBuilder(Material.LEATHER_HELMET)
                .setLeatherColor(CORRUPTION_HEX)
                .unbreakable()
                .toItemStack();
        setArmor(helmet, null, null, null);
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        if (!(spawned instanceof CraftMob craftMob)) return;
        var nmsMob = craftMob.getHandle();

        nmsMob.goalSelector.removeAllGoals(goal -> true);
        nmsMob.targetSelector.removeAllGoals(goal -> true);

        nmsMob.goalSelector.addGoal(0, new FloatGoal(nmsMob));
        if (nmsMob instanceof PathfinderMob pathfinderMob) {
            nmsMob.goalSelector.addGoal(2, new RestrictSunGoal(pathfinderMob));
            nmsMob.goalSelector.addGoal(3, new FleeSunGoal(pathfinderMob, 1.0D));
            nmsMob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(pathfinderMob, 1.0D));
            nmsMob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinderMob).setAlertOthers());
        }

        if (nmsMob instanceof AbstractSkeleton skeleton) {
            nmsMob.goalSelector.addGoal(4, new RangedBowAttackGoal<>(skeleton, 1.0D, 30, 16.0F));
        }

        nmsMob.goalSelector.addGoal(6, new LookAtPlayerGoal(nmsMob, net.minecraft.world.entity.player.Player.class, 16.0F));
        nmsMob.goalSelector.addGoal(7, new RandomLookAroundGoal(nmsMob));

        nmsMob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                nmsMob,
                net.minecraft.world.entity.player.Player.class,
                true
        ));
    }
}
