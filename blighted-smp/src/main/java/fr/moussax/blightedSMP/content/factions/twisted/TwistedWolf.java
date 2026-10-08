package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

/**
 * Twisted Wolf — pursuit / pack hunter mob.
 *
 * <p>Quickly acquires players, maintains persistent pursuit over distance, attacks aggressively
 * at close range, and nearby Twisted Wolves naturally contribute to the hunt.</p>
 */
public final class TwistedWolf extends TwistedCreature {

    public TwistedWolf() {
        super("TWISTED_WOLF", "Twisted Wolf", EntityType.WOLF, 20, 5);
        setDroppedExp(6);

        attributes(attr -> attr
                .movementSpeed(0.36)
                .followRange(40)
        );

        loot(loot -> loot
                .maxDrops(2)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    @Override
    public LivingEntity spawn(Location location) {
        LivingEntity spawned = super.spawn(location);
        configureWolfAppearance(spawned);
        return spawned;
    }

    @Override
    protected void onRehydrate(LivingEntity existing) {
        super.onRehydrate(existing);
        configureWolfAppearance(existing);
    }

    private void configureWolfAppearance(LivingEntity living) {
        if (living instanceof Wolf wolf) {
            wolf.setAngry(true);
            wolf.setCollarColor(DyeColor.PURPLE);
        }
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();
        // Pack hunting communication: nearby wolves contribute to the hunt
        addCoreAbility(15L, 15L, this::handlePackHunt);
    }

    private void handlePackHunt() {
        if (!isAlive()) return;
        Player target = getTargetPlayer();

        if (target != null) {
            for (Entity nearby : entity.getNearbyEntities(16.0, 8.0, 16.0)) {
                BlightedEntity blighted = EntityManager.getBlightedEntity(nearby);
                if (blighted instanceof TwistedWolf ally && ally.isAlive()) {
                    // Only assign target to wolves without an active, valid target
                    if (ally.getTarget() == null || !ally.getTarget().isValid()) {
                        ally.setAITarget(target);
                    }
                }
            }
        }
    }

    @Override
    protected void onTargetAcquired(Player target) {
        super.onTargetAcquired(target);
        playSound(Sound.ENTITY_WOLF_GROWL, 0.9f, 0.7f);
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        super.onConfigureAI(spawned);
        applyTwistedHostileGoals(spawned, 1.40D);
    }
}
