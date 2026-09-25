package org.tdddd.eej.impl.element.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.impl.eej;

/**
 * Client side wiring of the element model.
 *
 * <p>Registers the {@code eej:element} model loader and drops the sprite and per-element model
 * caches whenever the block atlas has been stitched again, so a texture pack change is picked up
 * without restarting the game.</p>
 */
@Mod.EventBusSubscriber(modid = eej.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElementClientSetup {
    private ElementClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ElementGeometry.LOADER_ID.getPath(), ElementGeometry.loader());
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Post event) {
        ElementSprites.invalidate();
        ElementGeometry.invalidate();
    }
}
