package fr.moussax.blightedSMP.content.bosses;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.bedrock.utils.debug.Log;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.defense.DamageType;
import fr.moussax.blightedSMP.engine.entities.defense.EntityImmunities;
import fr.moussax.blightedSMP.engine.entities.defense.EntityResistance;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@EntityImmunities({DamageType.FALL, DamageType.MACE, DamageType.PROJECTILE})
@EntityResistance(type = DamageType.MAGIC, percent = 75.0)
public class CorruptedChampion extends BlightedEntity {

    private static final int MELEE_DAMAGE = 28;
    private static final double STAB_DAMAGE = 16.0;
    private static final double STAB_RETURN_DAMAGE = 12.0;
    private static final double EARTHQUAKE_DAMAGE = 22.0;
    private static final double SWORD_THROW_FLIGHT_DAMAGE = 20.0;
    private static final double SWORD_THROW_RETURN_DAMAGE = 12.0;
    private static final double BLADENADO_DAMAGE = 14.0;
    private static final double PHASE_TRANSITION_BLAST_DAMAGE = 8.0;

    private static final ItemStack RED_BOOTS = createRocketBoots();
    private static final Particle.DustOptions VOID_PURPLE = new Particle.DustOptions(Color.fromRGB(150, 40, 240), 1.2f);
    private static final Particle.DustOptions CHAMPION_GOLD = new Particle.DustOptions(Color.fromRGB(255, 200, 30), 1.5f);

    private List<StabPlayer> activeStabs = new CopyOnWriteArrayList<>();
    private int currentPhase = 1;

    public CorruptedChampion() {
        super("CORRUPTED_CHAMPION", "Corrupted Champion", EntityType.ZOMBIE);
        attributes(attributes -> attributes
                .maxHealth(350)
                .attackDamage(MELEE_DAMAGE)
                .scale(4.0)
                .knockbackResistance(0.40)
                .set(Attribute.SPAWN_REINFORCEMENTS, 0.0)
        );
        boss();
        equipment(equipment -> equipment
                .helmet(new ItemBuilder(Material.PLAYER_HEAD)
                        .setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFiNTRjMWNlOTQyYzIzMjFkNmRiYjgzMjQ1ZWJmM2ZmZDY5NmJmOWQxZjAyNDY3MzY0NzFmNjdmNzJiYTI1MCJ9fX0=")
                        .toItemStack())
                .chestplate(new ItemBuilder(Material.GOLDEN_CHESTPLATE).setArmorTrim(TrimMaterial.GOLD, TrimPattern.SNOUT).toItemStack())
                .leggings(Material.IRON_LEGGINGS)
                .boots(Material.GOLDEN_BOOTS)
                .mainHand(new ItemBuilder(Material.NETHERITE_SWORD).glow().toItemStack())
        );
    }

    private static ItemStack createRocketBoots() {
        return new ItemBuilder(Material.LEATHER_BOOTS)
                .setLeatherColor("#B02E26")
                .setArmorTrim(TrimMaterial.QUARTZ, TrimPattern.BOLT)
                .setUnbreakable(true)
                .toItemStack();
    }

    public static Location calculateGiantAnchor(Location visualLocation, float yaw, boolean inverted, double scale) {
        double rad = Math.toRadians(yaw);
        Vector forward = new Vector(-Math.sin(rad), 0, Math.cos(rad));
        Vector right = new Vector(-Math.cos(rad), 0, -Math.sin(rad));

        Location anchor = visualLocation.clone();
        anchor.setYaw(yaw);
        anchor.setPitch(0);

        if (inverted) {
            Vector offset = right.clone().multiply(-1.85 * scale).add(forward.clone().multiply(0.4 * scale));
            return anchor.subtract(offset).subtract(0, 2.5 * scale, 0);
        }

        Vector offset = right.clone().multiply(1.85 * scale).add(forward.clone().multiply(0.4 * scale));
        return anchor.subtract(offset).subtract(0, 7.8 * scale, 0);
    }

    private Location calculateReturnLocation(Location origin, LivingEntity bossEntity, double progress) {
        double yawRad = Math.toRadians(bossEntity.getLocation().getYaw());
        Vector rightDirection = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        Location handLocation = bossEntity.getEyeLocation().clone()
                .add(rightDirection.multiply(2.2)).subtract(0, 1.5, 0);

        double arcY = Math.sin(progress * Math.PI) * 1.8;

        return origin.clone().add(handLocation.toVector().subtract(origin.toVector())
                .multiply(progress)).add(0, arcY, 0);
    }

