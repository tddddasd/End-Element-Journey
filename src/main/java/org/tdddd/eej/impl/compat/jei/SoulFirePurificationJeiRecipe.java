package org.tdddd.eej.impl.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.tdddd.eej.impl.soulfire.SoulFireFixedDrop;
import org.tdddd.eej.impl.soulfire.SoulFireOutput;
import org.tdddd.eej.impl.soulfire.SoulFirePurificationRecipe;

/**
 * Display-only wrapper for one loaded {@code eej:soul_fire_purification} recipe.
 *
 * <p>Built from the synced recipe content, so the JEI category is fully data driven: whatever the data packs
 * ship shows up without any code change.
 */
public class SoulFirePurificationJeiRecipe {
    /** One drop roll: all candidate stacks of one {@code results} entry plus its chance. */
    public record OutputEntry(List<ItemStack> displayStacks, int minCount, int maxCount, float chance) {
        public boolean hasCountRange() {
            return maxCount > minCount;
        }
    }

    private final List<ItemStack> inputs;
    private final List<OutputEntry> entries;
    private final float burnChance;
    private final boolean explosive;

    private SoulFirePurificationJeiRecipe(List<ItemStack> inputs, List<OutputEntry> entries, float burnChance,
                                          boolean explosive) {
        this.inputs = inputs;
        this.entries = entries;
        this.burnChance = burnChance;
        this.explosive = explosive;
    }

    /** @return the wrapper, or {@code null} when the recipe has no resolvable input stacks */
    public static SoulFirePurificationJeiRecipe of(SoulFirePurificationRecipe recipe) {
        List<ItemStack> inputs = new ArrayList<>();
        for (Holder<Item> item : recipe.ingredient().items().toList()) {
            inputs.add(new ItemStack(item));
        }
        if (inputs.isEmpty()) {
            return null;
        }

        List<OutputEntry> entries = new ArrayList<>();
        for (SoulFireOutput output : recipe.results()) {
            List<ItemStack> stacks = new ArrayList<>();
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (SoulFireFixedDrop drop : output.drops()) {
                stacks.add(new ItemStack(drop.item(), drop.count()));
                min = Math.min(min, drop.count());
                max = Math.max(max, drop.countMax());
            }
            if (stacks.isEmpty()) {
                continue;
            }
            entries.add(new OutputEntry(stacks, min, max, output.chance()));
        }
        return new SoulFirePurificationJeiRecipe(inputs, entries, recipe.burnChance(),
                recipe.explosion().isPresent());
    }

    public List<ItemStack> getInputs() {
        return inputs;
    }

    public List<OutputEntry> getEntries() {
        return entries;
    }

    public float getBurnChance() {
        return burnChance;
    }

    public boolean isExplosive() {
        return explosive;
    }
}
