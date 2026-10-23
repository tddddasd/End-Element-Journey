package org.tdddd.eej.impl.compat.jade;

import java.util.Map;

import java.util.HashMap;

import net.minecraft.client.gui.GuiGraphics;
import snownee.jade.api.ui.Element;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IBoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaFormat;
import org.tdddd.eej.impl.altar.PackedMudPedestal;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.capability.EejCapabilities;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.element.plate.ElementAggregationPlate;
import org.tdddd.eej.impl.element.plate.blockentity.ElementAggregationPlateBlockEntity;
import org.tdddd.eej.impl.mana.CreativeManaSourceBlock;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;


/**
 * Jade integration for the mod's mana blocks: a pink mana bar (black when empty) with the current
 * and maximum mana printed as pink text to its right.
 *
 * <p>Registered through the {@code @WailaPlugin} annotation, exactly like the sibling tree's
 * {@code EPCAJadePlugin} - Jade 1.20.1 scans mod classes for that annotation, there is no services
 * file to edit. The provider is registered for both the creative mana source and the packed mud
 * altar; the altar additionally reports the client-side red flash used when a craft is refused for
 * lack of mana.</p>
 */
@WailaPlugin
public class EejJadePlugin implements IWailaPlugin {

    public static final ResourceLocation MANA_INFO = new ResourceLocation(eej.MODID, "mana_info");

    
    private static final String TAG_MANA = "eej:Mana";
    private static final String TAG_MAX_MANA = "eej:MaxMana";

    
    private static final int COLOR_MANA_PINK = 0xFFFFB6C1;
    
    private static final int COLOR_MANA_EMPTY = 0xFF000000;
    
    private static final int COLOR_FLASH_RED = 0xFFFF2020;
    
    private static final int COLOR_MANA_EMPTY_FLASH = 0xFF4A0000;

