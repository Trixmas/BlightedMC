package fr.moussax.blightedSMP.engine.player.cinematic;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the first-join cinematic sequence for new players:
 * "The Blight Infusion".
 */
public final class FirstJoinCinematic {

    private static final Map<UUID, FirstJoinCinematic> ACTIVE_INSTANCES = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final Player player;
    private final List<BlockDisplay> shards = new ArrayList<>();
    private final Map<Location, BlockState> clearedBlocks = new LinkedHashMap<>();
    private BukkitRunnable runningTask;

    public FirstJoinCinematic(@NonNull Plugin plugin, @NonNull Player player) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public static boolean isCinematicActive(@NonNull Player player) {
        return ACTIVE_INSTANCES.containsKey(player.getUniqueId());
    }

    public static void cleanupAll() {
        for (FirstJoinCinematic cinematic : ACTIVE_INSTANCES.values()) {
            cinematic.stop();
        }
        ACTIVE_INSTANCES.clear();
    }

    public void start() {
        if (!player.isOnline() || ACTIVE_INSTANCES.containsKey(player.getUniqueId())) {
            return;
        }

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        Location center = player.getLocation().clone();
        clearObstructionsAbove(center);

        runningTask = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stop();
                    return;
                }

                // Phase 1: Heavy Snare & Ground Siphon (Ticks 0 - 30)
                if (tick < 30) {
                    if (tick == 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4, false, false, false));
                        player.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.45f);
                        player.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.6f, 0.4f);
                    } else if (tick == 20) {
                        player.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);
                    }
                    spawnConvergingSouls(center, 3.2 - (tick * 0.09));
                }

                // Phase 2: High Launch & Accelerating Inward Siphon (Ticks 30 - 74)
                else if (tick < 75) {
                    if (tick == 30) {
                        player.removePotionEffect(PotionEffectType.SLOWNESS);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 45, 2, false, false, false));
                        player.setVelocity(new Vector(0, 0.65, 0));

                        player.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 1.2f, 0.7f);
                        player.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.6f);
                        spawnOrbitingShards(player.getLocation(), shards);
                    }

                    // Escalating heartbeat and tendril clicks as tension builds
                    if (tick == 50) {
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.8f);
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 1.2f, 0.9f);
                    } else if (tick == 65) {
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.8f, 1.1f);
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 1.4f, 1.3f);
                    }

                    // Violent body spasms & camera flinches as corruption constricts
                    if (tick == 56) {
                        player.playHurtAnimation(90);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 1.4f);
                    } else if (tick == 64) {
                        player.playHurtAnimation(270);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 1.3f);
                    } else if (tick == 70) {
                        player.playHurtAnimation(0);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 0.8f);
                    }

                    // Physical convulsion: violent camera/model micro-jitter during final compression
                    if (tick >= 48 && tick < 71) {
                        ThreadLocalRandom rng = ThreadLocalRandom.current();
                        double jitterX = (rng.nextDouble() - 0.5) * 0.08;
                        double jitterZ = (rng.nextDouble() - 0.5) * 0.08;
                        player.setVelocity(player.getVelocity().add(new Vector(jitterX, 0, jitterZ)));
                    }

                    // Singularity Freeze & Sonic Windup (Ticks 70 - 74)
                    if (tick == 70) {
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.4f, 1.0f);
                        // Sudden FOV tunnel vision snap
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 6, 5, false, false, false));
                    }
                    if (tick >= 71) {
                        // Freeze in mid-air right before detonation
                        player.setVelocity(new Vector(0, 0, 0));
                    }

                    updateOrbitingShards(player.getLocation(), shards, tick);
                    spawnAscendingSoulSpiral(player.getLocation(), tick);
                }

                // Phase 3: The Infusion Burst (Tick 75)
                else if (tick == 75) {
                    player.removePotionEffect(PotionEffectType.LEVITATION);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);

                    // Instant sensory blackout followed by pulsing darkness & reality-warping nausea
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 22, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 50, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0, false, false, true));

                    // Corrupt vitality: halve health
                    player.setHealth(Math.max(1.0, player.getHealth() / 2.0));
                    player.playHurtAnimation(180);

                    // Silent lightning strike for full-screen flash and dramatic illumination
                    if (player.getWorld() != null) {
                        player.getWorld().strikeLightningEffect(player.getLocation());
                    }

                    Location chestLocation = player.getLocation().add(0, 1.2, 0);
                    if (chestLocation.getWorld() != null) {
                        chestLocation.getWorld().spawnParticle(Particle.FLASH, chestLocation, 4, 0, 0, 0, 0, Color.fromRGB(0, 200, 200));
                        chestLocation.getWorld().spawnParticle(Particle.SONIC_BOOM, chestLocation, 1, 0, 0, 0, 0);
                        chestLocation.getWorld().spawnParticle(Particle.EXPLOSION, chestLocation, 2, 0.2, 0.2, 0.2, 0);
                        chestLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, chestLocation, 100, 0.8, 1.0, 0.8, 0.18);
                        chestLocation.getWorld().spawnParticle(Particle.SCULK_SOUL, chestLocation, 50, 0.6, 0.8, 0.6, 0.1);
                        chestLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, chestLocation, 30, 0.5, 0.5, 0.5, 0.1);
                    }

                    // Devastating layered audio
                    player.playSound(chestLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.75f);
                    player.playSound(chestLocation, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.5f);
                    player.playSound(chestLocation, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.5f);
                    player.playSound(chestLocation, Sound.ITEM_TRIDENT_THUNDER, 0.7f, 1.9f);
                    player.playSound(chestLocation, Sound.PARTICLE_SOUL_ESCAPE, 1.6f, 0.45f);

                    BlightedItem banner = ItemRegistry.get("BLIGHTED_BANNER");
                    if (banner != null) {
                        player.getInventory().setHelmet(banner.toItemStack());
                    }

                    BlightedItem codex = ItemRegistry.get("BLIGHTED_CODEX");
                    if (codex != null) {
                        player.getInventory().setItemInOffHand(codex.toItemStack());
                    }

                    cleanupShards();
                    // Brief weightless hang at peak
                    player.setVelocity(new Vector(0, 0.08, 0));
                }

                // Phase 4: The Slam Downward (Tick 84)
                else if (tick == 84) {
                    player.setVelocity(new Vector(0, -1.2, 0));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.4f);
                }

                // Phase 5: Hard Touchdown (Tick 94)
                else if (tick == 94) {
                    player.setFallDistance(0);
                    player.playHurtAnimation(0);

                    // Staggered winded debuff: player crawls to catch breath
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 2, false, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, false, false));

                    Location feet = player.getLocation();
                    if (feet.getWorld() != null) {
                        feet.getWorld().spawnParticle(Particle.BLOCK, feet, 50, 0.6, 0.1, 0.6, Material.CRYING_OBSIDIAN.createBlockData());
                        feet.getWorld().spawnParticle(Particle.BLOCK, feet, 35, 0.5, 0.1, 0.5, Material.SOUL_SOIL.createBlockData());
                        feet.getWorld().spawnParticle(Particle.SCULK_SOUL, feet.clone().add(0, 0.2, 0), 20, 0.4, 0.1, 0.4, 0.05);
                    }

                    // Bone-crunching impact audio layering
                    player.playSound(feet, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.4f, 0.6f);
                    player.playSound(feet, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 0.5f);
                    player.playSound(feet, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.3f, 0.55f);
                    player.playSound(feet, Sound.ENTITY_IRON_GOLEM_HURT, 0.9f, 0.4f);
                    player.playSound(feet, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 0.5f);
                }

                // Phase 6: Silence & Lore Delivery (Tick 125)
                else if (tick >= 125) {
                    stop();
                    return;
                }
                tick++;
            }
        };

        runningTask.runTaskTimer(plugin, 40L, 1L);
    }

    public void stop() {
        if (runningTask != null) {
            try {
                runningTask.cancel();
            } catch (IllegalStateException _) {
            }
            runningTask = null;
        }
        cleanupShards();
        restoreBlocks();
        ACTIVE_INSTANCES.remove(player.getUniqueId());
    }

    private void clearObstructionsAbove(Location base) {
        if (base.getWorld() == null) return;
        for (int y = 1; y <= 16; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location location = base.clone().add(x, y, z);
                    Block block = location.getBlock();
                    if (!block.isPassable()) {
                        clearedBlocks.put(location, block.getState());
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void restoreBlocks() {
        if (clearedBlocks.isEmpty()) return;
        for (BlockState state : clearedBlocks.values()) {
            state.update(true, false);
        }
        clearedBlocks.clear();
    }

    private void spawnConvergingSouls(Location center, double radius) {
        if (center.getWorld() == null) return;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians((i * 60.0) + (radius * 50));
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location spawn = center.clone().add(x, 0.1, z);
            Vector direction = center.toVector().subtract(spawn.toVector()).normalize().multiply(0.24);
            center.getWorld().spawnParticle(Particle.SOUL, spawn, 0, direction.getX(), 0.06, direction.getZ(), 0.12);
        }
    }

    private void spawnOrbitingShards(Location location, List<BlockDisplay> list) {
        if (location.getWorld() == null) return;

        float size = 0.40f;
        for (int i = 0; i < 3; i++) {
            BlockDisplay display = (BlockDisplay) location.getWorld().spawnEntity(location, EntityType.BLOCK_DISPLAY);
            display.setBlock(Material.SCULK.createBlockData());
            display.setTransformation(new Transformation(
                    new Vector3f(-size / 2f, -size / 2f, -size / 2f),
                    new AxisAngle4f(),
                    new Vector3f(size, size, size),
                    new AxisAngle4f()
            ));
            display.setTeleportDuration(1);
            list.add(display);
        }
    }

    private void updateOrbitingShards(Location center, List<BlockDisplay> list, int tick) {
        if (center.getWorld() == null || list.isEmpty()) return;

        double progress = Math.clamp((tick - 30) / 44.0, 0.0, 1.0);
        double radius = 1.85 - (progress * 1.35); // Smoothly constricts 1.85 -> 0.50
        double baseAngle = (tick - 30) * 12.0 + (progress * progress * 320.0);

        Location chest = center.clone().add(0, 1.1, 0);

        for (int i = 0; i < list.size(); i++) {
            BlockDisplay display = list.get(i);
            if (!display.isValid()) continue;

            double angle = Math.toRadians(baseAngle + (i * (360.0 / list.size())));
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            // Coordinated 3D undulating wave around chest
            double offsetY = 1.05 + Math.sin(angle * 2.0) * 0.22;

            Location shardLocation = center.clone().add(offsetX, offsetY, offsetZ);
            shardLocation.setYaw((float) Math.toDegrees(angle) + 90.0f);
            shardLocation.setPitch((float) (-15.0 + Math.sin(Math.toRadians(tick * 16.0 + i * 90.0)) * 20.0));

            display.teleport(shardLocation);

            // Siphoning soul tendril pulling from the shard directly into player's chest
            Vector toChest = chest.toVector().subtract(shardLocation.toVector()).normalize().multiply(0.28);
            center.getWorld().spawnParticle(Particle.SOUL, shardLocation, 0, toChest.getX(), toChest.getY(), toChest.getZ(), 0.1);
            if (tick % 2 == 0) {
                center.getWorld().spawnParticle(Particle.SCULK_SOUL, shardLocation, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }
    }

    private void spawnAscendingSoulSpiral(Location center, int tick) {
        if (center.getWorld() == null) return;
        double progress = Math.min(1.0, (tick - 30) / 45.0);
        double radius = 0.9 - (progress * 0.4); // spiral compresses inward

        for (int arm = 0; arm < 2; arm++) {
            double angle = Math.toRadians((tick * (14.0 + progress * 16.0)) + (arm * 180.0));
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double yOffset = 0.2 + ((tick % 20) / 20.0) * 1.6;

            Location soulLocation = center.clone().add(x, yOffset, z);
            center.getWorld().spawnParticle(Particle.SOUL, soulLocation, 1, 0, 0.05, 0, 0.015);
            if (tick % 2 == 0) {
                center.getWorld().spawnParticle(Particle.SCULK_SOUL, soulLocation, 2, 0.02, 0.02, 0.02, 0.008);
            }
        }
    }

    private void cleanupShards() {
        for (BlockDisplay display : shards) {
            if (display.isValid()) {
                display.remove();
            }
        }
        shards.clear();
    }
}
