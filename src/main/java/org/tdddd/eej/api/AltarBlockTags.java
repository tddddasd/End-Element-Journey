package org.tdddd.eej.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.tdddd.eej.impl.eej;


public final class AltarBlockTags {
    
    public static final TagKey<Block> PEDESTAL_TAG = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(eej.MODID, "pedestals"));

    
    public static final TagKey<Block> ALTAR_STONE_TAG = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(eej.MODID, "altar_stones"));

    private AltarBlockTags() {
    }
}
