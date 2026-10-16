package org.tdddd.eej.impl.mana;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.api.mana.ManaStorage;


/**
 * The creative mana source's storage: always exactly full, and an infinite supply for anything that
 * pulls from it. The numbers themselves are never stored or changed, so the Jade bar always reads
 * {@code 2147483647/2147483647}.
 */
public class CreativeManaStorage extends ManaStorage {

    public CreativeManaStorage() {
        super(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public int getManaStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxManaStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFull() {
        return true;
    }

    @Override
    public boolean canReceive(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canExtract(@Nullable Direction side) {
        return true;
    }

    
    @Override
    public int receiveMana(int amount, @Nullable Direction side, boolean simulate) {
        return 0;
    }

    
    @Override
    public int extractMana(int amount, @Nullable Direction side, boolean simulate) {
        return Math.max(0, amount);
    }

    @Override
    public int consume(int amount) {
        return Math.max(0, amount);
    }

    @Override
    public int setMana(int value) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int setManaSilently(int value) {
        return Integer.MAX_VALUE;
    }
}
