package net.pttheta.loveandwar.blocks.petrochem;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.pttheta.loveandwar.blocks.LAWBlockEntityTypes;
import net.pttheta.loveandwar.item.ModItems;

public class DistillationColumnBlock extends Block implements IBE<DistillationColumnBlockEntity>, IWrenchable {
   public static final BooleanProperty TOP = BooleanProperty.create("top");
   public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
   public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

   public DistillationColumnBlock(Properties pProperties) {
      super(pProperties);
   }

   public static boolean isTank(BlockState state) {
      return state.getBlock() instanceof DistillationColumnBlock;
   }

   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      if (level.getBlockEntity(context.getClickedPos()) instanceof DistillationColumnBlockEntity dtbe) {
         int width = dtbe.getControllerBE().getWidth();
         BlockPos pos = dtbe.getController();
         IFluidHandler existing = (IFluidHandler)level.getCapability(FluidHandler.BLOCK, pos, null);
         FluidStack stackInTank = (existing != null ? existing : new FluidTank(1)).getFluidInTank(0).copy();

         for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
               level.setBlock(pos.offset(x, 0, z), AllBlocks.FLUID_TANK.getDefaultState(), 3);
               level.updateNeighborsAt(pos.offset(x, 0, z), (Block)AllBlocks.FLUID_TANK.get());
            }
         }

         if (!stackInTank.isEmpty() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity) {
            IFluidHandler tank = (IFluidHandler)level.getCapability(FluidHandler.BLOCK, pos, null);
            if (tank != null) {
               tank.fill(stackInTank, FluidAction.EXECUTE);
            }
         }

         if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getPlayer().getInventory().placeItemBackInInventory(new ItemStack(((Item)ModItems.THERMOSTAT.get()).asItem(), 1));
         }
      }

      return InteractionResult.SUCCESS;
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
      if (direction == Direction.DOWN && neighbourState.getBlock() != this) {
         this.withBlockEntityDo(level, pos, DistillationColumnBlockEntity::updateTemperature);
      }

      return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos otherPos, boolean p_60514_) {
      super.neighborChanged(state, level, pos, block, otherPos, p_60514_);
      this.withBlockEntityDo(level, pos, DistillationColumnBlockEntity::updateVerticalMulti);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{TOP, BOTTOM, SHAPE});
   }

   public Class<DistillationColumnBlockEntity> getBlockEntityClass() {
      return DistillationColumnBlockEntity.class;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      this.withBlockEntityDo(context.getLevel(), context.getClickedPos(), DistillationColumnBlockEntity::toggleWindows);
      return InteractionResult.SUCCESS;
   }

   public BlockEntityType<? extends DistillationColumnBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends DistillationColumnBlockEntity>)LAWBlockEntityTypes.DISTILLATION_COLUMN.get();
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
         if (!(world.getBlockEntity(pos) instanceof DistillationColumnBlockEntity tankBE)) {
            return;
         }

         world.removeBlockEntity(pos);
         ConnectivityHandler.splitMulti(tankBE);
      }
   }

   public ItemStack getCloneItemStack(BlockGetter getter, BlockPos pos, BlockState state) {
      return AllBlocks.FLUID_TANK.asStack();
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
      if (oldState.getBlock() != state.getBlock() && !moved) {
         this.withBlockEntityDo(level, pos, DistillationColumnBlockEntity::updateConnectivity);
         this.withBlockEntityDo(level, pos, DistillationColumnBlockEntity::updateVerticalMulti);
      }
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      if (mirror == Mirror.NONE) {
         return state;
      }

      boolean x = mirror == Mirror.FRONT_BACK;

      return switch ((Shape)state.getValue(SHAPE)) {
         case WINDOW_NE -> (BlockState)state.setValue(SHAPE, x ? Shape.WINDOW_NW : Shape.WINDOW_SE);
         case WINDOW_NW -> (BlockState)state.setValue(SHAPE, x ? Shape.WINDOW_NE : Shape.WINDOW_SW);
         case WINDOW_SE -> (BlockState)state.setValue(SHAPE, x ? Shape.WINDOW_SW : Shape.WINDOW_NE);
         case WINDOW_SW -> (BlockState)state.setValue(SHAPE, x ? Shape.WINDOW_SE : Shape.WINDOW_NW);
         default -> state;
      };
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      for (int i = 0; i < rotation.ordinal(); i++) {
         state = this.rotateOnce(state);
      }

      return state;
   }

   private BlockState rotateOnce(BlockState state) {
      return switch ((Shape)state.getValue(SHAPE)) {
         case WINDOW_NE -> (BlockState)state.setValue(SHAPE, Shape.WINDOW_SE);
         case WINDOW_NW -> (BlockState)state.setValue(SHAPE, Shape.WINDOW_NE);
         case WINDOW_SE -> (BlockState)state.setValue(SHAPE, Shape.WINDOW_SW);
         case WINDOW_SW -> (BlockState)state.setValue(SHAPE, Shape.WINDOW_NW);
         default -> state;
      };
   }
}
