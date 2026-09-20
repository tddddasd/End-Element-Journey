package org.tdddd.eej.api.mob;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.List;


public interface IMobEnchantments extends ValueIOSerializable {

    
    List<MobEnchantment> getAll();

    
    int getLevel(Holder<Enchantment> enchantment);

    boolean hasAny();

    
    boolean apply(Holder<Enchantment> enchantment, int level, int durationTicks);

    boolean remove(Holder<Enchantment> enchantment);

    boolean clear();

    
    boolean tickDown();

    
    void setAll(List<MobEnchantment> entries);

    void copyFrom(IMobEnchantments other);

    net.minecraft.nbt.CompoundTag save();

    void load(net.minecraft.nbt.CompoundTag tag);
}
