package org.tdddd.eej.impl.registry;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.altar.PackedMudAltarStone;
import org.tdddd.eej.impl.altar.PackedMudPedestal;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.element.plate.ElementAggregationPlate;
import org.tdddd.eej.impl.mana.CreativeManaSourceBlock;

public class EejBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, eej.MODID);

    
    public static final RegistryObject<Block> PACKED_MUD_PEDESTAL = BLOCKS.register(
            "packed_mud_pedestal",
            PackedMudPedestal::new
    );

    
    public static final RegistryObject<Block> PACKED_MUD_ALTAR_STONE = BLOCKS.register(
            "packed_mud_altar_stone",
            PackedMudAltarStone::new
    );

    
    public static final RegistryObject<Block> CREATIVE_MANA_SOURCE = BLOCKS.register(
            "creative_mana_source",
            CreativeManaSourceBlock::new
    );


    public static final RegistryObject<Block> ELEMENT_AGGREGATION_PLATE = BLOCKS.register(
            "element_aggregation_plate",
            () -> new ElementAggregationPlate(Block.Properties.of()
                    .strength(1.5F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.STONE)
                    .noOcclusion())
    );
}
