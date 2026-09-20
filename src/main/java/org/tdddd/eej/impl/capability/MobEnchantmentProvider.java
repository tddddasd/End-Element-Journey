package org.tdddd.eej.impl.capability;

import net.minecraft.nbt.CompoundTag;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantments;


public class MobEnchantmentProvider {
    private final MobEnchantments data = new MobEnchantments();

    public IMobEnchantments getData() {
        return this.data;
    }

    public CompoundTag serializeNBT() {
        return this.data.save();
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.data.load(nbt);
    }
}
