package org.tdddd.eej.impl.registry;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.altar.PackedMudAltarStone;
import org.tdddd.eej.impl.altar.PackedMudPedestal;
import org.tdddd.eej.impl.eej;

/**
 * Block registry.
 *
 * <p>Port notes: 1.20.1's {@code DeferredRegister.create(ForgeRegistries.BLOCKS, MODID)} became
 * {@link DeferredRegister#createBlocks(String)}, and {@code RegistryObject<Block>} became
 * {@link DeferredBlock}.
 *
 * <p><b>Must use {@code registerBlock}, not {@code register}.</b> In 26.1.2 a block needs an id
 * on its {@code BlockBehaviour.Properties}. Only
 * {@code DeferredRegister.Blocks#registerBlock(String, Function<Properties, B>)} calls
 * {@code properties.setId(...)}; the inherited
 * {@code register(String, Supplier)} / {@code register(String, Function<Identifier, B>)}
 * overloads do not, and registering through them aborts the whole block registry event with
 * {@code NullPointerException: Block id not set} (observed in a real {@code runData} run).
 * The {@code Function<Properties, B>} shape is also what {@code simpleCodec} expects.
 */
public class EejBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(eej.MODID);

    /** Packed mud pedestal: can hold an item stack. */
    public static final DeferredBlock<PackedMudPedestal> PACKED_MUD_PEDESTAL = BLOCKS.registerBlock(
            "packed_mud_pedestal",
            PackedMudPedestal::new
    );

    /** Packed mud altar stone: only contributes altar points. */
    public static final DeferredBlock<PackedMudAltarStone> PACKED_MUD_ALTAR_STONE = BLOCKS.registerBlock(
            "packed_mud_altar_stone",
            PackedMudAltarStone::new
    );

    private EejBlocks() {
    }
}
