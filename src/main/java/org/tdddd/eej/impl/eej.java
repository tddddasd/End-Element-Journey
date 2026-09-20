package org.tdddd.eej.impl;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdddd.eej.impl.altar.AltarPointManager;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.capability.EejCapabilities;
import org.tdddd.eej.impl.command.EejEnchantmentCommand;
import org.tdddd.eej.impl.command.EejIslandCommand;
import org.tdddd.eej.impl.island.IslandEvents;
import org.tdddd.eej.impl.datagen.EejDataGenEvent;
import org.tdddd.eej.impl.network.EejNetwork;
import org.tdddd.eej.impl.registry.EejArgumentTypes;
import org.tdddd.eej.impl.registry.EejBlockEntities;
import org.tdddd.eej.impl.registry.EejBlocks;
import org.tdddd.eej.impl.registry.EejCreativeTabs;
import org.tdddd.eej.impl.registry.EejDataComponents;
import org.tdddd.eej.impl.registry.EejItems;


@Mod(eej.MODID)
public class eej {
    public static final String MODID = "eej";
    public static final String MOD_NAME = "End:Element Journey";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static Identifier asResource(String path) {
        // 1.20.1: new ResourceLocation(MODID, path)
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public eej(IEventBus modEventBus, ModContainer modContainer) {
        // 1.20.1: IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EejNetwork.register(modEventBus);
        EejCapabilities.register(modEventBus);
        EejDataComponents.register(modEventBus);
        EejArgumentTypes.register(modEventBus);
        EejBlocks.BLOCKS.register(modEventBus);
        EejBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        EejItems.ITEMS.register(modEventBus);
        EejCreativeTabs.CREATIVE_TABS.register(modEventBus);

        
        modEventBus.addListener(PackedMudPedestalBlockEntity::registerCapabilities);

        
        modEventBus.addListener(EejDataGenEvent::gatherClientData);
        modEventBus.addListener(EejDataGenEvent::gatherServerData);

        // 1.20.1: MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners)
        NeoForge.EVENT_BUS.register(this);

        
        NeoForge.EVENT_BUS.register(new IslandEvents());

        
        
    }

    
    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "altar_points"), new AltarPointManager());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EejEnchantmentCommand.register(event.getDispatcher());
        EejIslandCommand.register(event.getDispatcher());
    }
}
