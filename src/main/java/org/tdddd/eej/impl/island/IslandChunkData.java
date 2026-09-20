package org.tdddd.eej.impl.island;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;


public class IslandChunkData extends SavedData {

    private static final String DATA_NAME = "eej_island_chunks";

    
    public record ChunkRecord(int x, int z, int bedrock, boolean vacuum) {
    }

    private final Map<Long, ChunkRecord> chunks = new HashMap<>();

    public IslandChunkData() {
    }

    
    public IslandChunkData(CompoundTag nbt) {
        ListTag list = nbt.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int x = tag.getInt("x");
            int z = tag.getInt("z");
            chunks.put(key(x, z), new ChunkRecord(x, z, tag.getInt("bedrock"), tag.getBoolean("vacuum")));
        }
    }

    
    public static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (ChunkRecord record : chunks.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", record.x());
            tag.putInt("z", record.z());
            tag.putInt("bedrock", record.bedrock());
            tag.putBoolean("vacuum", record.vacuum());
            list.add(tag);
        }
        nbt.put("chunks", list);
        return nbt;
    }

    public static IslandChunkData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(IslandChunkData::new, IslandChunkData::new, DATA_NAME);
    }

    
    public ChunkRecord get(int x, int z) {
        return chunks.get(key(x, z));
    }

    public void put(int x, int z, int bedrock, boolean vacuum) {
        chunks.put(key(x, z), new ChunkRecord(x, z, bedrock, vacuum));
        setDirty();
    }
}
