package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.eej.impl.eej;

import java.util.Map;
import java.util.Set;


public class EejBlockStateData extends BlockStateProvider {

    
    private static final Set<String> MANUAL_BLOCKS = Set.of(
            "packed_mud_pedestal"
    );

    public EejBlockStateData(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, eej.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ForgeRegistries.BLOCKS.getEntries().stream()
                .filter(e -> e.getKey().location().getNamespace().equals(eej.MODID))
                .filter(e -> !MANUAL_BLOCKS.contains(e.getKey().location().getPath()))
                .map(Map.Entry::getValue)
                .forEach(this::generateSimpleBlock);
    }

    private void generateSimpleBlock(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null) return;
        String name = key.getPath();
        simpleBlockWithItem(block, models().cubeAll(name, modLoc("block/" + name)));
    }
}
