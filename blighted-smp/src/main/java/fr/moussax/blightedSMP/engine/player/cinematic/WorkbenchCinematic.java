package fr.moussax.blightedSMP.engine.player.cinematic;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes the awakening ritual transforming an ordinary Crafting Table
 * into a Forgotten Workbench via the Echoing Twisted Orb and Glowing Ink Sac.
 */
public final class WorkbenchCinematic {

    private static final Map<Location, WorkbenchCinematic> ACTIVE_RITUALS = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final Player player;
    private final Block craftingTableBlock;
    private final Location tableCenter;
    private final ItemStack orbItemStack;

    private static final int[][] RITUAL_FIELD_OFFSETS = {
            {-1, -2}, {0, -2}, {1, -2},
            {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
            {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0},
            {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1},
            {-1, 2}, {0, 2}, {1, 2}
    };

    private ItemDisplay floatingOrbDisplay;
    private ItemDisplay floatingInkDisplay;
    private final Map<Location, BlockState> temporarySculkBlocks = new LinkedHashMap<>();
    private BukkitRunnable runningTask;

    public WorkbenchCinematic(
            @NonNull Plugin plugin,
            @NonNull Player player,
            @NonNull Block craftingTableBlock,
            @NonNull ItemStack orbItemStack
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.craftingTableBlock = Objects.requireNonNull(craftingTableBlock, "craftingTableBlock cannot be null");
        this.tableCenter = craftingTableBlock.getLocation().clone().add(0.5, 0.5, 0.5);
        this.orbItemStack = Objects.requireNonNull(orbItemStack, "orbItemStack cannot be null");
    }

    public static boolean isRitualActiveAt(@NonNull Location location) {
        Block block = location.getBlock();
        return ACTIVE_RITUALS.containsKey(block.getLocation());
    }

    public static void cleanupAll() {
        for (WorkbenchCinematic ritual : ACTIVE_RITUALS.values()) {
            ritual.stop(false);
        }
        ACTIVE_RITUALS.clear();
    }

    public void start() {
        Location blockLoc = craftingTableBlock.getLocation();
        if (ACTIVE_RITUALS.containsKey(blockLoc)) {
            return;
        }
        ACTIVE_RITUALS.put(blockLoc, this);

        runningTask = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stop(false);
                    return;
                }

                if (tableCenter.getWorld() == null
                        || !tableCenter.getWorld().isChunkLoaded(tableCenter.getBlockX() >> 4, tableCenter.getBlockZ() >> 4)
                        || player.getLocation().distanceSquared(tableCenter) > 400.0) {
                    stop(false);
                    return;
                }

                if (craftingTableBlock.getType() != Material.CRAFTING_TABLE && tick < 140) {
                    stop(false);
                    return;
                }

                if (tick < 25) {
                    updatePhase1Opening(tick);
                } else if (tick == 25) {
                    executePhase2Rejection();
                } else if (tick < 70) {
                    updatePhase3OrbInstability(tick);
                } else if (tick < 105) {
                    updatePhase4LivingSculkAndInk(tick);
                } else if (tick < 140) {
                    updatePhase5PatternAndEscalation(tick);
                } else if (tick == 140) {
                    executePhase6Overwrite();
                } else if (tick <= 190) {
                    updatePhase8SoulReformation(tick);
                } else {
                    executePhase10ResonantRelease();
                    consumeProgressionItems();
                    stop(true);
                    return;
                }
                tick++;
            }
        };
        runningTask.runTaskTimer(plugin, 1L, 1L);
    }

    private void updatePhase1Opening(int tick) {
        if (tableCenter.getWorld() == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Very subtle background ambience establishing that something is listening
        if (tick == 0) {
            tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_HEARTBEAT, 0.75f, 0.5f);
        } else if (tick == 12) {
            tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 0.8f, 0.75f);
        } else if (tick == 18) {
            tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CHARGE, 0.6f, 0.5f);
        }

        // Ticks 0 - 19: Souls sparsely and organically roam the 5x5 ritual space
        if (tick < 20) {
            if (tick % 3 == 0) {
                int[] offset = RITUAL_FIELD_OFFSETS[random.nextInt(RITUAL_FIELD_OFFSETS.length)];
                double offsetX = offset[0] + (random.nextDouble() - 0.5) * 0.8;
                double offsetZ = offset[1] + (random.nextDouble() - 0.5) * 0.8;
                Location roamLocation = tableCenter.clone().add(offsetX, 0.15, offsetZ);

                // Irregular wandering velocity
                Vector wanderVelocity = new Vector(random.nextDouble() - 0.5, 0.02, random.nextDouble() - 0.5).normalize().multiply(0.04);
                tableCenter.getWorld().spawnParticle(Particle.SOUL, roamLocation, 0, wanderVelocity.getX(), wanderVelocity.getY(), wanderVelocity.getZ(), 0.05);

                // Occasional Sculk trace traveling along the ground toward the center
                if (tick % 6 == 0) {
                    Vector towardCenter = tableCenter.toVector().subtract(roamLocation.toVector()).setY(0).normalize().multiply(0.08);
                    tableCenter.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, roamLocation, 1, towardCenter.getX(), 0.01, towardCenter.getZ(), 0.02);
                }
            }
        }

        // Ticks 20 - 24: Abrupt reaction! The roaming souls sharply turn and react toward the Crafting Table
        else {
            for (int i = 0; i < 3; i++) {
                int[] offset = RITUAL_FIELD_OFFSETS[random.nextInt(RITUAL_FIELD_OFFSETS.length)];
                Location reactLocation = tableCenter.clone().add(offset[0] * 0.8, 0.15, offset[1] * 0.8);
                Vector snapToTable = tableCenter.toVector().subtract(reactLocation.toVector()).normalize().multiply(0.22);
                tableCenter.getWorld().spawnParticle(Particle.SOUL, reactLocation, 0, snapToTable.getX(), 0.06, snapToTable.getZ(), 0.12);
                tableCenter.getWorld().spawnParticle(Particle.SCULK_SOUL, reactLocation, 1, 0.02, 0.02, 0.02, 0.01);
            }
            if (tick == 20) {
                tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CHARGE, 0.9f, 0.7f);
            }
        }
    }

    private void executePhase2Rejection() {
        if (tableCenter.getWorld() == null) return;

        // Culmination of the area reacting: sudden violent Sculk bloom & rejection
        tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.45f);
        tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.7f);
        tableCenter.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, tableCenter.clone().add(0, 0.6, 0), 16, 0.3, 0.2, 0.3, 0.05);

        // Violently push the player backward away from the table
        if (player.isOnline()) {
            Vector knockback = player.getLocation().toVector().subtract(tableCenter.toVector());
            knockback.setY(0);
            if (knockback.lengthSquared() > 0.001) {
                knockback.normalize().multiply(1.45).setY(0.42);
            } else {
                knockback = new Vector(0, 0.42, -1.45);
            }
            player.setVelocity(knockback);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.7f);
        }

        // The Echoing Twisted Orb manifests ~1.35 blocks above the Crafting Table
        Location orbSpawnLocation = tableCenter.clone().add(0, 1.35, 0);
        floatingOrbDisplay = (ItemDisplay) tableCenter.getWorld().spawnEntity(orbSpawnLocation, EntityType.ITEM_DISPLAY);
        floatingOrbDisplay.setItemStack(orbItemStack.clone());
        floatingOrbDisplay.setTeleportDuration(1);
        float scale = 0.85f;
        floatingOrbDisplay.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()
        ));
    }

    private void updatePhase3OrbInstability(int tick) {
        if (floatingOrbDisplay == null || !floatingOrbDisplay.isValid() || tableCenter.getWorld() == null) return;

        int relativeTick = tick - 26; // 0 to 43
        double progress = relativeTick / 44.0;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Initially stable (relativeTick < 12), gradually becoming erratic, twitching and jerking
        double jerkYaw = (relativeTick > 12) ? (random.nextDouble() - 0.5) * (15.0 + progress * 65.0) : 0.0;
        double jerkPitch = (relativeTick > 12) ? (random.nextDouble() - 0.5) * (10.0 + progress * 45.0) : 0.0;
        double rotationSpeed = 6.0 + (progress * progress * 35.0);
        double angleDegrees = (relativeTick * rotationSpeed) + jerkYaw;

        // Erratic downward dips toward the table as container loses containment
        double suddenDip = (relativeTick > 15 && relativeTick % 6 == 0) ? -0.14 * progress : 0.0;
        double yOffset = 1.35 + (Math.sin(relativeTick * 0.16) * 0.05) + suddenDip;
        Location orbLocation = tableCenter.clone().add(
                (random.nextDouble() - 0.5) * 0.06 * progress,
                yOffset,
                (random.nextDouble() - 0.5) * 0.06 * progress
        );
        orbLocation.setYaw((float) angleDegrees);
        orbLocation.setPitch((float) jerkPitch);
        floatingOrbDisplay.teleport(orbLocation);

        // Sound cues: heartbeats and tendril clicks pacing the container breakdown
        if (relativeTick % 14 == 0) {
            tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f + (float) progress * 0.4f, 0.7f + (float) progress * 0.3f);
        }
        if (relativeTick % 8 == 0) {
            tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 0.75f, 0.9f + random.nextFloat() * 0.3f);
        }

        // Souls escaping from the Orb in irregular bursts, pulled down toward the Crafting Table
        int interval = Math.max(2, (int) (7 - progress * 5));
        if (relativeTick % interval == 0) {
            Vector leakDirection = new Vector(random.nextDouble() - 0.5, (random.nextDouble() - 0.3) * 0.4, random.nextDouble() - 0.5).normalize().multiply(0.18 + progress * 0.15);
            tableCenter.getWorld().spawnParticle(Particle.SOUL, orbLocation, 0, leakDirection.getX(), leakDirection.getY(), leakDirection.getZ(), 0.1);
            if (relativeTick % (interval * 2) == 0) {
                tableCenter.getWorld().playSound(orbLocation, Sound.PARTICLE_SOUL_ESCAPE, 0.6f + (float) progress * 0.4f, 0.9f + random.nextFloat() * 0.4f);
            }
        }

        // Escaped souls curving into inward pull toward the table
        if (relativeTick > 18) {
            for (int i = 0; i < 2; i++) {
                double angle = Math.toRadians((relativeTick * 18.0) + (i * 180.0));
                double radius = 1.6 - (progress * 0.4);
                Location swirlLocation = tableCenter.clone().add(Math.cos(angle) * radius, 0.3 + Math.sin(angle * 2.0) * 0.15, Math.sin(angle) * radius);
                Vector toTable = tableCenter.toVector().subtract(swirlLocation.toVector()).normalize().multiply(0.16 + progress * 0.12);
                tableCenter.getWorld().spawnParticle(Particle.SOUL, swirlLocation, 0, toTable.getX(), -0.02, toTable.getZ(), 0.08);
            }
        }
    }

    private void updatePhase4LivingSculkAndInk(int tick) {
        if (tableCenter.getWorld() == null) return;

        int relativeTick = tick - 70; // 0 to 34
        double progress = relativeTick / 35.0;

        // Progressively corrupt the 5x5 modeled footprint in directional waves (outer edge -> inner field)
        if (relativeTick == 0 || relativeTick == 8 || relativeTick == 16 || relativeTick == 24) {
            corruptSculkFootprint(relativeTick / 8);
            tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.55f + (float) progress * 0.35f);
        }

        // Directional movement: sweeping Sculk and soul rotation along the outer field speeding up inwards
        int outerPoints = 4;
        for (int i = 0; i < outerPoints; i++) {
            double angle = Math.toRadians((tick * (18.0 + progress * 16.0)) + (i * (360.0 / outerPoints)));
            double outerRadius = 2.4 - (progress * 0.3);
            Location sweepLocation = tableCenter.clone().add(Math.cos(angle) * outerRadius, 0.1, Math.sin(angle) * outerRadius);
            tableCenter.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, sweepLocation, 1, 0.03, 0.01, 0.03, 0.01);
            if (relativeTick % 3 == 0) {
                tableCenter.getWorld().spawnParticle(Particle.SCULK_SOUL, sweepLocation, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // Glowing Ink Sac display rises from the player and moves toward the field
        if (relativeTick == 0 && floatingInkDisplay == null) {
            Location inkSpawnLocation = (player.isOnline()) ? player.getLocation().add(0, 1.0, 0) : tableCenter.clone().add(0, 0.4, 0);
            floatingInkDisplay = (ItemDisplay) tableCenter.getWorld().spawnEntity(inkSpawnLocation, EntityType.ITEM_DISPLAY);
            floatingInkDisplay.setItemStack(new ItemStack(Material.GLOW_INK_SAC));
            floatingInkDisplay.setTeleportDuration(1);
            float scale = 0.6f;
            floatingInkDisplay.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f()
            ));
            tableCenter.getWorld().playSound(inkSpawnLocation, Sound.ITEM_GLOW_INK_SAC_USE, 1.0f, 1.1f);
        }

        if (floatingInkDisplay != null && floatingInkDisplay.isValid()) {
            // Rise smoothly toward 0.8 blocks above the Crafting Table
            double inkElevation = 0.4 + (progress * 0.45);
            Location targetInkLocation = tableCenter.clone().add(0, inkElevation, 0);
            targetInkLocation.setYaw((float) (tick * 22.0));
            floatingInkDisplay.teleport(targetInkLocation);

            // Releasing luminous ink droplets that spread across the Sculk field
            if (relativeTick % 3 == 0) {
                tableCenter.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, targetInkLocation, 2, 0.08, 0.08, 0.08, 0.01);
                tableCenter.getWorld().spawnParticle(Particle.GLOW, targetInkLocation, 2, 0.06, 0.06, 0.06, 0.01);
            }
        }
    }

    private void updatePhase5PatternAndEscalation(int tick) {
        if (tableCenter.getWorld() == null) return;

        int relativeTick = tick - 105; // 0 to 34
        double progress = relativeTick / 35.0;

        // Floating Ink Sac hovers over table while ink markings lock into the Pattern geometry
        if (floatingInkDisplay != null && floatingInkDisplay.isValid()) {
            Location inkLocation = tableCenter.clone().add(0, 0.85 + Math.sin(tick * 0.25) * 0.04, 0);
            inkLocation.setYaw((float) (tick * 35.0));
            floatingInkDisplay.teleport(inkLocation);
        }

        // Orb violently jerking and shuddering downward toward the table
        if (floatingOrbDisplay != null && floatingOrbDisplay.isValid()) {
            double violentDip = (relativeTick % 3 == 0) ? -0.16 : 0.0;
            Location orbLocation = tableCenter.clone().add(0, 1.15 + violentDip, 0);
            orbLocation.setYaw((float) (tick * 75.0));
            floatingOrbDisplay.teleport(orbLocation);
        }

        // 1. The Ink locks into the shape of the Forgotten Pattern (segmented, indented rib/spine notches)
        // connecting the outer field to the Orb and Crafting Table
        Location orbPosition = (floatingOrbDisplay != null && floatingOrbDisplay.isValid())
                ? floatingOrbDisplay.getLocation()
                : tableCenter.clone().add(0, 1.15, 0);

        double markingRadius = 2.1 - (progress);
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians((i * 90.0) + (relativeTick * (12.0 + progress * 14.0)));
            double x = Math.cos(angle) * markingRadius;
            double z = Math.sin(angle) * markingRadius;
            Location groundMarkLocation = tableCenter.clone().add(x, 0.12, z);

            // Ground markings
            tableCenter.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, groundMarkLocation, 1, 0.02, 0.01, 0.02, 0.003);
            tableCenter.getWorld().spawnParticle(Particle.GLOW, groundMarkLocation, 1, 0.015, 0.01, 0.015, 0.002);

            // Lateral indents representing the BORDURE_INDENTED pattern notches
            double perpAngle = angle + (Math.PI / 2.0);
            double ribOffset = 0.22 * (1.0 - progress * 0.5);
            Location ribLeft = groundMarkLocation.clone().add(Math.cos(perpAngle) * ribOffset, 0, Math.sin(perpAngle) * ribOffset);
            Location ribRight = groundMarkLocation.clone().add(-Math.cos(perpAngle) * ribOffset, 0, -Math.sin(perpAngle) * ribOffset);
            tableCenter.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, ribLeft, 1, 0.005, 0.005, 0.005, 0.002);
            tableCenter.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, ribRight, 1, 0.005, 0.005, 0.005, 0.002);

            // 2. Luminous ink lines physically connect the Orb down to the markings
            Vector tetherVector = groundMarkLocation.toVector().subtract(orbPosition.toVector());
            double step = 0.35;
            for (double d = step; d < tetherVector.length(); d += step) {
                Location tetherPoint = orbPosition.clone().add(tetherVector.clone().normalize().multiply(d));
                tableCenter.getWorld().spawnParticle(Particle.GLOW, tetherPoint, 1, 0.01, 0.01, 0.01, 0.0);
            }

            // 3. Souls forcibly dragged inward through the pattern conduits into the Crafting Table
            Vector pullToCenter = tableCenter.toVector().subtract(groundMarkLocation.toVector()).normalize().multiply(0.22 + progress * 0.15);
            tableCenter.getWorld().spawnParticle(Particle.SOUL, groundMarkLocation, 0, pullToCenter.getX(), 0.04, pullToCenter.getZ(), 0.12);
        }

        // Wood distress on the Crafting Table as it is being physically overwhelmed
        tableCenter.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, tableCenter.clone().add(0, 0.5, 0), 4, 0.25, 0.25, 0.25, 0.03);
        if (relativeTick % 4 == 0) {
            tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_WOOD_BREAK, 0.6f + (float) progress * 0.5f, 0.55f);
            tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CHARGE, 0.9f, 0.75f + (float) progress * 0.45f);
        }
    }

    private void corruptSculkFootprint(int step) {
        Block base = craftingTableBlock.getRelative(BlockFace.DOWN);

        // Partition the 5x5 ritual field into 4 directional waves:
        // step 0: outermost ring (corners and far edges, dist >= 2.2)
        // step 1: intermediate ring (dist between 1.8 and 2.2)
        // step 2: close ring (dist between 1.2 and 1.8)
        // step 3: immediate neighbors (dist < 1.2)
        for (int[] offset : RITUAL_FIELD_OFFSETS) {
            int dx = offset[0];
            int dz = offset[1];
            double distance = Math.sqrt(dx * dx + dz * dz);

            boolean matchesWave = switch (step) {
                case 0 -> distance >= 2.2;
                case 1 -> distance >= 1.7 && distance < 2.2;
                case 2 -> distance >= 1.2 && distance < 1.7;
                default -> distance < 1.2;
            };

            if (matchesWave) {
                Block target = base.getRelative(dx, 0, dz);
                if (target.getType().isSolid() && target.getType() != Material.SCULK && !temporarySculkBlocks.containsKey(target.getLocation())) {
                    temporarySculkBlocks.put(target.getLocation(), target.getState());
                    target.setType(Material.SCULK, false);
                }
            }
        }
    }

    private void executePhase6Overwrite() {
        if (tableCenter.getWorld() == null) return;

        // Sequence: corruption reverses inward, ink collapses, Orb plunges, table is overwritten
        craftingTableBlock.setType(Material.AIR, false);

        if (floatingInkDisplay != null && floatingInkDisplay.isValid()) {
            floatingInkDisplay.remove();
            floatingInkDisplay = null;
        }

        // Sound: wood giving way + heavy sculk catalyst bloom and soul escape
        tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_WOOD_BREAK, 1.25f, 0.5f);
        tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.35f, 0.5f);
        tableCenter.getWorld().playSound(tableCenter, Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 0.45f);

        // Dense burst of ink and souls collapsing into the table's empty footprint
        tableCenter.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, tableCenter, 25, 0.3, 0.3, 0.3, 0.05);
        tableCenter.getWorld().spawnParticle(Particle.SCULK_SOUL, tableCenter, 50, 0.35, 0.35, 0.35, 0.06);
        tableCenter.getWorld().spawnParticle(Particle.SOUL, tableCenter, 35, 0.25, 0.25, 0.25, 0.05);
    }

    private void updatePhase8SoulReformation(int tick) {
        if (tableCenter.getWorld() == null) return;

        int relativeTick = tick - 151; // 0 to 39
        double progress = relativeTick / 40.0;

        // Remaining souls slowly reform where the table stood, rotate around the empty center, then tighten & compress
        double radius = 1.3 * (1.0 - progress);
        double outerAngle = Math.toRadians(relativeTick * (16.0 + progress * 24.0));
        double innerAngle = Math.toRadians(-relativeTick * (22.0 + progress * 28.0));

        for (int i = 0; i < 4; i++) {
            double a1 = outerAngle + (i * (Math.PI / 2.0));
            Location outerLocation = tableCenter.clone().add(Math.cos(a1) * radius, 0.2 + Math.sin(a1) * 0.1, Math.sin(a1) * radius);
            tableCenter.getWorld().spawnParticle(Particle.SOUL, outerLocation, 1, 0, 0, 0, 0);

            double a2 = innerAngle + (i * (Math.PI / 2.0));
            Location innerLocation = tableCenter.clone().add(Math.cos(a2) * (radius * 0.65), 0.35 + Math.cos(a2) * 0.1, Math.sin(a2) * (radius * 0.65));
            tableCenter.getWorld().spawnParticle(Particle.SCULK_SOUL, innerLocation, 1, 0, 0, 0, 0);
        }

        if (relativeTick == 0) {
            tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.15f, 1.0f);
        }

        if (relativeTick % 10 == 0) {
            tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 0.8f + (float) progress * 0.4f, 0.7f + (float) progress * 0.4f);
            tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CHARGE, 0.7f + (float) progress * 0.4f, 0.8f + (float) progress * 0.4f);
        }
    }

    private void executePhase10ResonantRelease() {
        if (tableCenter.getWorld() == null) return;

        tableCenter.getWorld().playSound(tableCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.45f, 0.85f);
        tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.35f, 0.6f);
        tableCenter.getWorld().playSound(tableCenter, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.95f, 0.8f);
        tableCenter.getWorld().spawnParticle(Particle.SONIC_BOOM, tableCenter.clone().add(0, 0.4, 0), 1, 0, 0, 0, 0);
        tableCenter.getWorld().spawnParticle(Particle.FLASH, tableCenter.clone().add(0, 0.4, 0), 2, 0, 0, 0, 0, Color.fromRGB(0, 220, 220));
        tableCenter.getWorld().spawnParticle(Particle.SCULK_SOUL, tableCenter.clone().add(0, 0.4, 0), 45, 0.35, 0.35, 0.35, 0.08);

        restoreTemporarySculk();

        if (floatingOrbDisplay != null && floatingOrbDisplay.isValid()) {
            floatingOrbDisplay.remove();
            floatingOrbDisplay = null;
        }

        // Spawn Forgotten Workbench as a dropped item in the world
        BlightedItem workbench = ItemRegistry.get("FORGOTTEN_WORKBENCH");
        if (workbench != null) {
            ItemStack drop = workbench.toItemStack();
            Location dropLocation = tableCenter.clone().add(0, 0.2, 0);
            tableCenter.getWorld().dropItem(dropLocation, drop);
        }
    }

    private void consumeProgressionItems() {
        if (!player.isOnline()) return;

        // 1. Consume 1 Glowing Ink Sac (check offhand first, then storage)
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem.getType() == Material.GLOW_INK_SAC && offHandItem.getAmount() >= 1) {
            offHandItem.setAmount(offHandItem.getAmount() - 1);
        } else {
            for (ItemStack item : player.getInventory().getStorageContents()) {
                if (item != null && item.getType() == Material.GLOW_INK_SAC && item.getAmount() >= 1) {
                    item.setAmount(item.getAmount() - 1);
                    break;
                }
            }
        }

        // 2. Consume 1 Forgotten Pattern (check storage contents)
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR) {
                BlightedItem blighted = BlightedItem.fromItemStack(item);
                if (blighted != null && "FORGOTTEN_PATTERN".equals(blighted.getItemId())) {
                    item.setAmount(item.getAmount() - 1);
                    break;
                }
            }
        }
        resetOrbSouls();
    }

    private void resetOrbSouls() {
        if (!player.isOnline()) return;
        NamespacedKey trappedSoulsKey = new NamespacedKey(plugin, "souls_trapped");
        NamespacedKey absorbedSoulKey = new NamespacedKey(plugin, "cipher_absorbed_soul");

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                BlightedItem blighted = BlightedItem.fromItemStack(item);
                if (blighted != null && "ECHOING_TWISTED_ORB".equals(blighted.getItemId())) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        pdc.set(trappedSoulsKey, PersistentDataType.INTEGER, 0);
                        pdc.set(absorbedSoulKey, PersistentDataType.BOOLEAN, false);
                        List<String> lore = meta.getLore();
                        if (lore != null) {
                            for (int i = 0; i < lore.size(); i++) {
                                String line = lore.get(i);
                                if (line.contains("Souls Bound:") || line.contains("Souls trapped:")) {
                                    lore.set(i, "§8 Souls Bound: §d0 ☠");
                                    break;
                                }
                            }
                            meta.setLore(lore);
                        }
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private void restoreTemporarySculk() {
        for (BlockState state : temporarySculkBlocks.values()) {
            state.update(true, false);
        }
        temporarySculkBlocks.clear();
    }

    public void stop(boolean completed) {
        if (runningTask != null) {
            try {
                runningTask.cancel();
            } catch (IllegalStateException _) {
            }
            runningTask = null;
        }

        if (floatingOrbDisplay != null && floatingOrbDisplay.isValid()) {
            floatingOrbDisplay.remove();
            floatingOrbDisplay = null;
        }

        if (floatingInkDisplay != null && floatingInkDisplay.isValid()) {
            floatingInkDisplay.remove();
            floatingInkDisplay = null;
        }

        if (!completed) {
            restoreTemporarySculk();
        }
        ACTIVE_RITUALS.remove(craftingTableBlock.getLocation());
    }
}
