package org.tdddd.eej.impl.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaStorage;


/**
 * Exposes a block entity's {@link ManaStorage} through {@link EejCapabilities#MANA_STORAGE}.
 *
 * <p>Shaped after {@link MobEnchantmentProvider}: the provider wraps the block entity's own storage
 * instance (so the block entity, the capability and the saved NBT never disagree) and hands it out on
 * every face, which is what lets the creative mana source push mana into all six neighbours.</p>
 */
public class ManaStorageProvider implements ICapabilitySerializable<CompoundTag> {

    private final ManaStorage data;
    private final LazyOptional<IManaStorage> optional;

    public ManaStorageProvider(ManaStorage data) {
        this.data = data;
        this.optional = LazyOptional.of(() -> this.data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == EejCapabilities.MANA_STORAGE ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.data.save(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.data.load(nbt);
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
