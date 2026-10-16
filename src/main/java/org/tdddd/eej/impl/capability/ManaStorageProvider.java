package org.tdddd.eej.impl.capability;

import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaStorage;


/**
 * Exposes a block entity's {@link ManaStorage} through {@link EejCapabilities#MANA_STORAGE}.
 *
 * <p>Port note: in NeoForge 26.1.2 a block-entity capability is a {@code BlockCapability} registered
 * on the mod bus with a plain {@code (blockEntity, side) -> value} lambda, so no
 * {@code ICapabilitySerializable} provider object is required the way it is on 1.20.1 Forge. The
 * wrapper is kept anyway so the registration sites read the same as the 1.20.1 tree, and it always
 * hands out the block entity's own storage (so the block entity, the capability and the saved data
 * can never disagree).</p>
 */
public class ManaStorageProvider {

    private final ManaStorage data;

    public ManaStorageProvider(ManaStorage data) {
        this.data = data;
    }

    
    public IManaStorage getManaStorage() {
        return this.data;
    }

    
    public ManaStorage getData() {
        return this.data;
    }
}
