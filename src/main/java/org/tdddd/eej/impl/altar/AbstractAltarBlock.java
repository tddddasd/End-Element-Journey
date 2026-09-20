package org.tdddd.eej.impl.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        
        if (itemStack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        
        InteractionResult external = AltarInteractionRegistry.dispatch(level, pos, state, player, hand, itemStack);
        if (external != InteractionResult.PASS) {
            return external;
        }

        
        
        if (itemStack.getItem() instanceof SmallItemFrame) {
            if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
                
                
                setMainAltar(level, pos);
                if (player.isShiftKeyDown()) {
                    
                    pedestal.setFilterData(Collections.emptyList());
                    sendFeedback(level, player, Component.translatable("altar.eej.filter_cleared"));
                } else {
                    List<String> ids = SmallItemFrame.getItemIds(itemStack);
                    pedestal.setFilterData(ids);
                }
                level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
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
                
                
                if (!container.acceptsInsertion(itemStack)) {
                    sendFeedback(level, player, Component.translatable("altar.eej.filter_rejected",
                            String.join(", ", container.getFilterData())));
                    if (!level.isClientSide()) {
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.5F, 0.7F);
                    }
                    return InteractionResult.SUCCESS;
                }
                ItemStack copy = itemStack.copy();
                copy.setCount(1);
                container.setItem(copy);
                itemStack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        
        
        
        
        InteractionResult external = AltarInteractionRegistry.dispatch(
                level, pos, state, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        if (external != InteractionResult.PASS) {
            return external;
        }

        
        if (player.isShiftKeyDown()) {
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

        
        AltarStructure data = findAltarStructure(level, pos);
        if (data == null || data.mainPedestal == null) {
            return InteractionResult.PASS;
        }

        if (!data.isValidForCrafting) {
            
            notifyFailure(level, pos, player, CraftingOutcome.STRUCTURE_INVALID, data);
            return InteractionResult.SUCCESS;
        }

        if (!data.mainPedestal.equals(pos)) {
            
            
            if (data.pedestalsWithItem > 0) {
                notifyFailure(level, pos, player, CraftingOutcome.NOT_MAIN_PEDESTAL, data);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        setMainAltar(level, pos);

        ItemStack targetStack = getFrameTarget(level, pos);
        CraftingOutcome outcome = performCrafting(level, pos, data, targetStack);
        if (outcome == CraftingOutcome.SUCCESS) {
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        
        
        notifyFailure(level, pos, player, outcome, data);
        return InteractionResult.SUCCESS;
    }

    
    private void notifyFailure(Level level, BlockPos pos, Player player, CraftingOutcome outcome, AltarStructure data) {
        Component message = switch (outcome) {
            case SUCCESS -> null;
            case NO_MATERIAL -> Component.translatable("altar.eej.no_material");
            case NO_RECIPE -> Component.translatable("altar.eej.no_recipe");
            case POINTS_INSUFFICIENT -> Component.translatable("altar.eej.points_insufficient",
                    data.totalPoints, data.requiredPoints);
            case NOT_MAIN_PEDESTAL -> data.mainPedestal == null
                    ? Component.translatable("altar.eej.not_main_pedestal_unknown")
                    : Component.translatable("altar.eej.not_main_pedestal",
                            data.mainPedestal.getX(), data.mainPedestal.getY(), data.mainPedestal.getZ());
            case STRUCTURE_INVALID -> data.invalidReason == null || data.invalidReason.isEmpty()
                    ? Component.translatable(CraftingOutcome.STRUCTURE_INVALID.translationKey())
                    : Component.translatable("altar.eej.structure_invalid_reason", data.invalidReason);
        };
        if (message == null) {
            return;
        }
        sendFeedback(level, player, message);
        
        
        if (!level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.5F, 0.7F);
        }
    }

    
    private static void sendFeedback(Level level, Player player, Component message) {
        if (level.isClientSide()) return;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
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
        if (pedestalCount < 1 || !pedestalPositions.contains(startPos)) {
            
            return null;
        }
        BlockPos mainPedestal = lowestPedestal(pedestalPositions);
        int requiredPoints = AltarPointManager.getRequiredPoints();

        
        for (Map.Entry<Block, Integer> entry : blockCounts.entrySet()) {
            if (entry.getKey() instanceof AbstractAltarBlock altarBlock
                    && entry.getValue() > altarBlock.getMaxCountInStructure()) {
                String reason = "祭坛方块数量超过单结构上限：" + entry.getKey().getName().getString()
                        + " × " + entry.getValue() + " > " + altarBlock.getMaxCountInStructure();
                return new AltarStructure(visited, totalPoints, pedestalCount, false, reason,
                        mainPedestal, null, 0, false, requiredPoints);
            }
        }

        for (BlockPos p : pedestalPositions) {
            BlockPos above = p.above();
            BlockState aboveState = level.getBlockState(above);
            FluidState aboveFluid = level.getFluidState(above);
            if (!aboveState.isAir() && aboveFluid.isEmpty() && aboveState.isCollisionShapeFullBlock(level, above)) {
                String reason = "祭台上方被方块阻挡：" + p.getX() + ", " + p.getY() + ", " + p.getZ();
                return new AltarStructure(visited, totalPoints, pedestalCount, false, reason,
                        mainPedestal, null, 0, false, requiredPoints);
            }
        }

        for (BlockPos p : pedestalPositions) {
            if (p.equals(mainPedestal)) continue;
            int dx = Math.abs(p.getX() - mainPedestal.getX());
            int dy = Math.abs(p.getY() - mainPedestal.getY());
            int dz = Math.abs(p.getZ() - mainPedestal.getZ());
            int maxDist = Math.max(dx, Math.max(dy, dz));
            if (maxDist < 2) {
                String reason = "祭台间距不足 2 格：" + p.getX() + ", " + p.getY() + ", " + p.getZ();
                return new AltarStructure(visited, totalPoints, pedestalCount, false, reason,
                        mainPedestal, null, 0, false, requiredPoints);
            }
        }

        if (pedestalCount > 1 && altarStonePositions.isEmpty()) {
            return new AltarStructure(visited, totalPoints, pedestalCount, false,
                    "多祭台结构必须至少有一块祭坛石",
                    mainPedestal, null, 0, false, requiredPoints);
        }

        int itemCount = 0;
        for (BlockPos p : pedestalPositions) {
            if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                itemCount++;
            }
        }

        return new AltarStructure(visited, totalPoints, pedestalCount, true, null,
                mainPedestal, pedestalPositions, itemCount, true, requiredPoints);
    }

    
    private static BlockPos lowestPedestal(List<BlockPos> pedestals) {
        BlockPos best = null;
        for (BlockPos p : pedestals) {
            if (best == null || comparePosition(p, best) < 0) {
                best = p;
            }
        }
        return best;
    }

    
    private static int comparePosition(BlockPos a, BlockPos b) {
        int result = Integer.compare(a.getX(), b.getX());
        if (result != 0) return result;
        result = Integer.compare(a.getY(), b.getY());
        if (result != 0) return result;
        return Integer.compare(a.getZ(), b.getZ());
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
        if (data != null && data.mainPedestal != null) {
            for (BlockPos p : data.pedestalPositions) {
                if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal) {
                    setMainPedestalFlag(level, p, pedestal, p.equals(data.mainPedestal));
                }
            }
        } else if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            setMainPedestalFlag(level, pos, pedestal, true);
        }
    }

    
    public void clearMainAltar(Level level, BlockPos pos) {
        AltarStructure data = findAltarStructure(level, pos);
        if (data != null) {
            for (BlockPos p : data.pedestalPositions) {
                if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal) {
                    setMainPedestalFlag(level, p, pedestal, false);
                }
            }
        } else if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            setMainPedestalFlag(level, pos, pedestal, false);
        }
    }

    
    private static void setMainPedestalFlag(Level level, BlockPos pos, AltarItemContainer pedestal, boolean main) {
        if (pedestal.isMainPedestal() == main) {
            return;
        }
        pedestal.setMainPedestal(main);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
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

    
    protected CraftingOutcome performCrafting(Level level, BlockPos corePos, AltarStructure data,
                                              @Nullable ItemStack targetStack) {
        if (!(level.getBlockEntity(corePos) instanceof AltarItemContainer corePedestal)) {
            return CraftingOutcome.NO_MATERIAL;
        }

        List<BlockPos> pedestalPositions = data.pedestalPositions;
        boolean isSingle = pedestalPositions.size() == 1;

        
        List<BlockPos> sourcePositions = new ArrayList<>();
        List<ItemStack> pool = new ArrayList<>();
        if (isSingle) {
            
            if (corePedestal.hasItem()) {
                sourcePositions.add(corePos);
                pool.add(corePedestal.getItem().copy());
            }
        } else {
            
            for (BlockPos p : pedestalPositions) {
                if (p.equals(corePos)) continue;
                if (level.getBlockEntity(p) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                    sourcePositions.add(p);
                    pool.add(pedestal.getItem().copy());
                }
            }
        }
        if (pool.isEmpty()) {
            return CraftingOutcome.NO_MATERIAL;
        }

        
        if (!data.hasEnoughPoints()) {
            return CraftingOutcome.POINTS_INSUFFICIENT;
        }

        
        
        
        StackedItemContents contents = new StackedItemContents();
        for (ItemStack stack : pool) {
            contents.accountStack(stack);
        }
        CraftingRecipe matchedRecipe = null;
        Placement placement = null;
        for (CraftingRecipe recipe : craftingRecipes(level)) {
            Placement candidate = buildPlacement(recipe, pool, contents, level);
            if (candidate == null) continue;
            ItemStack candidateResult = recipe.assemble(candidate.input());
            if (candidateResult.isEmpty()) continue;
            
            if (targetStack != null && !matchesTarget(candidateResult, targetStack)) continue;
            matchedRecipe = recipe;
            placement = candidate;
            break;
        }
        if (matchedRecipe == null) {
            return CraftingOutcome.NO_RECIPE;
        }

        
        int[] usedPerSource = new int[pool.size()];
        int[] slotToSource = placement.slotToSource();
        for (int source : slotToSource) {
            if (source >= 0) usedPerSource[source]++;
        }

        List<List<ItemStack>> remaindersBySource = new ArrayList<>(sourcePositions.size());
        for (int i = 0; i < sourcePositions.size(); i++) {
            remaindersBySource.add(new ArrayList<>());
        }
        List<ItemStack> orphanRemainders = new ArrayList<>();
        NonNullList<ItemStack> remaining = matchedRecipe.getRemainingItems(placement.input());
        for (int slot = 0; slot < slotToSource.length && slot < remaining.size(); slot++) {
            ItemStack remainder = remaining.get(slot);
            if (remainder == null || remainder.isEmpty()) continue;
            int source = slotToSource[slot];
            if (source >= 0 && source < remaindersBySource.size()) {
                remaindersBySource.get(source).add(remainder.copy());
            } else {
                orphanRemainders.add(remainder.copy());
            }
        }

        ItemStack result = matchedRecipe.assemble(placement.input());

        if (isSingle) {
            
            BlockPos dropPos = corePos.above();
            int leftover = pool.get(0).getCount() - usedPerSource[0];
            if (leftover > 0) {
                popResource(level, dropPos, pool.get(0).copyWithCount(leftover));
            }
            for (ItemStack extra : remaindersBySource.get(0)) {
                popResource(level, dropPos, extra);
            }
            for (ItemStack extra : orphanRemainders) {
                popResource(level, dropPos, extra);
            }
            corePedestal.clearItem();
            corePedestal.setItem(result);
        } else {
            for (int i = 0; i < sourcePositions.size(); i++) {
                BlockPos sourcePos = sourcePositions.get(i);
                int leftover = pool.get(i).getCount() - usedPerSource[i];
                List<ItemStack> remainders = remaindersBySource.get(i);
                ItemStack stored = leftover > 0 ? pool.get(i).copyWithCount(leftover) : ItemStack.EMPTY;
                int firstLooseRemainder = 0;
                if (stored.isEmpty() && !remainders.isEmpty()) {
                    stored = remainders.get(0);
                    firstLooseRemainder = 1;
                }
                if (level.getBlockEntity(sourcePos) instanceof AltarItemContainer pedestal) {
                    pedestal.clearItem();
                    if (!stored.isEmpty()) {
                        pedestal.setItem(stored);
                    }
                }
                
                for (int r = firstLooseRemainder; r < remainders.size(); r++) {
                    popResource(level, sourcePos.above(), remainders.get(r));
                }
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendBlockUpdated(sourcePos, level.getBlockState(sourcePos),
                            level.getBlockState(sourcePos), 3);
                }
            }
            for (ItemStack extra : orphanRemainders) {
                popResource(level, corePos.above(), extra);
            }
            if (corePedestal.hasItem()) {
                popResource(level, corePos, corePedestal.getItem());
                corePedestal.clearItem();
            }
            corePedestal.setItem(result);
        }
        return CraftingOutcome.SUCCESS;
    }

    
    private record Placement(CraftingInput input, int[] slotToSource) {}

    
    private static final int MAX_PLACEMENT_STEPS = 4096;

    
    @Nullable
    private static Placement buildPlacement(CraftingRecipe recipe, List<ItemStack> pool,
                                            StackedItemContents contents, Level level) {
        PlacementInfo info = recipe.placementInfo();
        if (info.isImpossibleToPlace()) return null;
        List<Ingredient> ingredients = info.ingredients();
        if (ingredients.isEmpty()) return null;

        if (recipe instanceof ShapedRecipe shaped) {
            
            if (!contents.canCraft(recipe, null)) return null;
            return confirm(recipe, buildPatternPlacement(shaped.pattern, pool), level);
        }
        if (recipe instanceof ShapelessRecipe) {
            if (!contents.canCraft(recipe, null)) return null;
            return confirm(recipe, buildBagPlacement(ingredients, ingredients.size(), pool), level);
        }
        int available = 0;
        for (ItemStack stack : pool) {
            available += Math.max(1, stack.getCount());
        }
        for (int count = Math.min(ingredients.size(), available); count >= 1; count--) {
            Placement bag = confirm(recipe, buildBagPlacement(ingredients, count, pool), level);
            if (bag != null) return bag;
        }
        return confirm(recipe, buildDenseGridPlacement(ingredients, pool), level);
    }

    
    @Nullable
    private static Placement buildPatternPlacement(ShapedRecipePattern pattern, List<ItemStack> pool) {
        List<Optional<Ingredient>> patternIngredients = pattern.ingredients();
        List<Ingredient> required = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < patternIngredients.size(); i++) {
            Optional<Ingredient> maybe = patternIngredients.get(i);
            if (maybe.isEmpty()) continue;
            required.add(maybe.get());
            slots.add(i);
        }
        if (required.isEmpty()) return null;
        int[] chosen = pick(required, pool);
        if (chosen == null) return null;
        return gridPlacement(pattern.width(), pattern.height(), slots, chosen, pool);
    }

    
    @Nullable
    private static Placement buildDenseGridPlacement(List<Ingredient> ingredients, List<ItemStack> pool) {
        if (ingredients.size() != 9) return null;
        List<Integer> slots = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            slots.add(i);
        }
        int[] chosen = pick(ingredients, pool);
        if (chosen == null) return null;
        return gridPlacement(3, 3, slots, chosen, pool);
    }

    
    @Nullable
    private static Placement gridPlacement(int width, int height, List<Integer> slots, int[] chosen,
                                           List<ItemStack> pool) {
        int size = width * height;
        List<ItemStack> grid = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            grid.add(ItemStack.EMPTY);
        }
        int[] slotToSource = new int[size];
        Arrays.fill(slotToSource, -1);
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (slot < 0 || slot >= size) return null;
            grid.set(slot, pool.get(chosen[i]).copyWithCount(1));
            slotToSource[slot] = chosen[i];
        }
        return new Placement(CraftingInput.of(width, height, grid), slotToSource);
    }

    
    @Nullable
    private static Placement buildBagPlacement(List<Ingredient> ingredients, int count, List<ItemStack> pool) {
        if (count < 1 || count > ingredients.size()) return null;
        int[] chosen = pick(ingredients.subList(0, count), pool);
        if (chosen == null) return null;
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(pool.get(chosen[i]).copyWithCount(1));
        }
        return new Placement(CraftingInput.of(count, 1, items), chosen);
    }

    @Nullable
    private static Placement confirm(CraftingRecipe recipe, @Nullable Placement placement, Level level) {
        if (placement == null) return null;
        CraftingInput input = placement.input();
        
        
        if (input.size() != placement.slotToSource().length) return null;
        return recipe.matches(input, level) ? placement : null;
    }

    
    @Nullable
    private static int[] pick(List<Ingredient> required, List<ItemStack> pool) {
        int[] chosen = new int[required.size()];
        int[] usedCount = new int[pool.size()];
        int[] budget = {MAX_PLACEMENT_STEPS};
        return pickFrom(required, pool, 0, chosen, usedCount, budget) ? chosen : null;
    }

    private static boolean pickFrom(List<Ingredient> required, List<ItemStack> pool, int index,
                                    int[] chosen, int[] usedCount, int[] budget) {
        if (index >= required.size()) return true;
        if (budget[0] <= 0) return false;
        Ingredient ingredient = required.get(index);
        for (int i = 0; i < pool.size(); i++) {
            ItemStack available = pool.get(i);
            if (usedCount[i] >= Math.max(1, available.getCount())) continue;
            if (!ingredient.test(available)) continue;
            budget[0]--;
            usedCount[i]++;
            chosen[index] = i;
            if (pickFrom(required, pool, index + 1, chosen, usedCount, budget)) return true;
            usedCount[i]--;
            if (budget[0] <= 0) return false;
        }
        return false;
    }

    
    private static List<CraftingRecipe> craftingRecipes(Level level) {
        var server = level.getServer();
        if (server == null) return List.of();
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe crafting && !crafting.isSpecial()) {
                holders.add(holder);
            }
        }
        holders.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));
        List<CraftingRecipe> result = new ArrayList<>(holders.size());
        for (RecipeHolder<?> holder : holders) {
            result.add((CraftingRecipe) holder.value());
        }
        return result;
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

    
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        if (!level.getBlockState(pos).is(state.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal && pedestal.hasItem()) {
                popResource(level, pos, pedestal.getItem());
                pedestal.clearItem();
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
            return pedestal.hasItem() ? 15 : 0;
        }
        return 0;
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);
}
