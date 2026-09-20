package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejBlocks;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.stream.Stream;


public class EejBlockStateData extends ModelProvider {

    public EejBlockStateData(PackOutput output) {
        super(output, eej.MODID);
    }

    
    private static final java.util.Set<String> MANUAL_BLOCKS = java.util.Set.of("packed_mud_pedestal");

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of(
                BuiltInRegistries.BLOCK.wrapAsHolder(EejBlocks.PACKED_MUD_ALTAR_STONE.get())
        );
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of(
                BuiltInRegistries.ITEM.wrapAsHolder(EejItems.PACKED_MUD_ALTAR_STONE.get()),
                BuiltInRegistries.ITEM.wrapAsHolder(EejItems.SMALL_ITEM_FRAME.get())
        );
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        
        Block altarStone = EejBlocks.PACKED_MUD_ALTAR_STONE.get();
        blockModels.createTrivialBlock(altarStone, TexturedModel.CUBE);
        
        blockModels.registerSimpleItemModel(altarStone, net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation(altarStone));

        
        itemModels.generateFlatItem(EejItems.SMALL_ITEM_FRAME.get(), ModelTemplates.FLAT_ITEM);
    }

    @Override
    public String getName() {
        return "eej Block States and Models";
    }
}
