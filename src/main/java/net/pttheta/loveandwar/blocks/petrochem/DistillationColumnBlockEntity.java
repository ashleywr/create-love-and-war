package net.pttheta.loveandwar.blocks.petrochem;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer.Fluid;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import java.util.List;
import java.util.stream.Collectors;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.pttheta.loveandwar.recipe.RecipeRegister;
import net.pttheta.loveandwar.recipe.petrochemical.FractionalDistillationRecipe;
import org.jetbrains.annotations.Nullable;

public class DistillationColumnBlockEntity extends SmartBlockEntity implements Fluid, IHaveGoggleInformation {
   public int FRACTIONAL_HEIGHT = 1;
   public boolean SPEED_BOOST = true;
   private static final int MAX_SIZE = 3;
   public float progress;
   public boolean[] occludedDirections = new boolean[]{true, true, true, true};
   public int heat;
   protected IFluidHandler fluidCapability;
   protected boolean forceFluidLevelUpdate = true;
   public FluidTank tankInventory = this.createInventory();
   protected BlockPos controller;
   protected BlockPos lastKnownPos;
   protected boolean updateConnectivity = false;
   public boolean window = false;
   protected int luminosity;
   protected int width = 1;
   protected int height = 1;
   protected BlockPos bottomCPos;
   private static final int SYNC_RATE = 8;
   protected int syncCooldown;
   protected boolean queuedSync;
   private LerpedFloat fluidLevel;
   public boolean hasDistillationC;
   int processingTime = -1;
   FractionalDistillationRecipe currentRecipe;

