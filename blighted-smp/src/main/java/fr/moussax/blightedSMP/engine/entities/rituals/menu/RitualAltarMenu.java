package fr.moussax.blightedSMP.engine.entities.rituals.menu;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.TickableMenu;
import fr.moussax.bedrock.ui.menu.interaction.MenuElementPreset;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.content.sound.BlightedSounds;
import fr.moussax.blightedSMP.engine.entities.rituals.AncientCreature;
import fr.moussax.blightedSMP.engine.entities.rituals.AncientRitual;
import fr.moussax.blightedSMP.engine.entities.rituals.RitualAnimations;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class RitualAltarMenu extends Menu implements TickableMenu {
    private static final int[] GRID_SLOTS = {19, 20, 21, 28, 29, 30};
    private static final int[] REQUIRED_ITEM_INDICATOR_SLOTS = {10, 11, 12, 13};
    private static final int[] INVOKED_MOB_INDICATOR_SLOTS = {15, 16};
    private static final int[] ITEM_INDICATOR = {23, 45, 46, 47, 48, 51, 52, 53};

    private final AncientRitual ritual;
    private final Menu previousMenu;
    private boolean canInvoke = false;
    private int lastPlayerLevel = -1;
    private double lastPlayerGems = -1;

    public RitualAltarMenu(AncientRitual ritual, Menu previousMenu) {
        super("Rituals Altar", 54);
        this.ritual = ritual;
        this.previousMenu = previousMenu;
    }

    @Override
    public long tickPeriodTicks() {
        return 10L;
    }

    @Override
    public void onTick(Player player) {
        if (ritual == null) return;

        boolean initialCanInvoke = this.canInvoke;
        checkRequirements(player);

        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        int currentLevel = player.getLevel();
        double currentGems = blightedPlayer != null ? blightedPlayer.getGems() : 0;

        if (initialCanInvoke != this.canInvoke || lastPlayerLevel != currentLevel || lastPlayerGems != currentGems) {
            this.lastPlayerLevel = currentLevel;
            this.lastPlayerGems = currentGems;
            refresh(player);
        }
    }

    public RitualAltarMenu(AncientRitual ritual) {
        this(ritual, null);
    }

    @Override
    public void build(Player player) {
        if (ritual == null) {
            setTitle("Rituals Altar");
        } else {
            setTitle("Invoke Creature");
        }

        if (ritual != null) {
            checkRequirements(player);
        }

        fillEmptyWith(MenuElementPreset.EMPTY_SLOT_FILLER);
        setupGrid();

        if (ritual != null) {
            displayRequiredIngredients();
        }

        setupStatusPanes();
        setupResultDisplay();
        setupActionButtons(player);
    }

    private void setupGrid() {
        ItemBuilder builder = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§cLocked Slot");
        if (ritual == null) {
            builder.addLore("§7Select an ancient ritual to view", "§7the required offerings.");
        } else {
            builder.addLore("§7This slot isn't used for", "§7the selected ritual.");
        }

        fillSlots(GRID_SLOTS, builder.toItemStack());
    }

    private void setupResultDisplay() {
        if (ritual == null) {
            ItemStack barrier = new ItemBuilder(Material.BARRIER, "§cRitual Required")
                    .addLore("§7Select an ancient ritual to start", "§7the invocation process.")
                    .toItemStack();
            setItem(25, barrier);
            return;
        }

        ItemStack result = ritual.getDisplayedItem().clone();
        if (!result.hasItemMeta() || !Objects.requireNonNull(result.getItemMeta()).hasDisplayName()) {
            ItemBuilder builder = new ItemBuilder(result);
            if (ritual.getSummonedCreature() != null) {
                builder.setDisplayName("§5" + ritual.getSummonedCreature().getName());
            }
            result = builder.toItemStack();
        }

        setItem(25, result);
    }

    private void checkRequirements(Player player) {
        if (ritual == null) {
            this.canInvoke = false;
            return;
        }

        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        Map<String, Integer> requiredCounts = aggregateRequirements();

        Map<String, Integer> inventoryCounts = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            for (String reqId : requiredCounts.keySet()) {
                if (Utilities.resolveItemId(item, reqId).equals(reqId)) {
                    inventoryCounts.put(reqId, inventoryCounts.getOrDefault(reqId, 0) + item.getAmount());
                }
            }
        }

        boolean hasItems = requiredCounts.entrySet().stream()
                .allMatch(entry -> inventoryCounts.getOrDefault(entry.getKey(), 0) >= entry.getValue());
        boolean hasGems = blightedPlayer.hasGems(ritual.getGemsCost());
        boolean hasXp = player.getLevel() >= ritual.getLevelCost();

        this.canInvoke = hasItems && hasGems && hasXp;
    }

    private Map<String, Integer> aggregateRequirements() {
        Map<String, Integer> counts = new HashMap<>();
        if (ritual == null) return counts;

        for (RecipeIngredient ingredient : ritual.getOfferings()) {
            counts.merge(ingredient.getId(), ingredient.getAmount(), Integer::sum);
        }
        return counts;
    }

    private void displayRequiredIngredients() {
        for (int i = 0; i < ritual.getOfferings().size() && i < GRID_SLOTS.length; i++) {
            RecipeIngredient ingredient = ritual.getOfferings().get(i);
            setItem(GRID_SLOTS[i], createDisplayItem(ingredient));
        }
    }

    private ItemStack createDisplayItem(RecipeIngredient ingredient) {
        return ingredient.toItemStack();
    }

    private void setupStatusPanes() {
        Material indicator = determineIndicatorMaterial();
        ItemStack sacrificedPane = new ItemBuilder(indicator, "§5Offerings to Sacrifice")
                .addLore("§7The items required to invoke", "§7the §4⚚ Ancient Creature §7are", "§7displayed in this side.")
                .toItemStack();
        ItemStack invokedPane = new ItemBuilder(indicator, "§5Creature to Invoke")
                .addLore("§7The §4⚚ Ancient Creature §7you will", "§7invoke with your offerings.")
                .toItemStack();
        ItemStack fillerPane = new ItemBuilder(indicator, "§r").hideTooltip().toItemStack();

        fillSlots(REQUIRED_ITEM_INDICATOR_SLOTS, sacrificedPane);
        fillSlots(INVOKED_MOB_INDICATOR_SLOTS, invokedPane);
        fillSlots(ITEM_INDICATOR, fillerPane);

        ItemStack shriekerIcon = new ItemBuilder(Material.SCULK_SHRIEKER, "§5The Altar")
                .addLore(
                        "§7The Ritual Altar allows you to offer",
                        "§7rare sacrifices, gems, and experience",
                        "§7to invoke ancient forgotten entities."
                )
                .toItemStack();
        setItem(14, shriekerIcon);
    }

    private Material determineIndicatorMaterial() {
        if (ritual == null) return Material.WHITE_STAINED_GLASS_PANE;
        return canInvoke ? Material.PURPLE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
    }

    private void setupActionButtons(Player player) {
        setupInvokeButton();
        setupNavigationButtons();
    }

    private void setupInvokeButton() {
        ItemBuilder builder = new ItemBuilder(Material.END_PORTAL_FRAME);
        if (ritual == null) {
            builder.setDisplayName("§5Initiate Invocation")
                    .addLore("§7Select an ancient ritual to start", "§7the invocation process.");
        } else {
            String creatureName = ritual.getSummonedCreature() != null
                    ? ritual.getSummonedCreature().getName()
                    : "Creature";
            builder.setDisplayName("§5Invoke " + creatureName)
                    .addLore("", " §7Offerings required:");

            for (RecipeIngredient ingredient : ritual.getOfferings()) {
                builder.addLore(" §8‣ " + Utilities.extractIngredientName(ingredient) + " §8x" + ingredient.getAmount());
            }

            if (ritual.getGemsCost() > 0) {
                builder.addLore(" §8‣ §d" + Formatter.formatDecimalWithCommas(ritual.getGemsCost()) + "✵ Gems");
            }
            if (ritual.getLevelCost() > 0) {
                builder.addLore(" §8‣ §3" + Formatter.formatDecimalWithCommas(ritual.getLevelCost()) + "◎ EXP Levels");
            }

            if (ritual.getSummonedCreature() != null) {
                builder.addLore(
                        "",
                        " §7Time limit: §c" + Formatter.formatTime(ritual.getSummonedCreature().getTimeAllowance())
                );
            }

            builder.addLore(
                    "",
                    " §c§lBEWARE!",
                    " §cSuch rituals demand sacrifice,",
                    " §cand the Ancients do not return",
                    " §cunchanged.",
                    "",
                    canInvoke ? "§eClick to confirm!" : "§cMissing offerings!"
            ).setEnchantmentGlint(canInvoke);
        }

        setItem(32, builder.toItemStack(), (clickingPlayer, _) -> {
            if (ritual != null && canInvoke) {
                invokeMob(clickingPlayer);
            } else {
                playErrorSound(clickingPlayer);
            }
        }).withoutSound();
    }

    private void setupNavigationButtons() {
        ItemStack recipeBook = new ItemBuilder(Material.FLOW_BANNER_PATTERN, "§5Ancient Rituals")
                .addLore(
                        "§7Browse ancient rituals written eons",
                        "§7ago by the §5Voidling Mages§7, devised",
                        "§7to invoke forgotten creatures",
                        "§7back into existence.",
                        "",
                        "§eClick to browse!"
                ).toItemStack();

        setCloseButton(49);
        setItem(50, recipeBook, (clickingPlayer, _) -> openSubMenu(new AncientRitualsMenu(this)));
    }

    private void consumeIngredients(Player player) {
        for (RecipeIngredient ingredient : ritual.getOfferings()) {
            Utilities.consumeItemsFromInventory(player, ingredient);
        }
        BlightedPlayer.get(player).removeGems(ritual.getGemsCost());
        player.setLevel(player.getLevel() - ritual.getLevelCost());
    }

    private void invokeMob(Player player) {
        checkRequirements(player);
        if (!canInvoke) {
            Messenger.warn(player, "You don't have the required ingredients!");
            refresh(player);
            return;
        }

        this.canInvoke = false;
        consumeIngredients(player);
        close();

        Bukkit.broadcastMessage("§5 ☤ §f" + player.getName() + " §dhas started an §5Ancient Ritual§d!");

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(3));
        spawnLoc.setY(player.getLocation().getY());

        BlightedSounds.ANCIENT_MOB_SPAWN.play(spawnLoc);
        RitualAnimations.playRiteAnimation(BlightedSMP.getInstance(), spawnLoc, () -> handleFinalImpact(spawnLoc));
    }

    private void handleFinalImpact(Location location) {
        Player player = getPlayer();
        if (ritual.getSummonedCreature() == null || player == null) {
            return;
        }

        Bukkit.broadcastMessage("§5 ☤ §dThe §4" + ritual.getSummonedCreature().getName() + " §dhas been summoned by §f" + player.getName() + "§d.");

        AncientCreature creature = ritual.getSummonedCreature().createInstance();
        creature.summoner(player);
        creature.spawn(location);
    }
}
