package org.tdddd.eej.api.mana;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;


/**
 * The mana a block can hold, in the mod's mana unit (1 mana is the smallest unit).
 *
 * <p>Deliberately shaped like Forge's {@code IEnergyStorage}: the same six methods make the
 * capability usable by every pipe/machine mod that understands sided storage, while {@link #receiveMana}
 * and {@link #extractMana} are the only entry points anyone else is expected to call.</p>
 */
public interface IManaStorage {

    
    int getManaStored();

    
    int getMaxManaStored();

    
    default boolean canReceive(@Nullable Direction side) {
        return true;
    }

    
    default boolean canExtract(@Nullable Direction side) {
        return false;
    }

    
    int receiveMana(int amount, @Nullable Direction side, boolean simulate);

    
    int extractMana(int amount, @Nullable Direction side, boolean simulate);

    
    default int consume(int amount) {
        if (amount <= 0) {
            return 0;
        }
        return this.extractMana(amount, null, false);
    }

    
    default boolean isFull() {
        return this.getManaStored() >= this.getMaxManaStored();
    }

    
    default boolean isEmpty() {
        return this.getManaStored() <= 0;
    }

    
    default float getFillRatio() {
        int max = this.getMaxManaStored();
        if (max <= 0) {
            return 0.0F;
        }
        return Mth.clamp((float) this.getManaStored() / (float) max, 0.0F, 1.0F);
    }
}
