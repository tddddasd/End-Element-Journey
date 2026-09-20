package org.tdddd.eej.impl.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.tdddd.eej.impl.datagen.gen.EejBlockStateData;
import org.tdddd.eej.impl.datagen.gen.EejBlockTagData;
import org.tdddd.eej.impl.datagen.gen.EejItemModelData;
import org.tdddd.eej.impl.datagen.gen.EejLootTableData;
import org.tdddd.eej.impl.datagen.gen.EejRecipeData;
import org.tdddd.eej.impl.datagen.gen.lang.EejLangCN;
import org.tdddd.eej.impl.datagen.gen.lang.EejLangEN;


public final class EejDataGenEvent {

    private EejDataGenEvent() {
    }

    
    public static void gatherClientData(GatherDataEvent.Client event) {
        PackOutput out = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(true, new EejBlockStateData(out));
        event.getGenerator().addProvider(true, new EejItemModelData(out));
        event.getGenerator().addProvider(true, new EejLangCN(out, "zh_cn"));
        event.getGenerator().addProvider(true, new EejLangEN(out, "en_us"));
    }

    
    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput out = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(true, new EejBlockTagData(out, event.getLookupProvider()));
        event.getGenerator().addProvider(true, new EejRecipeData.Runner(out, event.getLookupProvider()));
        event.getGenerator().addProvider(true, new EejLootTableData(out, event.getLookupProvider()));
        event.getGenerator().addProvider(true, new EejAltarPointData(out));
    }
}
