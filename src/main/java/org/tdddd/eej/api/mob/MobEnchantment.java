package org.tdddd.eej.api.mob;

import net.minecraft.world.item.enchantment.Enchantment;


public final class MobEnchantment {
    
    public static final int PERMANENT = -1;

    private final Enchantment enchantment;
    private int level;
    private int remainingTicks;

    public MobEnchantment(Enchantment enchantment, int level, int remainingTicks) {
        this.enchantment = enchantment;
        this.level = Math.max(1, level);
        this.remainingTicks = remainingTicks < 0 ? PERMANENT : remainingTicks;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    
    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void setRemainingTicks(int remainingTicks) {
        this.remainingTicks = remainingTicks < 0 ? PERMANENT : remainingTicks;
    }

    public boolean isPermanent() {
        return remainingTicks == PERMANENT;
    }

    
    public int getRemainingSeconds() {
        return isPermanent() ? PERMANENT : (remainingTicks + 19) / 20;
    }

    
    public boolean tickDown() {
        if (isPermanent()) return false;
        if (remainingTicks > 0) remainingTicks--;
        return remainingTicks <= 0;
    }

    public MobEnchantment copy() {
        return new MobEnchantment(enchantment, level, remainingTicks);
    }
}
