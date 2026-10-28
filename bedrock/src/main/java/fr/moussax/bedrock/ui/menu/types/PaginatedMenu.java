package fr.moussax.bedrock.ui.menu.types;

import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.interaction.MenuElementPreset;
import fr.moussax.bedrock.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class for menus with automatic pagination.
 *
 * <p>Items are displayed across pages, with navigation controls rendered in
 * the bottom row. The current page is preserved when the menu is refreshed.</p>
 *
 * <p>Supports optional framed layouts via {@link #useStandardFrame()}, parent
 * menu back navigation via {@link #getPreviousMenu()}, and title hooks via
 * {@link #onBuildHeader(Player)}.</p>
 *
 * <p>Subclasses provide the total item count, item contents, and optional
 * click handling through {@link #onItemClick(Player, int, ClickType)}.</p>
 */
public abstract class PaginatedMenu extends Menu {

    public static final int[] INNER_GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static final int[] FRAME_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36,
            44, 45, 46, 47, 51, 52, 53
    };

    @Getter
    protected final Menu previousMenu;
    @Getter
    protected int currentPage = 0;
    protected int totalItems = 0;

    /**
     * Returns the total number of items stored for the current page rendering.
     *
     * @return stored total item count
     */
    public int getTotalItemsCount() {
        return totalItems;
    }

    /**
     * Creates a paginated menu.
     *
     * @param title menu title
     * @param size  inventory size (multiple of 9)
     */
    protected PaginatedMenu(String title, int size) {
        this(title, size, null);
    }

    /**
     * Creates a paginated menu with a link to a previous parent menu.
     *
     * @param title        menu title
     * @param size         inventory size (multiple of 9)
     * @param previousMenu parent menu to open when returning from page 0
     */
    protected PaginatedMenu(String title, int size, @Nullable Menu previousMenu) {
        super(title, size);
        this.previousMenu = previousMenu;
    }

    /**
     * Returns the total number of items available for pagination.
     *
     * @param player player viewing the menu
     * @return total number of items
     */
    protected abstract int getTotalItems(@NonNull Player player);

    /**
     * Returns the item displayed at the specified global index.
     *
     * @param player player viewing the menu
     * @param index  global item index
     * @return item to display
     */
    protected abstract ItemStack getItem(@NonNull Player player, int index);

    /**
     * Determines whether this menu uses the standard 54-slot framed layout.
     *
     * <p>When enabled, {@link #getDisplaySlots()} defaults to {@link #INNER_GRID_SLOTS}
     * and the framing border slots are populated with {@link MenuElementPreset#EMPTY_SLOT_FILLER}.</p>
     *
     * @return {@code true} to enable framed layout
     */
    protected boolean useStandardFrame() {
        return false;
    }

    /**
     * Fills the framing border slots with the specified item.
     *
     * @param item item displayed in the frame
     */
    public void fillFrame(@NonNull ItemStack item) {
        for (int slot : FRAME_SLOTS) {
            if (slot < size) {
                setItem(slot, item);
            }
        }
    }

    /**
     * Fills the framing border slots with the specified preset.
     *
     * @param preset item preset displayed in the frame
     */
    public void fillFrame(@NonNull MenuElementPreset preset) {
        fillFrame(preset.getItem());
    }

    /**
     * Returns the target inventory slot indices for displaying paginated items.
     *
     * <p>If {@code null}, items are placed sequentially starting at slot 0.</p>
     *
     * @return slot index array, or {@code null} for default sequential placement
     */
    protected int[] getDisplaySlots() {
        return useStandardFrame() ? INNER_GRID_SLOTS : null;
    }

    /**
     * Returns an item to display when the menu has no items, or {@code null}.
     *
     * @param player player viewing the menu
     * @return empty state item, or {@code null} if none
     */
    protected ItemStack getEmptyStateItem(@NonNull Player player) {
        return null;
    }

    /**
     * Returns the 1-based number of the current page.
     *
     * @return current page number (1-based)
     */
    public int getCurrentPageNumber() {
        return currentPage + 1;
    }

    /**
     * Returns the total number of pages based on total items and items per page.
     *
     * @return total number of pages
     */
    public int getTotalPages() {
        int itemsPerPage = getItemsPerPage();
        if (itemsPerPage <= 0) return 1;
        return Math.max(1, (totalItems + itemsPerPage - 1) / itemsPerPage);
    }

    /**
     * Returns the maximum number of items displayed on each page.
     *
     * <p>The bottom row is reserved for pagination controls.</p>
     *
     * @return number of items displayed per page
     */
    protected int getItemsPerPage() {
        int[] displaySlots = getDisplaySlots();
        if (displaySlots != null) {
            return displaySlots.length;
        }
        return size - 9;
    }

    /**
     * Initializes pagination metrics and clamps {@code currentPage}.
     *
     * @param viewer player viewing the menu
     */
    protected void initPage(@NonNull Player viewer) {
        totalItems = Math.max(0, getTotalItems(viewer));
        int itemsPerPage = getItemsPerPage();
        if (itemsPerPage > 0 && totalItems > 0) {
            int maxPage = (totalItems - 1) / itemsPerPage;
            currentPage = Math.min(currentPage, maxPage);
        } else {
            currentPage = 0;
        }
    }

    /**
     * Hook called at the start of {@link #build(Player)}, after {@link #initPage(Player)}
     * and before slots are rendered.
     *
     * <p>Override to set dynamic titles (e.g. page counts).</p>
     *
     * @param viewer player viewing the menu
     */
    protected void onBuildHeader(@NonNull Player viewer) {
    }

    /**
     * Builds the current page and its navigation controls.
     *
     * <p>The current page is clamped to the last available page when the
     * total item count changes between refreshes.</p>
     *
     * @param viewer player viewing the menu
     */
    @Override
    public void build(@NonNull Player viewer) {
        initPage(viewer);
        slots.clear();
        onBuildHeader(viewer);

        if (useStandardFrame()) {
            fillFrame(MenuElementPreset.EMPTY_SLOT_FILLER);
        }

        int closeSlot = size - 5;
        int backSlot = size - 6;
        int nextSlot = size - 4;

        ItemStack emptyItem = getEmptyStateItem(viewer);
        if (totalItems == 0) {
            if (emptyItem != null) {
                int emptySlot = size >= 27 ? 22 : size / 2;
                setItem(emptySlot, emptyItem);
            }
            if (useStandardFrame() && nextSlot < size) {
                setItem(nextSlot, MenuElementPreset.EMPTY_SLOT_FILLER);
            }
            renderParentBackButton(backSlot);
            setCloseButton(closeSlot);
            return;
        }

        int itemsPerPage = getItemsPerPage();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        int[] displaySlots = getDisplaySlots();
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            final int index = i;
            int slot = displaySlots != null ? displaySlots[slotIndex++] : i - startIndex;
            setItem(
                    slot,
                    getItem(viewer, index),
                    (player, click) -> {
                        if (click.isShiftClick()) {
                            onItemShiftClick(player, index);
                        } else if (click.isRightClick()) {
                            onItemRightClick(player, index);
                        } else if (click.isLeftClick()) {
                            onItemLeftClick(player, index);
                        }
                        onItemClick(player, index, click);
                    }
            );
        }

        int totalPages = getTotalPages();
        int pageNumber = getCurrentPageNumber();

        if (currentPage > 0) {
            ItemStack previousItem = new ItemBuilder(Material.ARROW, "§aPrevious Page")
                    .addLore("§7Page " + (pageNumber - 1) + "/" + totalPages)
                    .toItemStack();
            setItem(backSlot, previousItem, (player, _) -> {
                currentPage--;
                refresh(player);
            }).withPageTurnSound();
        } else {
            renderParentBackButton(backSlot);
        }

        if (endIndex < totalItems) {
            ItemStack nextItem = new ItemBuilder(Material.ARROW, "§aNext Page")
                    .addLore("§7Page " + (pageNumber + 1) + "/" + totalPages)
                    .toItemStack();
            setItem(nextSlot, nextItem, (player, _) -> {
                currentPage++;
                refresh(player);
            }).withPageTurnSound();
        } else if (useStandardFrame() && nextSlot < size) {
            setItem(nextSlot, MenuElementPreset.EMPTY_SLOT_FILLER);
        }

        setCloseButton(closeSlot);
    }

    private void renderParentBackButton(int slot) {
        if (previousMenu == null) return;

        String rawTitle = previousMenu.getTitle();
        String stripped = ChatColor.stripColor(rawTitle);
        String targetName = stripped.isBlank() ? "Previous Menu" : stripped;

        ItemStack backItem = new ItemBuilder(Material.ARROW, "§aGo Back")
                .addLore("§7To " + targetName)
                .toItemStack();

        setItem(slot, backItem, (player, _) -> {
            if (menuSystem != null) {
                menuSystem.popAndOpen(player, previousMenu);
            } else {
                previousMenu.open(player);
            }
        });
    }

    /**
     * Called when a paginated item is clicked with a left-click.
     *
     * @param player clicking player
     * @param index  global item index
     */
    protected void onItemLeftClick(@NonNull Player player, int index) {
    }

    /**
     * Called when a paginated item is clicked with a right-click.
     *
     * @param player clicking player
     * @param index  global item index
     */
    protected void onItemRightClick(@NonNull Player player, int index) {
    }

    /**
     * Called when a paginated item is clicked with a shift-click.
     *
     * @param player clicking player
     * @param index  global item index
     */
    protected void onItemShiftClick(@NonNull Player player, int index) {
    }

    /**
     * Called when a paginated item is clicked.
     *
     * @param player    clicking player
     * @param index     global item index
     * @param clickType click type
     */
    protected void onItemClick(@NonNull Player player, int index, @NonNull ClickType clickType) {
    }
}
