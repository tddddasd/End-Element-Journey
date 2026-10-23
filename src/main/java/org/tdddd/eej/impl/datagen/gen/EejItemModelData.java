package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejItems;


public class EejItemModelData extends ItemModelProvider {

    public EejItemModelData(PackOutput output, ExistingFileHelper efh) {
        super(output, eej.MODID, efh);
    }

    @Override
    protected void registerModels() {
        // The element aggregation plate ships a hand written 3 pixel model; generating one here
        // would replace it with a plain cube (and duplicate the file).
        basicItem(EejItems.SMALL_ITEM_FRAME.get());
    }
}
