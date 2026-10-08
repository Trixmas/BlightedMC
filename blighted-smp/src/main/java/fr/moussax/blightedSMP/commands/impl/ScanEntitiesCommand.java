package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.text.InteractiveMessage;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.blightedSMP.commands.AdminCommand;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static fr.moussax.bedrock.text.Messenger.warn;

/**
 * Administrative scan utility that detects nearby active Blighted creatures,
 * displays their distance, health, and location, and provides a 1-click teleport action.
 */
public final class ScanEntitiesCommand extends AdminCommand {

    private static final double DEFAULT_SCAN_RADIUS = 150.0;
    private static final double MAX_SCAN_RADIUS = 1000.0;

    @Override
    protected boolean executeAdmin(Player player, Command command, String label, String[] args) {
        double radius = DEFAULT_SCAN_RADIUS;

        if (args.length > 0) {
            try {
                radius = Double.parseDouble(args[0]);
            } catch (NumberFormatException _) {
                warn(player, "Usage: /scan [radius]");
                return true;
            }

            if (radius <= 0) {
                warn(player, "Radius must be greater than 0.");
                return true;
            }

            if (radius > MAX_SCAN_RADIUS) {
                radius = MAX_SCAN_RADIUS;
            }
        }

        Location playerLoc = player.getLocation();
        double radiusSquared = radius * radius;

        record DetectedEntity(BlightedEntity blighted, double distance, Location targetLocation) {
        }

        List<DetectedEntity> detected = new ArrayList<>();

        for (BlightedEntity blighted : EntityManager.getActiveEntities()) {
            if (!blighted.isAlive()) continue;

            LivingEntity living = blighted.getEntity();
            if (living == null || !living.getWorld().equals(player.getWorld())) continue;

            double distanceSquared = living.getLocation().distanceSquared(playerLoc);
            if (distanceSquared <= radiusSquared) {
                detected.add(new DetectedEntity(blighted, Math.sqrt(distanceSquared), living.getLocation()));
            }
        }

        detected.sort(Comparator.comparingDouble(DetectedEntity::distance));

        if (detected.isEmpty()) {
            Messenger.warn(player, "No Blighted creatures found within §e" + Math.round(radius) + " §cblocks.");
            return true;
        }

        player.sendMessage("", "");
        Messenger.inform(player, " §c§lDEBUG! §eFound §c" + detected.size() + " §ecreatures within §b" + Math.round(radius) + "m§e. ");
        player.sendMessage("");

        for (DetectedEntity entry : detected) {
            BlightedEntity blightedEntity = entry.blighted();
            Location location = entry.targetLocation();
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            long distance = Math.round(entry.distance());

            String tpCommand = "/tppos " + x + " " + y + " " + z;

            InteractiveMessage.text(" §7∙ §f" + blightedEntity.getName() + " §eat §d" + x + " " + y + " " + z + " §7(" + distance + "m) §f§l» ")
                    .hoverAndExecute(
                            "§b[TP]",
                            "§fClick to teleport to §d" + blightedEntity.getName(),
                            tpCommand
                    )
                    .send(player);
        }

        Messenger.inform(player, "\n\n ");

        return true;
    }
}
