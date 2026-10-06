package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import fr.moussax.bedrock.utils.ItemBuilder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Objects;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornDrowned extends BlightswornCreature {
    private static final int MAX_HEALTH = 28;
    private static final double GEYSER_RANGE = 20.0;
    private static final double REPEL_RADIUS = 5.0;
    private static final int REPEL_COOLDOWN_TICKS = 120;

    private long nextRepelTick = 0;

    public BlightswornDrowned() {
        super("BLIGHTSWORN_DROWNED", "Blightsworn Drowned", EntityType.DROWNED, MAX_HEALTH);
        setDamage(6);
        setDroppedExp(10);
        loot(loot -> loot
                .drop(Material.ROTTEN_FLESH, 2, 5, 1.0)
                .drop(Material.COPPER_INGOT, 1, 3, 0.4)
                .drop(Material.NAUTILUS_SHELL, 0.08, RARE)
                .damagedItem(Material.TRIDENT, 0.05, 0.80, 0.02, VERY_RARE)
                .blight(5, 0.04, VERY_RARE)
        );

        equipment(eq -> eq.mainHand(new ItemBuilder(Material.TRIDENT).unbreakable().toItemStack()));

        spawning(spawn -> spawn
                .biomes(
                        Biome.RIVER, Biome.FROZEN_RIVER,
                        Biome.OCEAN, Biome.COLD_OCEAN, Biome.FROZEN_OCEAN,
                        Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN, Biome.DEEP_OCEAN,
                        Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN,
                        Biome.DEEP_LUKEWARM_OCEAN, Biome.DRIPSTONE_CAVES
                )
                .maxBlockLight(0)
                .maxLightLevel(7)
                .inLiquid()
        );
    }

    @Override
    protected void onDefineAdditionalBehavior() {
        super.onDefineAdditionalBehavior();
        addCoreAbility(20L, 10L, this::executeTidalRepel);
    }

    @Override
    protected void onNormalBehavior() {
        addPhaseAbility(120L, 120L, () -> executeAbyssalGeyser(false));
    }

    @Override
    protected void onEnrageBehavior() {
        addPhaseAbility(80L, 80L, () -> executeAbyssalGeyser(true));
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        Location location = entity.getLocation().add(0, 1, 0);
        entity.getWorld().playSound(location, Sound.ENTITY_DROWNED_DEATH, 1.5f, 0.5f);
        entity.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);

        entity.getWorld().spawnParticle(Particle.SPLASH, location, 150, 1.0, 1.0, 1.0, 0.2);
        entity.getWorld().spawnParticle(Particle.DUST, location, 50, 1.0, 1.0, 1.0, 0.0, BLIGHT_DUST);

        Objects.requireNonNull(entity.getEquipment()).setItemInMainHand(
                new ItemBuilder(Material.TRIDENT).addEnchantment(Enchantment.IMPALING, 2).unbreakable().toItemStack()
        );
    }

    private void executeTidalRepel() {
        if (!isAlive() || !entity.isInWater() || entity.getTicksLived() < nextRepelTick) return;

        Player threat = getNearestPlayer(REPEL_RADIUS);
        if (threat == null || threat.isDead() || threat.getWorld() != entity.getWorld() || !hasLineOfSight(threat)) return;

        Vector pushDirection = threat.getLocation().toVector().subtract(entity.getLocation().toVector());
        if (pushDirection.lengthSquared() > 0) {
            pushDirection.normalize().multiply(0.8).setY(0.2);
            threat.setVelocity(threat.getVelocity().add(pushDirection));

            Location location = entity.getLocation().add(0, 1, 0);
            entity.getWorld().playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 0.8f);
            entity.getWorld().playSound(location, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.5f);

            entity.getWorld().spawnParticle(Particle.SPLASH, location, 60, 1.0, 0.5, 1.0, 0.2);
            entity.getWorld().spawnParticle(Particle.NAUTILUS, location, 15, 0.5, 0.5, 0.5, 0.05);

            nextRepelTick = entity.getTicksLived() + REPEL_COOLDOWN_TICKS;
        }
    }

    private void executeAbyssalGeyser(boolean isPhaseTwo) {
        if (!isAlive() || !entity.isInWater()) return;

        Player target = getNearestPlayer(GEYSER_RANGE);
        if (target == null || target.isDead() || target.getWorld() != entity.getWorld() || !hasLineOfSight(target)) return;

        Location targetLocation = target.getLocation().clone();

        entity.getWorld().playSound(targetLocation, Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);
        entity.getWorld().playSound(targetLocation, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.5f);

        entity.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, targetLocation, 60, 0.8, 0.2, 0.8, 0.2);
        entity.getWorld().spawnParticle(Particle.FISHING, targetLocation, 30, 1.0, 0.2, 1.0, 0.1);

        if (isPhaseTwo) {
            entity.getWorld().spawnParticle(Particle.DUST, targetLocation, 30, 1.0, 0.2, 1.0, 0.0, BLIGHT_DUST);
        }

        int delayTicks = isPhaseTwo ? 20 : 30;
        addCoreDelayedAction(delayTicks, () -> eruptGeyser(targetLocation, isPhaseTwo));
    }

    private void eruptGeyser(Location center, boolean isPhaseTwo) {
        if (!isAlive()) return;

        Objects.requireNonNull(center.getWorld()).playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.2f);

        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2);
        center.getWorld().spawnParticle(Particle.SPLASH, center, 300, 2.0, 4.0, 2.0, 0.5);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 30, 1.0, 0.5, 1.0, 0.1);

        double radius = isPhaseTwo ? 3.5 : 2.5;

        center.getWorld().getNearbyEntities(center, radius, radius, radius).stream()
                .filter(entity -> entity instanceof Player player && player.getGameMode() == GameMode.SURVIVAL)
                .map(entity -> (Player) entity)
                .forEach(player -> {
                    player.damage(this.damage * 1.5, entity);
                    player.setVelocity(player.getVelocity().add(new Vector(0, 1.2, 0)));

                    if (isPhaseTwo) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                    }
                });
    }

    @Override
    protected void equipEnragedArmor(EntityEquipment equipment) {
        setArmor(
                new ItemBuilder(Material.LEATHER_HELMET)
                        .setArmorTrim(TrimMaterial.DIAMOND, TrimPattern.SILENCE)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE)
                        .setArmorTrim(TrimMaterial.DIAMOND, TrimPattern.SILENCE)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.IRON_LEGGINGS).unbreakable().toItemStack(),
                new ItemBuilder(Material.IRON_BOOTS).unbreakable().toItemStack()
        );
    }
}
