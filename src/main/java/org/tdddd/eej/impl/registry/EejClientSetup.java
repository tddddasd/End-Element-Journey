package org.tdddd.eej.impl.registry;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import org.tdddd.eej.impl.altar.client.PedestalItemRenderer;
import org.tdddd.eej.impl.element.client.ElementItemModel;
import org.tdddd.eej.impl.eej;


@EventBusSubscriber(modid = eej.MODID, value = Dist.CLIENT)
public class EejClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EejBlockEntities.PACKED_MUD_PEDESTAL.get(), PedestalItemRenderer::new);
    }

    /** Registers the custom item model type {@code eej:element} used by {@code assets/eej/items/element.json}. */
    @SubscribeEvent
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        event.register(Identifier.fromNamespaceAndPath(eej.MODID, "element"), ElementItemModel.Unbaked.MAP_CODEC);
    }

    /** Element sprites are resolved lazily and cached, so the cache has to be dropped on every resource reload. */
    @SubscribeEvent
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(eej.MODID, "element_model_cache"),
                ElementItemModel.RELOAD_LISTENER);
    }

    private EejClientSetup() {
    }
}
