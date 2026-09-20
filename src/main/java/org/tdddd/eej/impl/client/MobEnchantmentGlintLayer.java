package org.tdddd.eej.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.tdddd.eej.api.mob.IMobEnchantments;


public class MobEnchantmentGlintLayer extends RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {

    
    private static final int GLINT_ORDER = 1;

    public MobEnchantmentGlintLayer(RenderLayerParent<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
            LivingEntityRenderState state, float yRot, float xRot) {
        IMobEnchantments data = state.getRenderData(MobEnchantmentGlintHandler.MOB_ENCHANTMENTS);
        if (data == null || !data.hasAny()) {
            return;
        }

        MobEnchantmentGlintHandler.reportGlintSubmitted(state, "原版模型");
        
        
        submitNodeCollector.order(GLINT_ORDER).submitModel(
                this.getParentModel(), state, poseStack, RenderTypes.entityGlint(),
                lightCoords, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
    }
}
