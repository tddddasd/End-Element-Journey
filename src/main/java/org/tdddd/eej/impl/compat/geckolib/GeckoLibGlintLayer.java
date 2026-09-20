package org.tdddd.eej.impl.compat.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.client.MobEnchantmentGlintHandler;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.network.MobEnchantmentSyncPacket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class GeckoLibGlintLayer<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState>
        extends GeoRenderLayer<T, Void, R> {

    
    private static final int GLINT_ORDER = 1;

    
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private static final int REPORT_LIMIT = 12;

    public GeckoLibGlintLayer(GeoRenderer<T, Void, R> renderer) {
        super(renderer);
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> info, SubmitNodeCollector collector) {
        if (!info.willRender()) {
            return;
        }
        R state = info.renderState();

        
        IMobEnchantments data = state.getRenderData(MobEnchantmentGlintHandler.MOB_ENCHANTMENTS);
        Entity owner = null;
        if (data == null || !data.hasAny()) {
            
            owner = findEntity(state);
            if (owner != null) {
                if (!MobEnchantmentApi.hasAny(owner)) {
                    
                    MobEnchantmentSyncPacket.applyPending(owner);
                }
                data = MobEnchantmentApi.get(owner);
            }
        }

        boolean hasData = data != null && data.hasAny();
        if (report(state, owner, hasData)) {
            eej.LOGGER.info("[eej-glint] GeckoLib layer 调用：state={}，取到实体={}，有魔咒={}",
                    state.getClass().getName(),
                    owner == null ? "无" : owner.getType().toString(),
                    hasData);
        }
        if (!hasData) {
            return;
        }

        MobEnchantmentGlintHandler.reportGlintSubmitted(state, "GeckoLib 模型");
        
        this.renderer.submitRenderTasks(info, collector.order(GLINT_ORDER), RenderTypes.entityGlint());
    }

    
    private static Entity findEntity(GeoRenderState state) {
        for (Object value : state.getDataMap().values()) {
            if (value instanceof Entity entity) {
                return entity;
            }
        }
        return null;
    }

    
    private static boolean report(Object state, Entity owner, boolean hasData) {
        if (REPORTED.size() >= REPORT_LIMIT) {
            return false;
        }
        String key = state.getClass().getName() + '|' + (owner == null ? "-" : owner.getType().toString()) + '|' + hasData;
        return REPORTED.add(key);
    }
}
