package fr.moussax.blightedSMP.content.equipment.abilities.weave;

import fr.moussax.blightedSMP.BlightedSMP;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Enhanced set bonus granting fire resistance, resistance in lava, and lava fishing speed boost for Magmaweave armor.
 */
public final class MagmaweaveSetBonus extends EmberWeaveSetBonus {

    private int taskCounter = 0;

    @Override
    public String getName() {
        return "Blazing Shield";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Flames coalesce into a protective",
                "mantle, fortifying you against",
                "hostile attacks.",
                "",
                "Grants immunity to §cfire §7and §clava§7.",
                "Grants §dResistance §5I §7while in lava.",
                "Grants §b+30% §3Lava Fishing Speed§7."
        };
    }

    @Override
    public double getFishingSpeedBonus() {
        return 43.0;
    }

    @Override
    public void startAbilityEffect() {
        if (isActive || getAbilityOwner() == null) return;

        Player player = getAbilityOwner().getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);

        this.isActive = true;
        this.taskCounter = 0;
        this.passiveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isActive) {
                    this.cancel();
                    taskCounter = 0;
                    if (player.isOnline()) {
                        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                        player.removePotionEffect(PotionEffectType.RESISTANCE);
                    }
                    return;
                }
                taskCounter++;

                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 70, 0, true, false, true));

                if (player.getLocation().getBlock().isLiquid() && player.getLocation().getBlock().getType() == Material.LAVA) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false, true));
                    if (taskCounter % 10 == 0) {
                        player.getWorld().spawnParticle(
                                Particle.FALLING_OBSIDIAN_TEAR,
                                player.getLocation().clone().add(0, 1.2, 0),
                                1, 0.2, 0.1, 0.2, 0
                        );
                    }
                }

                player.getWorld().spawnParticle(
                        Particle.FLAME,
                        player.getLocation().clone().add(0, 0.4, 0),
                        2, 0.3, 0.1, 0.3, 0.02
                );
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 0L, 5L);
    }
}
