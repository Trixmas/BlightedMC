package fr.moussax.blightedSMP.commands.impl;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.blightedSMP.commands.AdminCommand;
import fr.moussax.bedrock.commands.CommandArgument;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.blightedSMP.engine.player.hud.PlayerHudManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.time.Duration;

import static fr.moussax.bedrock.text.Messenger.inform;
import static fr.moussax.bedrock.text.Messenger.warn;

@CommandArgument(position = 0, suggestions = {"add", "remove", "set", "reset", "resetall", "giveall", "help"})
@CommandArgument(position = 1, path = {"add|remove|set|reset"}, suggestions = {"$players"})
public final class BlightCommand extends AdminCommand {

    @Override
    protected boolean executeAdmin(Player player, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMenu(player);
            return false;
        }

        return switch (args[0].toLowerCase()) {
            case "add" -> handleModify(player, args, true);
            case "remove" -> handleModify(player, args, false);
            case "set" -> handleSet(player, args);
            case "reset" -> handleReset(player, args);
            case "resetall" -> handleResetAll(player);
            case "giveall" -> handleGiveAll(player, args);
            default -> {
                warn(player, "Unknown §4blight §csubcommand.");
                yield false;
            }
        };
    }

    private void sendHelpMenu(Player player) {
        player.sendMessage(" ");
        player.sendMessage(" ");
        player.sendMessage("    §3§lBLIGHT CURRENCY§f | §7Subcommands");
        player.sendMessage(" ");
        player.sendMessage("§f  • " + formatSyntax("blight add <player> <amount>") + " §f§l» §7Give Blight to a player.");
        player.sendMessage("§f  • " + formatSyntax("blight remove <player> <amount>") + " §f§l» §7Take Blight from a player.");
        player.sendMessage("§f  • " + formatSyntax("blight set <player> <amount>") + " §f§l» §7Set Blight for a player.");
        player.sendMessage("§f  • " + formatSyntax("blight reset <player>") + " §f§l» §7Reset Blight for a player.");
        player.sendMessage("§f  • " + formatSyntax("blight giveall <amount>") + " §f§l» §7Give Blight to everyone.");
        player.sendMessage("§f  • " + formatSyntax("blight resetall") + " §f§l» §7Reset everyone's balance.");
        player.sendMessage("§f  • " + formatSyntax("blight help") + " §f§l» §7Prints this help message.");
        player.sendMessage(" ");
    }

    private String formatSyntax(String syntax) {
        StringBuilder builder = new StringBuilder("§e/");
        String[] arguments = syntax.strip().split("\\s+");

        for (int index = 0; index < arguments.length; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(formatToken(arguments[index]));
        }
        return builder.toString();
    }

    private String formatToken(String token) {
        if (token.length() > 1 && token.charAt(0) == '<' && token.charAt(token.length() - 1) == '>') {
            return "§e<§f" + token.substring(1, token.length() - 1) + "§e>";
        }
        if (token.length() > 1 && token.charAt(0) == '[' && token.charAt(token.length() - 1) == ']') {
            return "§e[§f" + token.substring(1, token.length() - 1) + "§e]";
        }
        return "§f" + token;
    }

    private boolean handleModify(Player sender, String[] args, boolean add) {
        if (args.length < 3) {
            warn(sender, "Usage: /blight " + (add ? "add" : "remove") + " <player> <amount>");
            return false;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) return false;

        Integer amount = parseAmount(sender, args[2]);
        if (amount == null) return false;

        BlightedPlayer blightedPlayer = BlightedPlayer.get(target);
        if (blightedPlayer == null) return false;

        if (add) {
            blightedPlayer.addBlight(amount);
            inform(sender, " §eGave §3" + amount + "❖ Blight §eto §5" + target.getName() + "§e.");
            Actionbar.sendSlotAlert(
                    target,
                    PlayerHudManager.SECTION_BLIGHT,
                    "§3" + Formatter.formatDecimalWithCommas(blightedPlayer.getBlight()) + "❖ Blight §b(+" + Formatter.formatDecimalWithCommas(amount) + ")",
                    Duration.ofSeconds(2)
            );
        } else {
            blightedPlayer.removeBlight(amount);
            inform(sender, " §eRemoved §3" + amount + "❖ Blight §efrom §5" + target.getName() + "§e.");
            Actionbar.sendSlotAlert(
                    target,
                    PlayerHudManager.SECTION_BLIGHT,
                    "§3" + Formatter.formatDecimalWithCommas(blightedPlayer.getBlight()) + "❖ Blight §c(-" + Formatter.formatDecimalWithCommas(amount) + ")",
                    Duration.ofSeconds(2)
            );
        }
        return true;
    }

    private boolean handleSet(Player sender, String[] args) {
        if (args.length < 3) {
            warn(sender, "Usage: /blight set <player> <amount>");
            return false;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) return false;

        Integer amount = parseAmount(sender, args[2]);
        if (amount == null) return false;

        BlightedPlayer targetPlayer = BlightedPlayer.get(target);
        if (targetPlayer == null) return false;

        targetPlayer.setBlight(amount);
        inform(sender, "§e Set §d" + target.getName() + "§e's Blight balance to §3" + amount + "❖§e.");
        Actionbar.sendSlotAlert(
                target,
                PlayerHudManager.SECTION_BLIGHT,
                "§3" + Formatter.formatDecimalWithCommas(amount) + "❖ Blight",
                Duration.ofSeconds(2)
        );
        return true;
    }

    private boolean handleReset(Player sender, String[] args) {
        if (args.length < 2) {
            warn(sender, "Usage: /blight reset <player>");
            return false;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return false;
        }

        BlightedPlayer targetPlayer = BlightedPlayer.get(target);
        if (targetPlayer == null) return false;

        targetPlayer.setBlight(0);
        inform(sender, "§e You reset §d" + target.getName() + "§e's Blight.");
        Actionbar.sendSlotAlert(
                target,
                PlayerHudManager.SECTION_BLIGHT,
                "§30❖ Blight §c(Reset)",
                Duration.ofSeconds(2)
        );
        return true;
    }

    private boolean handleResetAll(Player sender) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            BlightedPlayer bp = BlightedPlayer.get(player);
            if (bp != null) {
                bp.setBlight(0);
                Actionbar.sendSlotAlert(
                        player,
                        PlayerHudManager.SECTION_BLIGHT,
                        "§30❖ Blight §c(Reset)",
                        Duration.ofSeconds(2)
                );
            }
        });

        inform(sender, "Reset all §donline §7players' Blight.");
        return true;
    }

    private boolean handleGiveAll(Player sender, String[] args) {
        if (args.length < 2) {
            warn(sender, "Usage: /blight giveall <amount>");
            return false;
        }

        Integer amount = parseAmount(sender, args[1]);
        if (amount == null) return false;

        Bukkit.getOnlinePlayers().forEach(player -> {
            BlightedPlayer bp = BlightedPlayer.get(player);
            if (bp != null) {
                bp.addBlight(amount);
                Actionbar.sendSlotAlert(
                        player,
                        PlayerHudManager.SECTION_BLIGHT,
                        "§3" + Formatter.formatDecimalWithCommas(bp.getBlight()) + "❖ Blight §b(+" + Formatter.formatDecimalWithCommas(amount) + ")",
                        Duration.ofSeconds(2)
                );
            }
        });

        inform(sender, " §eYou gave all players §3" + amount + "❖ Blight§e.");
        return true;
    }

    private Integer parseAmount(Player sender, String value) {
        try {
            int amount = Integer.parseInt(value);
            if (amount < 0) {
                warn(sender, "Amount must be a positive number.");
                return null;
            }
            return amount;
        } catch (NumberFormatException exception) {
            warn(sender, "Amount must be a positive number.");
            return null;
        }
    }
}
