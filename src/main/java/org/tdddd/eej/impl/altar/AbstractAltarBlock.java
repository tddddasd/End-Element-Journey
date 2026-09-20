package org.tdddd.eej.impl.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.api.AltarBlockTags;
import org.tdddd.eej.api.AltarInteractionRegistry;
import org.tdddd.eej.api.AltarItemContainer;
import org.tdddd.eej.api.AltarStructure;
import org.tdddd.eej.impl.altar.item.SmallItemFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class AbstractAltarBlock extends BaseEntityBlock {
    
    protected static final VoxelShape PEDESTAL_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    
    private final int altarPoints;
    
    private final int maxCountInStructure;

    protected AbstractAltarBlock(int altarPoints, int maxCountInStructure, Properties properties) {
        super(properties);
        this.altarPoints = altarPoints;
        this.maxCountInStructure = maxCountInStructure;
    }

    
    public int getAltarPoints() {
        return altarPoints;
    }

    
    public int getMaxCountInStructure() {
        return maxCountInStructure;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);

        
        InteractionResult external = AltarInteractionRegistry.dispatch(level, pos, state, player, hand, heldItem);
        if (external != InteractionResult.PASS) {
            return external;
        }

        
        if (heldItem.getItem() instanceof SmallItemFrame) {
            if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
                String filterData = "";
                if (heldItem.hasTag() && heldItem.getTag().contains("item_data")) {
                    filterData = heldItem.getTag().getString("item_data");
                }
                pedestal.setFilterData(SmallItemFrame.getItemIds(heldItem));
                if (filterData.isEmpty()) {
                    clearMainAltar(level, pos);
                }
                level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        
        if (heldItem.isEmpty() && player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof AltarItemContainer container) {
                if (container.hasItem()) {
                    ItemStack storedItem = container.getItem();
                    if (player.getInventory().add(storedItem)) {
                        container.clearItem();
                        player.inventoryMenu.sendAllDataToRemote();
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    } else {
                        ItemEntity itemEntity = new ItemEntity(level,
                                player.getX(), player.getY() + 0.5, player.getZ(),
                                storedItem);
                        itemEntity.setPickUpDelay(0);
                        level.addFreshEntity(itemEntity);
                        container.clearItem();
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }

        
        if (heldItem.isEmpty()) {
            AltarStructure data = findAltarStructure(level, pos);
            if (data != null && data.isValidForCrafting && data.mainPedestal != null && data.mainPedestal.equals(pos)) {
                setMainAltar(level, pos);

                ItemStack targetStack = getFrameTarget(level, pos);
                if (performCrafting(level, pos, data, targetStack)) {
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        
        if (isLocked(level, pos)) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof AltarItemContainer container) {
            if (container.hasItem()) {
                return InteractionResult.PASS;
            } else {
                ItemStack copy = heldItem.copy();
                copy.setCount(1);
                container.setItem(copy);
                heldItem.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    
    public boolean isLocked(Level level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluid = level.getFluidState(abovePos);
        if (aboveState.isAir() || !aboveFluid.isEmpty()) {
            return false;
        }
        if (!aboveState.isCollisionShapeFullBlock(level, abovePos)) {
            return false;
        }
        return true;
    }

    
    @Nullable
    public AltarStructure findAltarStructure(Level level, BlockPos startPos) {
        Block startBlock = level.getBlockState(startPos).getBlock();
        if (!isAltarBlock(startBlock)) return null;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        final int maxXOffset = 8, maxYOffset = 16, maxZOffset = 8;
        final BlockPos origin = startPos;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : getSixNeighbors(current)) {
                if (Math.abs(neighbor.getX() - origin.getX()) > maxXOffset ||
                        Math.abs(neighbor.getY() - origin.getY()) > maxYOffset ||
                        Math.abs(neighbor.getZ() - origin.getZ()) > maxZOffset) {
                    continue;
                }
                if (!visited.contains(neighbor)) {
                    Block neighborBlock = level.getBlockState(neighbor).getBlock();
                    if (isAltarBlock(neighborBlock)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        if (visited.isEmpty()) return null;

        List<BlockPos> pedestalPositions = new ArrayList<>();
        Set<BlockPos> altarStonePositions = new HashSet<>();
        Map<Block, Integer> blockCounts = new HashMap<>();
        int totalPoints = 0;

        for (BlockPos pos : visited) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            blockCounts.merge(block, 1, Integer::sum);
            totalPoints += AltarPointManager.getPoints(block);
            if (state.is(AltarBlockTags.PEDESTAL_TAG)) {
                pedestalPositions.add(pos);
            } else if (state.is(AltarBlockTags.ALTAR_STONE_TAG)) {
                altarStonePositions.add(pos);
            }
        }

        int pedestalCount = pedestalPositions.size();
        BlockPos mainPedestal = startPos;
        if (!pedestalPositions.contains(mainPedestal)) {
            return null;
        }

        String invalidReason = null;

        if (pedestalCount < 1) {
            return new AltarStructure(visited, totalPoints, pedestalCount, false, invalidReason,
                    mainPedestal, null, 0, false);
        }

        
        for (Map.Entry<Block, Integer> entry : blockCounts.entrySet()) {
            if (entry.getKey() instanceof AbstractAltarBlock altarBlock
                    && entry.getValue() > altarBlock.getMaxCountInStructure()) {
                return new AltarStructure(visited, totalPoints, pedestalCount, false, invalidReason,
                        mainPedestal, null, 0, false);
            }
        }

        for (BlockPos p : pedestalPositions) {
            BlockPos above = p.above();
            BlockState aboveState = level.getBlockState(above);
            FluidState aboveFluid = level.getFluidState(above);
            if (!aboveState.isAir() && aboveFluid.isEmpty() && aboveState.isCollisionShapeFullBlock(level, above)) {
                return new AltarStructure(visited, totalPoints, pedestalCount, false, invalidReason,
                        mainPedestal, null, 0, false);
            }
        }

        for (BlockPos p : pedestalPositions) {
            if (p.equals(mainPedestal)) continue;
            int dx = Math.abs(p.getX() - mainPedestal.getX());
            int dy = Math.abs(p.getY() - mainPedestal.getY());
            int dz = Math.abs(p.getZ() - mainPedestal.getZ());
            int maxDist = Math.max(dx, Math.max(dy, dz));
            if (maxDist < 2) {
                return new AltarStructure(visited, totalPoints, pedestalCount, false, invalidReason,
                        mainPedestal, null, 0, false);
            }
        }

        if (pedestalCount > 1 && altarStonePositions.isEmpty()) {
            return new AltarStructure(visited, totalPoints, pedestalCount, false, invalidReason,
                    mainPedestal, null, 0, false);
        }

        int itemCount = 0;
        for (BlockPos p : pedestalPositions) {
            if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                itemCount++;
            }
        }

        return new AltarStructure(visited, totalPoints, pedestalCount, true, null,
                mainPedestal, pedestalPositions, itemCount, true);
    }

    
    public boolean isAltarBlock(Block block) {
        return block.defaultBlockState().is(AltarBlockTags.PEDESTAL_TAG)
                || block.defaultBlockState().is(AltarBlockTags.ALTAR_STONE_TAG);
    }

    private Collection<BlockPos> getSixNeighbors(BlockPos pos) {
        return Arrays.asList(
                pos.north(), pos.south(), pos.west(), pos.east(),
                pos.above(), pos.below()
        );
    }

    
    public void setMainAltar(Level level, BlockPos pos) {
        AltarStructure data = findAltarStructure(level, pos);
        if (data != null) {
            for (BlockPos p : data.pedestalPositions) {
                if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal) {
                    pedestal.setMainPedestal(p.equals(pos));
                }
            }
        } else if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            pedestal.setMainPedestal(true);
        }
    }

    
    public void clearMainAltar(Level level, BlockPos pos) {
        AltarStructure data = findAltarStructure(level, pos);
        if (data != null) {
            for (BlockPos p : data.pedestalPositions) {
                if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal) {
                    pedestal.setMainPedestal(false);
                }
            }
        } else if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            pedestal.setMainPedestal(false);
        }
    }

    @Nullable
    private ItemStack getFrameTarget(Level level, BlockPos corePos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                mutable.set(corePos.getX() + dx, corePos.getY(), corePos.getZ() + dz);
                List<ItemFrame> frames = level.getEntitiesOfClass(
                        ItemFrame.class,
                        new AABB(mutable).inflate(0.1)
                );
                for (ItemFrame frame : frames) {
                    if (frame.blockPosition().equals(mutable)) {
                        ItemStack frameItem = frame.getItem();
                        if (!frameItem.isEmpty()) {
                            return frameItem.copy();
                        }
                    }
                }
            }
        }
        return null;
    }

    
    protected boolean performCrafting(Level level, BlockPos corePos, AltarStructure data,
                                      @Nullable ItemStack targetStack) {
        List<BlockPos> pedestalPositions = data.pedestalPositions;
        boolean isSingle = pedestalPositions.size() == 1;

        List<ItemStack> availableItems = new ArrayList<>();
        List<BlockPos> sourcePositions = new ArrayList<>();
        if (isSingle) {
            if (level.getBlockEntity(corePos) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                availableItems.add(pedestal.getItem().copy());
                sourcePositions.add(corePos);
            } else {
                return false;
            }
        } else {
            for (BlockPos p : pedestalPositions) {
                if (p.equals(corePos)) continue;
                if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                    availableItems.add(pedestal.getItem().copy());
                    sourcePositions.add(p);
                }
            }
            if (availableItems.isEmpty()) return false;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        List<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        CraftingRecipe matchedRecipe = null;
        List<Integer> ingredientSlots = new ArrayList<>();
        for (CraftingRecipe recipe : recipes) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            List<Integer> slots = new ArrayList<>();
            List<Ingredient> required = new ArrayList<>();
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ing = ingredients.get(i);
                if (!ing.isEmpty()) {
                    slots.add(i);
                    required.add(ing);
                }
            }
            if (required.isEmpty()) continue;
            if (!canCraft(availableItems, required)) continue;
            ItemStack result = recipe.assemble(new EmptyCraftingContainer(), level.registryAccess());
            if (result.isEmpty()) continue;
            if (targetStack != null && !matchesTarget(result, targetStack)) continue;
            matchedRecipe = recipe;
            ingredientSlots = slots;
            break;
        }
        if (matchedRecipe == null) return false;

        
        if (!(level.getBlockEntity(corePos) instanceof AltarItemContainer corePedestal)) return false;
        List<String> coreFilter = corePedestal.getFilterData();
        ItemStack result = matchedRecipe.assemble(new EmptyCraftingContainer(), level.registryAccess());
        if (!coreFilter.isEmpty() && !AltarItemContainer.matchesFilter(result, coreFilter)) return false;

        List<ItemStack> consumedStacks = new ArrayList<>();
        List<Integer> slotIndices = new ArrayList<>();
        List<Integer> sourceIndices = new ArrayList<>();
        List<ItemStack> tempItems = availableItems.stream().map(ItemStack::copy).collect(Collectors.toList());

        for (int slotIdx : ingredientSlots) {
            Ingredient ing = matchedRecipe.getIngredients().get(slotIdx);
            if (ing.isEmpty()) continue;
            boolean found = false;
            for (int i = 0; i < tempItems.size(); i++) {
                ItemStack stack = tempItems.get(i);
                if (!stack.isEmpty() && ing.test(stack)) {
                    stack.shrink(1);
                    consumedStacks.add(new ItemStack(stack.getItem(), 1));
                    slotIndices.add(slotIdx);
                    sourceIndices.add(i);
                    if (stack.isEmpty()) tempItems.set(i, ItemStack.EMPTY);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        CraftingContainer dummyContainer = new TransientCraftingContainer(
                new AbstractContainerMenu(null, 0) {
                    @Override
                    public boolean stillValid(Player p) {
                        return true;
                    }

                    @Override
                    public ItemStack quickMoveStack(Player p, int slot) {
                        return ItemStack.EMPTY;
                    }
                }, 3, 3
        );
        for (int j = 0; j < slotIndices.size(); j++) {
            int slot = slotIndices.get(j);
            dummyContainer.setItem(slot, consumedStacks.get(j).copy());
        }

        NonNullList<ItemStack> remaining = matchedRecipe.getRemainingItems(dummyContainer);

        if (isSingle) {
            for (int j = 0; j < sourceIndices.size(); j++) {
                int slot = slotIndices.get(j);
                ItemStack returnStack = remaining.get(slot);
                if (!returnStack.isEmpty()) {
                    popResource(level, corePos.above(), returnStack);
                }
            }
            corePedestal.clearItem();
            corePedestal.setItem(result);
        } else {
            for (int j = 0; j < sourceIndices.size(); j++) {
                int srcIdx = sourceIndices.get(j);
                BlockPos targetPos = sourcePositions.get(srcIdx);
                int slot = slotIndices.get(j);
                ItemStack returnStack = remaining.get(slot);

                if (level.getBlockEntity(targetPos) instanceof AltarItemContainer pedestal) {
                    pedestal.clearItem(); 
                    if (!returnStack.isEmpty()) {
                        pedestal.setItem(returnStack.copy());
                    }
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 3);
                    }
                }
            }

            if (corePedestal.hasItem()) {
                popResource(level, corePos, corePedestal.getItem());
                corePedestal.clearItem();
            }
            corePedestal.setItem(result);
            if (level instanceof ServerLevel serverLevel) {
                for (BlockPos p : pedestalPositions) {
                    serverLevel.sendBlockUpdated(p, level.getBlockState(p), level.getBlockState(p), 3);
                }
            }
        }
        return true;
    }

    private boolean canCraft(List<ItemStack> available, List<Ingredient> required) {
        List<ItemStack> remaining = available.stream().map(ItemStack::copy).collect(Collectors.toList());
        for (Ingredient ing : required) {
            boolean found = false;
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack stack = remaining.get(i);
                if (ing.test(stack)) {
                    remaining.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesTarget(ItemStack result, ItemStack target) {
        if (target.getItem().equals(result.getItem())) {
            return true;
        }
        Set<TagKey<Item>> targetTags = target.getItem().builtInRegistryHolder().tags().collect(Collectors.toSet());
        Set<TagKey<Item>> resultTags = result.getItem().builtInRegistryHolder().tags().collect(Collectors.toSet());
        targetTags.retainAll(resultTags);
        return !targetTags.isEmpty();
    }

    private static class EmptyCraftingContainer extends TransientCraftingContainer {
        public EmptyCraftingContainer() {
            super(new AbstractContainerMenu(null, 0) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }

                @Override
                public ItemStack quickMoveStack(Player p, int slot) {
                    return ItemStack.EMPTY;
                }
            }, 3, 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                popResource(level, pos, pedestal.getItem());
                pedestal.clearItem();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            return pedestal.hasItem() ? 15 : 0;
        }
        return 0;
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);
}
