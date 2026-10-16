package org.tdddd.eej.impl.capability;

import java.util.function.Supplier;

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
 * <p>Shaped after {@link MobEnchantmentProvider}: the provider hands the block entity's own storage
 * out on every face, which is what lets the creative mana source push mana into all six neighbours.</p>
 *
 * <p><b>The storage is resolved through a supplier, never captured.</b> Forge fires
 * {@code AttachCapabilitiesEvent<BlockEntity>} while the block entity is still inside its
 * {@code super(...)} call, i.e. before its own field initialisers have run. Capturing
 * {@code getManaStorage()} at attach time therefore captured {@code null}, which made
 * {@link #serializeNBT()} throw while the world was saved (crash report: "Exception ticking world")
 * and left the tooltip without any mana to show. Resolving on use fixes both.</p>
 */
public class ManaStorageProvider implements ICapabilitySerializable<CompoundTag> {

    private final Supplier<ManaStorage> storageSupplier;
    private LazyOptional<IManaStorage> optional = LazyOptional.empty();

    public ManaStorageProvider(Supplier<ManaStorage> storageSupplier) {
        this.storageSupplier = storageSupplier;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability != EejCapabilities.MANA_STORAGE) {
            return LazyOptional.empty();
        }
        ManaStorage storage = this.storageSupplier.get();
        if (storage == null) {
            return LazyOptional.empty();
        }
        if (!this.optional.isPresent()) {
            this.optional = LazyOptional.of(() -> storage);
        }
        return this.optional.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        ManaStorage storage = this.storageSupplier.get();
        return storage == null ? new CompoundTag() : storage.save(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ManaStorage storage = this.storageSupplier.get();
        if (storage != null) {
            storage.load(nbt);
        }
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
