package org.tdddd.eej.impl.registry;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.command.EntityTargetArgument;
import org.tdddd.eej.impl.eej;


public class EejArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, eej.MODID);

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> ENTITY_TARGET =
            ARGUMENT_TYPES.register("entity_target", () -> ArgumentTypeInfos.registerByClass(
                    EntityTargetArgument.class,
                    SingletonArgumentInfo.contextFree(EntityTargetArgument::new)));

    public static void register(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }

    private EejArgumentTypes() {
    }
}
