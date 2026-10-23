package net.pttheta.loveandwar.blocks.drawing;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.pttheta.loveandwar.recipe.ModRecipes;
import net.pttheta.loveandwar.recipe.drawing.DrawingRecipe;

public class DrawingPressBlockEntity extends KineticBlockEntity implements DrawingBehaviour.DrawingBehaviourSpecifics {
   public DrawingBehaviour drawingBehaviour;
   private int tracksCreated;
   private static final ItemStackHandler pressingInvHandler = new ItemStackHandler(1);
   private static final RecipeWrapper pressingInv = new RecipeWrapper(pressingInvHandler);

   public DrawingPressBlockEntity(BlockEntityType<? extends DrawingPressBlockEntity> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.drawingBehaviour = new DrawingBehaviour(this);
      behaviours.add(this.drawingBehaviour);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.PRESS, AllAdvancements.TRACK_CRAFTING});
   }

   protected AABB createRenderBoundingBox() {
      return new AABB(this.worldPosition).expandTowards(0.0, -1.5, 0.0).expandTowards(0.0, 1.0, 0.0);
   }

   public void onItemDrawn(ItemStack result) {
      if (AllBlockTags.TRACKS.matches(result)) {
         this.tracksCreated = this.tracksCreated + result.getCount();
      }

      if (this.tracksCreated >= 1000) {
         this.award(AllAdvancements.TRACK_CRAFTING);
         this.tracksCreated = 0;
      }
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      if (((AdvancementBehaviour)this.getBehaviour(AdvancementBehaviour.TYPE)).isOwnerPresent()) {
         compound.putInt("TracksCreated", this.tracksCreated);
      }
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.tracksCreated = compound.getInt("TracksCreated");
   }

   public DrawingBehaviour getDrawingBehaviour() {
      return this.drawingBehaviour;
   }

   @Override
   public boolean tryProcessInWorld(ItemEntity itemEntity, boolean simulate) {
      ItemStack item = itemEntity.getItem();
      Optional<RecipeHolder<DrawingRecipe>> recipe = this.getRecipe(item);
      if (recipe.isEmpty()) {
         return false;
      }

      if (simulate) {
         return true;
      }

      ItemStack itemCreated = ItemStack.EMPTY;
      this.drawingBehaviour.particleItems.add(item);
      if (!this.canProcessInBulk() && item.getCount() != 1) {
         for (ItemStack result : RecipeApplier.applyRecipeOn(this.level, item.copyWithCount(1), recipe.get().value(), true)) {
            if (itemCreated.isEmpty()) {
               itemCreated = result.copy();
            }

            ItemEntity created = new ItemEntity(this.level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), result);
            created.setDefaultPickUpDelay();
            created.setDeltaMovement(VecHelper.offsetRandomly(Vec3.ZERO, this.level.random, 0.05F));
            this.level.addFreshEntity(created);
         }

         item.shrink(1);
      } else {
         RecipeApplier.applyRecipeOn(itemEntity, recipe.get().value(), true);
         itemCreated = itemEntity.getItem().copy();
      }

      if (!itemCreated.isEmpty()) {
         this.onItemDrawn(itemCreated);
      }

      return true;
   }

   @Override
   public boolean tryProcessOnBelt(TransportedItemStack input, List<ItemStack> outputList, boolean simulate) {
      Optional<RecipeHolder<DrawingRecipe>> recipe = this.getRecipe(input.stack);
      if (recipe.isEmpty()) {
         return false;
      }

      if (simulate) {
         return true;
      }

      this.drawingBehaviour.particleItems.add(input.stack);
      List<ItemStack> outputs = RecipeApplier.applyRecipeOn(
         this.level, this.canProcessInBulk() ? input.stack : input.stack.copyWithCount(1), recipe.get().value(), true
      );

      for (ItemStack created : outputs) {
         if (!created.isEmpty()) {
            this.onItemDrawn(created);
            break;
         }
      }

      outputList.addAll(outputs);
      return true;
   }

   @Override
   public boolean canProcessInBulk() {
      return (Boolean)AllConfigs.server().recipes.bulkPressing.get();
   }

   @Override
   public void onDrawingCompleted() {
   }

   @Override
   public int getParticleAmount() {
      return 15;
   }

   @Override
   public float getKineticSpeed() {
      return this.getSpeed();
   }

   public void tick() {
      super.tick();
   }

   public Optional<RecipeHolder<DrawingRecipe>> getRecipe(ItemStack item) {
      Optional<RecipeHolder<DrawingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(
         this.level, item, (RecipeType)ModRecipes.DRAWING_TYPE.get(), DrawingRecipe.class
      );
      if (assemblyRecipe.isPresent()) {
         return assemblyRecipe;
      }

      pressingInvHandler.setStackInSlot(0, item);
      return this.find(pressingInv, this.level);
   }

   public Optional<RecipeHolder<DrawingRecipe>> find(RecipeWrapper inv, Level world) {
      Optional<RecipeHolder<DrawingRecipe>> sequenced = SequencedAssemblyRecipe.getRecipe(
         this.level, inv.getItem(0), (RecipeType)ModRecipes.DRAWING_TYPE.get(), DrawingRecipe.class
      );
      return sequenced.isPresent() ? sequenced : world.getRecipeManager().getRecipeFor((RecipeType)ModRecipes.DRAWING_TYPE.get(), inv, world);
   }
}
