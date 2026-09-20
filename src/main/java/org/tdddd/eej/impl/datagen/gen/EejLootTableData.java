package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.tdddd.eej.impl.registry.EejBlocks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


public class EejLootTableData extends LootTableProvider {

    public EejLootTableData(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(EejBlockLoot::new, LootContextParamSets.BLOCK)),
                registries);
    }

    private static class EejBlockLoot extends BlockLootSubProvider {
        protected EejBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
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