    private static final float BAR_WIDTH = 80.0F;
    private static final float BAR_HEIGHT = 10.0F;

    
    private static final float TEXT_GAP = 4.0F;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, PackedMudPedestalBlockEntity.class);
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, CreativeManaSourceBlockEntity.class);
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, ElementAggregationPlateBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ManaClientProvider.INSTANCE, PackedMudPedestal.class);
        registration.registerBlockComponent(ManaClientProvider.INSTANCE, CreativeManaSourceBlock.class);
        registration.registerBlockComponent(ManaClientProvider.INSTANCE, ElementAggregationPlate.class);
        try {
            registration.addConfig(MANA_INFO, true);
        } catch (IllegalArgumentException ignored) {
            
        }
    }

    /** Jade since MC 1.21.6 requires the data provider and the component provider to be
     *  different classes, so the tooltip rendering lives here and shares {@link #getUid()}
     *  with the server provider - that uid is how Jade pairs the two. */
    /**
     * Smooth mana bar. Jade 11.13.3's own progress element draws a <em>segmented</em> bar, which does
     * not match the smooth bar the 26.1.2 tree gets from {@code JadeUI.progress}, so both trees now
     * paint the same thing: a filled remainder plus a partial fill, in the same colours and size.
     */
    public static class ManaBarElement extends Element {
        private static final int BORDER_COLOR = 0xFFFFFFFF;
        private static final float RATIO_PER_SECOND = 1.5F;
        private static final int MAX_TRACKED = 256;

        /** Easing state is per block: two altars must never share one bar's animation. */
        private static final Map<Long, Float> RATIO = new HashMap<>();
        private static final Map<Long, Long> UPDATED = new HashMap<>();

        private final BlockEntity blockEntity;
        private final float target;
        private final int fillColor;
        private final int emptyColor;

        public ManaBarElement(BlockEntity blockEntity, float target, int fillColor, int emptyColor) {
            this.blockEntity = blockEntity;
            this.target = Mth.clamp(target, 0.0F, 1.0F);
            this.fillColor = fillColor;
            this.emptyColor = emptyColor;
            this.size = new Vec2(BAR_WIDTH, BAR_HEIGHT);
        }

        @Override
        public Vec2 getSize() {
            return this.size;
        }

        @Override
        public void render(GuiGraphics graphics, float x, float y, float maxX, float maxY) {
            int left = (int) x;
            int top = (int) y;
            int right = left + (int) BAR_WIDTH;
            int bottom = top + (int) BAR_HEIGHT;

            float shown = animate(this.key(), this.target);

            graphics.fill(left + 1, top + 1, right - 1, bottom - 1, this.emptyColor);
            int innerWidth = (int) BAR_WIDTH - 2;
            int filled = Math.round(innerWidth * shown);
            if (filled > 0) {
                graphics.fill(left + 1, top + 1, left + 1 + filled, bottom - 1, this.fillColor);
            }

            graphics.fill(left, top, right, top + 1, BORDER_COLOR);
            graphics.fill(left, bottom - 1, right, bottom, BORDER_COLOR);
            graphics.fill(left, top + 1, left + 1, bottom - 1, BORDER_COLOR);
            graphics.fill(right - 1, top + 1, right, bottom - 1, BORDER_COLOR);
        }

        private long key() {
            return this.blockEntity == null ? 0L : this.blockEntity.getBlockPos().asLong();
        }

        /** Time based, so the bar keeps easing even when Jade does not rebuild the tooltip. */
        private static float animate(long key, float target) {
            long now = System.nanoTime();
            Float current = RATIO.get(key);
            if (current == null) {
                if (RATIO.size() >= MAX_TRACKED) {
                    RATIO.clear();
                    UPDATED.clear();
                }
                RATIO.put(key, target);
                UPDATED.put(key, now);
                return target;
            }
            Long updated = UPDATED.get(key);
            float elapsed = updated == null ? 0.0F
                    : Math.min((now - updated) / 1_000_000_000.0F, 0.25F);
            UPDATED.put(key, now);
            float step = RATIO_PER_SECOND * elapsed;
            float delta = target - current;
            float next;
            if (Math.abs(delta) <= step) {
                next = target;
            } else {
                next = current + Math.signum(delta) * step;
            }
            next = Mth.clamp(next, 0.0F, 1.0F);
            RATIO.put(key, next);
            return next;
        }
    }

    public static class ManaClientProvider implements IBlockComponentProvider {
        public static final ManaClientProvider INSTANCE = new ManaClientProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            try {
                ManaBlockProvider.appendManaTooltip(tooltip, accessor, config);
            } catch (Throwable t) {
                // Jade runs component providers straight from the render path and does not catch,
                // so anything that escapes here would take the whole client down. Degrade to
                // "no bar" instead.
                org.apache.logging.log4j.LogManager.getLogger("eej-jade")
                        .debug("mana tooltip provider failed", t);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return MANA_INFO;
        }
    }


    
    public static class ManaBlockProvider implements IServerDataProvider<BlockAccessor> {
        public static final ManaBlockProvider INSTANCE = new ManaBlockProvider();

                public static void appendManaTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            int mana;
            int max;
            CompoundTag serverData = accessor.getServerData();
            if (serverData.contains(TAG_MANA) && serverData.contains(TAG_MAX_MANA)) {
                mana = serverData.getInt(TAG_MANA);
                max = serverData.getInt(TAG_MAX_MANA);
            } else {
                
                IManaStorage storage = manaOf(accessor.getBlockEntity());
                if (storage == null) {
                    return;
                }
                mana = storage.getManaStored();
                max = storage.getMaxManaStored();
            }

            float flash = flashOf(accessor.getBlockEntity());
            float ratio = max > 0 ? Mth.clamp((float) mana / (float) max, 0.0F, 1.0F) : 0.0F;

            IElementHelper helper = tooltip.getElementHelper();
            int fillColor = mix(COLOR_MANA_PINK, COLOR_FLASH_RED, flash);
            int emptyColor = mix(COLOR_MANA_EMPTY, COLOR_MANA_EMPTY_FLASH, flash);

            
            // canDecrease = false: Jade's progress tracker is keyed by plugin uid and shared by
            // every mana block, so letting it chase a downwards value would mix the creative
            // source's maximum with a drained altar's.
            IElement bar = new ManaBarElement(accessor.getBlockEntity(), ratio, fillColor, emptyColor);
            tooltip.add(bar);

            
            int textColor = mix(COLOR_MANA_PINK, COLOR_FLASH_RED, flash);
            IElement manaText = helper.text(Component.literal(ManaFormat.bar(mana, max))
                            .withStyle(style2 -> style2.withColor(TextColor.fromRgb(textColor))))
                    .translate(new Vec2(TEXT_GAP, 0.0F));
            // append() targets the last line and throws when there is none.
            if (tooltip.size() > 0) {
                tooltip.append(manaText);
            } else {
                tooltip.add(manaText);
            }
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            IManaStorage storage = manaOf(accessor.getBlockEntity());
            if (storage == null) {
                return;
            }
            tag.putInt(TAG_MANA, storage.getManaStored());
            tag.putInt(TAG_MAX_MANA, storage.getMaxManaStored());
        }

        @Override
        public ResourceLocation getUid() {
            return MANA_INFO;
        }

        private static IManaStorage manaOf(BlockEntity blockEntity) {
            if (blockEntity == null) {
                return null;
            }
            try {
                net.minecraftforge.common.util.LazyOptional<IManaStorage> capability =
                        blockEntity.getCapability(EejCapabilities.MANA_STORAGE);
                // When a block entity is removed Forge invalidates its capabilities, which leaves the
                // LazyOptional's supplier returning null and makes orElse() throw
                // "Supplier should not return null value". Jade keeps re-querying the last looked-at
                // position while the tooltip fades out, so that used to spam the log on every tick.
                return capability.isPresent() ? capability.orElse(null) : null;
            } catch (RuntimeException e) {
                return null;
            }
        }

        private static float flashOf(BlockEntity blockEntity) {
            return blockEntity instanceof PackedMudPedestalBlockEntity pedestal
                    ? pedestal.getManaFlashIntensity() : 0.0F;
        }

        
        private static int mix(int base, int flashColor, float intensity) {
            float t = Mth.clamp(intensity, 0.0F, 1.0F);
            if (t <= 0.0F) {
                return base;
            }
            if (t >= 1.0F) {
                return flashColor;
            }
            int red = (int) Mth.lerp(t, (base >> 16) & 0xFF, (flashColor >> 16) & 0xFF);
            int green = (int) Mth.lerp(t, (base >> 8) & 0xFF, (flashColor >> 8) & 0xFF);
            int blue = (int) Mth.lerp(t, base & 0xFF, flashColor & 0xFF);
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
    }
}
