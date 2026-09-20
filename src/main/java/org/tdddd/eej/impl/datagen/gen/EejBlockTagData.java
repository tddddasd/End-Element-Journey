package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.tdddd.eej.api.AltarBlockTags;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejBlocks;

import java.util.concurrent.CompletableFuture;


public class EejBlockTagData extends BlockTagsProvider {

    public EejBlockTagData(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, eej.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                EejBlocks.PACKED_MUD_PEDESTAL.get(),
                EejBlocks.PACKED_MUD_ALTAR_STONE.get());

        
        tag(AltarBlockTags.PEDESTAL_TAG).add(EejBlocks.PACKED_MUD_PEDESTAL.get());
        tag(AltarBlockTags.ALTAR_STONE_TAG).add(EejBlocks.PACKED_MUD_ALTAR_STONE.get());
    }

    @Override
    public String getName() {
        return "eej Block Tags";
    }
}
