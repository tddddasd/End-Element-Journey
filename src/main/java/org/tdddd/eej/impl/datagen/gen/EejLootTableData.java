package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.tdddd.eej.impl.registry.EejBlocks;

import java.util.List;
import java.util.Set;


public class EejLootTableData extends LootTableProvider {

    public EejLootTableData(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(EejBlockLoot::new, LootContextParamSets.BLOCK)));
    }

    private static class EejBlockLoot extends BlockLootSubProvider {
        protected EejBlockLoot() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            dropSelf(EejBlocks.PACKED_MUD_PEDESTAL.get());
            dropSelf(EejBlocks.PACKED_MUD_ALTAR_STONE.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(EejBlocks.PACKED_MUD_PEDESTAL.get(), EejBlocks.PACKED_MUD_ALTAR_STONE.get());
        }
    }
}
