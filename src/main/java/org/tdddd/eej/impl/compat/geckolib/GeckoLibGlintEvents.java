package org.tdddd.eej.impl.compat.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoRenderer;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.client.GlintBufferSource;


public class GeckoLibGlintEvents {

    @SubscribeEvent
    public static void onGeoEntityPost(GeoRenderEvent.Entity.Post event) {
        renderGlint(event.getRenderer(), event.getEntity(), event.getModel(), event.getPoseStack(),
                event.getBufferSource(), event.getPartialTick(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onGeoReplacedEntityPost(GeoRenderEvent.ReplacedEntity.Post event) {
        renderGlint(event.getRenderer(), event.getRenderer().getCurrentEntity(), event.getModel(), event.getPoseStack(),
                event.getBufferSource(), event.getPartialTick(), event.getPackedLight());
    }

    private static void renderGlint(GeoRenderer<?> renderer, Entity entity, BakedGeoModel model,
                                    PoseStack poseStack, MultiBufferSource bufferSource,
                                    float partialTick, int packedLight) {
        if (renderer == null || model == null || entity == null) return;
        if (!MobEnchantmentApi.hasAny(entity)) return;

        GlintBufferSource glint = new GlintBufferSource(bufferSource);
        VertexConsumer buffer = glint.getBuffer(RenderType.entityGlint());
        reRenderModel(renderer, model, poseStack, glint, buffer, partialTick, packedLight);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void reRenderModel(GeoRenderer renderer, BakedGeoModel model, PoseStack poseStack,
                                      MultiBufferSource bufferSource, VertexConsumer buffer,
                                      float partialTick, int packedLight) {
        renderer.reRender(model, poseStack, bufferSource, renderer.getAnimatable(),
                RenderType.entityGlint(), buffer, partialTick, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
