package org.tdddd.eej.impl.datagen.gen.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejBlocks;
import org.tdddd.eej.impl.registry.EejItems;

public class EejLangEN extends LanguageProvider {
    public EejLangEN(PackOutput output, String locale) {
        super(output, eej.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // Item group
        add("itemGroup." + eej.MODID + ".main_tab", "End:Element Journey");

        // Blocks and items
        add(EejBlocks.PACKED_MUD_PEDESTAL.get(), "Packed Mud Pedestal");
        add(EejBlocks.PACKED_MUD_ALTAR_STONE.get(), "Packed Mud Altar Stone");
        add(EejItems.SMALL_ITEM_FRAME.get(), "Small Item Filter Frame");

        // JEI category
        add("category.eej.altar_crafting", "Altar Crafting");

        // Small item frame tooltips
        add("item.small_item_frame.added", "Recorded: %s");
        add("item.small_item_frame.max_reached", "Cannot add (limit reached or duplicate)");
        add("item.small_item_frame.no_offhand", "No item in offhand");
        add("item.small_item_frame.count", "%d/%d stored");

        // Mob enchantment command
        add("commands.eej.enchantment.success", "Applied %2$s (level %3$s, %4$s) to %1$s creature(s)");
        add("commands.eej.enchantment.removed", "Removed %2$s from %1$s creature(s)");
        add("commands.eej.enchantment.cleared", "Cleared all mob enchantments from %s creature(s)");
        add("commands.eej.enchantment.unknown", "Unknown enchantment: %s");
        add("commands.eej.enchantment.unknown_target", "Unknown entity target: %s (use a selector, UUID, numeric entity id, entity type id or mod id)");
        add("commands.eej.enchantment.none", "No valid living target found");
        add("commands.eej.enchantment.no_effect", "Nothing to change on %s creature(s): already at the same or higher level with equal or longer duration, or the enchantment is not present");
        add("commands.eej.enchantment.permanent", "permanent");
        add("commands.eej.enchantment.seconds", "%s s");

        // Altar crafting feedback (AbstractAltarBlock / CraftingOutcome)
        // Argument counts must match the Component.translatable calls
        add("altar.eej.no_material", "No material on the altar");
        add("altar.eej.no_recipe", "No matching altar recipe");
        add("altar.eej.points_insufficient", "Not enough altar points (need %s, have %s)");
        add("altar.eej.not_main_pedestal", "Use the main pedestal (%s %s %s)");
        add("altar.eej.not_main_pedestal_unknown", "This is not the main pedestal");
        add("altar.eej.structure_invalid", "Invalid altar structure");
        add("altar.eej.structure_invalid_reason", "Invalid altar structure: %s");
        add("altar.eej.filter_rejected", "This item is rejected by the pedestal filter (%s)");
        add("altar.eej.filter_cleared", "Cleared this pedestal's filter");

        // JEI altar crafting category hint
        add("jei.eej.altar_crafting.hint", "Place the materials on any pedestals - order does not matter");

        // Soul fire purification
        add("category.eej.soul_fire_purification", "Soul Fire Purification");

        // Vacuum island / void chunk
        add("commands.eej.island.info", "Chunk %s,%s: bottom-layer bedrock %s, 3x3 non-vacuum bottom total %s, %s");
        add("commands.eej.island.set.vacuum", "Chunk %s,%s is now a vacuum-island chunk");
        add("commands.eej.island.set.nonvacuum", "Chunk %s,%s is now a non-vacuum-island chunk");
        add("commands.eej.island.status.vacuum", "permanent vacuum island");
        add("commands.eej.island.status.void", "void chunk");
        add("commands.eej.island.status.nonvacuum", "non-vacuum island");
        add("commands.eej.island.unknown", "not classified");
        add("commands.eej.island.needs_pos", "A position is required when run from the console");    }
}
