package org.tdddd.eej.impl.soulfire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The optional "explode on contact with fire" behaviour of a soul fire purification recipe.
 *
 * <p>JSON shape (every field optional, defaults shown):
 * <pre>
 *   "explode": { "power": 4.0, "radius": 8.0, "damage": 14.0, "knockback": 0.5, "blindness_ticks": 100 }
 * </pre>
 * <ul>
 *   <li>{@code power} - the vanilla explosion power handed to {@code Level#explode}.</li>
 *   <li>{@code radius} - the radius (in blocks) of the extra damage/knockback pass and, in soul fire, of the
 *       blindness pass. Kept separate from {@code power} so it can mirror the values the block forms already
 *       use ({@code power * 2}).</li>
 *   <li>{@code damage} - maximum extra damage at the centre, falling off linearly to 0 at {@code radius}.</li>
 *   <li>{@code knockback} - horizontal knockback strength at the centre.</li>
 *   <li>{@code blindness_ticks} - Blindness I duration applied to every living entity inside {@code radius}
 *       when the trigger block is soul fire (100 ticks = 5 s). {@code 0} disables it.</li>
 * </ul>
 */
public record SoulFireExplosion(float power, float radius, float damage, float knockback, int blindnessTicks) {
    public static final Codec<SoulFireExplosion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 100.0F).optionalFieldOf("power", 4.0F).forGetter(SoulFireExplosion::power),
            Codec.floatRange(0.0F, 100.0F).optionalFieldOf("radius", 8.0F).forGetter(SoulFireExplosion::radius),
            Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("damage", 14.0F).forGetter(SoulFireExplosion::damage),
            Codec.floatRange(0.0F, 100.0F).optionalFieldOf("knockback", 0.5F).forGetter(SoulFireExplosion::knockback),
            Codec.intRange(0, 20 * 60 * 60).optionalFieldOf("blindness_ticks", 100)
                    .forGetter(SoulFireExplosion::blindnessTicks)
    ).apply(instance, SoulFireExplosion::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulFireExplosion> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SoulFireExplosion::power,
            ByteBufCodecs.FLOAT, SoulFireExplosion::radius,
            ByteBufCodecs.FLOAT, SoulFireExplosion::damage,
            ByteBufCodecs.FLOAT, SoulFireExplosion::knockback,
            ByteBufCodecs.VAR_INT, SoulFireExplosion::blindnessTicks,
            SoulFireExplosion::new);
}
