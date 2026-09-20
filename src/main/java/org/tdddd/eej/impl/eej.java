package org.tdddd.eej.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdddd.eej.impl.altar.AltarPointManager;
import org.tdddd.eej.impl.command.EejEnchantmentCommand;
import org.tdddd.eej.impl.command.EejIslandCommand;
import org.tdddd.eej.impl.compat.geckolib.GeckoLibGlintCompat;
import org.tdddd.eej.impl.island.IslandEvents;
import org.tdddd.eej.impl.network.EejNetwork;
import org.tdddd.eej.impl.registry.EejArgumentTypes;
import org.tdddd.eej.impl.registry.EejBlockEntities;
import org.tdddd.eej.impl.registry.EejBlocks;
import org.tdddd.eej.impl.registry.EejCreativeTabs;
import org.tdddd.eej.impl.registry.EejItems;


@Mod(eej.MODID)
public class eej {
    public static final String MODID = "eej";
    public static final String MOD_NAME = "End:Element Journey";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }

    public eej() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EejNetwork.register();
        EejArgumentTypes.register(modEventBus);
        EejBlocks.BLOCKS.register(modEventBus);
        EejBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        EejItems.ITEMS.register(modEventBus);
        EejCreativeTabs.CREATIVE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        MinecraftForge.EVENT_BUS.register(new IslandEvents());

        
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("geckolib")) {
            GeckoLibGlintCompat.register();
        }
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AltarPointManager());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        EejEnchantmentCommand.register(event.getDispatcher());
        EejIslandCommand.register(event.getDispatcher());
    }
}
