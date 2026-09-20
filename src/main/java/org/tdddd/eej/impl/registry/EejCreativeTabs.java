package org.tdddd.eej.impl.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.eej;

public class EejCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, eej.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.eej.main_tab"))
                    .icon(() -> new ItemStack(EejItems.PACKED_MUD_PEDESTAL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(EejItems.PACKED_MUD_PEDESTAL.get());
                        output.accept(EejItems.PACKED_MUD_ALTAR_STONE.get());
                        output.accept(EejItems.SMALL_ITEM_FRAME.get());
                    })
                    .build());
}
