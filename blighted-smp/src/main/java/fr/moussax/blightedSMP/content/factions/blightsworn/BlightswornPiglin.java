package fr.moussax.blightedSMP.content.factions.blightsworn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornPiglin extends BlightswornEliteArchetype {

    private static final double DASH_RANGE = 14.0;

    public BlightswornPiglin() {
        super("BLIGHTSWORN_PIGLIN", "Blightsworn Piglin", EntityType.PIGLIN);
        equipment(eq -> eq.mainHand(Material.GOLDEN_SWORD));
        loot(loot -> loot
                .drop(Material.GOLD_NUGGET, 2, 6, 1.0)
                .drop(Material.GOLD_INGOT, 1, 3, 0.4)
                .damagedItem(Material.CROSSBOW, 0.10, 0.80, 0.1, RARE)
                .blight(5, 0.04, VERY_RARE)
        );
        setDamage(8);
        setDroppedExp(16);

        spawning(spawn -> spawn
                .biomes(Biome.NETHER_WASTES, Biome.CRIMSON_FOREST)
                .netherHostile()
        );
    }

    @Override
    public LivingEntity spawn(Location location) {
        LivingEntity spawnedEntity = super.spawn(location);
        ((Piglin) spawnedEntity).setImmuneToZombification(true);
        return spawnedEntity;
    }

    @Override
    protected void onNormalBehavior() {
        addPhaseAbility(110L, 110L, () -> executeHunterDash(false));
    }

    @Override
    protected void onEnrageBehavior() {
        addPhaseAbility(80L, 80L, () -> executeHunterDash(true));
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        Location location = entity.getLocation().add(0, 1, 0);
        entity.getWorld().playSound(location, Sound.ENTITY_PIGLIN_ANGRY, 1.5f, 0.5f);
        entity.getWorld().playSound(location, Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 0.5f);

        entity.getWorld().spawnParticle(Particle.CRIT, location, 40, 0.5, 1.0, 0.5, 0.1);
        entity.getWorld().spawnParticle(Particle.DUST, location, 30, 0.5, 1.0, 0.5, 0.0, BLIGHT_DUST);
    }

    private void executeHunterDash(boolean isPhaseTwo) {
        if (!isAlive()) return;

        Player target = getNearestPlayer(DASH_RANGE);
        if (target == null || target.isDead() || target.getWorld() != entity.getWorld() || !hasLineOfSight(target)) return;

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PIGLIN_JEALOUS, 1.0f, 1.2f);
        entity.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, entity.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.02);

        addCoreDelayedAction(12L, () -> {
            if (!isAlive()) return;

            Vector dashDirection = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            if (dashDirection.lengthSquared() > 0) {
                dashDirection.normalize().multiply(isPhaseTwo ? 1.5 : 1.2).setY(0.25);
                entity.setVelocity(dashDirection);

                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);

                if (isPhaseTwo) {
                    applyHuntingSnare();
                }
            }
        });
    }

    private void applyHuntingSnare() {
        getNearbyPlayers(4.0).forEach(player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            player.getWorld().spawnParticle(
                    Particle.BLOCK,
                    player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1,
                    Material.COBWEB.createBlockData()
            );
        });
    }
}
