package org.tdddd.eej.impl.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.element.plate.blockentity.ElementAggregationPlateBlockEntity;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;


public class EejBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, eej.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackedMudPedestalBlockEntity>> PACKED_MUD_PEDESTAL =
            BLOCK_ENTITIES.register("packed_mud_pedestal", () ->
                    new BlockEntityType<>(PackedMudPedestalBlockEntity::new, EejBlocks.PACKED_MUD_PEDESTAL.get()));

    /** Creative mana source: an always-full mana buffer that feeds its six neighbours every tick. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeManaSourceBlockEntity>> CREATIVE_MANA_SOURCE =
            BLOCK_ENTITIES.register("creative_mana_source", () ->
                    new BlockEntityType<>(CreativeManaSourceBlockEntity::new, EejBlocks.CREATIVE_MANA_SOURCE.get()));

    /** Element aggregation plate: nine element ids, one output slot, ten mana. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElementAggregationPlateBlockEntity>>
            ELEMENT_AGGREGATION_PLATE = BLOCK_ENTITIES.register("element_aggregation_plate", () ->
            new BlockEntityType<>(ElementAggregationPlateBlockEntity::new,
                    EejBlocks.ELEMENT_AGGREGATION_PLATE.get()));

    private EejBlockEntities() {
    }
}
