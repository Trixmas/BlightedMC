package fr.moussax.blightedSMP.engine.fishing.hooks;

import fr.moussax.bedrock.text.Messenger;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.fishing.FishingComboTracker;
import fr.moussax.blightedSMP.engine.fishing.FishingLootTable;
import fr.moussax.blightedSMP.engine.fishing.FishingMethod;
import fr.moussax.blightedSMP.engine.fishing.modifiers.FishingSpeedCalculator;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingLootRegistry;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class VoidFishingHook implements CustomFishingHook {
    private static final Map<UUID, VoidFishingHook> ACTIVE_HOOKS = new HashMap<>();

    private static final double CENTRAL_ISLAND_RADIUS_SQUARED = 1000.0 * 1000.0;
    private static final double MIN_DROP_DISTANCE = 20.0;
    private static final double MIN_Y_LEVEL = 0.0;

    private static final int BASE_MIN_WAIT_TICKS = 220; // 11 seconds
    private static final int BASE_MAX_WAIT_TICKS = 460; // 23 seconds

    private static final int BASE_BITE_WINDOW_TICKS = 40;
    private static final double PARTICLE_SPEED = 0.2;
    private static final double PARTICLE_DISTANCE_SQUARED = 0.25 * 0.25;

    private static final Vector FLOAT_UP = new Vector(0, 0.02, 0);
    private static final Vector FLOAT_DOWN = new Vector(0, -0.01, 0);

    private static final double IDLE_RING_RADIUS = 1.2;
    private static final int STARLIGHT_INTERVAL_TICKS = 40;

    private final FishHook hook;
    private final BlightedPlayer blightedPlayer;
    private final Player player;
    private final World.Environment environment;
    private final int luckOfSeaLevel;
    private final double fishingSpeedStat;

    private BukkitRunnable task;
    private boolean isReadyToCatch = false;
    private boolean suspendedInVoid = false;
    private double targetY;
    private double particleAngleOffset = 0.0;
    private int idleTickCounter = 0;

    public VoidFishingHook(FishHook hook, BlightedPlayer blightedPlayer, Player player, ItemStack rod, double fishingSpeedStat) {
        this.hook = hook;
        this.blightedPlayer = blightedPlayer;
        this.player = player;
        this.environment = hook.getWorld().getEnvironment();
        this.fishingSpeedStat = fishingSpeedStat;
        this.luckOfSeaLevel = rod != null ? rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA) : 0;

        ACTIVE_HOOKS.put(hook.getUniqueId(), this);
        startVoidDropTask();
    }

    private void startVoidDropTask() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (isInvalid()) return;
                Location hookLocation = hook.getLocation();

                if (hookLocation.getY() <= MIN_Y_LEVEL) {
                    Messenger.warn(player, "The void consumes your hook. Life cannot endure beneath Y0.");
                    remove();
                    hook.remove();
                    return;
                }

                double dropDistance = player.getLocation().getY() - hookLocation.getY();
                if (dropDistance >= MIN_DROP_DISTANCE) {
                    double distanceSquared = (hookLocation.getX() * hookLocation.getX()) + (hookLocation.getZ() * hookLocation.getZ());
                    if (distanceSquared <= CENTRAL_ISLAND_RADIUS_SQUARED) {
                        Messenger.warn(player, "The void beneath the central island is barren. Cast farther out.");
                        remove();
                        hook.remove();
                        return;
                    }

                    suspendedInVoid = true;
                    targetY = hookLocation.getY();

                    startWaitTask(calculateWaitTime());
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private int calculateWaitTime() {
        int baseTicks = ThreadLocalRandom.current().nextInt(BASE_MIN_WAIT_TICKS, BASE_MAX_WAIT_TICKS + 1);
        return FishingSpeedCalculator.applyToWaitTicks(baseTicks, fishingSpeedStat);
    }

    private int calculateBiteWindow() {
        return BASE_BITE_WINDOW_TICKS + (luckOfSeaLevel * 10);
    }

    private void startWaitTask(int ticksUntilCatch) {
        task = new BukkitRunnable() {
            private int timer = ticksUntilCatch;

            @Override
            public void run() {
                if (isInvalid()) return;
                maintainFloatPosition();
                spawnIdleParticles();

                if (--timer <= 0) {
                    startCatchAnimation();
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private void maintainFloatPosition() {
        hook.setGravity(false);
        double currentY = hook.getLocation().getY();
        hook.setVelocity(currentY < targetY ? FLOAT_UP : FLOAT_DOWN);
    }

    private void spawnIdleParticles() {
        World world = hook.getWorld();
        Location center = hook.getLocation();

        particleAngleOffset += 0.12;
        if (particleAngleOffset > Math.PI * 2) {
            particleAngleOffset -= Math.PI * 2;
        }

        double bob = Math.sin(particleAngleOffset * 1.7) * 0.15;
        Location ringCenter = center.clone().add(0, bob, 0);

        for (int i = 0; i < 3; i++) {
            double angle = particleAngleOffset + (Math.PI * 2 / 3) * i;
            double x = Math.cos(angle) * IDLE_RING_RADIUS;
            double z = Math.sin(angle) * IDLE_RING_RADIUS;
            Particle particle = (i % 2 == 0) ? Particle.PORTAL : Particle.REVERSE_PORTAL;
            world.spawnParticle(particle, ringCenter.clone().add(x, 0, z), 0, 0, 0, 0, 0);
        }

        if (++idleTickCounter >= STARLIGHT_INTERVAL_TICKS) {
            idleTickCounter = 0;
            spawnStarlightTwinkle(world, center);
        }
    }

    private void spawnStarlightTwinkle(World world, Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = IDLE_RING_RADIUS * (0.5 + random.nextDouble() * 0.8);
        double y = (random.nextDouble() - 0.5) * 0.8;

        Location point = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        world.playSound(point, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.6f);
    }

    private void startCatchAnimation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;

        Location particleLocation = hook.getLocation().clone().add(Math.cos(angle) * 5.0, 0, Math.sin(angle) * 5.0);

        task = new BukkitRunnable() {
            private boolean reachedHook = false;
            private final int totalBiteTicks = calculateBiteWindow();
            private int readyTicks = totalBiteTicks;

            @Override
            public void run() {
                if (isInvalid()) return;

                if (!reachedHook) {
                    maintainFloatPosition();
                    moveParticleToHook(particleLocation);

                    if (particleLocation.distanceSquared(hook.getLocation()) < PARTICLE_DISTANCE_SQUARED) {
                        reachedHook = true;
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 0.5f);
                        spawnImplosionFlash(hook.getLocation());
                        hook.setVelocity(new Vector(0, -0.2, 0));
                    }
                } else {
                    isReadyToCatch = true;
                    spawnAwaitingCatchParticles(readyTicks, totalBiteTicks);

                    if (--readyTicks <= 0) {
                        FishingComboTracker.resetCombo(player, FishingMethod.VOID);
                        remove();
                    }
                }
            }
        };
        task.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private void moveParticleToHook(Location particleLocation) {
        Vector direction = hook.getLocation().toVector().subtract(particleLocation.toVector());
        direction.normalize().multiply(PARTICLE_SPEED);
        particleLocation.add(direction);

        World world = Objects.requireNonNull(particleLocation.getWorld());
        world.spawnParticle(Particle.END_ROD, particleLocation, 2, 0.02, 0.02, 0.02, 0.0);
        world.spawnParticle(Particle.SMOKE, particleLocation, 1, 0.04, 0.04, 0.04, 0.001);
    }

    private void spawnImplosionFlash(Location center) {
        World world = Objects.requireNonNull(center.getWorld());

        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2 / 10) * i;
            Location point = center.clone().add(Math.cos(angle) * 0.8, 0.1, Math.sin(angle) * 0.8);
            Vector inward = center.toVector().subtract(point.toVector()).normalize().multiply(0.15);
            world.spawnParticle(Particle.PORTAL, point, 0, inward.getX(), inward.getY() + 0.05, inward.getZ(), 1.0);
        }

        world.spawnParticle(Particle.WITCH, center, 20, 0.3, 0.4, 0.3, 0.08);
    }

    private void spawnAwaitingCatchParticles(int readyTicks, int totalBiteTicks) {
        double urgency = 1.0 - (readyTicks / (double) totalBiteTicks);
        int pulseInterval = Math.max(1, 6 - (int) (urgency * 5));

        if (readyTicks % pulseInterval != 0) return;

        World world = hook.getWorld();
        Location center = hook.getLocation();
        double pullRadius = 0.6 - (urgency * 0.4);

        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 / 3) * i + (readyTicks * 0.3);
            double x = Math.cos(angle) * pullRadius;
            double z = Math.sin(angle) * pullRadius;
            world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(x, 0.1, z), 0, 0, -0.05, 0, 0.02);
        }

        world.spawnParticle(Particle.SOUL, center.clone().add(0, 0.1, 0), 1, 0.15, 0, 0.15, 0.01);
    }

    private boolean isInvalid() {
        if (hook.isDead() || !player.isOnline()) {
            remove();
            return true;
        }
        return false;
    }

    @Override
    public boolean reelIn() {
        remove();

        if (!isReadyToCatch || !suspendedInVoid) {
            FishingComboTracker.resetCombo(player, FishingMethod.VOID);
            return false;
        }

        Location hookLocation = hook.getLocation();
        World world = hookLocation.getWorld();
        if (world == null) return false;

        spawnVoidCollapseBurst(world, hookLocation);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.6f, 0.7f);

        Location playerLocation = player.getLocation();
        Vector velocity = CustomFishingHook.calculateLaunchVelocity(hookLocation, playerLocation);

        Location spawnLocation = hookLocation.add(0, 0.5, 0);

        int currentCombo = FishingComboTracker.getCombo(player, FishingMethod.VOID);
        FishingLootTable lootTable = FishingLootRegistry.getTable(environment, FishingMethod.VOID);
        boolean success = lootTable.roll(blightedPlayer, spawnLocation, velocity, luckOfSeaLevel, currentCombo);

        if (success) {
            FishingComboTracker.incrementCombo(player, FishingMethod.VOID);
            int newCombo = FishingComboTracker.getCombo(player, FishingMethod.VOID);

            ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(playerLocation, EntityType.EXPERIENCE_ORB);
            orb.setExperience(ThreadLocalRandom.current().nextInt(5, 12));
            FishingComboTracker.spawnBonusExperience(world, playerLocation, newCombo);
        }

        return success;
    }

    private void spawnVoidCollapseBurst(World world, Location center) {
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 / 16) * i;
            Vector outward = new Vector(Math.cos(angle), 0.05, Math.sin(angle)).multiply(0.3);
            world.spawnParticle(Particle.REVERSE_PORTAL, center, 0, outward.getX(), outward.getY(), outward.getZ(), 1.0);
        }

        world.spawnParticle(Particle.PORTAL, center, 30, 0.4, 0.6, 0.4, 0.1);
        world.spawnParticle(Particle.END_ROD, center, 8, 0.1, 0.3, 0.1, 0.02);
    }

    @Override
    public void remove() {
        ACTIVE_HOOKS.remove(hook.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        if (!hook.isDead()) {
            hook.setGravity(true);
        }
    }

    @Override
    public ItemType getRequiredRodType() {
        return ItemType.VOID_FISHING_ROD;
    }

    public static VoidFishingHook get(FishHook hook) {
        return ACTIVE_HOOKS.get(hook.getUniqueId());
    }

    public static void cleanupAll() {
        new ArrayList<>(ACTIVE_HOOKS.values()).forEach(VoidFishingHook::remove);
    }
}
