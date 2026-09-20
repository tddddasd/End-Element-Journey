package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.stream.Stream;


public class EejItemModelData extends ModelProvider {

    public EejItemModelData(PackOutput output) {
        super(output, eej.MODID);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        
        return Stream.empty();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of(
                BuiltInRegistries.ITEM.wrapAsHolder(EejItems.SMALL_ITEM_FRAME.get())
        );
    }

    @Override
    protected void registerModels(net.minecraft.client.data.models.BlockModelGenerators blockModels,
                                  ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(EejItems.SMALL_ITEM_FRAME.get(), ModelTemplates.FLAT_ITEM);
    }

    @Override
    public String getName() {
        return "eej Item Models";
    }
}
