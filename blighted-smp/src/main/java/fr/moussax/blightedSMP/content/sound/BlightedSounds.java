package fr.moussax.blightedSMP.content.sound;

import fr.moussax.bedrock.sound.SoundCue;
import fr.moussax.bedrock.sound.SoundSequence;
import org.bukkit.Sound;

public final class BlightedSounds {

    private BlightedSounds() {
    }

    public static final SoundSequence SLAYER_MOB_SPAWN = new SoundSequence(
            new SoundCue(Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 2.0f, 1L),
            new SoundCue(Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.8f, 4L),
            new SoundCue(Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.5f, 1.5f, 7L),
            new SoundCue(Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.5f, 1.3f, 10L),
            new SoundCue(Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.5f, 1.1f, 13L),
            new SoundCue(Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.5f, 0.9f, 16L),
            new SoundCue(Sound.BLOCK_HEAVY_CORE_PLACE, 2.0f, 0.8f, 19L),
            new SoundCue(Sound.BLOCK_HEAVY_CORE_PLACE, 2.0f, 0.6f, 22L),
            new SoundCue(Sound.BLOCK_HEAVY_CORE_PLACE, 2.0f, 0.5f, 25L),
            new SoundCue(Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 1.0f, 28L),
            new SoundCue(Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f, 28L),
            new SoundCue(Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.2f, 28L)
    );

    public static final SoundSequence ANCIENT_MOB_SPAWN = new SoundSequence(
            new SoundCue(Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.0f, 0.6f, 0L),
            new SoundCue(Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.5f, 0L),
            new SoundCue(Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.7f, 20L),
            new SoundCue(Sound.ENTITY_BREEZE_INHALE, 1.0f, 0.5f, 40L),
            new SoundCue(Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f, 45L),
            new SoundCue(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f, 60L),
            new SoundCue(Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 1.0f, 60L),
            new SoundCue(Sound.ITEM_TRIDENT_THUNDER, 1.0f, 0.7f, 60L)
    );

    public static final SoundSequence ANCIENT_MOB_DEFEAT = new SoundSequence(
            new SoundCue(Sound.ENTITY_WARDEN_DEATH, 1.0f, 0.7f, 0L),
            new SoundCue(Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.8f, 0L),
            new SoundCue(Sound.ITEM_TRIDENT_THUNDER, 1.5f, 0.7f, 0L),
            new SoundCue(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f, 0L),
            new SoundCue(Sound.BLOCK_TRIAL_SPAWNER_BREAK, 1.5f, 0.6f, 20L),
            new SoundCue(Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f, 20L),
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.8f, 40L),
            new SoundCue(Sound.PARTICLE_SOUL_ESCAPE, 2.0f, 0.6f, 45L),
            new SoundCue(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f, 60L),
            new SoundCue(Sound.BLOCK_VAULT_ACTIVATE, 1.0f, 1.2f, 60L),
            new SoundCue(Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f, 60L),
            new SoundCue(Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1.0f, 60L)
    );

    public static final SoundSequence ANCIENT_MOB_COLLAPSE = new SoundSequence(
            new SoundCue(Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.5f, 0L),
            new SoundCue(Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f, 0L),
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 0.6f, 0L),
            new SoundCue(Sound.BLOCK_END_PORTAL_SPAWN, 1.2f, 0.5f, 15L),
            new SoundCue(Sound.BLOCK_GLASS_BREAK, 1.8f, 0.5f, 15L),
            new SoundCue(Sound.ENTITY_BREEZE_INHALE, 1.5f, 0.4f, 20L),
            new SoundCue(Sound.ENTITY_ENDERMAN_TELEPORT, 1.8f, 0.5f, 35L),
            new SoundCue(Sound.PARTICLE_SOUL_ESCAPE, 2.0f, 0.5f, 35L),
            new SoundCue(Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.2f, 0.5f, 40L)
    );

    public static final SoundSequence XP_PICKUP = new SoundSequence(
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f, 0L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.6f, 2L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.7f, 4L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f, 6L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.9f, 8L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.0f, 10L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f, 12L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f, 14L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f, 16L),
            new SoundCue(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f, 18L)
    );

    public static final SoundSequence RESONANT_BLIGHTSTONE_CONSUME = new SoundSequence(
            new SoundCue(Sound.BLOCK_POINTED_DRIPSTONE_HIT, 1.0f, 0.6f, 0L),
            new SoundCue(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f, 0L),
            new SoundCue(Sound.BLOCK_POINTED_DRIPSTONE_HIT, 1.0f, 0.9f, 3L),
            new SoundCue(Sound.BLOCK_POINTED_DRIPSTONE_HIT, 1.0f, 1.3f, 6L),
            new SoundCue(Sound.BLOCK_POINTED_DRIPSTONE_HIT, 1.0f, 1.7f, 8L),
            new SoundCue(Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.9f, 1.3f, 8L),
            new SoundCue(Sound.PARTICLE_SOUL_ESCAPE, 0.9f, 1.1f, 9L),
            new SoundCue(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 1.4f, 13L),
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.7f, 16L),
            new SoundCue(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1.3f, 18L),
            new SoundCue(Sound.BLOCK_VAULT_OPEN_SHUTTER, 0.8f, 1.2f, 18L),
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 1.8f, 20L)
    );

    public static final SoundSequence FORGE_ITEM = new SoundSequence(
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.50f, 0L),
            new SoundCue(Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.9f, 0.50f, 2L),
            new SoundCue(Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.0f, 0.50f, 4L),
            new SoundCue(Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.50f, 6L),
            new SoundCue(Sound.ENTITY_WARDEN_ROAR, 0.6f, 0.50f, 6L),
            new SoundCue(Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.2f, 0.50f, 8L),
            new SoundCue(Sound.BLOCK_HEAVY_CORE_PLACE, 1.2f, 0.50f, 10L),
            new SoundCue(Sound.PARTICLE_SOUL_ESCAPE, 0.9f, 0.50f, 10L),
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.50f, 12L),
            new SoundCue(Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 0.50f, 14L),
            new SoundCue(Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.3f, 0.50f, 16L),
            new SoundCue(Sound.BLOCK_VAULT_ACTIVATE, 1.1f, 0.50f, 18L),
            new SoundCue(Sound.BLOCK_ANVIL_LAND, 0.9f, 0.50f, 20L)
    );
}
