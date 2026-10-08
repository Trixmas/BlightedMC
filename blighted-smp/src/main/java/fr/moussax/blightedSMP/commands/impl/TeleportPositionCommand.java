package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.commands.CommandArgument;
import fr.moussax.blightedSMP.commands.AdminCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import static fr.moussax.bedrock.text.Messenger.inform;
import static fr.moussax.bedrock.text.Messenger.warn;

@CommandArgument(position = 0, suggestions = {"$players"})
@CommandArgument(position = 3, suggestions = {"OVERWORLD", "NETHER", "THE_END"})
@CommandArgument(position = 4, suggestions = {"OVERWORLD", "NETHER", "THE_END"})
public final class TeleportPositionCommand extends AdminCommand {

    @Override
    protected boolean executeAdmin(Player player, Command command, String label, String[] args) {

        if (args.length < 3) {
            warn(player, "Usage: /tppos [player] <x> <y> <z> [world]");
            return true;
        }

        try {
            Player target = player;
            int coordIndex = 0;

            Player potentialTarget = Bukkit.getPlayerExact(args[0]);
            if (potentialTarget != null) {
                target = potentialTarget;
                coordIndex = 1;
            }

            if (args.length < coordIndex + 3) {
                warn(player, "Missing arguments! Please provide coordinates (x y z).");
                return true;
            }

            double x = Double.parseDouble(args[coordIndex]);
            double y = Double.parseDouble(args[coordIndex + 1]);
            double z = Double.parseDouble(args[coordIndex + 2]);

            World world = target.getWorld();
            if (args.length > coordIndex + 3) {
                String worldName = args[coordIndex + 3];
                world = parseWorld(worldName);
                if (world == null) {
                    warn(player, "Invalid world: " + worldName + ". Use OVERWORLD, NETHER, or THE_END.");
                    return false;
                }
            }

            Location location = new Location(world, x, y, z);
            target.teleport(location);

            String displayWorld = getWorldDisplayName(world);
            String coordinates = String.format("§d%.0f, %.0f, %.0f §ein §f%s§e.", x, y, z, displayWorld);

            if (target.equals(player)) {
                inform(player, " §eTeleported to " + coordinates);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
            } else {
                inform(player, " §eTeleported §f" + target.getName() + " §eto " + coordinates);
                inform(target, " §eTeleported to " + coordinates);
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
            }
        } catch (NumberFormatException _) {
            warn(player, "Invalid coordinates. Please provide valid numbers.");
            return false;
        }

        return true;
    }

    private World parseWorld(String name) {
        return switch (name.toUpperCase()) {
            case "NETHER" -> Bukkit.getWorld("world_nether");
            case "THE_END" -> Bukkit.getWorld("world_the_end");
            case "OVERWORLD" -> Bukkit.getWorld("world");
            default -> Bukkit.getWorld(name);
        };
    }

    private String getWorldDisplayName(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "The End";
            default -> "Overworld";
        };
    }
}
