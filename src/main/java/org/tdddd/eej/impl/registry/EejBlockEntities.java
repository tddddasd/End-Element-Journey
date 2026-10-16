package org.tdddd.eej.impl.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;

public class EejBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, eej.MODID);

    public static final RegistryObject<BlockEntityType<PackedMudPedestalBlockEntity>> PACKED_MUD_PEDESTAL =
            BLOCK_ENTITIES.register("packed_mud_pedestal", () ->
                    BlockEntityType.Builder.of(PackedMudPedestalBlockEntity::new, EejBlocks.PACKED_MUD_PEDESTAL.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<CreativeManaSourceBlockEntity>> CREATIVE_MANA_SOURCE =
            BLOCK_ENTITIES.register("creative_mana_source", () ->
                    BlockEntityType.Builder.of(CreativeManaSourceBlockEntity::new, EejBlocks.CREATIVE_MANA_SOURCE.get())
                            .build(null));
}
