package org.tdddd.eej.impl.altar.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.neoforged.api.distmarker.Dist;


public class PedestalRenderState extends BlockEntityRenderState {
    
    public final ItemStackRenderState item = new ItemStackRenderState();
    
    public final ItemStackRenderState[] filters = new ItemStackRenderState[]{
            new ItemStackRenderState(), new ItemStackRenderState(),
            new ItemStackRenderState(), new ItemStackRenderState()
    };
    public boolean hasItem;
    
    public float rotation;
}
