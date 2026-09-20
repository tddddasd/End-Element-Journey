package org.tdddd.eej.impl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.island.IslandChunkData;
import org.tdddd.eej.impl.island.IslandChunkManager;


public final class EejIslandCommand {

    private static final SimpleCommandExceptionType ERROR_NEEDS_POS =
            new SimpleCommandExceptionType(Component.translatable("commands.eej.island.needs_pos"));

    private EejIslandCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eej_island")
                
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("query")
                        .executes(context -> query(context, false))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> query(context, true))))
                .then(Commands.literal("set")
                        .then(Commands.literal("vacuum")
                                .executes(context -> set(context, false, true))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> set(context, true, true))))
                        .then(Commands.literal("nonvacuum")
                                .executes(context -> set(context, false, false))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> set(context, true, false))))));
    }

    private static int query(CommandContext<CommandSourceStack> context, boolean hasPos) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        ChunkPos chunk = toChunk(resolve(context, hasPos));

        IslandChunkData.ChunkRecord record = IslandChunkData.get(level).get(chunk.x, chunk.z);
        Integer group = IslandChunkManager.voidGroupBedrock(level, chunk);
        LevelChunk self = level.getChunkSource().getChunkNow(chunk.x, chunk.z);
        boolean vacuum = record != null && record.vacuum();
        boolean isVoid = IslandChunkManager.isVoidChunk(level, chunk);

        Component bottomText = self == null
                ? Component.translatable("commands.eej.island.unknown")
                : Component.literal(Integer.toString(IslandChunkManager.countBottomLayer(level, self)));
        Component groupText = group == null
                ? Component.translatable("commands.eej.island.unknown")
                : Component.literal(Integer.toString(group));
        Component status = Component.translatable(vacuum
                ? "commands.eej.island.status.vacuum"
                : (isVoid ? "commands.eej.island.status.void" : "commands.eej.island.status.nonvacuum"));

        source.sendSuccess(() -> Component.translatable("commands.eej.island.info",
                chunk.x, chunk.z, bottomText, groupText, status), false);
        
        eej.LOGGER.info("[eej-island] query chunk [{}, {}] bottom={} group={} vacuum={} void={}",
                chunk.x, chunk.z,
                self == null ? "?" : IslandChunkManager.countBottomLayer(level, self),
                group == null ? "?" : group,
                vacuum, isVoid);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean hasPos, boolean vacuum)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        ChunkPos chunk = toChunk(resolve(context, hasPos));
        IslandChunkManager.setVacuum(level, chunk, vacuum);
        source.sendSuccess(() -> Component.translatable(
                vacuum ? "commands.eej.island.set.vacuum" : "commands.eej.island.set.nonvacuum",
                chunk.x, chunk.z), true);
        return 1;
    }

    
    private static ChunkPos toChunk(BlockPos pos) {
        return new ChunkPos(pos);
    }

    
    private static BlockPos resolve(CommandContext<CommandSourceStack> context, boolean hasPos)
            throws CommandSyntaxException {
        if (hasPos) {
            return BlockPosArgument.getBlockPos(context, "pos");
        }
        Entity entity = context.getSource().getEntity();
        if (entity == null) {
            throw ERROR_NEEDS_POS.create();
        }
        return entity.blockPosition();
    }
}
