package org.tdddd.eej.impl.island;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class IslandChunkData extends SavedData {

    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("eej", "island_chunks");

    
    public record ChunkRecord(int x, int z, int bedrock, boolean vacuum) {
        public static final Codec<ChunkRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(ChunkRecord::x),
                Codec.INT.fieldOf("z").forGetter(ChunkRecord::z),
                Codec.INT.fieldOf("bedrock").forGetter(ChunkRecord::bedrock),
                Codec.BOOL.fieldOf("vacuum").forGetter(ChunkRecord::vacuum)
        ).apply(i, ChunkRecord::new));
    }

    public static final Codec<IslandChunkData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ChunkRecord.CODEC.listOf().optionalFieldOf("chunks", List.of()).forGetter(IslandChunkData::records)
    ).apply(i, IslandChunkData::new));

    public static final SavedDataType<IslandChunkData> TYPE =
            new SavedDataType<>(DATA_ID, IslandChunkData::new, CODEC);

    private final Map<Long, ChunkRecord> chunks = new HashMap<>();

    public IslandChunkData() {
    }

    private IslandChunkData(List<ChunkRecord> list) {
        for (ChunkRecord record : list) {
            chunks.put(key(record.x(), record.z()), record);
        }
    }

    private List<ChunkRecord> records() {
        return new ArrayList<>(chunks.values());
    }

    
    public static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public static IslandChunkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    
    public ChunkRecord get(int x, int z) {
        return chunks.get(key(x, z));
    }

    public void put(int x, int z, int bedrock, boolean vacuum) {
        chunks.put(key(x, z), new ChunkRecord(x, z, bedrock, vacuum));
        setDirty();
    }
}
