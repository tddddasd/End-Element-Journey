package org.tdddd.eej.impl.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;


public class GlintBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;

    public GlintBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return delegate.getBuffer(isText(renderType) ? renderType : RenderTypes.entityGlint());
    }

    
    private static boolean isText(RenderType renderType) {
        return renderType.toString().startsWith("text");
    }
}
