package org.tdddd.eej.impl.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.eej;

import java.util.List;


public class EejDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, eej.MODID);

    
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> SMALL_ITEM_FRAME_IDS =
            DATA_COMPONENTS.register("item_ids", () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                    .build());

    
    public static final String LEGACY_NBT_KEY = "item_ids";

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private EejDataComponents() {
    }
}
