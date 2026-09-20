package org.tdddd.eej.impl.island;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import org.tdddd.eej.impl.eej;

import java.util.concurrent.atomic.AtomicBoolean;


public final class IslandChunkManager {

    
    public static final int SCAN_HEIGHT = 4;
    
    public static final int ISLAND_MIN_BEDROCK = 16 * 16;
    
    public static final int VOID_GROUP_THRESHOLD = 16 * 16;
    
    public static final int VOID_RADIUS = 1;

    
    public static final int DAMAGE_INTERVAL_TICKS = 5 * 20;
    
    public static final double DAMAGE_MAX = 100.0;
    
    public static final double DAMAGE_MIN_PERCENT = 0.02;

    private static final AtomicBoolean LOGGED_FIRST = new AtomicBoolean();

    private IslandChunkManager() {
    }

    
    public static int countBedrock(ServerLevel level, LevelChunk chunk) {
        int minY = level.getMinY();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < minY + SCAN_HEIGHT; y++) {
                    pos.set(minX + x, y, minZ + z);
                    if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    
    public static int countBottomLayer(ServerLevel level, LevelChunk chunk) {
        int y = level.getMinY();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                pos.set(minX + x, y, minZ + z);
                if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                    found++;
                }
            }
        }
        return found;
    }

    
    public static IslandChunkData.ChunkRecord classify(ServerLevel level, LevelChunk chunk) {
        IslandChunkData data = IslandChunkData.get(level);
        ChunkPos pos = chunk.getPos();
        IslandChunkData.ChunkRecord existing = data.get(pos.x(), pos.z());
        if (existing != null) {
            return existing;
        }
        int bedrock = countBedrock(level, chunk);
        boolean vacuum = bedrock < ISLAND_MIN_BEDROCK;
        data.put(pos.x(), pos.z(), bedrock, vacuum);
        if (LOGGED_FIRST.compareAndSet(false, true)) {
            eej.LOGGER.info("[eej-island] {} 首个区块 {} 底部 {} 层基岩 = {}，{}",
                    level.dimension().identifier(), pos, SCAN_HEIGHT, bedrock,
                    vacuum ? "真空岛区块" : "非真空岛区块");
        }
        return data.get(pos.x(), pos.z());
    }

    
    public static boolean isVacuumIsland(ServerLevel level, ChunkPos pos) {
        IslandChunkData.ChunkRecord record = IslandChunkData.get(level).get(pos.x(), pos.z());
        return record != null && record.vacuum();
    }

    
    public static Integer voidGroupBedrock(ServerLevel level, ChunkPos center) {
        IslandChunkData data = IslandChunkData.get(level);
        int sum = 0;
        for (int dx = -VOID_RADIUS; dx <= VOID_RADIUS; dx++) {
            for (int dz = -VOID_RADIUS; dz <= VOID_RADIUS; dz++) {
                int x = center.x() + dx;
                int z = center.z() + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    return null;
                }
                IslandChunkData.ChunkRecord record = data.get(x, z);
                if (record == null) {
                    record = classify(level, chunk);
                }
                if (!record.vacuum()) {
                    sum += countBottomLayer(level, chunk);
                }
            }
        }
        return sum;
    }

    
    public static boolean isVoidChunk(ServerLevel level, ChunkPos pos) {
        if (isVacuumIsland(level, pos)) {
            return false;
        }
        LevelChunk self = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (self == null) {
            return false;
        }
        
        if (countBottomLayer(level, self) >= ISLAND_MIN_BEDROCK) {
            return false;
        }
        Integer sum = voidGroupBedrock(level, pos);
        return sum != null && sum < VOID_GROUP_THRESHOLD;
    }

    
    public static float voidDamage(ServerLevel level, double y) {
        double minY = level.getMinY();
        double height = Math.max(1.0, level.getMaxY() - minY);
        double percent = Mth.clamp((y - minY) / height, 0.0, 1.0);
        double damage = DAMAGE_MAX * (1.0 - (1.0 - DAMAGE_MIN_PERCENT) * percent);
        return (float) Mth.clamp(damage, DAMAGE_MAX * DAMAGE_MIN_PERCENT, DAMAGE_MAX);
    }

    
    public static IslandChunkData.ChunkRecord setVacuum(ServerLevel level, ChunkPos pos, boolean vacuum) {
        LevelChunk chunk = level.getChunk(pos.x(), pos.z());
        IslandChunkData.ChunkRecord record = classify(level, chunk);
        IslandChunkData data = IslandChunkData.get(level);
        data.put(pos.x(), pos.z(), record.bedrock(), vacuum);
        return data.get(pos.x(), pos.z());
    }
}
