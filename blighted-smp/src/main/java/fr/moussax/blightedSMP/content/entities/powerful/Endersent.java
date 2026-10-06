package fr.moussax.blightedSMP.content.entities.powerful;

import fr.moussax.blightedSMP.content.utils.ai.EndermanAI;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.attachment.AttachmentRole;
import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public class Endersent extends SpawnableEntity {

    private static final long SMASH_COOLDOWN = 5000;
    private int projectileHits = 0;
    private boolean enraged = false;
    private boolean isEscaping = false;
    private int escapeTicks = 0;

    public Endersent() {
        super("ENDERSENT", "Endersent", EntityType.ENDERMAN);
        setMaxHealth(200);
        setDamage(20);
        setDroppedExp(40);
        spawning(spawn -> spawn
                .probability(0.002)
                .biomes(Biome.END_MIDLANDS)
        );

        attributes(attributes -> attributes
                .followRange(60)
                .scale(2)
                .knockbackResistance(1.0)
                .movementSpeed(0.25)
        );

        loot(loot -> loot
                .maxDrops(2)
                .drop(Material.ENDER_PEARL, 4, 8, 1.0)
                .drop(Material.ENDER_EYE, 1, 3, 0.31)
                .drop("ENCHANTED_ENDER_PEARL", 1, 4, 0.11, RARE)
                .blight(30, 0.03, VERY_RARE)
        );

        boss();
    }

    @Override
    protected void onDefineBehavior() {
        addCoreAbility(20L, 20L, this::tickCombat);
    }

    private void tickCombat() {
        if (!isAlive()) return;
        tickSmash();
        tickEscape();
    }

    private void tickSmash() {
        if (!enraged || isEscaping) return;
        if (!isCooldownReady("smash", SMASH_COOLDOWN)) return;

        Player target = getNearestPlayer(30);
        if (target == null) return;

        if (entity.getLocation().distance(target.getLocation()) > 4) {
            performTeleportSmash(target);
        }
    }

    private void performTeleportSmash(Player target) {
        triggerCooldown("smash");

        swingMainHand();

        Location behind = target.getLocation().add(target.getLocation().getDirection().multiply(-1.5));
        behind.setY(target.getLocation().getY());

        entity.teleport(behind);
        playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        addCoreDelayedAction(9L, () -> {
            if (!isAlive()) return;
            spawnParticle(Particle.EXPLOSION, 1);
            playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            damageNearbyPlayers(3.0, 14.0);
        });
    }

    private void tickEscape() {
        if (!isEscaping) return;

        escapeTicks += 20;

        if (!hasSubordinateAttachments() || escapeTicks >= 300) {
            endDeadlyEscape();
        }
    }

    @Override
    public void onDamageTaken(EntityDamageEvent event) {
        if (enraged || isEscaping) return;

        boolean trigger = false;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            trigger = true;
        } else if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            projectileHits++;
            if (projectileHits >= 7) trigger = true;
        }

        if (trigger) startDeadlyEscape();
    }

    private void startDeadlyEscape() {
        enraged = true;
        isEscaping = true;
        escapeTicks = 0;

        Location location = entity.getLocation();

        spawnParticle(location, Particle.EXPLOSION_EMITTER, 1);
        playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

        damageNearbyPlayers(4.0, 18.0);

        entity.setInvisible(true);
        entity.setInvulnerable(true);
        entity.setAI(false);
        entity.teleport(location.clone().add(0, 50, 0));

        int count = 3 + new Random().nextInt(4);
        for (int i = 0; i < count; i++) {
            BlightedEntity watchling = EntitiesRegistry.create("WATCHLING");
            if (watchling == null) continue;
            LivingEntity wEntity = watchling.spawn(
                    location.clone().add((Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2)
            );
            addAttachment(wEntity, AttachmentRole.SUBORDINATE);
        }
    }

    private void endDeadlyEscape() {
        if (!isEscaping || !isAlive()) return;
        isEscaping = false;

        killAttachments(AttachmentRole.SUBORDINATE);

        Player target = getNearestPlayer(60);
        Location targetLoc = (target != null) ? target.getLocation() : entity.getLocation().subtract(0, 50, 0);
        Location reappearLoc = targetLoc.clone().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
        reappearLoc.setY(targetLoc.getY());

        entity.teleport(reappearLoc);
        entity.setInvisible(false);
        entity.setInvulnerable(false);
        entity.setAI(true);

        playSound(reappearLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        EndermanAI.init(spawned);
    }
}
