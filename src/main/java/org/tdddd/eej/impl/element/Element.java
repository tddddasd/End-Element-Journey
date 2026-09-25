package org.tdddd.eej.impl.element;

import com.mojang.serialization.Codec;

import javax.annotation.Nullable;

/**
 * One entry of the synced {@code eej:element} datapack registry.
 *
 * <p>The record is intentionally empty for now: an element data file is a plain JSON object
 * ({@code {}}). Add new fields as record components and wire them into {@link #CODEC} as
 * optional fields so existing data files keep loading.</p>
 *
 * <p>{@link #CODEC} is a {@link Codec#unit} codec: it decodes any JSON value (in particular an
 * empty object) into the single {@link #INSTANCE} and always encodes back to {@code {}}. That is
 * the shape datapack registry files need, because a registry entry with no content is written as
 * an empty object. A no-field {@code RecordCodecBuilder} cannot be used here: it produces a
 * record with no components, the Java compiler rejects a zero-argument canonical constructor on a
 * record, and an empty record instance codec encodes to {@code null} rather than to {@code {}}.</p>
 */
public record Element() {
    /** The single (and currently only) value an element data file can describe. */
    public static final Element INSTANCE = new Element();

    /** Codec used for both the datapack and the network copy of the {@code eej:element} registry. */
    public static final Codec<Element> CODEC = Codec.unit(INSTANCE);

    /**
     * @param element a decoded element, may be {@code null}
     * @return the decoded element, or {@link #INSTANCE} when {@code element} is {@code null}
     */
    public static Element orDefault(@Nullable Element element) {
        return element != null ? element : INSTANCE;
    }
}