   public DistillationColumnBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.refreshCapability();
   }

   private BlockPos getBottomControllerPos() {
      if (this.isBottom()) {
         return this.getController();
      } else {
         return this.level.getBlockEntity(this.getBlockPos().below()) instanceof DistillationColumnBlockEntity be
            ? be.getBottomControllerPos()
            : this.getController();
      }
   }

   protected SmartFluidTank createInventory() {
      return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
   }

   public int updateTemperature() {
      if (this.getControllerBE() == null) {
         return 0;
      }

      int width = this.getControllerBE().width;
      int heatN = 0;
      if (this.getController() != null && width != 0) {
         for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
               BlockPos pos = this.getController().offset(xOffset, -1, zOffset);
               BlockState blockState = this.level.getBlockState(pos);
               float heat = BoilerHeater.findHeat(this.level, pos, blockState);
               heatN = (int)(heatN + heat);
            }
         }

         return heatN / (width * width);
      } else {
         return 0;
      }
   }

   public void updateConnectivity() {
      this.updateConnectivity = false;
      if (!this.level.isClientSide && this.isController()) {
         ConnectivityHandler.formMulti(this);
      }
   }

   private void startProcessing() {
      if (this.currentRecipe != null) {
         this.processingTime = this.currentRecipe.getProcessingDuration();
      }
   }

   public void tick() {
      this.bottomCPos = this.getBottomControllerPos();
      if (this.isController() && this.isBottom()) {
         int oldHeat = this.heat;
         this.heat = this.updateTemperature();
         if (oldHeat != this.heat && this.processingTime <= -1) {
            List<FractionalDistillationRecipe> r = this.getMatchingRecipes();
            if (!r.isEmpty()) {
               this.currentRecipe = r.get(0);
               if (this.getHeat(this.currentRecipe.getRequiredHeat()) <= this.heat) {
                  this.startProcessing();
               } else {
                  this.currentRecipe = null;
               }
            } else {
               this.currentRecipe = null;
            }
         }

         if (this.processingTime > -1 && this.currentRecipe != null) {
            boolean canFill = true;
            int i = 0;

            while (i < this.currentRecipe.getFluidResults().size() * this.FRACTIONAL_HEIGHT) {
               if (this.level.getBlockEntity(this.getBlockPos().above(i + 1)) instanceof DistillationColumnBlockEntity be) {
                  if (be.getControllerBE().width != this.getControllerBE().width) {
                     canFill = false;
                     break;
                  }

                  if (i % this.FRACTIONAL_HEIGHT == 0
                     && be.getTank(0).getFluidAmount()
                        <= getCapacityMultiplier() * this.width * this.width
                           - ((FluidStack)this.currentRecipe.getFluidResults().get(i / this.FRACTIONAL_HEIGHT)).getAmount()) {
                     i++;
                     continue;
                  }

                  canFill = false;
                  break;
               }

               canFill = false;
               break;
            }

            if (canFill) {
               this.processingTime = this.processingTime - Mth.clamp(this.SPEED_BOOST ? this.width * this.width : 1, 1, this.processingTime);
            }

            if (this.tankInventory.getFluid().getAmount() < ((SizedFluidIngredient)this.currentRecipe.getFluidIngredients().get(0)).amount()
               || this.getHeat(this.currentRecipe.getRequiredHeat()) > this.heat) {
               this.currentRecipe = null;
               this.processingTime = -1;
               this.onFluidStackChanged(this.tankInventory.getFluid());
            }
         }

         if (this.processingTime == 0 && this.currentRecipe != null) {
            if (this.tankInventory.getFluid().getAmount() >= ((SizedFluidIngredient)this.currentRecipe.getFluidIngredients().get(0)).amount()
               && this.getHeat(this.currentRecipe.getRequiredHeat()) <= this.heat) {
               this.tankInventory.drain(((SizedFluidIngredient)this.currentRecipe.getFluidIngredients().get(0)).amount(), FluidAction.EXECUTE);
               if (this.currentRecipe != null) {
                  for (int i = 0; i < this.currentRecipe.getFluidResults().size() * this.FRACTIONAL_HEIGHT; i++) {
                     if (!(this.level.getBlockEntity(this.getBlockPos().above(i + 1)) instanceof DistillationColumnBlockEntity be)
                        || be.getControllerBE().width != this.getControllerBE().width) {
                        break;
                     }

                     if (i % this.FRACTIONAL_HEIGHT == 0) {
                        be.tankInventory.fill((FluidStack)this.currentRecipe.getFluidResults().get(i / this.FRACTIONAL_HEIGHT), FluidAction.EXECUTE);
                     }
                  }
               }
            }

            this.currentRecipe = null;
            this.processingTime = -1;
            this.onFluidStackChanged(this.tankInventory.getFluid());
         }

         if (this.currentRecipe == null || this.width != 0 && this.currentRecipe.getProcessingDuration() != 0) {
            this.progress = this.currentRecipe != null
               ? this.processingTime / ((float)this.currentRecipe.getProcessingDuration() / (this.width * this.width))
               : 0.0F;
         }
      }

      super.tick();
      if (this.syncCooldown > 0) {
         this.syncCooldown--;
         if (this.syncCooldown == 0 && this.queuedSync) {
            this.sendData();
         }
      }

      if (this.lastKnownPos == null) {
         this.lastKnownPos = this.getBlockPos();
      } else if (!this.lastKnownPos.equals(this.worldPosition) && this.worldPosition != null) {
         this.onPositionChanged();
         return;
      }

      if (this.updateConnectivity) {
         this.updateConnectivity();
      }

      if (this.fluidLevel != null) {
         this.fluidLevel.tickChaser();
      }
   }

   public BlockPos getLastKnownPos() {
      return this.lastKnownPos;
   }

   public boolean isController() {
      return this.controller == null
         || this.worldPosition.getX() == this.controller.getX()
            && this.worldPosition.getY() == this.controller.getY()
            && this.worldPosition.getZ() == this.controller.getZ();
   }

   public void initialize() {
      super.initialize();
      this.sendData();
      if (this.level.isClientSide) {
         this.invalidateRenderBoundingBox();
      }
   }

   private void onPositionChanged() {
      this.removeController(true);
      this.lastKnownPos = this.worldPosition;
   }

   protected List<FractionalDistillationRecipe> getMatchingRecipes() {
      List<RecipeHolder<? extends Recipe<?>>> found = RecipeFinder.get(
         new Object(), this.level, holder -> holder.value().getType() == RecipeRegister.FRACTIONAL_DISTILLATION.getType()
      );
      return found.stream()
         .map(holder -> (FractionalDistillationRecipe)holder.value())
         .filter(
            recipe -> !recipe.getFluidIngredients().isEmpty()
               && ((SizedFluidIngredient)recipe.getFluidIngredients().get(0)).ingredient().test(this.tankInventory.getFluid())
               && this.getHeat(recipe.getRequiredHeat()) <= this.heat
         )
         .collect(Collectors.toList());
   }

   int getHeat(HeatCondition heatCondition) {
      if (heatCondition == HeatCondition.SUPERHEATED) {
         return 2;
      } else {
         return heatCondition == HeatCondition.HEATED ? 1 : 0;
      }
   }

   protected void onFluidStackChanged(FluidStack newFluidStack) {
      if (this.hasLevel()) {
         if (this.processingTime <= -1) {
            List<FractionalDistillationRecipe> r = this.getMatchingRecipes();
            if (!r.isEmpty()) {
               this.currentRecipe = r.get(0);
               this.startProcessing();
            } else {
               this.currentRecipe = null;
            }
         }

         FluidType attributes = newFluidStack.getFluid().getFluidType();
         int luminosity = (int)(attributes.getLightLevel(newFluidStack) / 1.2F);
         boolean reversed = attributes.isLighterThanAir();
         int maxY = (int)(this.getFillState() * this.height + 1.0F);

         for (int yOffset = 0; yOffset < this.height; yOffset++) {
            boolean isBright = reversed ? this.height - yOffset <= maxY : yOffset < maxY;
            int actualLuminosity = isBright ? luminosity : (luminosity > 0 ? 1 : 0);

            for (int xOffset = 0; xOffset < this.width; xOffset++) {
               for (int zOffset = 0; zOffset < this.width; zOffset++) {
                  BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                  DistillationColumnBlockEntity tankAt = (DistillationColumnBlockEntity)ConnectivityHandler.partAt(this.getType(), this.level, pos);
                  if (tankAt != null) {
                     this.level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
                     if (tankAt.luminosity != actualLuminosity) {
                        tankAt.setLuminosity(actualLuminosity);
                     }
                  }
               }
            }
         }

         if (!this.level.isClientSide) {
            this.setChanged();
            this.sendData();
         }

         if (this.isVirtual()) {
            if (this.fluidLevel == null) {
               this.fluidLevel = LerpedFloat.linear().startWithValue(this.getFillState());
            }

            this.fluidLevel.chase(this.getFillState(), 0.5, Chaser.EXP);
         }
      }
   }

   protected void setLuminosity(int luminosity) {
      if (!this.level.isClientSide && this.luminosity != luminosity) {
         this.luminosity = luminosity;
         this.sendData();
      }
   }

   public DistillationColumnBlockEntity getControllerBE() {
      if (this.isController()) {
         return this;
      }

      BlockEntity blockEntity = this.level.getBlockEntity(this.controller);
      return blockEntity instanceof DistillationColumnBlockEntity ? (DistillationColumnBlockEntity)blockEntity : null;
   }

   public void applyFluidTankSize(int blocks) {
      this.tankInventory.setCapacity(blocks * getCapacityMultiplier());
      int overflow = this.tankInventory.getFluidAmount() - this.tankInventory.getCapacity();
      if (overflow > 0) {
         this.tankInventory.drain(overflow, FluidAction.EXECUTE);
      }

      this.forceFluidLevelUpdate = true;
   }

   public void removeController(boolean keepFluids) {
      if (!this.level.isClientSide) {
         this.updateConnectivity = true;
         if (!keepFluids) {
            this.applyFluidTankSize(1);
         }

         this.controller = null;
         this.width = 1;
         this.height = 1;
         this.onFluidStackChanged(this.tankInventory.getFluid());
         BlockState state = this.getBlockState();
         if (DistillationColumnBlock.isTank(state)) {
            state = (BlockState)state.setValue(DistillationColumnBlock.BOTTOM, true);
            state = (BlockState)state.setValue(DistillationColumnBlock.TOP, true);
            state = (BlockState)state.setValue(DistillationColumnBlock.SHAPE, this.window ? Shape.WINDOW : Shape.PLAIN);
            this.getLevel().setBlock(this.worldPosition, state, 6);
         }

         this.refreshCapability();
         this.setChanged();
         this.sendData();
      }
   }

   public void toggleWindows() {
      DistillationColumnBlockEntity be = this.getControllerBE();
      if (be != null) {
         be.setWindows(!be.window);
      }
   }

   public void sendData() {
      if (this.syncCooldown > 0) {
         this.queuedSync = true;
      } else {
         super.sendData();
         this.queuedSync = false;
         this.syncCooldown = 8;
      }
   }

   public void setWindows(boolean window) {
      if (!window || this.FRACTIONAL_HEIGHT == 1 || (this.getBottomControllerPos().getY() + 1 - this.worldPosition.getY()) % this.FRACTIONAL_HEIGHT == 0) {
         this.window = window;

         for (int yOffset = 0; yOffset < this.height; yOffset++) {
            for (int xOffset = 0; xOffset < this.width; xOffset++) {
               for (int zOffset = 0; zOffset < this.width; zOffset++) {
                  BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                  BlockState blockState = this.level.getBlockState(pos);
                  if (DistillationColumnBlock.isTank(blockState)) {
                     Shape shape = Shape.PLAIN;
                     if (window) {
                        if (this.width == 1) {
                           shape = Shape.WINDOW;
                        }

                        if (this.width == 2) {
                           shape = xOffset == 0 ? (zOffset == 0 ? Shape.WINDOW_NW : Shape.WINDOW_SW) : (zOffset == 0 ? Shape.WINDOW_NE : Shape.WINDOW_SE);
                        }

                        if (this.width == 3 && Math.abs(Math.abs(xOffset) - Math.abs(zOffset)) == 1) {
                           shape = Shape.WINDOW;
                        }
                     }

                     this.level.setBlock(pos, (BlockState)blockState.setValue(DistillationColumnBlock.SHAPE, shape), 22);
                     this.level.getChunkSource().getLightEngine().checkBlock(pos);
                  }
               }
            }
         }
      }
   }

   public void setController(BlockPos controller) {
      if ((!this.level.isClientSide || this.isVirtual()) && !controller.equals(this.controller)) {
         this.controller = controller;
         this.refreshCapability();
         this.setChanged();
         this.sendData();
      }
   }

   private void refreshCapability() {
      this.fluidCapability = null;
      this.invalidateCapabilities();
   }

   private IFluidHandler handlerForCapability() {
      return (IFluidHandler)(this.isController()
         ? this.tankInventory
         : (this.getControllerBE() != null ? this.getControllerBE().handlerForCapability() : new FluidTank(0)));
   }

   public IFluidHandler getFluidHandlerCapability() {
      if (this.FRACTIONAL_HEIGHT != 1 && (this.getBottomControllerPos().getY() + 1 - this.worldPosition.getY()) % this.FRACTIONAL_HEIGHT != 0) {
         return null;
      }

      this.fluidCapability = this.handlerForCapability();
      return this.fluidCapability;
   }

   public BlockPos getController() {
      return this.isController() ? this.worldPosition : this.controller;
   }

   protected AABB createRenderBoundingBox() {
      return this.isController()
         ? super.createRenderBoundingBox().expandTowards(this.width - 1, this.height - 1, this.width - 1)
         : super.createRenderBoundingBox();
   }

   @Nullable
   public DistillationColumnBlockEntity getOtherDistillationColumnBlockEntity(Direction direction) {
      BlockEntity otherBE = this.level.getBlockEntity(this.worldPosition.relative(direction));
      return otherBE instanceof DistillationColumnBlockEntity ? (DistillationColumnBlockEntity)otherBE : null;
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (this.FRACTIONAL_HEIGHT != 1 && (this.getBottomControllerPos().getY() + 1 - this.worldPosition.getY()) % this.FRACTIONAL_HEIGHT != 0) {
         return false;
      }

      DistillationColumnBlockEntity controllerBE = this.getControllerBE();
      return controllerBE == null
         ? false
         : this.containedFluidTooltip(tooltip, isPlayerSneaking, (IFluidHandler)this.level.getCapability(FluidHandler.BLOCK, controllerBE.getBlockPos(), null));
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.hasDistillationC = compound.getBoolean("HasDistillationC");
      BlockPos controllerBefore = this.controller;
      int prevSize = this.width;
      int prevHeight = this.height;
      int prevLum = this.luminosity;
      this.updateConnectivity = compound.contains("Uninitialized");
      this.luminosity = compound.getInt("Luminosity");
      this.controller = null;
      this.lastKnownPos = null;
      if (compound.contains("LastKnownPos")) {
         this.lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");
      }

      if (compound.contains("Controller")) {
         this.controller = NBTHelper.readBlockPos(compound, "Controller");
      }
      this.refreshCapability();

      if (this.isController()) {
         this.window = compound.getBoolean("Window");
         this.width = compound.getInt("Size");
         this.height = compound.getInt("Height");
         this.tankInventory.setCapacity(this.getTotalTankSize() * getCapacityMultiplier());
         this.tankInventory.readFromNBT(registries, compound.getCompound("TankContent"));
         if (this.tankInventory.getSpace() < 0) {
            this.tankInventory.drain(-this.tankInventory.getSpace(), FluidAction.EXECUTE);
         }
      }

      if (compound.contains("ForceFluidLevel") || this.fluidLevel == null) {
         this.fluidLevel = LerpedFloat.linear().startWithValue(this.getFillState());
      }

      if (clientPacket) {
         boolean changeOfController = controllerBefore == null ? this.controller != null : !controllerBefore.equals(this.controller);
         if (changeOfController || prevSize != this.width || prevHeight != this.height) {
            if (this.hasLevel()) {
               this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
            }

            if (this.isController()) {
               this.tankInventory.setCapacity(getCapacityMultiplier() * this.getTotalTankSize());
            }

            this.invalidateRenderBoundingBox();
         }

         if (this.isController()) {
            float fillState = this.getFillState();
            if (compound.contains("ForceFluidLevel") || this.fluidLevel == null) {
               this.fluidLevel = LerpedFloat.linear().startWithValue(fillState);
            }

            this.fluidLevel.chase(fillState, 0.5, Chaser.EXP);
            this.processingTime = compound.getInt("Progress");
         }

         if (this.luminosity != prevLum && this.hasLevel()) {
            this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
         }

         if (compound.contains("LazySync")) {
            this.fluidLevel.chase(this.fluidLevel.getChaseTarget(), 0.125, Chaser.EXP);
         }
      }
   }

   public float getFillState() {
      return (float)this.tankInventory.getFluidAmount() / this.tankInventory.getCapacity();
   }

   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putBoolean("HasDistillationC", this.hasDistillationC);
      if (this.updateConnectivity) {
         compound.putBoolean("Uninitialized", true);
      }

      if (this.lastKnownPos != null) {
         compound.put("LastKnownPos", NbtUtils.writeBlockPos(this.lastKnownPos));
      }

      if (!this.isController()) {
         compound.put("Controller", NbtUtils.writeBlockPos(this.controller));
      }

      if (this.isController()) {
         compound.putBoolean("Window", this.window);
         compound.put("TankContent", this.tankInventory.writeToNBT(registries, new CompoundTag()));
         compound.putInt("Size", this.width);
         compound.putInt("Height", this.height);
         compound.putInt("Progress", this.processingTime);
      }

      compound.putInt("Luminosity", this.luminosity);
      super.write(compound, registries, clientPacket);
      if (clientPacket) {
         if (this.forceFluidLevelUpdate) {
            compound.putBoolean("ForceFluidLevel", true);
         }

         if (this.queuedSync) {
            compound.putBoolean("LazySync", true);
         }

         this.forceFluidLevelUpdate = false;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public int getTotalTankSize() {
      return this.width * this.width;
   }

   public static int getCapacityMultiplier() {
      return 500;
   }

   public LerpedFloat getFluidLevel() {
      return this.fluidLevel;
   }

   public void preventConnectivityUpdate() {
      this.updateConnectivity = false;
   }

   public void notifyMultiUpdated() {
      BlockState state = this.getBlockState();
      if (DistillationColumnBlock.isTank(state)) {
         state = (BlockState)state.setValue(DistillationColumnBlock.BOTTOM, this.getBottomConnectivity());
         state = (BlockState)state.setValue(DistillationColumnBlock.TOP, this.getTopConnectivity());
         this.level.setBlock(this.getBlockPos(), state, 6);
      }

      if (this.isController()) {
         this.setWindows(this.window);
      }

      this.onFluidStackChanged(this.tankInventory.getFluid());
      this.setChanged();
   }

   private boolean getBottomConnectivity() {
      if (this.level.getBlockEntity(this.getBlockPos().below()) instanceof DistillationColumnBlockEntity be) {
         DistillationColumnBlockEntity otherControllerBE = be.getControllerBE();
         if (otherControllerBE != null) {
            DistillationColumnBlockEntity controllerBE = this.getControllerBE();
            if (controllerBE != null && controllerBE.getBlockPos().below().equals(otherControllerBE.getBlockPos())) {
               return controllerBE.getWidth() != otherControllerBE.getWidth();
            }
         }
      }

      return true;
   }

   private boolean getTopConnectivity() {
      if (this.level.getBlockEntity(this.getBlockPos().above()) instanceof DistillationColumnBlockEntity be) {
         DistillationColumnBlockEntity otherControllerBE = be.getControllerBE();
         if (otherControllerBE != null) {
            DistillationColumnBlockEntity controllerBE = this.getControllerBE();
            if (controllerBE != null && controllerBE.getBlockPos().above().equals(otherControllerBE.getBlockPos())) {
               return controllerBE.getWidth() != otherControllerBE.getWidth();
            }
         }
      }

      return true;
   }

   public void setExtraData(@Nullable Object data) {
      if (data instanceof Boolean) {
         this.window = (Boolean)data;
      }
   }

   @Nullable
   public Object getExtraData() {
      return this.window;
   }

   public Object modifyExtraData(Object data) {
      return data instanceof Boolean windows ? windows | this.window : data;
   }

   public Axis getMainConnectionAxis() {
      return Axis.Y;
   }

   public int getMaxLength(Axis longAxis, int width) {
      return longAxis == Axis.Y ? 1 : this.getMaxWidth();
   }

   public int getMaxWidth() {
      return 3;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public boolean hasTank() {
      return true;
   }

   public int getTankSize(int tank) {
      return getCapacityMultiplier();
   }

   public void setTankSize(int tank, int blocks) {
      this.applyFluidTankSize(blocks);
   }

   public IFluidTank getTank(int tank) {
      return this.tankInventory;
   }

   public FluidStack getFluid(int tank) {
      return this.tankInventory.getFluid().copy();
   }

   public void updateVerticalMulti() {
      BlockState state = this.getBlockState();
      if (DistillationColumnBlock.isTank(state)) {
         state = (BlockState)state.setValue(DistillationColumnBlock.BOTTOM, this.getBottomConnectivity());
         state = (BlockState)state.setValue(DistillationColumnBlock.TOP, this.getTopConnectivity());
         if (state != this.getBlockState()) {
            this.level.setBlock(this.getBlockPos(), state, 3);
         }
      }

      if (this.level.getBlockEntity(this.getBlockPos().below()) instanceof DistillationColumnBlockEntity be) {
         be.updateVerticalMulti();
      }
   }

   public boolean isBottom() {
      return !(this.level.getBlockEntity(this.getBlockPos().below()) instanceof DistillationColumnBlockEntity);
   }
}
