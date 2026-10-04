package fr.moussax.blightedSMP.engine.recipes;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Encodes a 3x3 shaped crafting recipe into a list of {@link CraftingObject}s.
 * <p>
 * Each recipe shape is defined by three lines of three characters each,
 * where each character represents a crafting ingredient bound to a {@link CraftingObject}.
 * Spaces or unbound characters are treated as empty slots.
 */
public final class ShapeEncoder {
    private final String line1;
    private final String line2;
    private final String line3;

    private final HashMap<Character, CraftingObject> keys = new HashMap<>();

    /**
     * Constructs a shape encoder for a 3x3 crafting recipe.
     *
     * @param line1 first row of the recipe (must be 3 characters)
     * @param line2 second row of the recipe (must be 3 characters)
     * @param line3 third row of the recipe (must be 3 characters)
     * @throws StringIndexOutOfBoundsException if any line is not exactly 3 characters
     */
    public ShapeEncoder(String line1, String line2, String line3) {
        if (line1.length() != 3 || line2.length() != 3 || line3.length() != 3) {
            throw new StringIndexOutOfBoundsException("Each crafting line must be 3 characters long");
        }
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
    }

    /**
     * Binds a shape character to a {@link CraftingObject}.
     *
     * @param key    the character representing the ingredient in the recipe shape
     * @param object the crafting object to associate with this key
     */
    public void bindKey(char key, CraftingObject object) {
        keys.put(key, object);
    }

    /**
     * Binds a shape character to a custom item managed by {@link BlightedItem}.
     *
     * @param key     the character representing the ingredient
     * @param manager the item manager for the custom item
     * @param amount  the quantity required
     */
    public void bindKey(char key, BlightedItem manager, int amount) {
        keys.put(key, new CraftingObject(manager, amount));
    }

    /**
     * Binds a shape character to a vanilla material.
     *
     * @param key      the character representing the ingredient
     * @param material the vanilla material
     * @param amount   the quantity required
     */
    public void bindKey(char key, Material material, int amount) {
        keys.put(key, new CraftingObject(material, amount));
    }

    /**
     * Converts the 3x3 shape and bound keys into a list of 9 crafting objects,
     * corresponding to the recipe's slots in row-major order.
     * <p>
     * Empty or unbound characters produce {@code null} entries.
     *
     * @return a list of 9 elements representing the recipe
     * @throws RuntimeException if the encoded list does not contain exactly 9 elements
     */
    public ArrayList<CraftingObject> encodeCraftingRecipe() {
        ArrayList<CraftingObject> objects = new ArrayList<>();
        String[] lines = {line1, line2, line3};

        for (String line : lines) {
            for (char c : line.toCharArray()) {
                if (c == ' ' || !keys.containsKey(c)) {
                    objects.add(null);
                } else {
                    objects.add(keys.get(c));
                }
            }
        }

        if (objects.size() != 9) {
            throw new RuntimeException("Failed to encode crafting recipe");
        }

        return objects;
    }
}
