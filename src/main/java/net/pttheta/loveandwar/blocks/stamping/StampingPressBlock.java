package net.pttheta.loveandwar.blocks.stamping;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.pttheta.loveandwar.blocks.LAWBlockEntityTypes;

public class StampingPressBlock extends HorizontalKineticBlock implements IBE<StampingPressBlockEntity> {
   public StampingPressBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof Player
         ? AllShapes.CASING_14PX.get(Direction.DOWN)
         : AllShapes.MECHANICAL_PROCESSOR_SHAPE;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction prefferedSide = this.getPreferredHorizontalFacing(context);
      return prefferedSide != null ? (BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, prefferedSide) : super.getStateForPlacement(context);
   }

   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   public Class<StampingPressBlockEntity> getBlockEntityClass() {
      return StampingPressBlockEntity.class;
   }

   public BlockEntityType<? extends StampingPressBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends StampingPressBlockEntity>)LAWBlockEntityTypes.STAMPING_PRESS.get();
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      this.withBlockEntityDo(level, pos, be -> be.templateInteract(stack, player, hand));
      return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }
}
