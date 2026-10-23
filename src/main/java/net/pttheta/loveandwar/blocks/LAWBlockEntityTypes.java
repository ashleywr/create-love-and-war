package net.pttheta.loveandwar.blocks;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.blocks.drawing.DrawingPressBlockEntity;
import net.pttheta.loveandwar.blocks.drawing.DrawingPressInstance;
import net.pttheta.loveandwar.blocks.drawing.DrawingPressRenderer;
import net.pttheta.loveandwar.blocks.petrochem.DistillationColumnBlockEntity;
import net.pttheta.loveandwar.blocks.petrochem.DistillationColumnRenderer;
import net.pttheta.loveandwar.blocks.stamping.StampingPressBlockEntity;
import net.pttheta.loveandwar.blocks.stamping.StampingPressInstance;
import net.pttheta.loveandwar.blocks.stamping.StampingPressRenderer;

public class LAWBlockEntityTypes {
   public static final BlockEntityEntry<DrawingPressBlockEntity> DRAWING_PRESS = LoveAndWarMod.LAWREGISTRATE
      .blockEntity("drawing_press", DrawingPressBlockEntity::new)
      .visual(() -> DrawingPressInstance::new)
      .validBlocks(LAWBlocks.DRAWING_PRESS)
      .renderer(() -> DrawingPressRenderer::new)
      .register();
   public static final BlockEntityEntry<StampingPressBlockEntity> STAMPING_PRESS = LoveAndWarMod.LAWREGISTRATE
      .blockEntity("stamping_press", StampingPressBlockEntity::new)
      .visual(() -> StampingPressInstance::new)
      .validBlocks(LAWBlocks.STAMPING_PRESS)
      .renderer(() -> StampingPressRenderer::new)
      .register();
   public static final BlockEntityEntry<DistillationColumnBlockEntity> DISTILLATION_COLUMN = LoveAndWarMod.LAWREGISTRATE
      .blockEntity("distillation_column_block_entity", DistillationColumnBlockEntity::new)
      .validBlocks(LAWBlocks.DISTILLATION_COLUMN)
      .renderer(() -> DistillationColumnRenderer::new)
      .registerCapability(
         event -> event.registerBlockEntity(FluidHandler.BLOCK, distillationColumnType(), (be, ctx) -> be.getFluidHandlerCapability())
      )
      .register();

   private static BlockEntityType<DistillationColumnBlockEntity> distillationColumnType() {
      return DISTILLATION_COLUMN.get();
   }

   public static void register() {
   }
}
