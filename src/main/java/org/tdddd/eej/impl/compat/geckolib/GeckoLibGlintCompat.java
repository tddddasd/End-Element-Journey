package org.tdddd.eej.impl.compat.geckolib;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.tdddd.eej.impl.eej;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;


public final class GeckoLibGlintCompat {

    
    private static final Set<EntityRenderer<?, ?>> INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<EntityRenderer<?, ?>, Boolean>());

    private GeckoLibGlintCompat() {
    }

    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean tryInstall(EntityRenderer<?, ?> renderer) {
        if (!(renderer instanceof GeoEntityRenderer<?, ?> geo)) {
            return false;
        }
        if (INSTALLED.add(renderer)) {
            
            ((GeoEntityRenderer) geo).withRenderLayer(new GeckoLibGlintLayer((GeoRenderer) geo));
            eej.LOGGER.debug("生物附魔光效：已给 GeckoLib 渲染器 {} 挂上附魔光效 layer。", renderer.getClass().getName());
        }
        return true;
    }
}
