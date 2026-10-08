package fr.moussax.blightedSMP.engine.items;

import fr.moussax.blightedSMP.content.materials.ThermalFuels;
import fr.moussax.blightedSMP.content.equipment.abilities.BonemerangAbility;
import fr.moussax.blightedSMP.content.equipment.abilities.tools.AutosmeltAbility;
import fr.moussax.blightedSMP.content.equipment.abilities.tools.HammerAbility;
import fr.moussax.blightedSMP.content.equipment.abilities.tools.TimberAbility;
import fr.moussax.blightedSMP.content.equipment.abilities.tools.VeinmineAbility;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.lore.ItemLoreRenderer;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.engine.loot.results.blightstone.ResonantBlightstoneItem;
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
    @DisplayName("BlightedItem soulbound configuration sets soulbound flag and drop prevention")
    void testSoulboundItem() {
        BlightedItem item = new BlightedItem("soulbound_relic", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.BLACK_BANNER)
                .soulbound();

        assertTrue(item.isSoulbound());
        assertTrue(item.hasRestriction(ItemRestriction.PREVENT_DROP));

        item.soulbound(false);
        assertFalse(item.isSoulbound());
        assertFalse(item.hasRestriction(ItemRestriction.PREVENT_DROP));
    }

    @Test
    @DisplayName("ItemLoreRenderer renders canonical footer, description, and abilities")
    void testItemLoreRenderer() {
        BlightedItem item = new BlightedItem("test_sword", ItemType.SWORD, ItemRarity.UNIQUE, Material.DIAMOND_SWORD)
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
        assertTrue(lore.stream().anyMatch(l -> l.contains(ItemRarity.UNIQUE.getName() + " SWORD")));
        long rarityLineCount = lore.stream().filter(l -> l.contains(ItemRarity.UNIQUE.getName())).count();
        assertEquals(1, rarityLineCount, "Item lore should contain exactly one canonical rarity footer");
    }

    @Test
    @DisplayName("Special rarity items render canonical SPECIAL footer")
    void testSpecialRarityLoreRendering() {
        BlightedItem specialItem = new BlightedItem("special_relic", ItemType.UNCATEGORIZED, ItemRarity.SPECIAL, Material.PLAYER_HEAD)
                .description("A lost relic from an ancient epoch.");
        List<String> lore = ItemLoreRenderer.render(specialItem);

        assertNotNull(lore);
        assertTrue(lore.getLast().contains(ItemRarity.SPECIAL.getName()), "Last line must contain the SPECIAL footer");
    }

    @Test
    @DisplayName("Thermal Fuel items render flush header without leading blank line")
    void testThermalFuelLoreRendering() {
        ItemRegistry.clear();
        new ThermalFuels().register(ItemRegistry::register);

        BlightedItem enchantedCoal = ItemRegistry.getOrThrow("ENCHANTED_COAL");
        List<String> lore = ItemLoreRenderer.render(enchantedCoal);

        assertNotNull(lore);
        assertFalse(lore.isEmpty());
        assertEquals("§8Thermal Fuel", lore.getFirst(), "First lore line must be the flush subtitle header without empty line above");
        assertEquals("", lore.get(1), "Second lore line should separate subtitle from body description");
        assertEquals(ItemRarity.COMMON.getName(), lore.getLast(), "Material items must render bare rarity without 'MATERIAL' suffix");
        assertFalse(lore.getLast().contains("MATERIAL"), "Lore footer must not contain 'MATERIAL'");
        ItemRegistry.clear();
    }

    @Test
    @DisplayName("ItemType footer distinguishes equipment from materials and blocks")
    void testItemTypeLoreFooterFormatting() {
        BlightedItem material = new BlightedItem("mat_iron", ItemType.MATERIAL, ItemRarity.RARE, Material.IRON_INGOT);
        assertEquals(ItemRarity.RARE.getName(), ItemLoreRenderer.render(material).getLast(), "Material must render just rarity");

        BlightedItem block = new BlightedItem("custom_furnace", ItemType.BLOCK, ItemRarity.RARE, Material.BLAST_FURNACE);
        assertEquals(ItemRarity.RARE.getName(), ItemLoreRenderer.render(block).getLast(), "Block must render just rarity");

        BlightedItem lavaRod = new BlightedItem("lava_rod", ItemType.LAVA_FISHING_ROD, ItemRarity.RARE, Material.FISHING_ROD);
        assertEquals(ItemRarity.RARE.getName() + " FISHING ROD", ItemLoreRenderer.render(lavaRod).getLast(), "Lava fishing rod must format cleanly as FISHING ROD");

        BlightedItem helmet = new BlightedItem("rare_helmet", ItemType.HELMET, ItemRarity.UNIQUE, Material.DIAMOND_HELMET);
        assertEquals(ItemRarity.UNIQUE.getName() + " HELMET", ItemLoreRenderer.render(helmet).getLast(), "Armor piece must include slot suffix");
    }

    @Test
    @DisplayName("Rarity spacing DX controls empty line before canonical footer")
    void testRaritySpacingDX() {
        // 1. Default is spaced (blank line above rarity footer)
        BlightedItem defaultSpaced = new BlightedItem("spaced_sword", ItemType.SWORD, ItemRarity.UNIQUE, Material.DIAMOND_SWORD)
                .description("Line 1", "Line 2");
        List<String> spacedLore = ItemLoreRenderer.render(defaultSpaced);
        assertEquals("", spacedLore.get(spacedLore.size() - 2), "Default item must have empty line before rarity");
        assertEquals(ItemRarity.UNIQUE.getName() + " SWORD", spacedLore.getLast());

        // 2. Flush rarity (no empty line above rarity footer)
        BlightedItem flushItem = new BlightedItem("flush_ingot", ItemType.MATERIAL, ItemRarity.RARE, Material.IRON_INGOT)
                .description("Dense ingot.")
                .flushRarity();
        List<String> flushLore = ItemLoreRenderer.render(flushItem);
        assertEquals("§7 Dense ingot.", flushLore.get(flushLore.size() - 2), "Flush item must have text directly above rarity");
        assertEquals(ItemRarity.RARE.getName(), flushLore.getLast());

        // 3. Flush item strips accidental trailing empty line from custom lore
        BlightedItem flushTrailingEmpty = new BlightedItem("flush_extra_empty", ItemType.MATERIAL, ItemRarity.RARE, Material.IRON_INGOT)
                .description("Dense ingot.", "")
                .flushRarity();
        List<String> flushExtraEmptyLore = ItemLoreRenderer.render(flushTrailingEmpty);
        assertEquals("§7 Dense ingot.", flushExtraEmptyLore.get(flushExtraEmptyLore.size() - 2), "Flush item must strip trailing empty line");
        assertEquals(ItemRarity.RARE.getName(), flushExtraEmptyLore.getLast());

        // 4. Spaced item does not duplicate blank lines if one is already present
        BlightedItem spacedWithTrailingEmpty = new BlightedItem("spaced_extra_empty", ItemType.MATERIAL, ItemRarity.RARE, Material.IRON_INGOT)
                .description("Dense ingot.", "");
        List<String> dedupedLore = ItemLoreRenderer.render(spacedWithTrailingEmpty);
        assertEquals("", dedupedLore.get(dedupedLore.size() - 2), "Line before footer must be blank");
        assertNotEquals("", dedupedLore.get(dedupedLore.size() - 3), "Must not have duplicate blank lines");

        // 5. padRarity toggle programmatic API
        BlightedItem toggledItem = new BlightedItem("toggle_test", ItemType.MATERIAL, ItemRarity.RARE, Material.IRON_INGOT)
                .description("Toggle test")
                .padRarity(false);
        assertFalse(toggledItem.isSpacedRarity());
        toggledItem.padRarity(true);
        assertTrue(toggledItem.isSpacedRarity());
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
        assertEquals("§b§lRIGHT CLICK", AbilityTrigger.RIGHT_CLICK.getDisplayName());
        assertEquals("§b§lLEFT CLICK", AbilityTrigger.LEFT_CLICK.getDisplayName());
        assertEquals("§b§lSNEAK RIGHT CLICK", AbilityTrigger.SNEAK_RIGHT_CLICK.getDisplayName());
        assertEquals("§b§lSNEAK LEFT CLICK", AbilityTrigger.SNEAK_LEFT_CLICK.getDisplayName());
    }

    @Test
    @DisplayName("Tool abilities preserve block break/drop events while interactive abilities cancel them")
    void testAbilityEventCancellation() {
        AutosmeltAbility autosmelt = new AutosmeltAbility();
        TimberAbility timber = new TimberAbility();
        VeinmineAbility veinmine = new VeinmineAbility();
        HammerAbility hammer = new HammerAbility();

        assertFalse(autosmelt.cancelEvent(false), "Autosmelt should not cancel unhandled drops");
        assertFalse(autosmelt.cancelEvent(true), "Autosmelt should not cancel handled drops");

        assertFalse(timber.cancelEvent(false), "Timber should not cancel unhandled block breaks");
        assertFalse(timber.cancelEvent(true), "Timber should not cancel handled block breaks");

        assertFalse(veinmine.cancelEvent(false), "Veinmine should not cancel unhandled block breaks");
        assertFalse(veinmine.cancelEvent(true), "Veinmine should not cancel handled block breaks");

        assertFalse(hammer.cancelEvent(false), "Hammer should not cancel unhandled block breaks");
        assertFalse(hammer.cancelEvent(true), "Hammer should not cancel handled block breaks");

        BonemerangAbility bonemerang = new BonemerangAbility();
        assertTrue(bonemerang.cancelEvent(false), "Interactive abilities cancel on failure by default");
        assertTrue(bonemerang.cancelEvent(true), "Interactive abilities cancel on success by default");
    }

    @Test
    @DisplayName("Abilities with hasLore false do not inject ability lore into item tooltip")
    void testAbilityLoreSuppression() {
        BlightedItem item = new BlightedItem("test_blightstone", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.PLAYER_HEAD)
                .description("A rare blightstone.")
                .addAbility(new ResonantBlightstoneItem.ResonantBlightstoneAbility());

        List<String> lore = ItemLoreRenderer.render(item);

        assertNotNull(lore);
        assertTrue(lore.stream().anyMatch(l -> l.contains("A rare blightstone.")));
        assertFalse(lore.stream().anyMatch(l -> l.contains("Ability:")), "Lore must not contain ability header when hasLore is false");
        assertFalse(lore.stream().anyMatch(l -> l.contains("Consume Blightstone")), "Lore must not contain ability name when hasLore is false");
        assertEquals(ItemRarity.RARE.getName(), lore.getLast());
    }

    @Test
    @DisplayName("ResonantBlightstoneAbility has expected trigger and name")
    void testBlightstoneAbilityMetadata() {
        ResonantBlightstoneItem.ResonantBlightstoneAbility ability = new ResonantBlightstoneItem.ResonantBlightstoneAbility();
        assertEquals("Consume Blightstone", ability.getName());
        assertEquals(AbilityTrigger.RIGHT_CLICK, ability.getTrigger());
        assertFalse(ability.hasLore());
    }
}
