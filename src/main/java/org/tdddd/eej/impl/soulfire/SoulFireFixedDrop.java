package org.tdddd.eej.impl.soulfire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One concrete item (with an optional count range) that a soul fire purification roll can produce.
 *
 * <p>JSON shape inside a {@code results} entry:
 * <pre>
 *   { "item": "minecraft:raw_iron", "count": 2, "count_max": 3 }
 * </pre>
 * {@code count} defaults to 1; {@code count_max} defaults to {@code count} (a fixed count). When
 * {@code count_max > count} the produced count is uniform in {@code [count, count_max]}.
 */
public record SoulFireFixedDrop(Holder<Item> item, int count, int countMax) {
    public static final Codec<SoulFireFixedDrop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Item.CODEC.fieldOf("item").forGetter(SoulFireFixedDrop::item),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(SoulFireFixedDrop::count),
            Codec.intRange(1, 99).optionalFieldOf("count_max").forGetter(drop ->
                    drop.countMax() > drop.count() ? Optional.of(drop.countMax()) : Optional.empty())
    ).apply(instance, (item, count, countMax) -> new SoulFireFixedDrop(item, count, countMax.orElse(count))));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulFireFixedDrop> STREAM_CODEC = StreamCodec.composite(
            Item.STREAM_CODEC, SoulFireFixedDrop::item,
            ByteBufCodecs.VAR_INT, SoulFireFixedDrop::count,
            ByteBufCodecs.VAR_INT, SoulFireFixedDrop::countMax,
            SoulFireFixedDrop::new);

    public SoulFireFixedDrop {
        if (countMax < count) {
            countMax = count;
        }
    }

    /** Rolls the concrete stack size for one successful purification. */
    public int rollCount(RandomSource random) {
        return countMax <= count ? count : count + random.nextInt(countMax - count + 1);
    }

    /** Builds the stack that is spawned in the world (fire immunity is attached by the caller). */
    public ItemStack createStack(RandomSource random) {
        return new ItemStack(item, rollCount(random));
    }
}
