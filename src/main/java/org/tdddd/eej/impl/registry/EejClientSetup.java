package org.tdddd.eej.impl.registry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.tdddd.eej.impl.altar.client.PedestalItemRenderer;
import org.tdddd.eej.impl.eej;


@EventBusSubscriber(modid = eej.MODID, value = Dist.CLIENT)
public class EejClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EejBlockEntities.PACKED_MUD_PEDESTAL.get(), PedestalItemRenderer::new);
    }

    private EejClientSetup() {
    }
}
