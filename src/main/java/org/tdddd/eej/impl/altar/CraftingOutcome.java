package org.tdddd.eej.impl.altar;

import org.jetbrains.annotations.Nullable;


public enum CraftingOutcome {
    
    SUCCESS(null),
    
    NOT_MAIN_PEDESTAL("altar.eej.not_main_pedestal"),
    
    STRUCTURE_INVALID("altar.eej.structure_invalid"),
    
    NO_MATERIAL("altar.eej.no_material"),
    
    POINTS_INSUFFICIENT("altar.eej.points_insufficient"),
    
    NO_RECIPE("altar.eej.no_recipe");

    private final String translationKey;

    CraftingOutcome(String translationKey) {
        this.translationKey = translationKey;
    }

    
    public @Nullable String translationKey() {
        return this.translationKey;
    }
}
