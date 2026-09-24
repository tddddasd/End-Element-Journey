package org.tdddd.eej.impl.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;


/**
 * One weighted output group of a soul fire purification recipe.
 *
 * <p>JSON form (exactly the shape written by epca's data files):
 * <pre>
 * {
 *   "chance": 0.15,                          // optional, defaults to 1.0
 *   "weight": 1,                             // optional, defaults to 1
 *   "count_min": 2,                          // optional, defaults to 1
 *   "count_max": 3,                          // optional, defaults to count_min
 *   "random": false,                         // optional, defaults to false
 *   "items": [ "minecraft:raw_iron" ]        // at least one item id
 * }
 * </pre>
 *
 * <p>Semantics: a group fires when its roll succeeds. When it fires, one of its item ids is picked
 * uniformly at random ({@code random = false} always uses the first entry) and a count between
 * {@code count_min} and {@code count_max} inclusive is spawned.
 *
 * <p>Two roll kinds exist and are selected per recipe:
 * <ul>
 *   <li><b>Independent</b> (default): every group rolls its own {@code chance}.</li>
 *   <li><b>Weighted alternative</b>: as soon as at least one group carries {@code weight > 1}, all
 *       weighted groups of that recipe share one roll and group {@code i} fires when
 *       {@code random < chance * weight_i / sum(weight)}. This is how "30 % chance to spawn one
 *       random original block out of the N blocks converting into this infested block" is stored
 *       without repeating the 30 % on every candidate.</li>
 * </ul>
 */
public record SoulFireOutput(boolean random, List<Item> items, int countMin, int countMax,
                             float chance, int weight) {

    /** Resolves the rolled amount, inclusive of both bounds. */
    public int rollCount(RandomSource source) {
        if (countMax <= countMin) {
            return Math.max(1, countMin);
        }
        return countMin + source.nextInt(countMax - countMin + 1);
    }

    /** Picks one stack from this group; empty when the group has no resolvable items. */
    public ItemStack rollStack(RandomSource source) {
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = (!random || items.size() == 1)
                ? items.get(0)
                : items.get(source.nextInt(items.size()));
        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.max(1, rollCount(source)));
        return stack;
    }

    /** Resource ids of the candidates, in declaration order. */
    public List<ResourceLocation> itemIds() {
        List<ResourceLocation> ids = new ArrayList<>(items.size());
        for (Item item : items) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            if (key != null) {
                ids.add(key);
            }
        }
        return ids;
    }

    /** Human readable summary used by the JEI wrapper and by diagnostics. */
    public String describeItems() {
        List<ResourceLocation> ids = itemIds();
        if (ids.isEmpty()) {
            return "minecraft:air";
        }
        List<String> text = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            text.add(id.toString());
        }
        return String.join(" | ", text);
    }

    public static SoulFireOutput ofItem(Item item, int countMin, int countMax, float chance, int weight) {
        return new SoulFireOutput(false, List.of(item), countMin, countMax, chance, weight);
    }

    public static SoulFireOutput ofRandom(List<Item> items, int countMin, int countMax, float chance, int weight) {
        return new SoulFireOutput(true, List.copyOf(items), countMin, countMax, chance, weight);
    }

    /** Parses the compact JSON form; unknown item ids are skipped instead of failing the recipe. */
    public static SoulFireOutput fromJson(JsonObject json) {
        boolean random = GsonHelper.getAsBoolean(json, "random", false);
        JsonArray ids = GsonHelper.getAsJsonArray(json, "items");
        List<Item> items = new ArrayList<>(ids.size());
        for (JsonElement element : ids) {
            ResourceLocation itemId = ResourceLocation.tryParse(element.getAsString());
            if (itemId == null) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                items.add(item);
            }
        }
        int countMin = GsonHelper.getAsInt(json, "count_min", 1);
        int countMax = Math.max(countMin, GsonHelper.getAsInt(json, "count_max", countMin));
        float chance = GsonHelper.getAsFloat(json, "chance", 1.0F);
        int weight = GsonHelper.getAsInt(json, "weight", 1);
        return new SoulFireOutput(random, List.copyOf(items), countMin, countMax, chance, weight);
    }

    /** Serialises back to the compact JSON form; used by the datagen and by tooling. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("chance", chance);
        if (weight != 1) {
            json.addProperty("weight", weight);
        }
        if (countMin != 1 || countMax != 1) {
            json.addProperty("count_min", countMin);
            json.addProperty("count_max", countMax);
        }
        if (random) {
            json.addProperty("random", true);
        }
        JsonArray ids = new JsonArray();
        for (ResourceLocation id : itemIds()) {
            ids.add(id.toString());
        }
        json.add("items", ids);
        return json;
    }
}
