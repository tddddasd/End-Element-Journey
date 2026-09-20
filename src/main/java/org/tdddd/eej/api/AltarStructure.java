package org.tdddd.eej.api;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class AltarStructure {
    
    public final Set<BlockPos> allPositions;
    
    public final int totalPoints;
    
    public final int pedestalCount;
    
    public final boolean isValid;
    
    public final String invalidReason;
    
    public final BlockPos mainPedestal;
    
    public final List<BlockPos> pedestalPositions;
    
    public final int pedestalsWithItem;
    
    public final boolean isValidForCrafting;

    public AltarStructure(Set<BlockPos> allPositions, int totalPoints, int pedestalCount,
                          boolean isValid, String invalidReason, BlockPos mainPedestal,
                          List<BlockPos> pedestalPositions, int pedestalsWithItem,
                          boolean isValidForCrafting) {
        this.allPositions = allPositions;
        this.totalPoints = totalPoints;
        this.pedestalCount = pedestalCount;
        this.isValid = isValid;
        this.invalidReason = invalidReason;
        this.mainPedestal = mainPedestal;
        this.pedestalPositions = pedestalPositions != null ? pedestalPositions : Collections.emptyList();
        this.pedestalsWithItem = pedestalsWithItem;
        this.isValidForCrafting = isValidForCrafting;
    }
}
