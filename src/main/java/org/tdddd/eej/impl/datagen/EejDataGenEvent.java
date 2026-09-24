package org.tdddd.eej.impl.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.impl.datagen.gen.EejBlockStateData;
import org.tdddd.eej.impl.datagen.gen.EejBlockTagData;
import org.tdddd.eej.impl.datagen.gen.EejItemModelData;
import org.tdddd.eej.impl.datagen.gen.EejLootTableData;
import org.tdddd.eej.impl.datagen.gen.EejRecipeData;
import org.tdddd.eej.impl.datagen.gen.lang.EejLangCN;
import org.tdddd.eej.impl.datagen.gen.lang.EejLangEN;
import org.tdddd.eej.impl.eej;


@Mod.EventBusSubscriber(modid = eej.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EejDataGenEvent {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        ExistingFileHelper efh = event.getExistingFileHelper();
        PackOutput out = event.getGenerator().getPackOutput();
        var lookupProvider = event.getLookupProvider();

        
        event.getGenerator().addProvider(event.includeClient(), new EejBlockStateData(out, efh));
        event.getGenerator().addProvider(event.includeClient(), new EejItemModelData(out, efh));
        event.getGenerator().addProvider(event.includeClient(), new EejLangCN(out, "zh_cn"));
        event.getGenerator().addProvider(event.includeClient(), new EejLangEN(out, "en_us"));

        
        event.getGenerator().addProvider(event.includeServer(), new EejBlockTagData(out, lookupProvider, efh));
        event.getGenerator().addProvider(event.includeServer(), new EejRecipeData(out));
        event.getGenerator().addProvider(event.includeServer(), new EejLootTableData(out));

        
        event.getGenerator().addProvider(event.includeServer(), new EejAltarPointData(out));

        
        event.getGenerator().addProvider(event.includeServer(), new EejSoulFireDocData(out));
    }
}
