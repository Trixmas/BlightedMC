package fr.moussax.blightedSMP.engine.recipes.forging.menu;

import fr.moussax.blightedSMP.content.sound.BlightedSounds;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeFuel;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.interaction.MenuElementPreset;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.moussax.bedrock.ui.menu.TickableMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ForgeMenu extends Menu implements TickableMenu {

    private static final int[] GRID_SLOTS = {19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int[] REQUIRED_ITEM_INDICATOR_SLOTS = {10, 11, 12, 13};
    private static final int[] FORGED_ITEM_INDICATOR_SLOTS = {15, 16};
    private static final int ITEM_INDICATOR = 23;
    private static final int MAXIMUM_FORGE_FUEL = 250_000;

    private final ForgeRecipe recipe;
    private final Menu previousMenu;
    private boolean canForge = false;
    private boolean isForging = false;
    private int lastInventoryFuel = -1;

    public ForgeMenu(ForgeRecipe recipe, Menu previousMenu) {
        super("Blighted Forge", 54);
        this.recipe = recipe;
        this.previousMenu = previousMenu;
    }

    @Override
    public long tickPeriodTicks() {
        return 10L;
    }

    @Override
    public void onTick(Player player) {
        if (recipe == null || isForging) return;

        boolean initialCanForge = this.canForge;
        checkRequirements(player);
        int currentFuelInInventory = calculateInventoryFuel(player);

        if (initialCanForge != this.canForge || lastInventoryFuel != currentFuelInInventory) {
            this.lastInventoryFuel = currentFuelInInventory;
            refresh(player);
        }
    }

    public ForgeMenu(ForgeRecipe recipe) {
        this(recipe, null);
    }

    @Override
    public void build(Player player) {
        if (recipe != null) {
            setTitle("Forge Item");
        } else {
            setTitle("Blighted Forge");
        }

        if (recipe != null && !isForging) {
            checkRequirements(player);
        }

        fillEmptyWith(MenuElementPreset.EMPTY_SLOT_FILLER);
        setupGrid();

        if (recipe != null) {
            displayRequiredIngredients();
        }

        setupStatusPanes();
        setupResultDisplay();
        setupActionButtons(player);
    }

    private void setupGrid() {
        ItemBuilder recipeSlot = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§cLocked Slot");
        if (recipe == null) {
            recipeSlot.addLore("§7Select a recipe to view", "§7the required materials.");
        } else {
            recipeSlot.addLore("§7This slot isn't used for", "§7the selected recipe.");
        }

        fillSlots(GRID_SLOTS, recipeSlot.toItemStack());
    }

    private void setupResultDisplay() {
        if (recipe == null) {
            ItemStack noRecipe = new ItemBuilder(Material.BARRIER, "§cRecipe Required")
                    .addLore("§7Select a recipe from the", "§7recipe book to start forging.")
                    .toItemStack();
            setItem(25, noRecipe);
            return;
        }

        ItemStack result = recipe.getForgedItem().toItemStack().clone();
        result.setAmount(recipe.getForgedAmount());
        setItem(25, result);
    }

    private void checkRequirements(Player player) {
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

        boolean hasAllIngredients = requiredCounts.entrySet().stream()
                .allMatch(entry -> inventoryCounts.getOrDefault(entry.getKey(), 0) >= entry.getValue());

        boolean hasSufficientFuel = blightedPlayer.getForgeFuel() >= recipe.getFuelCost();

        this.canForge = hasAllIngredients && hasSufficientFuel;
    }

    private Map<String, Integer> aggregateRequirements() {
        Map<String, Integer> counts = new HashMap<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            String id = ingredient.getId();
            counts.put(id, counts.getOrDefault(id, 0) + ingredient.getAmount());
        }
        return counts;
    }

    private void displayRequiredIngredients() {
        for (int i = 0; i < recipe.getIngredients().size() && i < GRID_SLOTS.length; i++) {
            RecipeIngredient ingredient = recipe.getIngredients().get(i);
            ItemStack displayItem = createDisplayItem(ingredient);
            setItem(GRID_SLOTS[i], displayItem);
        }
    }

    private ItemStack createDisplayItem(RecipeIngredient ingredient) {
        return ingredient.toItemStack();
    }

    private void setupStatusPanes() {
        Material indicator = determineIndicatorMaterial();

        ItemStack sacrificedItemPane = new ItemBuilder(indicator, "§6Items to Process")
                .addLore("§7The materials required to forge", "§7the item are displayed in this side.")
                .toItemStack();

        ItemStack forgedItemPane = new ItemBuilder(indicator, "§6Item to Forge")
                .addLore("§7The item you will create", "§7with the required materials.")
                .toItemStack();

        fillSlots(REQUIRED_ITEM_INDICATOR_SLOTS, sacrificedItemPane);
        fillSlots(FORGED_ITEM_INDICATOR_SLOTS, forgedItemPane);
        setItem(ITEM_INDICATOR, new ItemBuilder(indicator).hideTooltip().toItemStack());
        setItem(14, new ItemBuilder(Material.BLAST_FURNACE).hideTooltip().toItemStack());
    }

    private Material determineIndicatorMaterial() {
        if (recipe == null) return Material.WHITE_STAINED_GLASS_PANE;
        if (isForging) return Material.ORANGE_STAINED_GLASS_PANE;
        return canForge ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
    }

    private void setupActionButtons(Player player) {
        setupForgeButton();
        setupNavigationButtons();
        setupFuelButtons(player);
    }

    private void setupForgeButton() {
        ItemBuilder builder = new ItemBuilder(Material.ANVIL);

        if (recipe == null) {
            builder.setDisplayName("§aForge Item")
                    .addLore("§7Select a recipe from the", "§7Recipe Book to start forging.");
        } else if (isForging) {
            builder.setDisplayName("§6Forging...");
        } else {
            builder.setDisplayName("§aConfirm process").addLore("", " §7Items required: ");
            for (RecipeIngredient ingredient : recipe.getIngredients()) {
                builder.addLore(" §8‣ " + Utilities.extractIngredientName(ingredient) + " §8x" + ingredient.getAmount());
            }

            builder.addLore(
                    "",
                    " §8Consumes §6🪣 " + Formatter.formatDecimalWithCommas(recipe.getFuelCost()) + "mB §8of fuel to ",
                    " §8start the forging process.",
                    "",
                    canForge ? "§eClick to confirm!" : "§cMissing ingredients or insufficient fuel!"
            ).setEnchantmentGlint(canForge);
        }

        setItem(32, builder.toItemStack(), (clickingPlayer, _) -> {
            if (isForging) return;

            if (recipe != null && canForge) {
                forgeItem(clickingPlayer);
            } else {
                playErrorSound(clickingPlayer);
            }
        }).withoutSound();
    }

    private void setupNavigationButtons() {
        ItemStack recipeBook = new ItemBuilder(Material.KNOWLEDGE_BOOK, "§6Forge Recipes")
                .addLore("§7View all §6Forge Recipes §7and ", "§7their requirements.", "", "§eClick to view!")
                .toItemStack();

        setCloseButton(49);
        setItem(50, recipeBook, (clickingPlayer, _) -> openSubMenu(new ForgeRecipesMenu(this)));
    }

    private void setupFuelButtons(Player player) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        int currentFuel = blightedPlayer.getForgeFuel();

        setItem(34, createFuelMeter(currentFuel));
        setItem(52, createFuelGuide());
        setItem(53, createInsertFuelButton(player), (_, _) -> handleFuelInsertion(blightedPlayer));
    }

    private ItemStack createFuelMeter(int currentFuel) {
        String formattedCurrent = Formatter.formatDecimalWithCommas(currentFuel);
        String formattedMax = Formatter.formatDecimalWithCommas(MAXIMUM_FORGE_FUEL);

        int barLength = 20;
        double valuePerSegment = MAXIMUM_FORGE_FUEL / (double) barLength;

        return new ItemBuilder(Material.CAMPFIRE, "§6Fuel Meter")
                .addLore(
                        "§r " + Formatter.createProgressBar(currentFuel, barLength, valuePerSegment, ChatColor.YELLOW) + " §e" + formattedCurrent + "§6/§e" + formattedMax + "mB ",
                        "",
                        " §7Fill your forge with §c🔥 thermal fuel §7like",
                        " §7§fLava Bucket§7, to power the forge and",
                        " §7craft advanced items.",
                        ""
                )
                .glow().toItemStack();
    }

    private ItemStack createFuelGuide() {
        return new ItemBuilder(Material.BLAZE_POWDER, "§6Thermal Fuel")
                .addLore(
                        "§8Measured in millibuckets (mB)", "",
                        " §7There are various types of §6Thermal Fuel ",
                        " §7that you can use to power your Forge.",
                        " §7Each offers various §6🪣 mB §7of fuel",
                        " §7based on their §c🔥 heat strength§7.",
                        " ",
                        "   §8‣ §fCoal §8- §610mB",
                        "   §8‣ §fMagma Block §8- §640mB",
                        "   §8‣ §fBlaze Rod §8- §6200mB",
                        "   §8‣ §fLava Bucket §8- §61,000mB",
                        "   §8‣ §eEnchanted Coal §8- §63,000mB",
                        "   §8‣ §bEnchanted Lava Bucket §8- §610,000mB ",
                        "   §8‣ §dMagma Bucket §8- §620,000mB",
                        "   §8‣ §cPlasma Bucket §8- §650,000mB",
                        ""
                )
                .glow()
                .toItemStack();
    }

    private ItemStack createInsertFuelButton(Player player) {
        int fuelInInventory = calculateInventoryFuel(player);

        return new ItemBuilder(Material.CAULDRON, "§fInsert Fuel from Inventory")
                .addLore(
                        "§7Grab as much fuel that will fit into ",
                        "§7the forge from your inventory.", "",
                        "§7 In your inventory: §6🪣 " + Formatter.formatDecimalWithCommas(fuelInInventory) + "mB", "",
                        "§eClick to insert!"
                )
                .toItemStack();
    }

    private int calculateInventoryFuel(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                total += ForgeFuel.getFuelAmount(item);
            }
        }
        return total;
    }

    private void handleFuelInsertion(BlightedPlayer blightedPlayer) {
        Player player = blightedPlayer.getPlayer();

        int currentFuel = blightedPlayer.getForgeFuel();
        int availableSpace = MAXIMUM_FORGE_FUEL - currentFuel;

        if (availableSpace <= 0) {
            Messenger.warn(player, "Your forge is already full!");
            return;
        }

        int addedFuel = consumeFuelFromInventory(player, availableSpace);

        if (addedFuel > 0) {
            blightedPlayer.addForgeFuel(addedFuel);
            blightedPlayer.saveData();
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1f, 0f);
            refresh(player);
        } else {
            Messenger.warn(player, "No suitable fuel found in your inventory!");
        }
    }

    private int consumeFuelFromInventory(Player player, int availableSpace) {
        int totalAdded = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;

            final int fuelPerItem = ForgeFuel.getFuelPerItem(item);
            if (fuelPerItem <= 0) continue;

            int remainingSpace = availableSpace - totalAdded;
            if (remainingSpace <= 0) break;

            if (fuelPerItem > remainingSpace) continue;
            int stackAmount = item.getAmount();

            int itemsToConsume = Math.min(stackAmount, remainingSpace / fuelPerItem);

            if (itemsToConsume > 0) {
                totalAdded += itemsToConsume * fuelPerItem;

                int newAmount = stackAmount - itemsToConsume;
                if (newAmount > 0) {
                    item.setAmount(newAmount);
                    player.getInventory().setItem(slot, item);
                } else {
                    player.getInventory().setItem(slot, null);
                }
            }

            if (totalAdded >= availableSpace) break;
        }
        return totalAdded;
    }

    private void forgeItem(Player player) {
        if (isForging) return;

        checkRequirements(player);
        if (!canForge) {
            Messenger.warn(player, "Not enough resources!");
            refresh(player);
            return;
        }

        this.isForging = true;
        this.canForge = false;

        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);

        consumeIngredients(player);
        blightedPlayer.removeForgeFuel(recipe.getFuelCost());
        blightedPlayer.saveData();

        refresh(player);
        BlightedSounds.FORGE_ITEM.play(player.getLocation());

        int[] inputPath = {10, 11, 12, 13};
        for (int i = 0; i < inputPath.length; i++) {
            final int slot = inputPath[i];
            PluginContext.delay(() -> setStatusSlot(slot, Material.PURPLE_STAINED_GLASS_PANE), i * 2L);
        }

        PluginContext.delay(() -> {
            updateStatusPanes(Material.MAGENTA_STAINED_GLASS_PANE, REQUIRED_ITEM_INDICATOR_SLOTS);
            setStatusSlot(ITEM_INDICATOR, Material.MAGENTA_STAINED_GLASS_PANE);
        }, 8L);

        PluginContext.delay(() -> setStatusSlot(15, Material.CYAN_STAINED_GLASS_PANE), 12L);
        PluginContext.delay(() -> setStatusSlot(16, Material.CYAN_STAINED_GLASS_PANE), 14L);

        PluginContext.delay(() -> {
            updateStatusPanes(Material.OBSIDIAN, REQUIRED_ITEM_INDICATOR_SLOTS);
            updateStatusPanes(Material.OBSIDIAN, FORGED_ITEM_INDICATOR_SLOTS);
            setStatusSlot(ITEM_INDICATOR, Material.OBSIDIAN);
        }, 16L);

        PluginContext.delay(() -> {
            updateStatusPanes(Material.PURPLE_STAINED_GLASS_PANE, REQUIRED_ITEM_INDICATOR_SLOTS);
            updateStatusPanes(Material.PURPLE_STAINED_GLASS_PANE, FORGED_ITEM_INDICATOR_SLOTS);
            setStatusSlot(ITEM_INDICATOR, Material.PURPLE_STAINED_GLASS_PANE);
        }, 18L);

        PluginContext.delay(() -> {
            Player viewingPlayer = getPlayer();
            if (viewingPlayer == null || !viewingPlayer.isOnline()) {
                this.isForging = false;
                return;
            }

            ItemStack result = recipe.getForgedItem().toItemStack().clone();
            result.setAmount(recipe.getForgedAmount());

            HashMap<Integer, ItemStack> leftover = viewingPlayer.getInventory().addItem(result);
            for (ItemStack item : leftover.values()) {
                viewingPlayer.getWorld().dropItemNaturally(viewingPlayer.getLocation(), item);
            }

            this.isForging = false;

            if (viewingPlayer.getOpenInventory().getTopInventory().getHolder() instanceof ForgeMenu) {
                refresh(viewingPlayer);
            }
        }, 22L);
    }

    private void updateStatusPanes(Material material, int[] slots) {
        Player player = getPlayer();
        if (player == null || !(player.getOpenInventory().getTopInventory().getHolder() instanceof ForgeMenu)) {
            return;
        }

        ItemStack pane = new ItemBuilder(material).hideTooltip().toItemStack();
        for (int slot : slots) {
            setItem(slot, pane);
            inventory.setItem(slot, pane);
        }
    }

    private void setStatusSlot(int slot, Material material) {
        Player player = getPlayer();
        if (player == null || !(player.getOpenInventory().getTopInventory().getHolder() instanceof ForgeMenu)) {
            return;
        }

        ItemStack pane = new ItemBuilder(material).hideTooltip().toItemStack();
        setItem(slot, pane);
        inventory.setItem(slot, pane);
    }

    private void consumeIngredients(Player player) {
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            Utilities.consumeItemsFromInventory(player, ingredient);
        }
    }
}
