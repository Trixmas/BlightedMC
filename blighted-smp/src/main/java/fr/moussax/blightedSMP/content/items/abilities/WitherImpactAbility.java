package fr.moussax.blightedSMP.content.items.abilities;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class WitherImpactAbility implements ItemAbility<PlayerInteractEvent> {
    private static final String WITHER_SHIELD_COOLDOWN = "wither_shield";
    private static final double TELEPORT_DISTANCE = 10.0;
    private static final double TELEPORT_STEP = 0.5;
    private static final double MIN_DAMAGE = 15000.0;
    private static final double MAX_DAMAGE = 150000.0;
    private static final double DAMAGE_RANGE = 5.0;

    @Override
    public String getName() {
        return "Wither Impact";
    }

    @Override
    public AbilityTrigger getTrigger() {
        return AbilityTrigger.RIGHT_CLICK;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Teleport §a10 §7blocks ahead of you, dealing §c15,000 ",
                "damage to nearby enemies. Also applies the wither",
                "shield scroll reducing damage taken and granting",
                "an §6absorption §7shield for §e5 §7seconds."
        };
    }

    @Override
    public int getManaCost() {
        return 50;
    }

    @Override
    public boolean triggerAbility(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return false;
        }

        Player player = event.getPlayer();
        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        if (blightedPlayer == null) return false;

        teleport(player);
        damageNearbyEntities(player);

        if (blightedPlayer.getRemainingCooldown(WITHER_SHIELD_COOLDOWN, getTrigger()) <= 0) {
            applyHealingEffect(player);
            blightedPlayer.setCooldown(WITHER_SHIELD_COOLDOWN, getTrigger(), 5);
        }
        return true;
    }

    private void teleport(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        Location teleportDestination = findTeleportDestination(player, direction);
        player.teleport(teleportDestination);
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
        world.spawnParticle(Particle.EXPLOSION, player.getLocation(), 5);
    }

    private Location findTeleportDestination(Player player, Vector direction) {
        Location location = player.getLocation();
        for (double i = 0; i < TELEPORT_DISTANCE; i += TELEPORT_STEP) {
            Location testLocation = location.clone().add(direction.clone().multiply(i));
            if (!testLocation.getBlock().isPassable())
                return location.clone().add(direction.clone().multiply(i - TELEPORT_STEP));
        }
        return location.clone().add(direction.clone().multiply(TELEPORT_DISTANCE));
    }

    private double damageNearbyEntities(Player origin) {
        double damage = MIN_DAMAGE + (Math.random() * (MAX_DAMAGE - MIN_DAMAGE));
        double totalDamageDealt = 0.0;
        int entitiesDamaged = 0;

        for (Entity entity : origin.getNearbyEntities(DAMAGE_RANGE, DAMAGE_RANGE, DAMAGE_RANGE)) {
            if (entity instanceof LivingEntity livingEntity && !(entity instanceof Player)) {
                livingEntity.damage(damage, origin);
                totalDamageDealt += damage;
                entitiesDamaged++;
            }
        }
        notifyPlayerOfAbilityDamage(origin, entitiesDamaged, totalDamageDealt);
        return totalDamageDealt;
    }

    private void notifyPlayerOfAbilityDamage(Player player, int entitiesDamaged, double totalDamage) {
        if (entitiesDamaged > 0) {
            Messenger.inform(player, "Your implosion hit §d" + entitiesDamaged + " §7enem" + (entitiesDamaged > 1 ? "ies" : "y") + " for §d" + Formatter.formatDecimalWithCommas(totalDamage) + " §7damage.");
        }
    }

    private void applyHealingEffect(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 10));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1));
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        world.playSound(playerLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
        world.spawnParticle(Particle.EXPLOSION, playerLocation, 1);
    }
}
