package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.commands.CommandArgument;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.commands.AdminCommand;
import fr.moussax.blightedSMP.engine.player.cinematic.FirstJoinCinematic;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Administrative testing command providing test suites and debug routines
 * such as the Blight Infusion cinematic.
 */
@CommandArgument(position = 0, suggestions = {"cinematic", "lore"})
@CommandArgument(position = 1, path = {"cinematic"}, suggestions = {"$players"})
public final class TestCommand extends AdminCommand {

    @Override
    protected boolean executeAdmin(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            Messenger.warn(player, "Usage: /test <cinematic|lore> [player]");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        return switch (action) {
            case "cinematic" -> handleCinematic(player, args);
            case "lore" -> handleLore(player);
            default -> {
                Messenger.warn(player, "Unknown test action '%s'. Usage: /test <cinematic|lore> [player]".formatted(args[0]));
                yield true;
            }
        };
    }

    private boolean handleCinematic(Player player, String[] args) {
        Player target = player;
        if (args.length > 1) {
            target = requireTarget(player, args[1]);
            if (target == null) {
                return true;
            }
        }

        if (FirstJoinCinematic.isCinematicActive(target)) {
            Messenger.warn(player, "The cinematic is already playing for " + (target.equals(player) ? "you." : target.getName() + "."));
            return true;
        }

        if (target.equals(player)) {
            Messenger.inform(player, "§7Starting the Blight Infusion cinematic test...");
        } else {
            Messenger.inform(player, "§7Starting the Blight Infusion cinematic test for §f" + target.getName() + "§7...");
        }

        new FirstJoinCinematic(BlightedSMP.getInstance(), target).start();
        return true;
    }

    private boolean handleLore(Player player) {
        player.sendMessage("");
        player.sendMessage("");
        player.sendMessage(" §7Cold silk weighs down your brow, blinding the edges of your vision. In your left hand, an ancient spine of parchment rests motionless. §3The Blight didn't kill you. It claimed you.");
        player.sendMessage("");
        player.sendMessage("");
        return true;
    }
}

