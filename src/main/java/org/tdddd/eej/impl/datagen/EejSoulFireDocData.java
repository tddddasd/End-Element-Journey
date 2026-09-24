package org.tdddd.eej.impl.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.tdddd.eej.impl.eej;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * Documents the {@code eej:soul_fire_purification} recipe type as a datapack-reloadable resource so
 * the schema ships inside the mod jar instead of only living in the source.
 *
 * <p>Output: {@code data/eej/soul_fire_purification/schema.json}
 */
public class EejSoulFireDocData implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final PackOutput output;

    public EejSoulFireDocData(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject root = new JsonObject();
        root.addProperty("type", eej.MODID + ":soul_fire_purification");
        root.addProperty("summary",
                "An item entity standing inside minecraft:soul_fire is rolled once per single item. "
                        + "Successful rolls spawn the configured results, failed rolls consume the item.");

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ingredient", "Standard ingredient object, e.g. {\"item\":\"epca:infested_raw_iron\"}.");
        fields.put("burn_chance", "Optional float, informational only; the chance the item is destroyed. Shown by JEI.");
        fields.put("weighted", "Optional bool, default false. When true the result groups share one roll (see result_fields weight).");
        fields.put("destroyed", "Optional bool, default false. With no results this means the item is consumed without any roll.");
        fields.put("results", "Optional array of output groups. A recipe needs results, explode or destroyed.");
        fields.put("explode", "Optional explosion block; when present the item detonates on contact with fire instead of rolling.");
        JsonObject fieldDoc = new JsonObject();
        fields.forEach(fieldDoc::addProperty);
        root.add("fields", fieldDoc);

        JsonArray resultFields = new JsonArray();
        resultFields.add("chance: float, default 1.0. Independent roll chance, or the total chance of a weighted alternative group.");
        resultFields.add("weight: int, default 1. As soon as one group of a recipe uses weight > 1 all weighted groups share one roll and fire when random < chance * weight / sum(weight).");
        resultFields.add("count_min / count_max: int, default 1. Inclusive count range of the spawned stack.");
        resultFields.add("items: array of item ids, required. The candidates of the group.");
        resultFields.add("random: bool, default false. When true one entry of items is picked uniformly; when false the first entry is used.");
        root.add("result_fields", resultFields);

        JsonArray explodeFields = new JsonArray();
        explodeFields.add("power: float, default 4.0. Vanilla explosion power; the damage sweep radius is power * 2.");
        explodeFields.add("fire: bool, default false. Whether the explosion creates fire.");
        explodeFields.add("blindness_ticks: int, default 100. Duration of Blindness I applied in the radius, soul fire only.");
        root.add("explode_fields", explodeFields);

        root.addProperty("processing_interval_ticks", 10);
        root.addProperty("fire_immunity_ticks", 600);
        root.addProperty("fire_immunity_note",
                "Purified drops carry the eej_soul_fire_purified flag and an eej_soul_fire_immune_until game time inside their ItemStack NBT, so the timer survives pick up and drop.");

        Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(eej.MODID).resolve("soul_fire_purification").resolve("schema.json");
        return DataProvider.saveStable(cache, GSON.toJsonTree(root), path);
    }

    @Override
    public String getName() {
        return "EEJ Soul Fire Purification Documentation";
    }
}
