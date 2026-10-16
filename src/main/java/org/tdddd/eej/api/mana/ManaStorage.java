package org.tdddd.eej.api.mana;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;


/**
 * Plain {@link IManaStorage} implementation: a fixed-capacity buffer with an optional change
 * callback, which is how block entities mark themselves dirty and resync their clients.
 *
 * <p>Port note (26.1.2): {@code BlockEntity#saveAdditional}/{@code loadAdditional} take
 * {@link ValueOutput}/{@link ValueInput} instead of a {@code CompoundTag}, so {@link #save} and
 * {@link #load} were re-shaped to that pair. The stored key is unchanged ({@code "Mana"}), and the
 * type is unchanged (an int), so a world written by either tree reads back identically.</p>
 */
public class ManaStorage implements IManaStorage {

    
    public static final String TAG_MANA = "Mana";

    
    protected final int capacity;

    protected int mana;

    @Nullable
    private Runnable listener;

    public ManaStorage(int capacity) {
        this(capacity, 0);
    }

    public ManaStorage(int capacity, int initialMana) {
        this.capacity = Math.max(0, capacity);
        this.mana = Mth.clamp(initialMana, 0, this.capacity);
    }

    
    public ManaStorage onChanged(@Nullable Runnable listener) {
        this.listener = listener;
        return this;
    }

    protected void onChanged() {
        if (this.listener != null) {
            this.listener.run();
        }
    }

    @Override
    public int getManaStored() {
        return this.mana;
    }

    @Override
    public int getMaxManaStored() {
        return this.capacity;
    }

    
    public int setMana(int value) {
        int clamped = Mth.clamp(value, 0, this.capacity);
        if (clamped != this.mana) {
            this.mana = clamped;
            this.onChanged();
        }
        return this.mana;
    }

    
    public int setManaSilently(int value) {
        this.mana = Mth.clamp(value, 0, this.capacity);
        return this.mana;
    }

    
    @Override
    public int consume(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, this.mana);
        if (taken > 0) {
            this.mana -= taken;
            this.onChanged();
        }
        return taken;
    }

    @Override
    public int receiveMana(int amount, @Nullable Direction side, boolean simulate) {
        if (amount <= 0 || !this.canReceive(side)) {
            return 0;
        }
        int accepted = Math.min(amount, this.capacity - this.mana);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            this.mana += accepted;
            this.onChanged();
        }
        return accepted;
    }

    @Override
    public int extractMana(int amount, @Nullable Direction side, boolean simulate) {
        if (amount <= 0 || !this.canExtract(side)) {
            return 0;
        }
        int extracted = Math.min(amount, this.mana);
        if (extracted <= 0) {
            return 0;
        }
        if (!simulate) {
            this.mana -= extracted;
            this.onChanged();
        }
        return extracted;
    }

    
    public ValueOutput save(ValueOutput output) {
        output.putInt(TAG_MANA, this.mana);
        return output;
    }

    
    public void load(ValueInput input) {
        input.getInt(TAG_MANA).ifPresent(this::setManaSilently);
    }
}
