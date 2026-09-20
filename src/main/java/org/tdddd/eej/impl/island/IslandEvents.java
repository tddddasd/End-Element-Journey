package org.tdddd.eej.impl.island;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        
        if (event.getChunk() instanceof LevelChunk chunk) {
            IslandChunkManager.classify(level, chunk);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
                living.hurt(living.damageSources().fellOutOfWorld(), damage);
            }
        }
    }
}
