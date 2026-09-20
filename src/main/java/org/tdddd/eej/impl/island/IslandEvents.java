package org.tdddd.eej.impl.island;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.tdddd.eej.impl.eej;

import java.util.HashMap;
import java.util.Map;


public class IslandEvents {

    
    private int tickCounter;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return; 
        }
        IslandChunkManager.classify(level, event.getChunk());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (++this.tickCounter < IslandChunkManager.DAMAGE_INTERVAL_TICKS) {
            return;
        }
        this.tickCounter = 0;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            
            Map<ChunkPos, Boolean> voidCache = new HashMap<>();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || living.isDeadOrDying()) {
                    continue;
                }
                ServerPlayer player = living instanceof ServerPlayer p ? p : null;
                if (player != null && (player.isCreative() || player.isSpectator())) {
                    continue;
                }
                ChunkPos chunk = living.chunkPosition();
                if (!voidCache.computeIfAbsent(chunk, c -> IslandChunkManager.isVoidChunk(level, c))) {
                    continue; 
                }
                float damage = IslandChunkManager.voidDamage(level, living.getY());
                if (player != null) {
                    
                    eej.LOGGER.info("[eej-island] {} 位于虚空区块 {}，虚空伤害 {} 点",
                            player.getName().getString(), chunk, damage);
                }
                living.hurtServer(level, living.damageSources().fellOutOfWorld(), damage);
            }
        }
    }
}
