package fr.moussax.blightedSMP.content.equipment.abilities.weave;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.fishing.modifiers.FishingSpeedModifier;
import fr.moussax.blightedSMP.engine.items.abilities.AbstractFullSetBonus;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Full set bonus granting fire resistance and lava fishing speed boost for Emberweave armor.
 */
public sealed class EmberWeaveSetBonus extends AbstractFullSetBonus implements FishingSpeedModifier permits MagmaweaveSetBonus {
    protected BukkitTask passiveTask;
    protected boolean isActive = false;

    /**
     * Constructs an Emberweave set bonus requiring 4 armor pieces.
     */
    public EmberWeaveSetBonus() {
        super(4);
    }

    @Override
    public String getName() {
        return "Molten Attunement";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Impervious to the inferno, your",
                "heat signature synchronizes",
                "with §6molten currents§7.",
                "",
                "Grants immunity to §cfire §7and §clava§7.",
                "Grants §b+15% §3Lava Fishing Speed§7."
        };
    }

    @Override
    public double getFishingSpeedBonus() {
        return 18.0;
    }

    @Override
    public void onFishingCast(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 0.8f);
    }

    @Override
    public void startAbilityEffect() {
        if (isActive || getAbilityOwner() == null) return;

        Player player = getAbilityOwner().getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.5f);

        this.isActive = true;
        this.passiveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isActive) {
                    this.cancel();
                    return;
                }
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.FIRE_RESISTANCE,
                                70,
                                0,
                                true,
                                false,
                                true
                        )
                );

                player.getWorld().spawnParticle(
                        Particle.SMALL_FLAME,
                        player.getLocation(),
                        2,
                        0.3,
                        0.1,
                        0.3,
                        0.02
                );

                if (ThreadLocalRandom.current().nextBoolean()) {
                    player.getWorld().spawnParticle(
                            Particle.SMOKE,
                            player.getLocation().add(0, 0.2, 0),
                            2,
                            0.2, 0.3, 0.2,
                            0.01
                    );
                }
            }
        }.runTaskTimer(BlightedSMP.getInstance(), 0L, 20L);
    }

    @Override
    public void stopAbilityEffect() {
        if (!isActive) return;

        this.isActive = false;

        if (passiveTask != null) {
            passiveTask.cancel();
            passiveTask = null;
        }

        if (getAbilityOwner() == null) return;
        Player player = getAbilityOwner().getPlayer();
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }
}
