package org.tdddd.eej.impl.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.altar.item.SmallItemFrame;
import org.tdddd.eej.impl.element.ElementItem;
import org.tdddd.eej.impl.eej;

public class EejItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, eej.MODID);

    public static final RegistryObject<Item> PACKED_MUD_PEDESTAL = ITEMS.register(
            "packed_mud_pedestal",
            () -> new BlockItem(EejBlocks.PACKED_MUD_PEDESTAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKED_MUD_ALTAR_STONE = ITEMS.register(
            "packed_mud_altar_stone",
            () -> new BlockItem(EejBlocks.PACKED_MUD_ALTAR_STONE.get(), new Item.Properties()));

    
    public static final RegistryObject<Item> SMALL_ITEM_FRAME = ITEMS.register(
            "small_item_frame",
            () -> new SmallItemFrame(new Item.Properties()));

    public static final RegistryObject<Item> ELEMENT = ITEMS.register(
            "element",
            () -> new ElementItem(new Item.Properties()));
}
