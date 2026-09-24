package org.tdddd.eej.impl.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;


/**
 * Plain JEI view model of one {@code eej:soul_fire_purification} recipe.
 *
 * <p>It is built from the loaded recipes in {@link EejJeiPlugin} so the JEI category is fully data
 * driven: adding a datapack recipe automatically adds a JEI entry.
 */
public class SoulFirePurificationJeiRecipe {
    private final ItemStack input;
    private final List<Entry> outputs;
    private final float burnChance;
    private final boolean explosive;
    private final boolean destroyOnly;
    private final float explosionPower;
    private final boolean explosionFire;
    private final int blindnessTicks;

    public SoulFirePurificationJeiRecipe(ItemStack input, List<Entry> outputs, float burnChance,
                                         boolean explosive, boolean destroyOnly, float explosionPower,
                                         boolean explosionFire, int blindnessTicks) {
        this.input = input;
        this.outputs = List.copyOf(outputs);
        this.burnChance = burnChance;
        this.explosive = explosive;
        this.destroyOnly = destroyOnly;
        this.explosionPower = explosionPower;
        this.explosionFire = explosionFire;
        this.blindnessTicks = blindnessTicks;
    }

    public ItemStack getInput() {
        return input;
    }

    public List<Entry> getOutputs() {
        return outputs;
    }

    public float getBurnChance() {
        return burnChance;
    }

    public boolean isExplosive() {
        return explosive;
    }

    public boolean isDestroyOnly() {
        return destroyOnly;
    }

    public float getExplosionPower() {
        return explosionPower;
    }

    public boolean isExplosionFire() {
        return explosionFire;
    }

    public int getBlindnessTicks() {
        return blindnessTicks;
    }

    /**
     * One output roll with its resolved probability and count range.
     *
     * <p>{@link #getStacks()} holds every stack the single roll can produce (one entry per candidate
     * item), so the JEI slot can cycle through them instead of listing one slot per candidate.
     */
    public static final class Entry {
        private final List<ItemStack> stacks;
        private final float probability;
        private final int countMin;
        private final int countMax;

        public Entry(List<ItemStack> stacks, float probability, int countMin, int countMax) {
            this.stacks = List.copyOf(stacks);
            this.probability = probability;
            this.countMin = countMin;
            this.countMax = countMax;
        }

        public List<ItemStack> getStacks() {
            return stacks;
        }

        /** First candidate; kept as a convenience for single-candidate rolls. */
        public ItemStack getStack() {
            return stacks.get(0);
        }

        public float getProbability() {
            return probability;
        }

        public int getCountMin() {
            return countMin;
        }

        public int getCountMax() {
            return countMax;
        }

        /** Formatted percentage, e.g. {@code 15%} or {@code 2.5%}. */
        public String getPercentText() {
            float percent = probability * 100.0F;
            if (Math.abs(percent - Math.round(percent)) < 0.05F) {
                return Math.round(percent) + "%";
            }
            return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
        }

        /** Formatted amount, either {@code x2} or {@code x2-3}. */
        public String getCountText() {
            if (countMin == countMax) {
                return "x" + countMin;
            }
            return "x" + countMin + "-" + countMax;
        }
    }
}
