package org.tdddd.eej.api.mob;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MobEnchantments implements IMobEnchantments {
    
    public static final String DATA_KEY = "eej_mob_enchantments";

    private final Map<Holder<Enchantment>, MobEnchantment> entries = new LinkedHashMap<>();

    @Override
    public List<MobEnchantment> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries.values()));
    }

    @Override
    public int getLevel(Holder<Enchantment> enchantment) {
        MobEnchantment entry = entries.get(enchantment);
        return entry == null ? 0 : entry.getLevel();
    }

    @Override
    public boolean hasAny() {
        return !entries.isEmpty();
    }

    @Override
    public boolean apply(Holder<Enchantment> enchantment, int level, int durationTicks) {
        if (enchantment == null || level <= 0) return false;
        MobEnchantment existing = entries.get(enchantment);
        int newLevel = existing == null ? level : Math.max(existing.getLevel(), level);
        int newDuration = mergeDuration(existing, durationTicks);
        if (existing != null && existing.getLevel() == newLevel && existing.getRemainingTicks() == newDuration) {
            return false;
        }
        entries.put(enchantment, new MobEnchantment(enchantment, newLevel, newDuration));
        return true;
    }

    
    private static int mergeDuration(MobEnchantment existing, int requestedTicks) {
        if (existing == null) {
            return requestedTicks < 0 ? MobEnchantment.PERMANENT : requestedTicks;
        }
        if (existing.isPermanent() || requestedTicks < 0) {
            return MobEnchantment.PERMANENT;
        }
        return Math.max(existing.getRemainingTicks(), requestedTicks);
    }

    @Override
    public boolean remove(Holder<Enchantment> enchantment) {
        return entries.remove(enchantment) != null;
    }

    @Override
    public boolean clear() {
        if (entries.isEmpty()) return false;
        entries.clear();
        return true;
    }

    @Override
    public boolean tickDown() {
        if (entries.isEmpty()) return false;
        return entries.values().removeIf(MobEnchantment::tickDown);
    }

    @Override
    public void setAll(List<MobEnchantment> list) {
        entries.clear();
        if (list != null) {
            for (MobEnchantment entry : list) {
                if (entry != null && entry.getEnchantment() != null) {
                    entries.put(entry.getEnchantment(), entry.copy());
                }
            }
        }
    }

    @Override
    public void copyFrom(IMobEnchantments other) {
        setAll(other == null ? List.of() : other.getAll());
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (MobEnchantment entry : entries.values()) {
            Identifier id = enchantmentRegistry().getKey(entry.getEnchantment().value());
            if (id == null) continue;
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", id.toString());
            entryTag.putInt("level", entry.getLevel());
            entryTag.putInt("ticks", entry.getRemainingTicks());
            list.add(entryTag);
        }
        tag.put("Enchantments", list);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        entries.clear();
        if (tag == null) return;
        
        
        ListTag list = tag.getListOrEmpty("Enchantments");
        Registry<Enchantment> registry = enchantmentRegistry();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompoundOrEmpty(i);
            
            Identifier id = Identifier.tryParse(entryTag.getStringOr("id", ""));
            if (id == null) continue;
            Holder<Enchantment> enchantment = registry.get(id).orElse(null);
            if (enchantment == null) continue;
            int level = Math.max(1, entryTag.getIntOr("level", 1));
            int ticks = entryTag.contains("ticks") ? entryTag.getIntOr("ticks", MobEnchantment.PERMANENT) : MobEnchantment.PERMANENT;
            entries.put(enchantment, new MobEnchantment(enchantment, level, ticks));
        }
    }

    
    private static Registry<Enchantment> enchantmentRegistry() {
        return org.tdddd.eej.impl.registry.EejEnchantments.registry();
    }

    

    @Override
    public void serialize(ValueOutput output) {
        output.store(DATA_KEY, CompoundTag.CODEC, save());
    }

    @Override
    public void deserialize(ValueInput input) {
        CompoundTag tag = input.read(DATA_KEY, CompoundTag.CODEC).orElse(null);
        load(tag == null ? new CompoundTag() : tag);
    }
}
