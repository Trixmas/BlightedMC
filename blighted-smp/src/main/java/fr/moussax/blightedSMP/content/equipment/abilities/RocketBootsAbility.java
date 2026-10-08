package fr.moussax.blightedSMP.content.equipment.abilities;

import fr.moussax.blightedSMP.engine.items.abilities.AbstractFullSetBonus;
import fr.moussax.blightedSMP.engine.items.equipment.ArmorSetManager;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Piece bonus granting double jump propulsion to players wearing Rocket Boots.
 */
public class RocketBootsAbility extends AbstractFullSetBonus implements Listener {

    /**
     * Constructs a Rocket Boots piece bonus requiring 1 piece.
     */
    public RocketBootsAbility() {
        super(1);
    }

    @Override
    public String getName() {
        return "Propulsion Burst";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Double jump in the air to launch",
                "yourself forward and take flight.",
                "§8Consumes durability on use."
        };
    }

    @Override
    public BonusCategory getCategory() {
        return BonusCategory.ABILITY;
    }

    @Override
    public void startAbilityEffect() {
        if (getAbilityOwner() == null) return;
        Player player = getAbilityOwner().getPlayer();
        player.setAllowFlight(true);
    }

    @Override
    public void stopAbilityEffect() {
        if (getAbilityOwner() == null) return;
        Player player = getAbilityOwner().getPlayer();

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!isAbilityOwner(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (!player.isFlying()) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setVelocity(player.getLocation().getDirection().setY(0.5).multiply(1.25));

            applyDurabilityDamageToBoots(player);

            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.2, 0.05, 0.2, 0.001);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 60f, 0f);
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player landedPlayer = event.getPlayer();

        if (landedPlayer.getAllowFlight()) return;
        if (!isAbilityOwner(landedPlayer)) return;

        if (((Entity) landedPlayer).isOnGround()) {
            landedPlayer.setAllowFlight(true);
        }
    }

    private void applyDurabilityDamageToBoots(Player player) {
        int damage = 1;

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !boots.hasItemMeta()) return;

        ItemMeta meta = boots.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int unbreakingLevel = boots.getEnchantmentLevel(Enchantment.UNBREAKING);

        int appliedDamage = 0;
        for (int i = 0; i < damage; i++) {
            if (unbreakingLevel > 0) {
                double skipChance = (double) unbreakingLevel / (unbreakingLevel + 1);
                if (ThreadLocalRandom.current().nextDouble() < skipChance) {
                    continue;
                }
            }
            appliedDamage++;
        }

        if (appliedDamage <= 0) return;

        int currentDamage = damageable.getDamage();
        int maxDurability = boots.getType().getMaxDurability();
        int durabilityDamage = currentDamage + appliedDamage;

        if (durabilityDamage >= maxDurability) {
            player.getInventory().setBoots(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            ArmorSetManager.updatePlayerArmor(getAbilityOwner());
        } else {
            damageable.setDamage(durabilityDamage);
            boots.setItemMeta(meta);
        }
    }
}
