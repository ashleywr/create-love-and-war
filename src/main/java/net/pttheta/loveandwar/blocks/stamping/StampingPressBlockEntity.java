package net.pttheta.loveandwar.blocks.stamping;

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
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
import net.pttheta.loveandwar.recipe.stamping.StampingRecipe;
import net.pttheta.loveandwar.util.ModTags;

public class StampingPressBlockEntity extends KineticBlockEntity implements StampingBehaviour.StampingBehaviourSpecifics {
   private final ItemStackHandler templateHolder = new ItemStackHandler(1) {
      protected void onContentsChanged(int slot) {
         StampingPressBlockEntity.this.setChanged();
         if (!StampingPressBlockEntity.this.level.isClientSide()) {
            StampingPressBlockEntity.this.level
               .sendBlockUpdated(
                  StampingPressBlockEntity.this.getBlockPos(), StampingPressBlockEntity.this.getBlockState(), StampingPressBlockEntity.this.getBlockState(), 3
               );
         }
      }
   };
   private int ticksSinceSwap = 0;
   public StampingBehaviour stampingBehaviour;
   private int tracksCreated;
   private static final ItemStackHandler pressingInvHandler = new ItemStackHandler(1);
   private static final RecipeWrapper pressingInv = new RecipeWrapper(pressingInvHandler);

   public ItemStack getRenderStack() {
      return this.templateHolder.getStackInSlot(0);
   }

   public StampingPressBlockEntity(BlockEntityType<? extends StampingPressBlockEntity> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.stampingBehaviour = new StampingBehaviour(this);
      behaviours.add(this.stampingBehaviour);
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

   public void destroy() {
      super.destroy();
      this.dropTemplate();
   }

   public void dropTemplate() {
      SimpleContainer inventory = new SimpleContainer(this.templateHolder.getSlots());

      for (int i = 0; i < this.templateHolder.getSlots(); i++) {
         inventory.setItem(i, this.templateHolder.getStackInSlot(i));
      }

      Containers.dropContents(this.level, this.worldPosition, inventory);
   }

   public void templateInteract(ItemStack item, Player player, InteractionHand hand) {
      boolean isTemplate = item.is(ModTags.Items.STAMPING_TEMPLATE);
      if (this.ticksSinceSwap >= 5) {
         if (player.isShiftKeyDown()) {
            player.setItemInHand(hand, this.templateHolder.getStackInSlot(0));
            this.templateHolder.setStackInSlot(0, item);
            this.ticksSinceSwap = 0;
         } else if (isTemplate) {
            player.setItemInHand(hand, this.templateHolder.getStackInSlot(0));
            this.templateHolder.setStackInSlot(0, new ItemStack(item.getItem(), 1));
            this.ticksSinceSwap = 0;
         }
      }
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      if (((AdvancementBehaviour)this.getBehaviour(AdvancementBehaviour.TYPE)).isOwnerPresent()) {
         compound.putInt("TracksCreated", this.tracksCreated);
      }

      compound.put("template", this.templateHolder.serializeNBT(registries));
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.tracksCreated = compound.getInt("TracksCreated");
      this.templateHolder.deserializeNBT(registries, compound.getCompound("template"));
   }

   public StampingBehaviour getStampingBehaviour() {
      return this.stampingBehaviour;
   }

   @Override
   public boolean tryProcessInWorld(ItemEntity itemEntity, boolean simulate) {
      ItemStack item = itemEntity.getItem();
      Optional<RecipeHolder<StampingRecipe>> recipe = this.getRecipes(item, this.templateHolder.getStackInSlot(0));
      if (recipe.isEmpty()) {
         return false;
      }

      if (simulate) {
         return true;
      }

      if (this.templateHolder.getStackInSlot(0).isEmpty()) {
         return false;
      }

      ItemStack itemCreated = ItemStack.EMPTY;
      this.stampingBehaviour.particleItems.add(item);
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
      Optional<RecipeHolder<StampingRecipe>> recipe = this.getRecipes(input.stack, this.templateHolder.getStackInSlot(0));
      if (recipe.isEmpty()) {
         return false;
      }

      if (simulate) {
         return true;
      }

      this.stampingBehaviour.particleItems.add(input.stack);
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
   public void onStampingCompleted() {
   }

   @Override
   public int getParticleAmount() {
      return 15;
   }

   @Override
   public float getKineticSpeed() {
      return this.getSpeed();
   }

   @Override
   public void setTemplate(ItemStack stack) {
      this.templateHolder.setStackInSlot(0, stack);
   }

   public void tick() {
      super.tick();
      this.ticksSinceSwap++;
   }

   public Optional<RecipeHolder<StampingRecipe>> getRecipes(ItemStack item, ItemStack template) {
      Optional<RecipeHolder<StampingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(
         this.level, item, (RecipeType)ModRecipes.STAMPING_TYPE.get(), StampingRecipe.class
      );
      if (assemblyRecipe.isPresent()) {
         return assemblyRecipe;
      }

      for (RecipeHolder<StampingRecipe> holder : this.getLevel().getRecipeManager().getAllRecipesFor((RecipeType)ModRecipes.STAMPING_TYPE.get())) {
         StampingRecipe recipe = (StampingRecipe)holder.value();
         if (recipe.getTemplate().getItem() == template.getItem() && recipe.getIngredient().test(item)) {
            return Optional.of(holder);
         }
      }

      pressingInvHandler.setStackInSlot(0, item);
      return this.find(pressingInv, this.level);
   }

   public Optional<RecipeHolder<StampingRecipe>> find(RecipeWrapper inv, Level world) {
      Optional<RecipeHolder<StampingRecipe>> sequenced = SequencedAssemblyRecipe.getRecipe(
         this.level, inv.getItem(0), (RecipeType)ModRecipes.STAMPING_TYPE.get(), StampingRecipe.class
      );
      return sequenced.isPresent() ? sequenced : world.getRecipeManager().getRecipeFor((RecipeType)ModRecipes.STAMPING_TYPE.get(), inv, world);
   }
}
