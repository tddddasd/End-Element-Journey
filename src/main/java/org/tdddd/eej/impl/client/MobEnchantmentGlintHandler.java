package org.tdddd.eej.impl.client;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.compat.geckolib.GeckoLibGlintCompat;
import org.tdddd.eej.impl.eej;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@EventBusSubscriber(modid = eej.MODID, value = Dist.CLIENT)
public class MobEnchantmentGlintHandler {

    
    public static final ContextKey<IMobEnchantments> MOB_ENCHANTMENTS =
            new ContextKey<>(Identifier.fromNamespaceAndPath(eej.MODID, "mob_enchantments"));

    
    private static final Set<LivingEntityRenderer<?, ?, ?>> LAYER_INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<LivingEntityRenderer<?, ?, ?>, Boolean>());

    
    private static final Set<Class<?>> UNSUPPORTED_REPORTED = new HashSet<>();

    
    private static final int DIAG_LIMIT = 6;

    private static final AtomicInteger MODIFIER_LOGGED = new AtomicInteger();

    private static final AtomicInteger DATA_LOGGED = new AtomicInteger();

    private static final AtomicInteger SUMMARY_LOGGED = new AtomicInteger();

    private static final Map<String, AtomicInteger> SUBMIT_LOGGED = new ConcurrentHashMap<>();

    
    @SubscribeEvent
    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        
        
        event.registerEntityModifier(
                new TypeToken<EntityRenderer<Entity, EntityRenderState>>() {},
                (entity, renderState) -> {
                    if (MODIFIER_LOGGED.incrementAndGet() <= 3) {
                        eej.LOGGER.info("[eej-glint] render state modifier 已运行：实体 {} → state {}（有魔咒={}）",
                                entity.getType(), renderState.getClass().getName(), MobEnchantmentApi.hasAny(entity));
                    }
                    
                    if (!MobEnchantmentApi.hasAny(entity)
                            && !org.tdddd.eej.impl.network.MobEnchantmentSyncPacket.applyPending(entity)) {
                        return;
                    }
                    renderState.setRenderData(MOB_ENCHANTMENTS, MobEnchantmentApi.get(entity));
                    if (DATA_LOGGED.incrementAndGet() <= DIAG_LIMIT) {
                        eej.LOGGER.info("[eej-glint] 实体 {} 带有魔咒 → render state {} 已写入数据快照",
                                entity.getType(), renderState.getClass().getName());
                    }
                });
    }

    
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        int vanillaInstalled = 0;
        int geckoInstalled = 0;
        int unsupported = 0;

        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(type);
            if (renderer == null) {
                continue;
            }
            
            if (geckolibPresent() && GeckoLibGlintCompat.tryInstall(renderer)) {
                geckoInstalled++;
                continue;
            }
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> living) {
                if (installLayer(living)) {
                    vanillaInstalled++;
                }
                continue;
            }
            
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                unsupported++;
                reportUnsupported(renderer.getClass());
            }
        }

        
        for (PlayerModelType skin : event.getSkins()) {
            if (installLayer(event.getPlayerRenderer(skin))) {
                vanillaInstalled++;
            }
            if (installLayer(event.getMannequinRenderer(skin))) {
                vanillaInstalled++;
            }
        }

        if (SUMMARY_LOGGED.incrementAndGet() <= 3) {
            eej.LOGGER.info("[eej-glint] 附魔光效 layer 安装完成：原版模型渲染器 {} 个、GeckoLib 渲染器 {} 个、不兼容 {} 个",
                    vanillaInstalled, geckoInstalled, unsupported);
        }
    }

    
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean installLayer(LivingEntityRenderer<?, ?, ?> renderer) {
        if (renderer == null || !LAYER_INSTALLED.add(renderer)) {
            return false;
        }
        
        
        
        ((LivingEntityRenderer) renderer).addLayer(new MobEnchantmentGlintLayer((RenderLayerParent) renderer));
        return true;
    }

    
    private static void reportUnsupported(Class<?> rendererClass) {
        if (UNSUPPORTED_REPORTED.add(rendererClass)) {
            eej.LOGGER.debug("生物附魔光效：渲染器 {} 既不是 LivingEntityRenderer 也不是 GeckoLib 的 GeoEntityRenderer，已跳过该生物的光效。",
                    rendererClass.getName());
        }
    }

    
    public static void reportGlintSubmitted(EntityRenderState state, String path) {
        AtomicInteger counter = SUBMIT_LOGGED.computeIfAbsent(path, key -> new AtomicInteger());
        if (counter.incrementAndGet() <= DIAG_LIMIT) {
            eej.LOGGER.info("[eej-glint] 已提交附魔光效（{}），render state {}", path, state.getClass().getName());
        }
    }

    
    private static boolean geckolibPresent() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("geckolib");
    }

    private MobEnchantmentGlintHandler() {
    }
}
