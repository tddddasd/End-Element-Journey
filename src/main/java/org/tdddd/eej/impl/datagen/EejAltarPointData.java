package org.tdddd.eej.impl.datagen;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.tdddd.eej.impl.altar.AbstractAltarBlock;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejBlocks;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;


public class EejAltarPointData implements DataProvider {
    private final PackOutput out;

    public EejAltarPointData(PackOutput out) {
        this.out = out;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject root = new JsonObject();
        add(root, EejBlocks.PACKED_MUD_PEDESTAL.get());
        add(root, EejBlocks.PACKED_MUD_ALTAR_STONE.get());

        Path path = out.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(eej.MODID + "/altar_points/" + eej.MODID + "_altar_points.json");
        return DataProvider.saveStable(cache, root, path);
    }

    private static void add(JsonObject root, Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null || !(block instanceof AbstractAltarBlock altarBlock)) return;
        JsonObject entry = new JsonObject();
        entry.addProperty("points", altarBlock.getAltarPoints());
        root.add(key.toString(), entry);
    }

    @Override
    public String getName() {
        return "eej Altar Points";
    }
}
