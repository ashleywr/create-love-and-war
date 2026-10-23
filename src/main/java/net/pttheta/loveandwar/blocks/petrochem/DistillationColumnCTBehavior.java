package net.pttheta.loveandwar.blocks.petrochem;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.pttheta.loveandwar.LoveAndWarMod;
import org.jetbrains.annotations.Nullable;

public class DistillationColumnCTBehavior extends Base {
   public static final CTSpriteShiftEntry DISTILLATION_TANK = rectangle("distillation_column/distillation_column");
   public static final CTSpriteShiftEntry DISTILLATION_TANK_TOP = rectangle("distillation_column/distillation_column_top");
   public static final CTSpriteShiftEntry DISTILLATION_TANK_NORTH = rectangle(
      "distillation_column/distillation_column", "distillation_column/distillation_column_pipes_connected"
   );

   private static CTSpriteShiftEntry rectangle(String name) {
      return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, LoveAndWarMod.asResource("block/" + name), LoveAndWarMod.asResource("block/" + name + "_connected"));
   }

   private static CTSpriteShiftEntry rectangle(String name, String connectedName) {
      return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, LoveAndWarMod.asResource("block/" + name), LoveAndWarMod.asResource("block/" + connectedName));
   }

   @Nullable
   public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
      if (direction.getAxis().isVertical()) {
         return DISTILLATION_TANK_TOP;
      } else {
         return direction == Direction.NORTH ? DISTILLATION_TANK_NORTH : DISTILLATION_TANK;
      }
   }

   public boolean connectsTo(
      BlockState state,
      BlockState other,
      BlockAndTintGetter reader,
      BlockPos pos,
      BlockPos otherPos,
      Direction face,
      Direction primaryOffset,
      Direction secondaryOffset
   ) {
      if (pos.above(1).equals(otherPos)) {
         return !(Boolean)state.getValue(FluidTankBlock.TOP);
      } else {
         return pos.below(1).equals(otherPos)
            ? !(Boolean)state.getValue(FluidTankBlock.BOTTOM)
            : other.getBlock() instanceof DistillationColumnBlock && ConnectivityHandler.isConnected(reader, pos, otherPos);
      }
   }
}
