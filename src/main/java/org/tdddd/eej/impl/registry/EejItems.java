package org.tdddd.eej.impl.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.altar.item.SmallItemFrame;
import org.tdddd.eej.impl.eej;

/**
 * Item registry.
 *
 * <p>Port notes: {@code DeferredRegister.create(ForgeRegistries.ITEMS, MODID)} became
 * {@link DeferredRegister#createItems(String)}, and {@code RegistryObject<Item>} became
 * {@link DeferredItem}.
 *
 * <p><b>Must use {@code registerItem}, not {@code register}.</b> As with blocks, 26.1.2 requires
 * an id on {@code Item.Properties}; only
 * {@code DeferredRegister.Items#registerItem(String, Function<Properties, I>)} sets it. Going
 * through {@code register(String, Supplier)} leaves the id unset (same class of failure as
 * "Block id not set").
 */
public class EejItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(eej.MODID);

    public static final DeferredItem<Item> PACKED_MUD_PEDESTAL = ITEMS.registerItem(
            "packed_mud_pedestal",
            properties -> new BlockItem(EejBlocks.PACKED_MUD_PEDESTAL.get(), properties.useBlockDescriptionPrefix()));

    public static final DeferredItem<Item> PACKED_MUD_ALTAR_STONE = ITEMS.registerItem(
            "packed_mud_altar_stone",
            properties -> new BlockItem(EejBlocks.PACKED_MUD_ALTAR_STONE.get(), properties.useBlockDescriptionPrefix()));

    /** Small item filter frame: sets the pedestal's filter condition. */
    public static final DeferredItem<Item> SMALL_ITEM_FRAME = ITEMS.registerItem(
            "small_item_frame",
            properties -> new SmallItemFrame(properties));

    private EejItems() {
    }
}