    @Override
    protected void onDefineBehavior() {
        registerPhase(1.0, () -> {
            currentPhase = 1;
            setMainHandEquipped(true);
            addPhaseAbility(80L, 180L, this::stabTargetPlayers);
            addPhaseAbility(20L, 30L, () -> meleeAttackNearestPlayer(6));
        });

        registerPhase(0.66, () -> {
            setMainHandEquipped(true);
            addPhaseAbility(120L, 220L, this::executeStomp);
            addPhaseAbility(160L, 260L, this::executeSwordThrow);
            addPhaseAbility(20L, 30L, () -> meleeAttackNearestPlayer(6));
        });

        registerPhase(0.33, () -> {
            setMainHandEquipped(true);
            addPhaseAbility(140L, 200L, this::executeSwordThrow);
            addPhaseAbility(240L, 340L, this::executeBladenado);
            addPhaseAbility(20L, 30L, () -> meleeAttackNearestPlayer(6));
        });
    }

    @Override
    protected long onPhaseTransition(double healthThreshold) {
        if (healthThreshold == 1.0) {
            return 0L;
        }

        setBoots(new ItemStack(Material.GOLDEN_BOOTS));

        if (healthThreshold == 0.66) {
            currentPhase = 2;
            setBossBarAppearance(BarColor.RED, BarStyle.SEGMENTED_6);
        } else if (healthThreshold == 0.33) {
            currentPhase = 3;
            setBossBarAppearance(BarColor.RED, BarStyle.SEGMENTED_6);
        }

        if (entity instanceof Mob mob) {
            mob.setAI(false);
            mob.setTarget(null);
        }

        Location center = entity.getLocation().clone();
        World world = Objects.requireNonNull(center.getWorld());

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }

                ticks++;

