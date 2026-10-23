package net.pttheta.loveandwar.gun;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorkbenchBlock extends BaseEntityBlock {
   private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);
   public static final MapCodec<WorkbenchBlock> CODEC = simpleCodec(WorkbenchBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private final String workbenchName;

   public WorkbenchBlock(Properties properties) {
      this(properties, "");
   }

   public WorkbenchBlock(Properties properties, String workbenchName) {
      super(properties);
      this.workbenchName = workbenchName;
      this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   public String workbenchName() {
      return this.workbenchName;
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      BlockEntityType<WorkbenchBlockEntity> type = LAWGunItems.workbenchBlockEntityType(this.workbenchName);
      return type == null ? null : type.create(pos, state);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
      if (!state.is(replacement.getBlock()) && !level.isClientSide
         && level.getBlockEntity(pos) instanceof WorkbenchBlockEntity bench) {
         for (var inventory : new net.neoforged.neoforge.items.ItemStackHandler[]{
            bench.partsInv(), bench.outputHandler(), bench.gunHandler()
         }) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
               var stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
               Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
         }
         level.updateNeighbourForOutputSignal(pos, this);
      }
      super.onRemove(state, level, pos, replacement, moving);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof WorkbenchBlockEntity be && player instanceof ServerPlayer) {
         player.openMenu(be);
      }

      return InteractionResult.SUCCESS;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (level.isClientSide) {
         return null;
      }

      BlockEntityType<WorkbenchBlockEntity> expected = LAWGunItems.workbenchBlockEntityType(this.workbenchName);
      return expected == null ? null : createTickerHelper(type, expected, WorkbenchBlockEntity::serverTick);
   }
}
