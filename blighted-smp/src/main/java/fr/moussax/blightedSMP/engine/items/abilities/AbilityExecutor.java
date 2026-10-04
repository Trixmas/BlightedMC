package fr.moussax.blightedSMP.engine.items.abilities;

import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.bedrock.utils.debug.Log;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.blightedSMP.engine.player.hud.PlayerHudManager;
import org.bukkit.Sound;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.time.Duration;

import static fr.moussax.bedrock.text.Messenger.warn;

/**
 * Evaluates requirements, resource costs, and active cooldowns to execute item abilities.
 */
public final class AbilityExecutor {

    private AbilityExecutor() {
    }

    /**
     * Evaluates active cooldowns, verifies mana sufficiency, and executes an ability.
     *
     * @param <T>     event type
     * @param ability ability to execute
     * @param player  player context triggering the ability
     * @param event   triggering Bukkit event
     */
    public static <T extends Event> void execute(ItemAbility<T> ability, BlightedPlayer player, T event) {
        String cooldownKey = ability.getName() != null && !ability.getName().isBlank()
                ? ability.getName()
                : ability.getClass().getName();

        double remaining = player.getRemainingCooldown(cooldownKey, ability.getTrigger());
        if (remaining > 0) {
            warn(player.getPlayer(), "§c⌚ Your §f" + ability.getName() + " §cability is on cooldown for §d" + (int) Math.ceil(remaining) + "s§c!");
            cancel(event);
            return;
        }

        if (!ability.canTrigger(player)) {
            cancel(event);
            return;
        }

        int manaCost = ability.getManaCost();
        if (!player.hasMana(manaCost)) {
            player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 100f, 0.5f);
            Actionbar.sendSlotAlert(player.getPlayer(), PlayerHudManager.SECTION_MANA, "§c§lNOT ENOUGH MANA", Duration.ofSeconds(2));
            cancel(event);
            return;
        }

        try {
            boolean success = ability.triggerAbility(event);
            if (!success) {
                if (ability.cancelEvent(false)) cancel(event);
                return;
            }

            if (ability.cancelEvent(true)) cancel(event);

            if (manaCost > 0) {
                player.consumeMana(manaCost);
                Actionbar.update(player.getPlayer());
            }

            ability.start(player);

            if (ability.getCooldownSeconds() > 0) {
                player.setCooldown(cooldownKey, ability.getTrigger(), ability.getCooldownSeconds());
            }
        } catch (Exception exception) {
            Log.error("AbilityExecutor", "Ability execution failed: " + exception.getClass().getSimpleName());
            cancel(event);
        }
    }

    private static void cancel(Event event) {
        if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
    }
}
