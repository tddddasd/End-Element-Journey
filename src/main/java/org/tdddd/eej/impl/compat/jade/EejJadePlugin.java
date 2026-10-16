package org.tdddd.eej.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaFormat;
import org.tdddd.eej.impl.altar.PackedMudPedestal;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.capability.EejCapabilities;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.mana.CreativeManaSourceBlock;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.ColorPalette;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ResizeableElement;
import snownee.jade.api.view.ProgressView;

import java.util.List;


/**
 * Jade integration for the mod's mana blocks: a pink mana bar (black when empty) with the current
 * and maximum mana printed as pink text to its right.
 *
 * <p>Registered through the {@code @WailaPlugin} annotation, exactly like the sibling tree's
 * {@code EPCAJadePlugin}: Jade 26.1.11 still ships the annotation ({@code snownee.jade.api.WailaPlugin},
 * a runtime-retained {@code @interface} whose {@code value()} defaults to the empty string) and the
 * jar has <b>no</b> {@code META-INF/services} entry, so annotation scanning is still the discovery
 * mechanism. The provider is registered for both the creative mana source and the packed mud altar;
 * the altar additionally reports the client-side red flash used when a craft is refused for lack of
 * mana.</p>
 *
 * <h2>Why this is not a line-for-line port of the 1.20.1 provider</h2>
 * Jade 1.20.1's element API ({@code IElementHelper}, {@code IProgressStyle}, {@code IBoxStyle},
 * {@code Vec2}-sized {@code IElement}s) does not exist in 26.1.11. It was replaced by
 * {@code JadeUI} + {@code ProgressView}, where
 * <ul>
 *   <li>the bar is a {@code ProgressView} whose per-bar fill colour comes from
 *       {@code ProgressView.Part.color(int)} (or, when unset, from
 *       {@code Theme.progressColors.get(messageType)});</li>
 *   <li>the empty part comes from the {@code BoxStyle} the view carries — its
 *       {@code boxProgressColors} palette is what paints the unfilled remainder, so black is
 *       installed there rather than through a {@code color(fill, empty)} call;</li>
 *   <li>the element is sized with {@code JadeUI.progress(view, width, height)} and the label is a
 *       second element placed on the same tooltip line, which is how the 1.20.1 code's
 *       {@code progress(...)}+{@code append(...)}+{@code translate(...)} pair is expressed here.</li>
 * </ul>
 */
@WailaPlugin
public class EejJadePlugin implements IWailaPlugin {

    public static final Identifier MANA_INFO = Identifier.fromNamespaceAndPath(eej.MODID, "mana_info");

    
    private static final String TAG_MANA = "eej:Mana";
    private static final String TAG_MAX_MANA = "eej:MaxMana";

    
    private static final int COLOR_MANA_PINK = 0xFFFF69B4;
    
    private static final int COLOR_MANA_EMPTY = 0xFF000000;
    
    private static final int COLOR_FLASH_RED = 0xFFFF2020;
    
    private static final int COLOR_MANA_EMPTY_FLASH = 0xFF4A0000;

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 10;

    
    private static final float TEXT_GAP = 4.0F;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, PackedMudPedestalBlockEntity.class);
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, CreativeManaSourceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ManaBlockProvider.INSTANCE, PackedMudPedestal.class);
        registration.registerBlockComponent(ManaBlockProvider.INSTANCE, CreativeManaSourceBlock.class);
        try {
            registration.addConfig(MANA_INFO, true);
        } catch (IllegalArgumentException ignored) {
            
        }
    }

    
    public static class ManaBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        public static final ManaBlockProvider INSTANCE = new ManaBlockProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!config.get(MANA_INFO)) {
                return;
            }

            int mana;
            int max;
            CompoundTag serverData = accessor.getServerData();
            if (serverData.getInt(TAG_MANA).isPresent() && serverData.getInt(TAG_MAX_MANA).isPresent()) {
                mana = serverData.getIntOr(TAG_MANA, 0);
                max = serverData.getIntOr(TAG_MAX_MANA, 0);
            } else {
                
                IManaStorage storage = manaOf(accessor);
                if (storage == null) {
                    return;
                }
                mana = storage.getManaStored();
                max = storage.getMaxManaStored();
            }

            float flash = flashOf(accessor.getBlockEntity());
            float ratio = max > 0 ? Mth.clamp((float) mana / (float) max, 0.0F, 1.0F) : 0.0F;

            int fillColor = mix(COLOR_MANA_PINK, COLOR_FLASH_RED, flash);
            int emptyColor = mix(COLOR_MANA_EMPTY, COLOR_MANA_EMPTY_FLASH, flash);

            
            
            ProgressView view = new ProgressView(
                    List.of(ProgressView.Part.of(ratio, fillColor)),
                    Component.empty(),
                    JadeUI.progressStyle(),
                    emptyPartBoxStyle(emptyColor));

            
            
            int line = tooltip.size();
            tooltip.add(line, JadeUI.progress(view, BAR_WIDTH, BAR_HEIGHT));

            
            int textColor = mix(COLOR_MANA_PINK, COLOR_FLASH_RED, flash);
            ResizeableElement label = JadeUI.text(Component.literal(ManaFormat.bar(mana, max))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(textColor))));
            label.offset((int) TEXT_GAP, 0);
            tooltip.add(line + 1, label);
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            IManaStorage storage = manaOf(accessor);
            if (storage == null) {
                return;
            }
            tag.putInt(TAG_MANA, storage.getManaStored());
            tag.putInt(TAG_MAX_MANA, storage.getMaxManaStored());
        }

        @Override
        public Identifier getUid() {
            return MANA_INFO;
        }

        
        private static IManaStorage manaOf(BlockAccessor accessor) {
            Level level = accessor.getLevel();
            if (level == null) {
                return null;
            }
            
            return level.getCapability(EejCapabilities.MANA_STORAGE, accessor.getPosition(),
                    (net.minecraft.core.Direction) null);
        }

        private static float flashOf(BlockEntity blockEntity) {
            return blockEntity instanceof PackedMudPedestalBlockEntity pedestal
                    ? pedestal.getManaFlashIntensity() : 0.0F;
        }

        /**
         * The style that paints the <b>empty</b> part of the bar.
         *
         * <p>{@code JadeClient} reads the progress-bar colours off
         * {@code Theme.tooltipStyle.boxProgressColors}, so the unfilled remainder is a box-style
         * property in this Jade line rather than the second argument of a
         * {@code color(fill, empty)} call. {@code nestedBox()} is copied before it is touched:
         * the instance it returns belongs to the active theme and must not be mutated globally.</p>
         */
        private static snownee.jade.api.ui.BoxStyle emptyPartBoxStyle(int emptyColor) {
            ColorPalette palette = new ColorPalette(emptyColor, emptyColor, emptyColor, emptyColor,
                    emptyColor, emptyColor, emptyColor);
            snownee.jade.api.ui.BoxStyle style = snownee.jade.api.ui.BoxStyle.nestedBox().copy();
            style.boxProgressColors = palette;
            return style;
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
