package org.tdddd.eej.impl.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.eej;


public class EejBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, eej.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackedMudPedestalBlockEntity>> PACKED_MUD_PEDESTAL =
            BLOCK_ENTITIES.register("packed_mud_pedestal", () ->
                    new BlockEntityType<>(PackedMudPedestalBlockEntity::new, EejBlocks.PACKED_MUD_PEDESTAL.get()));

    private EejBlockEntities() {
    }
}
