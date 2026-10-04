package fr.moussax.blightedSMP.engine.items.abilities;

/**
 * Tracks an active cooldown for a specific ability execution key and trigger.
 *
 * @param key                  unique identifier owning this cooldown
 * @param trigger              ability trigger under cooldown
 * @param expirationTimeMillis epoch timestamp in milliseconds when the cooldown expires
 */
public record CooldownEntry(String key, AbilityTrigger trigger, long expirationTimeMillis) {

    /**
     * Checks whether the cooldown period has elapsed.
     *
     * @return {@code true} if the current time has reached or passed the expiration timestamp, {@code false} otherwise
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTimeMillis;
    }

    /**
     * Calculates the remaining cooldown duration in seconds.
     *
     * @return remaining duration in seconds, or {@code 0.0} if expired
     */
    public double getRemainingCooldownTimeInSeconds() {
        long remainingMillis = expirationTimeMillis - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000.0);
    }
}
