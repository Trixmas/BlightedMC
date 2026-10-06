package fr.moussax.blightedSMP.engine.loot.results.blightstone;

import fr.moussax.blightedSMP.BlightedSMP;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class BlightstoneConsumptionEffect {

    private static final Particle.DustOptions BLIGHT_CYAN_DUST =
            new Particle.DustOptions(Color.fromRGB(0, 185, 205), 0.75f);
    private static final Particle.DustOptions BLIGHT_PULSE_DUST =
            new Particle.DustOptions(Color.fromRGB(0, 215, 230), 1.0f);
    private static final BlockData DARK_PRISMARINE_DATA = Material.DARK_PRISMARINE.createBlockData();
    private static final BlockData AMETHYST_BLOCK_DATA = Material.AMETHYST_BLOCK.createBlockData();

    private BlightstoneConsumptionEffect() {
    }

    public static void play(Player player) {
        if (player == null || !player.isOnline()) return;

        BlightedSMP plugin = BlightedSMP.getInstance();
        if (plugin == null) return;

        new BukkitRunnable() {
            int tick = 0;
            Location gemOrigin;
            final double[][] wispOffsets = new double[4][2];

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                World world = player.getWorld();

                if (tick == 0) {
                    Location eyeLocation = player.getEyeLocation();
                    Vector direction = eyeLocation.getDirection().setY(0);
                    if (direction.lengthSquared() < 1e-4) {
                        direction = new Vector(0, 0, 1);
                    } else {
                        direction.normalize();
                    }
                    gemOrigin = eyeLocation.clone().subtract(0, 0.35, 0).add(direction.multiply(0.45));

                    double baseAngle = Math.toRadians(player.getLocation().getYaw());
                    for (int i = 0; i < 4; i++) {
                        double angle = baseAngle + (i * Math.PI / 2.0);
                        wispOffsets[i][0] = Math.cos(angle);
                        wispOffsets[i][1] = Math.sin(angle);
                    }
                }

                // Phase 1: Resonance builds (Ticks 0 - 7)
                if (tick < 8) {
                    world.spawnParticle(Particle.DUST, gemOrigin, 2, 0.05, 0.05, 0.05, 0, BLIGHT_CYAN_DUST);
                    if (tick == 3 || tick == 7) {
                        world.spawnParticle(Particle.SCULK_CHARGE_POP, gemOrigin, 1, 0.02, 0.02, 0.02, 0.01);
                    }
                }

                // Phase 2: Sealed Blight escapes (Ticks 8 - 12)
                else if (tick < 13) {
                    double progress = (tick - 8) / 5.0;
                    double radius = 0.75 * progress;
                    double yOffset = 0.25 * progress;

                    for (int i = 0; i < 4; i++) {
                        Location wispLocation = gemOrigin.clone().add(
                                wispOffsets[i][0] * radius,
                                yOffset,
                                wispOffsets[i][1] * radius
                        );
                        Particle soulParticle = (i % 2 == 0) ? Particle.SCULK_SOUL : Particle.SOUL;
                        world.spawnParticle(soulParticle, wispLocation, 1, 0, 0.01, 0, 0.005);
                        world.spawnParticle(Particle.DUST, wispLocation, 1, 0.02, 0.02, 0.02, 0, BLIGHT_CYAN_DUST);
                    }
                }

                // Phase 3: Energy drawn into player's chest (Ticks 13 - 17)
                else if (tick < 18) {
                    double progress = (tick - 13) / 5.0;
                    Location chest = player.getLocation().add(0, 1.2, 0);

                    for (int i = 0; i < 4; i++) {
                        Location startLocation = gemOrigin.clone().add(
                                wispOffsets[i][0] * 0.75,
                                0.25,
                                wispOffsets[i][1] * 0.75
                        );
                        Location currentLocation = startLocation.clone().add(
                                (chest.getX() - startLocation.getX()) * progress,
                                (chest.getY() - startLocation.getY()) * progress,
                                (chest.getZ() - startLocation.getZ()) * progress
                        );
                        world.spawnParticle(Particle.SOUL, currentLocation, 1, 0, 0, 0, 0.005);
                        world.spawnParticle(Particle.DUST, currentLocation, 1, 0.01, 0.01, 0.01, 0, BLIGHT_CYAN_DUST);
                    }

                    if (tick >= 16) {
                        world.spawnParticle(Particle.REVERSE_PORTAL, chest, 3, 0.1, 0.1, 0.1, 0.05);
                    }
                }

                // Phase 4: Crystalline fracture and final pulse (Ticks 18 - 20)
                else if (tick == 18) {
                    Location chest = player.getLocation().add(0, 1.2, 0);
                    world.spawnParticle(Particle.BLOCK, chest, 6, 0.15, 0.15, 0.15, DARK_PRISMARINE_DATA);
                    world.spawnParticle(Particle.BLOCK, chest, 4, 0.15, 0.15, 0.15, AMETHYST_BLOCK_DATA);

                    for (int step = 0; step < 8; step++) {
                        double theta = (step * Math.PI * 2.0) / 8.0;
                        Location pulsePoint = chest.clone().add(Math.cos(theta) * 0.35, 0, Math.sin(theta) * 0.35);
                        world.spawnParticle(Particle.DUST, pulsePoint, 1, 0, 0, 0, 0, BLIGHT_PULSE_DUST);
                    }
                }

                if (tick >= 20) {
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
