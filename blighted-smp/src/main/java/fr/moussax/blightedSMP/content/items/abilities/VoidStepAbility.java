package fr.moussax.blightedSMP.content.items.abilities;

import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.bedrock.text.Messenger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class VoidStepAbility implements ItemAbility<PlayerInteractEvent> {
    private static final int MAX_DISTANCE = 40;

    @Override
    public String getName() {
        return "Voidstep";
    }

    @Override
    public AbilityTrigger getTrigger() {
        return AbilityTrigger.RIGHT_CLICK;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Teleport through the void to the ",
                "block you're looking at, up to §e" + MAX_DISTANCE,
                "blocks away."
        };
    }

    @Override
    public boolean triggerAbility(PlayerInteractEvent event) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(event.getPlayer());
        Location targetLocation = getTargetedEyeLocation(blightedPlayer);

        if (targetLocation == null) {
            Messenger.warn(blightedPlayer.getPlayer(), "Obstructed destination!");
            return false;
        }

        Location currentLook = blightedPlayer.getPlayer().getLocation();
        targetLocation.setYaw(currentLook.getYaw());
        targetLocation.setPitch(currentLook.getPitch());

        blightedPlayer.getPlayer().teleport(targetLocation);

        var world = blightedPlayer.getPlayer().getWorld();
        world.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 100f, 0.5f);
        world.spawnParticle(Particle.PORTAL, targetLocation, 40, 0.6, 1.0, 0.6, 0.15f);
        world.spawnParticle(Particle.WITCH, targetLocation, 20, 0.5, 0.5, 0.5, 0.05f);

        var isInSurvival = blightedPlayer.getPlayer().getGameMode() == GameMode.SURVIVAL;

        ItemStack usedItem = event.getItem();
        if ((usedItem != null && usedItem.getAmount() > 0) && isInSurvival) {
            usedItem.setAmount(usedItem.getAmount() - 1);
        }

        return true;
    }

    @Override
    public int getManaCost() {
        return 5;
    }

    private Location getTargetedEyeLocation(BlightedPlayer blightedPlayer) {
        var player = blightedPlayer.getPlayer();
        var eyeLocation = player.getEyeLocation();
        var direction = eyeLocation.getDirection();

        RayTraceResult traceResult = player.getWorld().rayTraceBlocks(
                eyeLocation, direction, MAX_DISTANCE, FluidCollisionMode.NEVER, true
        );

        if (traceResult != null && traceResult.getHitBlock() != null) {
            Block hitBlock = traceResult.getHitBlock();
            BlockFace hitFace = traceResult.getHitBlockFace();

            if (hitFace != null) {
                Block adjacentBlock = hitBlock.getRelative(hitFace);
                Location adjacentLocation = adjacentBlock.getLocation().add(0.5, 0.0, 0.5);

                if (isSafe(adjacentLocation, false)) {
                    return adjacentLocation;
                }
            }

            Location topOfBlock = hitBlock.getLocation().add(0.5, 1.0, 0.5);
            if (isSafe(topOfBlock, false)) {
                return topOfBlock;
            }

            return null;
        }

        Vector maxVec = direction.clone().normalize().multiply(MAX_DISTANCE);
        Location inAir = eyeLocation.clone().add(maxVec);

        if (isSafe(inAir, false)) return inAir;

        return null;
    }

    private boolean isSafe(Location location, boolean requireGround) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block below = feet.getRelative(BlockFace.DOWN);

        if (feet.getType().isSolid()) return false;
        if (head.getType().isSolid()) return false;

        return !requireGround || below.getType().isSolid();
    }
}
