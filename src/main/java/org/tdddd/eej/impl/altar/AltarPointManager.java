package org.tdddd.eej.impl.altar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Block;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class AltarPointManager implements ResourceManagerReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final int DEFAULT_POINTS = 0;
    
    public static final String THRESHOLD_KEY = "threshold";
    
    public static final int DEFAULT_REQUIRED_POINTS = 0;
    private static final Map<Identifier, Integer> POINTS_MAP = new HashMap<>();
    private static int requiredPoints = DEFAULT_REQUIRED_POINTS;

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        POINTS_MAP.clear();
        requiredPoints = DEFAULT_REQUIRED_POINTS;

        resourceManager.listResources("altar_points", path -> path.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonObject root = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                        parseConfig(root);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void parseConfig(JsonObject root) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String blockId = entry.getKey();
            JsonElement value = entry.getValue();
            
            if (THRESHOLD_KEY.equals(blockId)) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    int threshold = value.getAsInt();
                    if (threshold < 0) {
                        threshold = 0;
                    }
                    
                    requiredPoints = Math.max(requiredPoints, threshold);
                }
                continue;
            }
            if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                if (obj.has("points")) {
                    int points = obj.get("points").getAsInt();
                    if (points < 0) {
                        points = 0;
                    }
                    Identifier key = Identifier.tryParse(blockId);
                    if (key != null) {
                        POINTS_MAP.put(key, points);
                    }
                }
            }
        }
    }

    
    public static int getPoints(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key != null && POINTS_MAP.containsKey(key)) {
            return POINTS_MAP.get(key);
        }
        if (block instanceof AbstractAltarBlock altarBlock) {
            return altarBlock.getAltarPoints();
        }
        return DEFAULT_POINTS;
    }

    public static int getPoints(String blockId) {
        Identifier key = Identifier.tryParse(blockId);
        if (key == null) return DEFAULT_POINTS;
        Block block = BuiltInRegistries.BLOCK.getValue(key);
        if (block != null) {
            return getPoints(block);
        }
        return POINTS_MAP.getOrDefault(key, DEFAULT_POINTS);
    }

    
    public static int getRequiredPoints() {
        return requiredPoints;
    }

    public static Map<Identifier, Integer> getAllPoints() {
        return new HashMap<>(POINTS_MAP);
    }
}
