package org.tdddd.eej.impl.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * One entry of the data-driven {@code eej:element} registry.
 *
 * <p>The registry is synced to clients, so everything an element needs on the client side (currently only its
 * id, which already is the registry key) can live here.
 *
 * <p>The value is intentionally empty for now: every element is fully described by its registry id, and the
 * client texture is looked up by convention ({@code <ns>:item/element/<path>}). To grow the type later, replace
 * the unit codec in {@link #CODEC} with a {@code RecordCodecBuilder.mapCodec} whose new fields are all
 * {@code optionalFieldOf(...)} with a default; such a codec still accepts the existing {@code {}} files.
 */
public record Element() {
    /** Codec for a single element value; decodes an empty JSON object and is ready for extra optional fields. */
    public static final Codec<Element> CODEC = MapCodec.unit(Element::new).codec();
}
