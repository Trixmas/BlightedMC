package fr.moussax.blightedSMP.engine.items;

import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.lore.ItemLoreRenderer;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @BeforeAll
    static void setUpBukkitServer() {
        if (Bukkit.getServer() == null) {
            ItemMeta mockMeta = (ItemMeta) Proxy.newProxyInstance(
                    ItemMeta.class.getClassLoader(),
                    new Class<?>[]{ItemMeta.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "clone" -> proxy;
                        case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                        case "hashCode" -> 1;
                        default -> method.getReturnType().isPrimitive() ? 0 : null;
                    }
            );

            ItemFactory mockFactory = (ItemFactory) Proxy.newProxyInstance(
                    ItemFactory.class.getClassLoader(),
                    new Class<?>[]{ItemFactory.class},
                    (_, method, _) -> switch (method.getName()) {
                        case "getItemMeta" -> mockMeta;
                        case "isApplicable" -> true;
                        case "equals" -> true;
                        default -> null;
                    }
            );

            Server mockServer = (Server) Proxy.newProxyInstance(
                    Server.class.getClassLoader(),
                    new Class<?>[]{Server.class},
                    (_, method, _) -> switch (method.getName()) {
                        case "getItemFactory" -> mockFactory;
                        case "getLogger" -> java.util.logging.Logger.getGlobal();
                        default -> null;
                    }
            );

            Bukkit.setServer(mockServer);
        }
    }

    @Test
    @DisplayName("BlightedItem restrictions can be configured fluently and checked")
    void testItemRestrictions() {
        BlightedItem item = new BlightedItem("test_item", ItemType.MATERIAL, ItemRarity.COMMON, Material.STICK)
                .preventPlacement()
                .preventConsume()
                .preventProjectileLaunch()
                .preventBucketInteractions()
                .preventDrop()
                .preventInteraction();

        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_PLACEMENT));
        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_CONSUMPTION));
        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_PROJECTILE_LAUNCH));
        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_BUCKET_INTERACTIONS));
        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_DROP));
        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_INTERACTION));
    }

    @Test
    @DisplayName("ItemLoreRenderer renders canonical footer, description, and abilities")
    void testItemLoreRenderer() {
        BlightedItem item = new BlightedItem("test_sword", ItemType.SWORD, ItemRarity.LEGENDARY, Material.DIAMOND_SWORD)
                .description("A blade forged in ancient fires.", "Destroys enemies.")
                .addAbility(new ItemAbility<PlayerInteractEvent>() {
                    @Override
                    public String getName() {
                        return "Flaming Slash";
                    }

                    @Override
                    public AbilityTrigger getTrigger() {
                        return AbilityTrigger.RIGHT_CLICK;
                    }

                    @Override
                    public int getManaCost() {
                        return 30;
                    }

                    @Override
                    public int getCooldownSeconds() {
                        return 5;
                    }

                    @Override
                    public String[] getDescription() {
                        return new String[]{"Launches a wave of flames."};
                    }

                    @Override
                    public boolean triggerAbility(PlayerInteractEvent event) {
                        return true;
                    }
                });

        List<String> lore = ItemLoreRenderer.render(item);

        assertNotNull(lore);
        assertTrue(lore.stream().anyMatch(l -> l.contains("A blade forged in ancient fires.")));
        assertTrue(lore.stream().anyMatch(l -> l.contains("Ability: Flaming Slash")));
        assertTrue(lore.stream().anyMatch(l -> l.contains("RIGHT CLICK")));
        assertTrue(lore.stream().anyMatch(l -> l.contains("Mana Cost: §330")));
        assertTrue(lore.stream().anyMatch(l -> l.contains("Cooldown: §a5s")));
        assertTrue(lore.stream().anyMatch(l -> l.contains(ItemRarity.LEGENDARY.getName() + " SWORD")));
        long rarityLineCount = lore.stream().filter(l -> l.contains(ItemRarity.LEGENDARY.getName())).count();
        assertEquals(1, rarityLineCount, "Item lore should contain exactly one canonical rarity footer");
    }

    @Test
    @DisplayName("Thermal Fuel items render flush header without leading blank line")
    void testThermalFuelLoreRendering() {
        ItemRegistry.clear();
        new fr.moussax.blightedSMP.content.items.ThermalFuels().register(ItemRegistry::register);

        BlightedItem enchantedCoal = ItemRegistry.getOrThrow("ENCHANTED_COAL");
        List<String> lore = ItemLoreRenderer.render(enchantedCoal);

        assertNotNull(lore);
        assertFalse(lore.isEmpty());
        assertEquals("§8Thermal Fuel", lore.getFirst(), "First lore line must be the flush subtitle header without empty line above");
        assertEquals("", lore.get(1), "Second lore line should separate subtitle from body description");
        assertTrue(lore.getLast().contains(ItemRarity.UNCOMMON.getName()), "Last line must be canonical rarity footer");
        ItemRegistry.clear();
    }

    @Test
    @DisplayName("ItemRegistry safe get and getOrThrow contract")
    void testItemRegistryLookup() {
        ItemRegistry.clear();
        BlightedItem registered = new BlightedItem("custom_gem", ItemType.MATERIAL, ItemRarity.RARE, Material.EMERALD);
        ItemRegistry.register(registered);

        assertSame(registered, ItemRegistry.get("custom_gem"));
        assertSame(registered, ItemRegistry.getOrThrow("custom_gem"));

        assertNull(ItemRegistry.get("non_existent_item_id"));
        assertThrows(IllegalArgumentException.class, () -> ItemRegistry.getOrThrow("non_existent_item_id"));

        ItemRegistry.clear();
        assertNull(ItemRegistry.get("custom_gem"));
    }

    @Test
    @DisplayName("AbilityTrigger correctly identifies click actions and formatted display names")
    void testAbilityTriggerMatches() {
        assertEquals("§d§lRIGHT CLICK", AbilityTrigger.RIGHT_CLICK.getDisplayName());
        assertEquals("§d§lLEFT CLICK", AbilityTrigger.LEFT_CLICK.getDisplayName());
        assertEquals("§d§lSNEAK RIGHT CLICK", AbilityTrigger.SNEAK_RIGHT_CLICK.getDisplayName());
        assertEquals("§d§lSNEAK LEFT CLICK", AbilityTrigger.SNEAK_LEFT_CLICK.getDisplayName());
    }

    @Test
    @DisplayName("Tool abilities preserve block break/drop events while interactive abilities cancel them")
    void testAbilityEventCancellation() {
        fr.moussax.blightedSMP.content.items.abilities.tools.AutosmeltAbility autosmelt =
                new fr.moussax.blightedSMP.content.items.abilities.tools.AutosmeltAbility();
        fr.moussax.blightedSMP.content.items.abilities.tools.TimberAbility timber =
                new fr.moussax.blightedSMP.content.items.abilities.tools.TimberAbility();
        fr.moussax.blightedSMP.content.items.abilities.tools.VeinmineAbility veinmine =
                new fr.moussax.blightedSMP.content.items.abilities.tools.VeinmineAbility();
        fr.moussax.blightedSMP.content.items.abilities.tools.HammerAbility hammer =
                new fr.moussax.blightedSMP.content.items.abilities.tools.HammerAbility();

        assertFalse(autosmelt.cancelEvent(false), "Autosmelt should not cancel unhandled drops");
        assertFalse(autosmelt.cancelEvent(true), "Autosmelt should not cancel handled drops");

        assertFalse(timber.cancelEvent(false), "Timber should not cancel unhandled block breaks");
        assertFalse(timber.cancelEvent(true), "Timber should not cancel handled block breaks");

        assertFalse(veinmine.cancelEvent(false), "Veinmine should not cancel unhandled block breaks");
        assertFalse(veinmine.cancelEvent(true), "Veinmine should not cancel handled block breaks");

        assertFalse(hammer.cancelEvent(false), "Hammer should not cancel unhandled block breaks");
        assertFalse(hammer.cancelEvent(true), "Hammer should not cancel handled block breaks");

        fr.moussax.blightedSMP.content.items.abilities.BonemerangAbility bonemerang =
                new fr.moussax.blightedSMP.content.items.abilities.BonemerangAbility();
        assertTrue(bonemerang.cancelEvent(false), "Interactive abilities cancel on failure by default");
        assertTrue(bonemerang.cancelEvent(true), "Interactive abilities cancel on success by default");
    }
}
