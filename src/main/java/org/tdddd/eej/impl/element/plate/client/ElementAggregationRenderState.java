package org.tdddd.eej.impl.element.plate.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Everything the element aggregation plate renderer needs, copied out of the block entity. */
public class ElementAggregationRenderState extends BlockEntityRenderState {

    /** Element ids stored in the input slots, in insertion order. */
    public final List<Identifier> elements = new ArrayList<>();

    /** Element id in the output slot, or {@code null} when it is empty. */
    public Identifier output;

    /** Game time plus partial tick, used for the orbit, bob and flicker. */
    public float time;
}
