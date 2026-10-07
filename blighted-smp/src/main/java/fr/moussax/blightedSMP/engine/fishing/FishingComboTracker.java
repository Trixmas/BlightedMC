package fr.moussax.blightedSMP.engine.fishing;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.bedrock.text.Messenger;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FishingComboTracker {

    private static final int MAXIMUM_COMBO = 20;
    private static final long EXPIRATION_DELAY_TICKS = 1800L;
    private static final Map<UUID, EnumMap<FishingMethod, Integer>> PLAYER_COMBOS = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> EXPIRATION_TASKS = new ConcurrentHashMap<>();

    private FishingComboTracker() {
    }

    public static int getCombo(@NonNull Player player, @NonNull FishingMethod method) {
        EnumMap<FishingMethod, Integer> methodMap = PLAYER_COMBOS.get(player.getUniqueId());
        if (methodMap == null) return 0;
        return methodMap.getOrDefault(method, 0);
    }

    public static double getSeaCreatureChanceBonus(int combo) {
        return Math.min(0.05, combo * 0.0025);
    }

    public static int getBonusExperience(int combo) {
        return Math.min(4, combo / 5) * 2;
    }

    public static void incrementCombo(@NonNull Player player, @NonNull FishingMethod method) {
        UUID playerId = player.getUniqueId();
        EnumMap<FishingMethod, Integer> methodMap = PLAYER_COMBOS.computeIfAbsent(
                playerId, key -> new EnumMap<>(FishingMethod.class)
        );

        int currentCombo = methodMap.getOrDefault(method, 0);
        int newCombo = Math.min(MAXIMUM_COMBO, currentCombo + 1);
        methodMap.put(method, newCombo);

        refreshExpirationTimer(player);
        if (newCombo > currentCombo) {
            handleMilestoneFeedback(player, newCombo);
        }
    }

    public static void resetCombo(Player player, FishingMethod method) {
        if (player == null || method == null) return;

        UUID playerUniqueId = player.getUniqueId();
        EnumMap<FishingMethod, Integer> methodMap = PLAYER_COMBOS.get(playerUniqueId);
        if (methodMap == null) return;

        methodMap.remove(method);
        if (methodMap.isEmpty()) {
            clear(player);
        }
    }

    public static void clear(Player player) {
        if (player == null) return;

        UUID playerUniqueId = player.getUniqueId();
        PLAYER_COMBOS.remove(playerUniqueId);

        BukkitTask expirationTask = EXPIRATION_TASKS.remove(playerUniqueId);
        if (expirationTask != null && !expirationTask.isCancelled()) {
            expirationTask.cancel();
        }
    }

    private static void refreshExpirationTimer(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        BukkitTask existingTask = EXPIRATION_TASKS.remove(playerUniqueId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                EnumMap<FishingMethod, Integer> methodMap = PLAYER_COMBOS.remove(playerUniqueId);
                EXPIRATION_TASKS.remove(playerUniqueId);

                if (methodMap == null || methodMap.isEmpty()) return;

                int highestCombo = methodMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                if (highestCombo >= 5 && player.isOnline()) {
                    Messenger.warn(player, String.format(Locale.ROOT, "§c🎣 Your combo has expired! You reached a %d Catch Combo!", highestCombo));
                }
            }
        }.runTaskLater(BlightedSMP.getInstance(), EXPIRATION_DELAY_TICKS);
        EXPIRATION_TASKS.put(playerUniqueId, newTask);
    }

    private static void handleMilestoneFeedback(Player player, int combo) {
        String milestoneMessage = switch (combo) {
            case 5 -> " §8§l+§f§l5 Catch Combo §b+1.25% \uD83D\uDC20 Sea Creature Chance";
            case 10 -> " §8§l+§e§l10 Catch Combo §b+2.5% \uD83D\uDC20 Sea Creature Chance";
            case 15 -> " §8§l+§6§l15 Catch Combo §b+3.75% \uD83D\uDC20 Sea Creature Chance";
            case 20 -> " §8§l+§5§l20 Catch Combo §b+5.0% \uD83D\uDC20 Sea Creature Chance";
            default -> null;
        };

        if (milestoneMessage == null) {
            return;
        }

        player.sendMessage(milestoneMessage);
        float pitch = Math.min(2.0f, 0.8f + (combo * 0.06f));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, pitch);
    }

    public static void spawnBonusExperience(World world, Location location, int combo) {
        if (world == null || location == null) return;
        int bonusExperience = getBonusExperience(combo);
        if (bonusExperience <= 0) return;

        ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(location, EntityType.EXPERIENCE_ORB);
        orb.setExperience(bonusExperience);
    }
}
