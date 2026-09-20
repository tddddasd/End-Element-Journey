package org.tdddd.eej.impl.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public interface EntityTarget {
    List<Entity> resolve(CommandSourceStack source) throws CommandSyntaxException;

    final class SelectorTarget implements EntityTarget {
        private final net.minecraft.commands.arguments.selector.EntitySelector selector;

        public SelectorTarget(net.minecraft.commands.arguments.selector.EntitySelector selector) {
            this.selector = selector;
        }

        @Override
        public List<Entity> resolve(CommandSourceStack source) throws CommandSyntaxException {
            
            return new ArrayList<>(selector.findEntities(source));
        }
    }

    final class UuidTarget implements EntityTarget {
        private final UUID uuid;

        public UuidTarget(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public List<Entity> resolve(CommandSourceStack source) {
            List<Entity> found = new ArrayList<>();
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) found.add(player);
            for (ServerLevel level : source.getServer().getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity.getUUID().equals(uuid) && !found.contains(entity)) found.add(entity);
                }
            }
            return found;
        }
    }

    final class IdTarget implements EntityTarget {
        private final int id;

        public IdTarget(int id) {
            this.id = id;
        }

        @Override
        public List<Entity> resolve(CommandSourceStack source) {
            Entity entity = source.getLevel().getEntity(id);
            return entity == null ? List.of() : List.of(entity);
        }
    }

    final class TypeTarget implements EntityTarget {
        private final EntityType<?> type;

        public TypeTarget(EntityType<?> type) {
            this.type = type;
        }

        @Override
        public List<Entity> resolve(CommandSourceStack source) {
            List<Entity> found = new ArrayList<>();
            
            for (Entity entity : source.getLevel().getAllEntities()) {
                if (entity.getType() == type) found.add(entity);
            }
            return found;
        }
    }

    
    final class ModTarget implements EntityTarget {
        private final String modId;

        public ModTarget(String modId) {
            this.modId = modId;
        }

        @Override
        public List<Entity> resolve(CommandSourceStack source) {
            List<Entity> found = new ArrayList<>();
            for (Entity entity : source.getLevel().getAllEntities()) {
                // 26.1.2: ForgeRegistries.ENTITY_TYPES.getKey(...) -> BuiltInRegistries.ENTITY_TYPE.getKey(...)
                Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (key != null && key.getNamespace().equals(modId)) found.add(entity);
            }
            return found;
        }
    }
}
