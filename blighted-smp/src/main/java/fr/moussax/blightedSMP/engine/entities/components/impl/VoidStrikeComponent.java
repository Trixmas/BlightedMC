package fr.moussax.blightedSMP.engine.entities.components.impl;

import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.components.EntityComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Void strike component marking hit players with scaling damage multipliers over time.
 */
public final class VoidStrikeComponent implements EntityComponent {

    private Map<UUID, Long> markedPlayers = new HashMap<>();
    private static final long MARK_DURATION_MS = 5000L;
    private static final double MAX_MULTIPLIER = 3.0;

    @Override
    public String getId() {
        return "AFFIX_VOID_STRIKE";
    }

    @Override
    public void onDestroy(LivingEntity entity) {
        markedPlayers.clear();
    }

    @Override
    public void onTick(BlightedEntity owner) {
        LivingEntity entity = owner.getEntity();
        Location mobLocation = entity.getLocation().add(0, entity.getHeight() / 2, 0);

        entity.getWorld().spawnParticle(Particle.SCULK_SOUL, mobLocation, 5, 0.4, 0.4, 0.4, 0.05);
        entity.getWorld().spawnParticle(Particle.DUST, mobLocation, 3, 0.3, 0.3, 0.3, 0.0, new Particle.DustOptions(Color.fromRGB(20, 0, 40), 1.0f));

        if (markedPlayers.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = markedPlayers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            Player player = entity.getServer().getPlayer(entry.getKey());

            if (player == null || !player.isOnline() || player.isDead() || now - entry.getValue() > MARK_DURATION_MS) {
                iterator.remove();
                continue;
            }

            Particle.DustOptions deepPurpleDust = new Particle.DustOptions(Color.fromRGB(112, 0, 180), 1.3f);
            Particle.DustOptions neonVibrantPurpleDust = new Particle.DustOptions(Color.fromRGB(200, 0, 255), 0.9f);

            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.2, 0), 4, 0.2, 0.4, 0.2, 0.0, deepPurpleDust);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.2, 0), 3, 0.3, 0.5, 0.3, 0.0, neonVibrantPurpleDust);
            player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1.2, 0), 2, 0.2, 0.4, 0.2, 0.01);
            player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation().add(0, 1.2, 0), 2, 0.1, 0.2, 0.1, 0.01);

            if (System.currentTimeMillis() % 200 < 50) {
                Vector direction = player.getLocation().add(0, 1, 0).toVector().subtract(mobLocation.toVector()).normalize().multiply(0.5);
                Location current = mobLocation.clone();
                for (int i = 0; i < 10; i++) {
                    current.add(direction);
                    entity.getWorld().spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(140, 0, 210), 0.6f));
                }
            }
        }
    }

    @Override
    public void onDealDamage(BlightedEntity owner, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long timeApplied = markedPlayers.get(playerId);
        if (timeApplied != null) {
            long elapsed = now - timeApplied;
            if (elapsed <= MARK_DURATION_MS) {
                double secondsElapsed = elapsed / 1000.0;
                double multiplier = 1.0 + Math.min(MAX_MULTIPLIER - 1.0, secondsElapsed * 0.4);

                event.setDamage(event.getDamage() * multiplier);

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.4f, 0.5f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.5f);

                player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
            } else {
                markedPlayers.remove(playerId);
            }
        }

        markedPlayers.put(playerId, now);
    }
}
