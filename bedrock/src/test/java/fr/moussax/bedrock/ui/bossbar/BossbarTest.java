package fr.moussax.bedrock.ui.bossbar;

import fr.moussax.bedrock.sound.SoundCue;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class BossbarTest {

    private Player createMockPlayer(UUID uuid, World world, Location location) {
        return createMockPlayer(uuid, world, location, new AtomicInteger());
    }

    private Player createMockPlayer(UUID uuid, World world, Location location, AtomicInteger soundCount) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getWorld" -> world;
                    case "getLocation" -> location;
                    case "isOnline" -> true;
                    case "isValid" -> true;
                    case "playSound" -> {
                        soundCount.incrementAndGet();
                        yield null;
                    }
                    default -> null;
                }
        );
    }

    private Entity createMockEntity(World world, Location location, boolean valid, boolean dead) {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getLocation" -> location;
                    case "isValid" -> valid;
                    case "isDead" -> dead;
                    default -> null;
                }
        );
    }

    private World createMockWorld(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "equals" -> args != null && args.length > 0 && args[0] == proxy;
                    default -> null;
                }
        );
    }

    @Test
    @DisplayName("BossbarAlert maintains immutability and with* methods create distinct copies")
    void testBossbarAlertImmutability() {
        BossbarAlert alert = BossbarAlert.of("Alert 1", BarColor.WHITE, 5, Duration.ofSeconds(3));
        BossbarAlert withRed = alert.withColor(BarColor.RED);
        BossbarAlert withPriority = alert.withPriority(20);
        BossbarAlert exclusive = alert.exclusive(true);
        BossbarAlert withSound = alert.withSound(new SoundCue(null, 1f, 1f, 0L));

        assertEquals(BarColor.WHITE, alert.color());
        assertEquals(5, alert.priority());
        assertFalse(alert.exclusive());
        assertNull(alert.soundCue());

        assertEquals(BarColor.RED, withRed.color());
        assertEquals(20, withPriority.priority());
        assertTrue(exclusive.exclusive());
        assertNotNull(withSound.soundCue());
    }

    @Test
    @DisplayName("ActiveAlert session calculates progress and expiration independently of alert instantiation")
    void testActiveAlertSessionLifecycle() throws InterruptedException {
        BossbarAlert constantAlert = BossbarAlert.of("Constant Alert", Duration.ofMillis(200));

        // Sleep to ensure instantiation time is in the past
        Thread.sleep(50);

        // When a session is created from the constant alert, its timer begins now
        BossbarComposer.ActiveAlert session = new BossbarComposer.ActiveAlert(constantAlert);
        assertFalse(session.isExpired(), "Session should not be expired immediately upon creation");
        assertTrue(session.progress(null) > 0.0, "Progress should be positive");

        Thread.sleep(220);
        assertTrue(session.isExpired(), "Session should be expired after its duration elapses");
        assertEquals(0.0, session.progress(null), "Progress should be 0 on expiry");
    }

    @Test
    @DisplayName("ActiveAlert plays sound cue only once per player across multiple render passes")
    void testActiveAlertAudioDeduplication() {
        AtomicInteger playCount = new AtomicInteger(0);
        World world = createMockWorld("world");
        Player player = createMockPlayer(UUID.randomUUID(), world, new Location(world, 0, 0, 0), playCount);

        SoundCue cue = new SoundCue(null, 1.0f, 1.0f, 0L);
        BossbarAlert alert = BossbarAlert.of("Sound Alert", Duration.ofSeconds(5), cue);
        BossbarComposer.ActiveAlert session = new BossbarComposer.ActiveAlert(alert);

        session.playSoundIfNeeded(player);
        session.playSoundIfNeeded(player);
        session.playSoundIfNeeded(player);

        assertEquals(1, playCount.get(), "Sound cue must only play once per player session");
    }

    @Test
    @DisplayName("Countdown alert formats decrementing remaining seconds correctly in active session")
    void testCountdownAlertSession() {
        BossbarAlert alert = BossbarAlert.countdown("Starting in %ds", 10, 5);
        BossbarComposer.ActiveAlert session = new BossbarComposer.ActiveAlert(alert);

        String title = session.title(null);
        assertNotNull(title);
        assertTrue(title.contains("5s") || title.contains("4s"));
    }

    @Test
    @DisplayName("BossbarSection.Builder supports static progress, double suppliers, and single flags")
    void testBossbarSectionBuilderOverloads() {
        BossbarSection section = BossbarSection.builder("test-section")
                .title("Section Title")
                .color(BarColor.BLUE)
                .style(BarStyle.SEGMENTED_6)
                .progress(0.75)
                .flag(BarFlag.DARKEN_SKY)
                .build();

        assertEquals("test-section", section.id());
        assertEquals(BarStyle.SEGMENTED_6, section.style());
        assertEquals(0.75, section.progressSupplier().applyAsDouble(null), 0.001);
        assertTrue(section.flags().contains(BarFlag.DARKEN_SKY));

        BossbarSection dynamicProgressSection = BossbarSection.builder("dyn-section")
                .title("Dynamic")
                .progress(() -> 0.42)
                .build();
        assertEquals(0.42, dynamicProgressSection.progressSupplier().applyAsDouble(null), 0.001);
    }

    @Test
    @DisplayName("Bossbar.within(Entity, radius) validates world, distance, and entity alive status")
    void testBossbarWithinEntityPredicate() {
        World world = createMockWorld("world");
        World otherWorld = createMockWorld("nether");

        Location entityLoc = new Location(world, 100, 64, 100);
        Location nearbyPlayerLoc = new Location(world, 110, 64, 100); // dist 10
        Location farPlayerLoc = new Location(world, 200, 64, 100);   // dist 100
        Location diffWorldPlayerLoc = new Location(otherWorld, 110, 64, 100);

        Player nearPlayer = createMockPlayer(UUID.randomUUID(), world, nearbyPlayerLoc);
        Player farPlayer = createMockPlayer(UUID.randomUUID(), world, farPlayerLoc);
        Player diffWorldPlayer = createMockPlayer(UUID.randomUUID(), otherWorld, diffWorldPlayerLoc);

        Entity livingBoss = createMockEntity(world, entityLoc, true, false);
        Predicate<Player> within20 = Bossbar.within(livingBoss, 20.0);

        assertTrue(within20.test(nearPlayer), "Near player in same world must be visible");
        assertFalse(within20.test(farPlayer), "Far player must not be visible");
        assertFalse(within20.test(diffWorldPlayer), "Player in different world must not be visible");

        Entity deadBoss = createMockEntity(world, entityLoc, true, true);
        Predicate<Player> withinDead = Bossbar.within(deadBoss, 20.0);
        assertFalse(withinDead.test(nearPlayer), "Dead entity must hide boss bar from viewers");

        Entity invalidBoss = createMockEntity(world, entityLoc, false, false);
        Predicate<Player> withinInvalid = Bossbar.within(invalidBoss, 20.0);
        assertFalse(withinInvalid.test(nearPlayer), "Invalid entity must hide boss bar from viewers");
    }

    @Test
    @DisplayName("BossbarComposer idle fast path executes safely without initializing handle map")
    void testBossbarComposerIdleFastPath() {
        World world = createMockWorld("world");
        Player player = createMockPlayer(UUID.randomUUID(), world, new Location(world, 0, 0, 0));

        BossbarComposer composer = new BossbarComposer();
        // Should return immediately via fast-path without throwing or allocating
        assertDoesNotThrow(() -> composer.render(player));
    }
}
