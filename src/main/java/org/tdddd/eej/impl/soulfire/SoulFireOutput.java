package org.tdddd.eej.impl.soulfire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

/**
 * One independent drop roll of a soul fire purification recipe.
 *
 * <p>JSON shape - either a single fixed drop:
 * <pre>
 *   { "chance": 0.15, "item": "minecraft:raw_iron", "count": 2, "count_max": 3 }
 * </pre>
 * or "exactly one of these, picked uniformly at random":
 * <pre>
 *   { "chance": 0.30, "options": [ { "item": "minecraft:dirt" }, { "item": "minecraft:gravel" } ] }
 * </pre>
 * {@code count} defaults to 1 and {@code count_max} defaults to {@code count}. When both {@code item} and
 * {@code options} are present {@code options} wins. Every entry rolls on its own, so a recipe with two entries
 * can produce up to two drops.
 */
public record SoulFireOutput(float chance, Optional<Holder<Item>> item, int count, int countMax,
                             List<SoulFireFixedDrop> options) {
    public static final Codec<SoulFireOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(SoulFireOutput::chance),
            Item.CODEC.optionalFieldOf("item").forGetter(SoulFireOutput::item),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(SoulFireOutput::count),
            Codec.intRange(1, 99).optionalFieldOf("count_max").forGetter(output ->
                    output.countMax() > output.count() ? Optional.of(output.countMax()) : Optional.empty()),
            SoulFireFixedDrop.CODEC.listOf().optionalFieldOf("options", List.of()).forGetter(SoulFireOutput::options)
    ).apply(instance, (chance, item, count, countMax, options) ->
            new SoulFireOutput(chance, item, count, countMax.orElse(count), options)));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulFireOutput> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SoulFireOutput::chance,
            ByteBufCodecs.optional(Item.STREAM_CODEC), SoulFireOutput::item,
            ByteBufCodecs.VAR_INT, SoulFireOutput::count,
            ByteBufCodecs.VAR_INT, SoulFireOutput::countMax,
            SoulFireFixedDrop.STREAM_CODEC.apply(ByteBufCodecs.list()), SoulFireOutput::options,
            SoulFireOutput::new);

    public SoulFireOutput {
        if (countMax < count) {
            countMax = count;
        }
    }

    /** All concrete drops this roll can produce; more than one means "pick one uniformly at random". */
    public List<SoulFireFixedDrop> drops() {
        if (!options.isEmpty()) {
            return options;
        }
        return item.map(held -> List.of(new SoulFireFixedDrop(held, count, countMax))).orElse(List.of());
    }

    /** @return the drop to spawn, or {@code null} when this entry is malformed/empty */
    public SoulFireFixedDrop pick(RandomSource random) {
        List<SoulFireFixedDrop> available = drops();
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }
}
