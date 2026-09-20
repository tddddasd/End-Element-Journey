package org.tdddd.eej.impl.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.eej;


@Mod.EventBusSubscriber(modid = eej.MODID, value = Dist.CLIENT)
public class MobEnchantmentGlintHandler {
    private static final ThreadLocal<Boolean> RENDERING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRenderLivingPost(RenderLivingEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (RENDERING.get()) return;
        if (!MobEnchantmentApi.hasAny(entity)) return;

        RENDERING.set(Boolean.TRUE);
        try {
            float partialTick = event.getPartialTick();
            float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            LivingEntityRenderer renderer = event.getRenderer();
            renderer.render(entity, yaw, partialTick, event.getPoseStack(),
                    new GlintBufferSource(event.getMultiBufferSource()), event.getPackedLight());
        } finally {
            RENDERING.set(Boolean.FALSE);
        }
    }
}