                if (ticks <= 40) {
                    double riseProgress = ticks / 40.0;
                    double yOffset = Math.sin(riseProgress * (Math.PI / 2.0)) * 2.5;
                    Location hoverLocation = center.clone().add(0, yOffset, 0);
                    hoverLocation.setYaw((hoverLocation.getYaw() + 8) % 360);
                    entity.teleport(hoverLocation);

                    double helixRadius = 2.2 * (1.0 - riseProgress * 0.4);
                    double angleOffset = ticks * 0.25;

                    for (int i = 0; i < 4; i++) {
                        double angle = angleOffset + (i * (Math.PI / 2.0));
                        double x = helixRadius * Math.cos(angle);
                        double z = helixRadius * Math.sin(angle);

                        Location goldPosition = hoverLocation.clone().add(x, -0.8 + (riseProgress * 1.5), z);
                        world.spawnParticle(Particle.DUST, goldPosition, 2, 0.05, 0.05, 0.05, 0.0, CHAMPION_GOLD);
                        Location voidPosition = hoverLocation.clone().add(-x, 2.2 - (riseProgress * 1.5), -z);
                        world.spawnParticle(Particle.DUST, voidPosition, 2, 0.05, 0.05, 0.05, 0.0, VOID_PURPLE);
                    }

                    world.spawnParticle(Particle.ENCHANT, hoverLocation.clone().add(0, 1.2, 0), 3, 0.3, 0.5, 0.3, 0.8);

                    if (ticks % 8 == 0) {
                        world.playSound(hoverLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.2f, 0.5f + (ticks * 0.025f));
                        world.playSound(hoverLocation, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f + (ticks * 0.02f));
                    }
                } else if (ticks <= 45) {
                    Location bossLocation = entity.getLocation().add(0, 1.2, 0);
                    for (int i = 0; i < 12; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 1.5 + Math.random() * 1.5;
                        Location spawnPosition = bossLocation.clone().add(dist * Math.cos(angle), (Math.random() - 0.5) * 2, dist * Math.sin(angle));
                        Vector inward = bossLocation.toVector().subtract(spawnPosition.toVector()).normalize().multiply(0.35);

                        world.spawnParticle(Particle.PORTAL, spawnPosition, 0, inward.getX(), inward.getY(), inward.getZ(), 1.0);
                    }
                } else if (ticks == 46) {
                    Location blastLocation = entity.getLocation();

                    world.playSound(blastLocation, Sound.ENTITY_WITHER_SPAWN, 1.8f, 0.85f);
                    world.playSound(blastLocation, Sound.ITEM_TRIDENT_THUNDER, 2.0f, 0.7f);
                    world.playSound(blastLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.6f);

                    world.spawnParticle(Particle.FLASH, blastLocation.clone().add(0, 1.5, 0), 3, Color.WHITE);
                    world.spawnParticle(Particle.FLAME, blastLocation.clone().add(0, 1.5, 0), 100, 2.5, 1.8, 2.5, 0.2);
                    world.spawnParticle(Particle.DUST, blastLocation.clone().add(0, 1.5, 0), 60, 2.0, 1.0, 2.0, 0.0, CHAMPION_GOLD);
                    world.spawnParticle(Particle.DUST, blastLocation.clone().add(0, 1.5, 0), 60, 2.0, 1.0, 2.0, 0.0, VOID_PURPLE);

                    for (int i = 0; i < 32; i++) {
                        double angle = (2 * Math.PI / 32) * i;
                        Location ring = blastLocation.clone().add(4.5 * Math.cos(angle), 0.2, 4.5 * Math.sin(angle));
                        world.spawnParticle(Particle.SWEEP_ATTACK, ring, 1);
                    }

                    damageAndKnockbackNearbyPlayers(blastLocation, 14.0, PHASE_TRANSITION_BLAST_DAMAGE, 1.8, 0.5);

                    for (Player player : getNearbyPlayers(blastLocation, 14.0)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, true));
                    }
                } else if (ticks >= 60) {
                    Location groundLocation = center.clone();
                    groundLocation.setPitch(0);
                    entity.teleport(groundLocation);

                    cancel();
                    if (entity instanceof Mob mob) {
                        mob.setAI(true);
                    }
                }
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
        return 60L;
    }

    private void applyHeavyAttack(Player target, double damage, int shieldCooldownTicks) {
        disableShieldIfBlocking(target, shieldCooldownTicks);
        target.damage(damage, entity);
    }

    private void stabTargetPlayers() {
        if (currentPhase != 1 || isPerformingAbility()) {
            return;
        }

        Player target = getNearestPlayer(20);
        if (target == null) {
            return;
        }

        BlightedPlayer blightedTarget = BlightedPlayer.get(target);
        if (blightedTarget == null) {
            return;
        }

        setPerformingAbility(true);
        if (entity instanceof Mob mob) {
            mob.swingMainHand();
        }

        activeStabs.add(new StabPlayer(blightedTarget, this));
        addPhaseDelayedAction(15L, () -> setPerformingAbility(false));
    }

    private void executeStomp() {
        if (isPerformingAbility()) {
            return;
        }

        setPerformingAbility(true);
        setBoots(RED_BOOTS);

        if (entity instanceof Mob mob) {
            mob.setAI(false);
        }

        Location startLocation = entity.getLocation().clone();
        World initialWorld = entity.getWorld();
        initialWorld.playSound(startLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.6f, 0.6f);
        initialWorld.playSound(startLocation, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.4f);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!isAlive()) {
                    cleanup();
                    return;
                }

                ticks++;
                World world = entity.getWorld();

                if (ticks <= 14) {
                    double riseProgress = ticks / 14.0;
                    double yOffset = Math.sin(riseProgress * (Math.PI / 2.0)) * 6.0;
                    Location currentLocation = startLocation.clone().add(0, yOffset, 0);
                    entity.teleport(currentLocation);

                    world.spawnParticle(Particle.DUST, currentLocation.clone().add(0, 0.5, 0), 4, 0.3, 0.1, 0.3, 0.0, VOID_PURPLE);
                    world.spawnParticle(Particle.FLAME, currentLocation.clone().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0.02);

                    if (ticks == 14) {
                        world.playSound(currentLocation, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.5f);
                    }
                } else if (ticks <= 22) {
                    double plungeProgress = (ticks - 14) / 8.0;
                    double yOffset = 6.0 * (1.0 - (plungeProgress * plungeProgress));
                    Location currentLocation = startLocation.clone().add(0, Math.max(0, yOffset), 0);
                    entity.teleport(currentLocation);

                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 6, 0.3, 0.5, 0.3, 0.04);
                    world.spawnParticle(Particle.CRIT, currentLocation.clone().add(0, 1.0, 0), 4, 0.2, 0.4, 0.2, 0.05);
                } else {
                    entity.teleport(startLocation);
                    cleanup();
                    detonateEarthquake();
                }
            }

            private void cleanup() {
                cancel();
                setBoots(new ItemStack(Material.GOLDEN_BOOTS));
                if (entity instanceof Mob mob) {
                    mob.setAI(true);
                }
                setPerformingAbility(false);
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private void detonateEarthquake() {
        Location center = entity.getLocation().clone();
        World world = Objects.requireNonNull(center.getWorld());

        world.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.8f, 0.5f);
        world.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 2.5f, 0.5f);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.7f);

        new BukkitRunnable() {
            private double currentRadius = 1.5;
            private final double maxRadius = 13.0;

            @Override
            public void run() {
                if (currentRadius > maxRadius || !isAlive()) {
                    cancel();
                    return;
                }

                int points = (int) (currentRadius * 14);
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    double x = currentRadius * Math.cos(angle);
                    double z = currentRadius * Math.sin(angle);
                    Location ringLocation = center.clone().add(x, 0, z);

                    Block highestBlock = world.getHighestBlockAt(ringLocation);
                    Location blockSurface = highestBlock.getLocation().add(0.5, 1.0, 0.5);

                    world.spawnParticle(Particle.BLOCK, blockSurface, 3, 0.2, 0.3, 0.2, highestBlock.getBlockData());
                    world.spawnParticle(Particle.DUST, blockSurface.clone().add(0, 0.2, 0), 1, 0.0, 0.0, 0.0, 0.0, CHAMPION_GOLD);
                }

                for (Entity nearby : world.getNearbyEntities(center, currentRadius + 1.2, 3.5, currentRadius + 1.2)) {
                    if (nearby.equals(entity)) {
                        continue;
                    }

                    if (nearby instanceof Player player && player.getGameMode() == GameMode.SURVIVAL) {
                        double distance = player.getLocation().distance(center);
                        if (Math.abs(distance - currentRadius) <= 1.4) {
                            player.damage(EARTHQUAKE_DAMAGE, entity);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 255, false, false, true));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 4, false, false, true));
                            player.setVelocity(new Vector(0, 0.6, 0));
                        }
                    }
                }
                currentRadius += 1.2;
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 1L, 3L);
    }

    private void executeSwordThrow() {
        if (isPerformingAbility()) {
            return;
        }

        List<Player> nearby = getNearbyPlayers(26.0);

        if (nearby.isEmpty()) {
            return;
        }

        Player target = nearby.stream()
                .max(Comparator.comparingDouble(
                        player -> player.getLocation().distanceSquared(entity.getLocation()))
                )
                .orElse(nearby.getFirst());

        setPerformingAbility(true);

        Location bossLocation = entity.getLocation();
        Vector toTarget = target.getLocation().toVector().subtract(bossLocation.toVector()).setY(0);

        if (toTarget.lengthSquared() > 0.001) {
            float throwYaw = (float) Math.toDegrees(-Math.atan2(toTarget.getX(), toTarget.getZ()));
            bossLocation.setYaw(throwYaw);
            entity.teleport(bossLocation);
        }

        World world = entity.getWorld();
        world.playSound(bossLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        world.playSound(bossLocation, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.7f);

        new BukkitRunnable() {
            private int tick = 0;
            private Giant thrownSword;
            private Location handOrigin;
            private Location targetApex;
            private boolean pinned = false;
            private int pinTicks = 0;
            private int returnStep = 0;
            private final int totalReturnSteps = 12;

            @Override
            public void run() {
                if (!isAlive()) {
                    cleanup();
                    return;
                }

                tick++;

                if (tick <= 6) {
                    if (tick == 1 && entity instanceof Mob mob) {
                        mob.swingMainHand();
                    }
                    world.spawnParticle(Particle.DUST, entity.getEyeLocation().add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0.0, VOID_PURPLE);
                    return;
                }

                if (tick == 7) {
                    setMainHandEquipped(false);

                    double yawRad = Math.toRadians(entity.getLocation().getYaw());
                    Vector rightDirection = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
                    handOrigin = entity.getEyeLocation().clone().add(rightDirection.multiply(2.2)).subtract(0, 1.5, 0);

                    Location spawnLocation = calculateGiantAnchor(handOrigin, entity.getLocation().getYaw(), true, 0.85);
                    thrownSword = spawnInvertedSword(spawnLocation, 0.85);

                    targetApex = target.isOnline() ? target.getLocation().clone()
                            .add(0, 0.5, 0) : entity.getLocation()
                            .add(entity.getLocation().getDirection().multiply(12));

                    world.playSound(handOrigin, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.8f, 0.9f);
                    return;
                }

                if (thrownSword == null || thrownSword.isDead()) {
                    cleanup();
                    return;
                }

                int flightTick = tick - 7;

                if (flightTick <= 10 && !pinned) {
                    double progress = flightTick / 10.0;
                    double arcY = Math.sin(progress * Math.PI) * 1.2;

                    Location currentLocation = handOrigin.clone().add(targetApex.toVector().subtract(handOrigin.toVector()).multiply(progress)).add(0, arcY, 0);
                    float spinYaw = (float) ((flightTick * 72) % 360);
                    thrownSword.teleport(calculateGiantAnchor(currentLocation, spinYaw, true, 0.85));

                    world.spawnParticle(Particle.SWEEP_ATTACK, currentLocation, 1, 0.0, 0.0, 0.0, 0.0);
                    world.spawnParticle(Particle.CRIT, currentLocation, 3, 0.15, 0.15, 0.15, 0.03);

                    for (Player player : getNearbyPlayers(26)) {
                        if (player.getLocation().distanceSquared(currentLocation) <= 6.0) {
                            applyHeavyAttack(player, SWORD_THROW_FLIGHT_DAMAGE, 60);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
                        }
                    }

                    if (flightTick == 10) {
                        pinned = true;
                        world.playSound(targetApex, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.6f);
                        world.playSound(targetApex, Sound.BLOCK_STONE_BREAK, 1.8f, 0.5f);
                        world.spawnParticle(Particle.BLOCK, targetApex, 16, 0.4, 0.3, 0.4,
                                world.getBlockAt(targetApex.clone().subtract(0, 1, 0)).getBlockData()
                        );
                        world.spawnParticle(Particle.DUST, targetApex.clone().add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0.0, VOID_PURPLE);
                    }
                    return;
                }

                if (pinned && pinTicks < 12) {
                    pinTicks++;
                    Location pinLocation = targetApex.clone().add((Math.random() - 0.5) * 0.06, 0, (Math.random() - 0.5) * 0.06);
                    thrownSword.teleport(calculateGiantAnchor(pinLocation, entity.getLocation().getYaw(), true, 0.85));

                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, targetApex, 1, 0.05, 0.1, 0.05, 0.01);

                    if (pinTicks == 10) {
                        world.playSound(entity.getLocation(), Sound.BLOCK_CHAIN_FALL, 1.5f, 1.4f);
                    }
                    return;
                }

                returnStep++;
                double returnProgress = (double) returnStep / totalReturnSteps;

                if (returnStep == 1) {
                    world.playSound(targetApex, Sound.BLOCK_CHAIN_FALL, 1.5f, 1.4f);
                }

                Location currentReturnLocation = calculateReturnLocation(targetApex, entity, returnProgress);

                float spinYaw = (float) ((returnStep * 60) % 360);
                thrownSword.teleport(calculateGiantAnchor(currentReturnLocation, spinYaw, true, 0.85));
                world.spawnParticle(Particle.DUST, currentReturnLocation, 5, 0.15, 0.15, 0.15, 0.0, CHAMPION_GOLD);

                for (Player player : getNearbyPlayers(26)) {
                    if (player.getLocation().distanceSquared(currentReturnLocation) <= 5.0) {
                        player.damage(SWORD_THROW_RETURN_DAMAGE, entity);
                    }
                }

                if (returnStep >= totalReturnSteps) {
                    if (entity instanceof Mob mob) {
                        mob.swingMainHand();
                    }
                    world.playSound(entity.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.5f, 1.0f);
                    cleanup();
                }
            }

            private void cleanup() {
                cancel();
                if (thrownSword != null) {
                    if (!thrownSword.isDead()) {
                        thrownSword.remove();
                    }
                }
                setMainHandEquipped(true);
                setPerformingAbility(false);
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
    }

    private void cleanupBladenado(List<Giant> tornadoSwords) {
        for (Giant sword : tornadoSwords) {
            if (sword != null) {
                if (!sword.isDead()) {
                    sword.remove();
                }
            }
        }

        setMainHandEquipped(true);

        if (entity instanceof Mob mob) {
            mob.setAI(true);
        }

        endAbility();
    }

    private void executeBladenado() {
        if (currentPhase != 3) {
            return;
        }

        if (!tryStartAbility()) {
            return;
        }


        setPerformingAbility(true);

        Location startCenter = entity.getLocation().clone();
        int bladeCount = 8;
        double radius = 4.8;
        List<Giant> tornadoSwords = new ArrayList<>();

        setMainHandEquipped(false);

        if (entity instanceof Mob mob) {
            mob.setAI(false);
        }

        for (int i = 0; i < bladeCount; i++) {
            double angle = i * (2 * Math.PI / bladeCount);
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            double yWave = Math.sin(angle) * 0.4;
            Location visualBladeTarget = startCenter.clone().add(xOffset, 2.2 + yWave, zOffset);

            float tangentYaw = (float) Math.toDegrees(angle);
            Location giantAnchor = calculateGiantAnchor(visualBladeTarget, tangentYaw, true, 0.85);
            Giant sword = spawnInvertedSword(giantAnchor, 0.85);
            tornadoSwords.add(sword);
        }

        World world = Objects.requireNonNull(startCenter.getWorld());
        world.playSound(startCenter, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.0f, 0.6f);

        new BukkitRunnable() {
            private int ticks = 0;
            private double spinAngle = 0;

            @Override
            public void run() {
                if (!isAlive()) {
                    cleanup();
                    return;
                }

                if (ticks >= 140) {
                    cleanup();
                    return;
                }

                try {
                    Location currentCenter = entity.getLocation().clone();
                    Player chaseTarget = getNearestPlayer(16.0);

                    if (chaseTarget != null) {
                        Vector chaseVector = chaseTarget.getLocation().toVector().subtract(currentCenter.toVector()).setY(0);
                        if (chaseVector.lengthSquared() > 0.5) {
                            double currentSpeed = 0.13 + ((double) ticks / 140.0) * 0.10;
                            currentCenter.add(chaseVector.normalize().multiply(currentSpeed));
                        }
                    }

                    currentCenter.setYaw((currentCenter.getYaw() + 25) % 360);
                    entity.teleport(currentCenter);
                    spinAngle = (spinAngle + 0.22) % (2 * Math.PI);

                    for (int i = 0; i < bladeCount; i++) {
                        Giant sword = tornadoSwords.get(i);
                        if (sword == null || !sword.isValid() || sword.isDead()) {
                            continue;
                        }

                        double angle = spinAngle + (i * (2 * Math.PI / bladeCount));
                        double xOffset = radius * Math.cos(angle);
                        double zOffset = radius * Math.sin(angle);
                        double yWave = Math.sin(ticks * 0.25 + (i * (2 * Math.PI / bladeCount))) * 0.4;

                        Location visualBladeTarget = currentCenter.clone().add(xOffset, 2.2 + yWave, zOffset);
                        float tangentYaw = (float) Math.toDegrees(angle);
                        visualBladeTarget.setYaw(tangentYaw);
                        visualBladeTarget.setPitch(0);

                        Location giantAnchor = calculateGiantAnchor(visualBladeTarget, tangentYaw, true, 0.85);
                        sword.teleport(giantAnchor);
                        world.spawnParticle(Particle.SWEEP_ATTACK, visualBladeTarget, 1);
                        world.spawnParticle(Particle.DUST, visualBladeTarget.clone().add(0, 0.2, 0), 2, 0.15, 0.15, 0.15, 0.0, CHAMPION_GOLD);
                    }

                    world.spawnParticle(Particle.LARGE_SMOKE, currentCenter.clone().add(0, 2.0, 0), 4, 1.5, 0.4, 1.5, 0.03);

                    if (ticks % 2 == 0) {
                        for (Player player : getNearbyPlayers(12.0)) {
                            double distance = player.getLocation().distance(currentCenter);
                            if (distance > 6.0 && distance <= 12.0) {
                                Vector pull = currentCenter.toVector().subtract(player.getLocation().toVector()).setY(0).normalize().multiply(0.08);
                                player.setVelocity(player.getVelocity().add(pull));
                            }
                        }
                    }

                    if (ticks % 8 == 0) {
                        if (entity instanceof Mob mob) {
                            mob.swingMainHand();
                        }
                        world.playSound(currentCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
                        for (Player player : getNearbyPlayers(6.5)) {
                            player.damage(BLADENADO_DAMAGE, entity);
                            player.setNoDamageTicks(0);
                            Vector knockback = player.getLocation().toVector().subtract(currentCenter.toVector())
                                    .normalize()
                                    .multiply(0.6)
                                    .setY(0.2);

                            player.setVelocity(knockback);
                        }
                    }
                    ticks++;
                } catch (Throwable throwable) {
                    Log.error("CorruptedChampion", "Bladenado error: " + throwable.getMessage());
                    cleanup();
                }
            }

            private void cleanup() {
                cancel();
                cleanupBladenado(tornadoSwords);
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 0L, 1L);
    }

    @Override
    public void onDamageTaken(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    public Giant spawnInvertedSword(Location spawnLocation, double scale) {
        Location targetLocation = spawnLocation.clone();
        targetLocation.setPitch(0);
        targetLocation.setYaw(spawnLocation.getYaw());
        Giant giant = Objects.requireNonNull(spawnLocation.getWorld()).spawn(targetLocation, Giant.class, giantEntity -> {
                    configureSwordGiant(giantEntity, scale);
                    giantEntity.setCustomName("Dinnerbone");
                }
        );
        return giant;
    }

    private void configureSwordGiant(Giant giant, double scale) {
        giant.setAI(false);
        giant.setCustomNameVisible(false);
        giant.setInvisible(true);
        giant.setInvulnerable(true);
        giant.setSilent(true);
        giant.setGravity(false);
        giant.setCollidable(false);
        giant.setPersistent(false);
        giant.setRemoveWhenFarAway(false);

        AttributeInstance scaleAttribute = giant.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        Objects.requireNonNull(giant.getEquipment()).setItemInMainHand(new ItemBuilder(Material.NETHERITE_SWORD).glow().toItemStack());
    }

    private boolean tryStartAbility() {
        if (isPerformingAbility()) {
            return false;
        }
        setPerformingAbility(true);
        return true;
    }

    private void endAbility() {
        setPerformingAbility(false);
    }

    @Override
    public LivingEntity spawn(Location location) {
        super.spawn(location);
        if (entity instanceof Zombie zombie) {
            zombie.setAdult();
            zombie.setConversionTime(-1);
        }
        return entity;
    }

    @Override
    public void onDeath(Location location) {
        stopAllStabs();
    }

    private void stopAllStabs() {
        for (StabPlayer stab : new ArrayList<>(activeStabs)) {
            try {
                stab.cancel();
            } catch (IllegalStateException ignored) {
            }
        }
        activeStabs.clear();
    }

    private static class StabPlayer extends BukkitRunnable {
        private final BlightedPlayer target;
        private final CorruptedChampion owner;
        private Location stabledLocation;
        private Giant swordEntity;
        private int tick = 0;
        private boolean returning = false;
        private int returnStep = 0;
        private final int totalReturnSteps = 12;
        private Location groundApex;

        StabPlayer(BlightedPlayer target, CorruptedChampion owner) {
            this.target = target;
            this.owner = owner;
            summonSwordEntity();
            runTaskTimer(BlightedSMP.getInstance(), 1L, 1L);
        }

        private void summonSwordEntity() {
            owner.setMainHandEquipped(false);
            Location spawnLocation = target.getPlayer().getLocation().clone();
            spawnLocation.setPitch(0);
            spawnLocation.setYaw(0);
            spawnLocation.subtract(2, -4, 4);

            swordEntity = owner.spawnInvertedSword(spawnLocation, 1.0);
        }

        @Override
        public void run() {
            if (!owner.isAlive() || swordEntity == null || swordEntity.isDead()) {
                cancel();
                return;
            }

            if (tick == 0) {
                stabledLocation = target.getPlayer().getLocation();
                Objects.requireNonNull(stabledLocation.getWorld()).playSound(stabledLocation, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 0.75f);
            }

            if (!returning) {
                if (tick < 90) {
                    if (target.getPlayer().isOnline()) {
                        stabledLocation = target.getPlayer().getLocation().clone();
                        trackSwordAbovePlayer();
                    }
                } else if (tick < 101) {
                    if (target.getPlayer().isOnline()) {
                        stabledLocation = target.getPlayer().getLocation().clone();
                        trackSwordAbovePlayer();
                    }
                    drawWarningCircle(stabledLocation, Objects.requireNonNull(stabledLocation.getWorld()));

                    if (tick == 90) {
                        stabledLocation.getWorld().playSound(stabledLocation, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 1.6f);
                    }
                } else if (tick == 101) {
                    if (owner.getEntity() instanceof Mob mob) {
                        mob.swingMainHand();
                    }

                    plungeAndDamage();
                    groundApex = stabledLocation.clone().add(0, 0.5, 0);
                } else if (tick >= 115) {
                    returning = true;

                    if (owner.getEntity() != null) {
                        owner.getEntity().getWorld().playSound(owner.getEntity().getLocation(), Sound.BLOCK_CHAIN_FALL, 1.5f, 1.4f);
                    }
                }
            } else {
                executeReturnFlight();
            }

            tick++;
        }

        private void executeReturnFlight() {
            returnStep++;
            double progress = (double) returnStep / totalReturnSteps;
            LivingEntity bossEntity = owner.getEntity();
            if (bossEntity == null || !bossEntity.isValid()) {
                cancel();
                return;
            }

            Location currentReturnLocation = owner.calculateReturnLocation(groundApex, bossEntity, progress);
            float spinYaw = (float) ((returnStep * 60) % 360);
            swordEntity.teleport(CorruptedChampion.calculateGiantAnchor(currentReturnLocation, spinYaw, true, 0.85));
            World world = Objects.requireNonNull(currentReturnLocation.getWorld());
            world.spawnParticle(Particle.DUST, currentReturnLocation, 5, 0.15, 0.15, 0.15, 0.0, CHAMPION_GOLD);

            for (Player player : owner.getNearbyPlayers(26)) {
                if (player.getLocation().distanceSquared(currentReturnLocation) <= 5.0) {
                    player.damage(STAB_RETURN_DAMAGE, bossEntity);
                }
            }

            if (returnStep >= totalReturnSteps) {
                if (bossEntity instanceof Mob mob) {
                    mob.swingMainHand();
                }
                world.playSound(bossEntity.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.5f, 1.0f);
                cancel();
            }
        }

        private void trackSwordAbovePlayer() {
            Location above = target.getPlayer().getLocation().clone();
            above.setPitch(0);
            above.setYaw(0);
            above.subtract(2, -4, 4);
            swordEntity.teleport(above);

            if (tick % 2 == 0) {
                target.getPlayer().getWorld().spawnParticle(Particle.DUST, target.getPlayer()
                        .getLocation().add(0, 3.5, 0), 2, 0.1, 0.3, 0.1, 0.0, VOID_PURPLE);
            }
        }

        private void drawWarningCircle(Location center, World world) {
            double radius = 1.6;
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI / 12) * i;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location ringLocation = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.DUST, ringLocation, 1, 0.0, 0.0, 0.0, 0.0, VOID_PURPLE);
            }
        }

        private void plungeAndDamage() {
            Location plungeLocation = stabledLocation.clone();
            plungeLocation.setPitch(0);
            plungeLocation.setYaw(0);
            plungeLocation.subtract(2, 1, 4);
            swordEntity.teleport(plungeLocation);

            World world = Objects.requireNonNull(stabledLocation.getWorld());

            world.spawnParticle(Particle.BLOCK, stabledLocation, 16, 0.4, 0.3, 0.4, world.getBlockAt(stabledLocation.clone().subtract(0, 1, 0)).getBlockData());
            world.spawnParticle(Particle.DUST, stabledLocation.clone().add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0.0, VOID_PURPLE);
            world.playSound(stabledLocation, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            world.playSound(stabledLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.8f);

            for (Player player : owner.getNearbyPlayers(stabledLocation, 6.0)) {
                owner.applyHeavyAttack(player, STAB_DAMAGE, 70);
            }
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            if (swordEntity != null && !swordEntity.isDead()) {
                swordEntity.remove();
            }
            owner.activeStabs.remove(this);
            if (owner.activeStabs.isEmpty()) {
                owner.setMainHandEquipped(true);
            }
        }
    }
}
