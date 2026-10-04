package fr.moussax.bedrock.sound;

import fr.moussax.bedrock.scheduling.PluginContext;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Represents a configurable sound effect in BlightedMC.
 *
 * <p>Each sound has a type, volume, pitch, and optional delay before playing.
 * Provides a method to play the sound at a specific {@link Location} using
 * {@link PluginContext#delay(Runnable, long)}.</p>
 *
 * @param sound  the Bukkit {@link org.bukkit.Sound} type
 * @param volume the volume of the sound
 * @param pitch  the pitch of the sound
 * @param delay  the delay in ticks before the sound is played
 */
public record SoundCue(Sound sound, float volume, float pitch, long delay) {

    /**
     * Creates an immediate sound cue without delay.
     *
     * @param sound  Bukkit sound type
     * @param volume sound volume
     * @param pitch  sound pitch
     * @return sound cue
     */
    @NonNull
    public static SoundCue of(@NonNull Sound sound, float volume, float pitch) {
        Objects.requireNonNull(sound, "sound cannot be null");
        return new SoundCue(sound, volume, pitch, 0L);
    }

    /**
     * Creates a sound cue with a configured delay in ticks.
     *
     * @param sound  Bukkit sound type
     * @param volume sound volume
     * @param pitch  sound pitch
     * @param delay  delay in ticks
     * @return sound cue
     */
    @NonNull
    public static SoundCue of(@NonNull Sound sound, float volume, float pitch, long delay) {
        Objects.requireNonNull(sound, "sound cannot be null");
        return new SoundCue(sound, volume, pitch, delay);
    }

    /**
     * Plays this sound at the specified location after the configured delay.
     *
     * <p>If {@code delay <= 0}, plays immediately on the current thread.</p>
     *
     * @param location the location where the sound should be played
     */
    public void play(@NonNull Location location) {
        if (delay <= 0) {
            Objects.requireNonNull(location.getWorld()).playSound(location, sound, volume, pitch);
            return;
        }
        PluginContext.delay(() -> Objects.requireNonNull(location.getWorld())
            .playSound(location, sound, volume, pitch), delay);
    }

    /**
     * Plays this sound directly to the specified player after the configured delay.
     *
     * <p>If {@code delay <= 0}, plays immediately on the current thread.</p>
     *
     * @param player the player who should hear the sound
     */
    public void play(@NonNull Player player) {
        if (delay <= 0) {
            player.playSound(player.getLocation(), sound, volume, pitch);
            return;
        }
        PluginContext.delay(() -> player.playSound(player.getLocation(), sound, volume, pitch), delay);
    }
}


