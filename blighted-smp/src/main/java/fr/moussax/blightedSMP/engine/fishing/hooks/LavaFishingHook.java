package fr.moussax.blightedSMP.engine.fishing.hooks;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.fishing.FishingLootTable;
import fr.moussax.blightedSMP.engine.fishing.FishingMethod;
import fr.moussax.blightedSMP.engine.fishing.FishingComboTracker;
import fr.moussax.blightedSMP.engine.fishing.modifiers.FishingSpeedCalculator;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingLootRegistry;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.blightedSMP.engine.items.ItemType;
import org.bukkit.*;
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

public final class LavaFishingHook implements CustomFishingHook {
    private static final Map<UUID, LavaFishingHook> ACTIVE_HOOKS = new HashMap<>();

    private static final double FLOAT_TARGET_HIGH = 0.95;
    private static final double FLOAT_TARGET_LOW = 0.80;
    private static final double PARTICLE_SPEED = 0.15;
    private static final double PARTICLE_DISTANCE_SQUARED = 0.25 * 0.25;

    private static final Vector FLOAT_UP = new Vector(0, 0.04, 0);
    private static final Vector FLOAT_DOWN = new Vector(0, -0.01, 0);

    private static final int BASE_MIN_WAIT_TICKS = 160; // 8 seconds
    private static final int BASE_MAX_WAIT_TICKS = 400; // 20 seconds
    private static final int BASE_BITE_WINDOW_TICKS = 30;

    private static final int PARTICLE_COUNT = 16; // Player feedback

    private final FishHook hook;
    private final BlightedPlayer blightedPlayer;
    private final Player player;
    private final World.Environment environment;
    private final int luckOfSeaLevel;
    private final double fishingSpeedStat;

    private BukkitRunnable task;
    private boolean isReadyToCatch = false;

    public LavaFishingHook(FishHook hook, BlightedPlayer blightedPlayer, Player player, ItemStack rod, double fishingSpeedStat) {
        this.hook = hook;
        this.blightedPlayer = blightedPlayer;
        this.player = player;
        this.environment = hook.getWorld().getEnvironment();
        this.fishingSpeedStat = fishingSpeedStat;
        this.luckOfSeaLevel = rod != null ? rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA) : 0;
        this.hook.setVisualFire(false);
        this.hook.setFireTicks(0);

        ACTIVE_HOOKS.put(hook.getUniqueId(), this);
        startLavaTask(calculateWaitTime());
    }

    private int calculateWaitTime() {
        int baseTicks = ThreadLocalRandom.current().nextInt(BASE_MIN_WAIT_TICKS, BASE_MAX_WAIT_TICKS + 1);
        return FishingSpeedCalculator.applyToWaitTicks(baseTicks, fishingSpeedStat);
    }

    private int calculateBiteWindow() {
        return BASE_BITE_WINDOW_TICKS + (luckOfSeaLevel * 10);
    }

    private void startLavaTask(int ticksUntilCatch) {
        task = new BukkitRunnable() {
            private int timer = ticksUntilCatch;

            @Override
            public void run() {
                if (isInvalid()) return;

                if (hook.getLocation().getBlock().getType() != Material.LAVA) {
                    hook.setGravity(true);
                    return;
                }

                adjustHookFloat(FLOAT_TARGET_HIGH);

                if (--timer <= 0) {
                    startCatchAnimation();
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private void startCatchAnimation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;

        Location particleLocation = hook.getLocation().clone().add(
                Math.cos(angle) * 4.0,
                0,
                Math.sin(angle) * 4.0
        );

        task = new BukkitRunnable() {
            private boolean reachedHook = false;
            private int readyTicks = calculateBiteWindow();

            @Override
            public void run() {
                if (isInvalid()) return;

                if (!reachedHook) {
                    moveParticleToHook(particleLocation);
                    adjustHookFloat(FLOAT_TARGET_HIGH);

                    if (particleLocation.distanceSquared(hook.getLocation()) < PARTICLE_DISTANCE_SQUARED) {
                        reachedHook = true;
                        hook.getWorld().playSound(hook.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1f, 0.5f);
                    }
                } else {
                    isReadyToCatch = true;
                    adjustHookFloat(FLOAT_TARGET_LOW);

                    if (--readyTicks <= 0) {
                        FishingComboTracker.resetCombo(player, FishingMethod.LAVA);
                        remove();
                    }
                }
            }
        };
        task.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private boolean isInvalid() {
        if (hook.isDead() || !player.isOnline()) {
            remove();
            return true;
        }

        hook.setVisualFire(false);
        hook.setFireTicks(0);

        if (hook.isInWater()) {
            hook.remove();
            remove();
            Messenger.warn(player, "This rod thirsts for molten depths, not ordinary waters.");
            return true;
        }
        return false;
    }

    private void moveParticleToHook(Location particleLocation) {
        Vector direction = hook.getLocation().toVector().subtract(particleLocation.toVector());
        direction.normalize().multiply(PARTICLE_SPEED);
        particleLocation.add(direction);

        Objects.requireNonNull(particleLocation.getWorld()).spawnParticle(
                Particle.SMOKE, particleLocation, PARTICLE_COUNT, 0.05, 0.05, 0.05, 0.01
        );
    }

    private void adjustHookFloat(double targetOffset) {
        hook.setGravity(false);
        double currentOffset = hook.getLocation().getY() - hook.getLocation().getBlockY();

        hook.setVelocity(currentOffset < targetOffset ? FLOAT_UP : FLOAT_DOWN);
    }

    @Override
    public boolean reelIn() {
        remove();

        if (!isReadyToCatch) {
            FishingComboTracker.resetCombo(player, FishingMethod.LAVA);
            return false;
        }

        Location hookLocation = hook.getLocation();
        World world = hookLocation.getWorld();
        if (world == null) return false;

        world.spawnParticle(Particle.LAVA, hookLocation, 10, 0.3, 0.3, 0.3, 0.1);
        world.playSound(hookLocation, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);

        Location playerLocation = player.getLocation();
        Vector velocity = CustomFishingHook.calculateLaunchVelocity(hookLocation, playerLocation);

        Location spawnLocation = hookLocation.add(0, 0.5, 0);

        int currentCombo = FishingComboTracker.getCombo(player, FishingMethod.LAVA);
        FishingLootTable lootTable = FishingLootRegistry.getTable(environment, FishingMethod.LAVA);
        boolean success = lootTable.roll(blightedPlayer, spawnLocation, velocity, luckOfSeaLevel, currentCombo);

        if (success) {
            FishingComboTracker.incrementCombo(player, FishingMethod.LAVA);
            int newCombo = FishingComboTracker.getCombo(player, FishingMethod.LAVA);
            ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(playerLocation, EntityType.EXPERIENCE_ORB);
            orb.setExperience(ThreadLocalRandom.current().nextInt(3, 8));
            FishingComboTracker.spawnBonusExperience(world, playerLocation, newCombo);
        }

        return success;
    }

    @Override
    public void remove() {
        ACTIVE_HOOKS.remove(hook.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public ItemType getRequiredRodType() {
        return ItemType.LAVA_FISHING_ROD;
    }

    public static LavaFishingHook get(FishHook hook) {
        return ACTIVE_HOOKS.get(hook.getUniqueId());
    }

    public static void cleanupAll() {
        new ArrayList<>(ACTIVE_HOOKS.values()).forEach(LavaFishingHook::remove);
    }
}
