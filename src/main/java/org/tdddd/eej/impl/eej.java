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
import org.tdddd.eej.impl.element.EejElementCreativeTab;
import org.tdddd.eej.impl.element.EejElements;
import org.tdddd.eej.impl.element.ElementEvents;
import org.tdddd.eej.impl.island.IslandEvents;
import org.tdddd.eej.impl.datagen.EejDataGenEvent;
import org.tdddd.eej.impl.network.EejNetwork;
import org.tdddd.eej.impl.registry.EejArgumentTypes;
import org.tdddd.eej.impl.registry.EejBlockEntities;
import org.tdddd.eej.impl.registry.EejBlocks;
import org.tdddd.eej.impl.registry.EejCreativeTabs;
import org.tdddd.eej.impl.registry.EejDataComponents;
import org.tdddd.eej.impl.registry.EejItems;
import org.tdddd.eej.impl.registry.EejRecipeTypes;
import org.tdddd.eej.impl.soulfire.SoulFirePurificationEvents;


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
        EejRecipeTypes.register(modEventBus);

        // 1.20.1: the element table was a plain map; in 26.1.2 it is a synced data pack registry, so it must be
        // declared on the mod bus before any data pack loads.
        EejElements.register(modEventBus);

        // Adds the element items (plain + one per registered element) to the eej creative tab.
        EejElementCreativeTab.register(modEventBus);

        
        modEventBus.addListener(PackedMudPedestalBlockEntity::registerCapabilities);

        
        modEventBus.addListener(EejDataGenEvent::gatherClientData);
        modEventBus.addListener(EejDataGenEvent::gatherServerData);

        // 1.20.1: MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners)
        NeoForge.EVENT_BUS.register(this);

        
        NeoForge.EVENT_BUS.register(new IslandEvents());

        // Soul fire purification: item entity / player tick hooks for the mechanic and the 30 s fire immunity.
        NeoForge.EVENT_BUS.register(SoulFirePurificationEvents.class);

        // Element items: non-creative inventory sweep and the "never a dropped item" rule.
        NeoForge.EVENT_BUS.register(new ElementEvents());

        
        
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
