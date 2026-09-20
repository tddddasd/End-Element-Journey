package org.tdddd.eej.api.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;


public interface IMobEnchantments {

    
    List<MobEnchantment> getAll();

    
    int getLevel(Enchantment enchantment);

    boolean hasAny();

    
    boolean apply(Enchantment enchantment, int level, int durationTicks);

    boolean remove(Enchantment enchantment);

    boolean clear();

    
    boolean tickDown();

    
    void setAll(List<MobEnchantment> entries);

    void copyFrom(IMobEnchantments other);

    CompoundTag save();

    void load(CompoundTag tag);
}
