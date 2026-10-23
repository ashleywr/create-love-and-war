package net.pttheta.loveandwar.gun;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.workbench.GunFabricationRecipe;
import net.pttheta.loveandwar.gun.workbench.ModWorkbenchRecipes;
import net.pttheta.loveandwar.gun.workbench.PartsInput;
import net.pttheta.loveandwar.gun.workbench.WorkbenchDef;
import net.pttheta.loveandwar.gun.workbench.WorkbenchDefs;
import net.pttheta.loveandwar.gun.workbench.WorkbenchMenu;

public class WorkbenchBlockEntity extends BlockEntity implements MenuProvider {
   private final String workbenchName;
   @Nullable
   private final WorkbenchDef def;
   private final ItemStackHandler partsInv;
   private final ItemStackHandler outputHandler = new ItemStackHandler(1) {
      protected void onContentsChanged(int slot) {
         WorkbenchBlockEntity.this.setChanged();
      }
   };
   private final ItemStackHandler gunHandler = new ItemStackHandler(1) {
      protected void onContentsChanged(int slot) {
         WorkbenchBlockEntity.this.setChanged();
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return stack.isEmpty() || stack.getItem() instanceof GunItem;
      }
   };
   @Nullable
   private ResourceLocation craftingRecipeId;
   private int craftTicksRemaining;
   private int craftTicksTotal;

   public WorkbenchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.workbenchName = state.getBlock() instanceof WorkbenchBlock block ? block.workbenchName() : "";
      this.def = WorkbenchDefs.get(this.workbenchName).orElse(null);
      int slots = this.def != null ? Math.max(1, this.def.gunCrafting().maxSlots()) : 10;
      this.partsInv = new ItemStackHandler(slots) {
         protected void onContentsChanged(int slot) {
            WorkbenchBlockEntity.this.setChanged();
         }
      };
   }

   public Optional<WorkbenchDef> def() {
      return Optional.ofNullable(this.def);
   }

   public ItemStackHandler partsInv() {
      return this.partsInv;
   }

   public ItemStackHandler outputHandler() {
      return this.outputHandler;
   }

   public ItemStackHandler gunHandler() {
      return this.gunHandler;
   }

   public boolean isCrafting() {
      return this.craftingRecipeId != null;
   }

   public int craftTicksRemaining() {
      return this.craftTicksRemaining;
   }

   public int craftTicksTotal() {
      return this.craftTicksTotal;
   }

   public void tryStartCraft(Level level) {
      if (!level.isClientSide && this.def != null && this.craftingRecipeId == null) {
         PartsInput input = new PartsInput(this.partsInv);

         for (RecipeHolder<GunFabricationRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get())) {
            GunFabricationRecipe recipe = (GunFabricationRecipe)holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (this.def.canCraft(result) && !recipe.planConsumption(input).isEmpty() && this.canAcceptOutput(result)) {
               this.craftingRecipeId = holder.id();
               this.craftTicksTotal = Math.max(1, recipe.craftTimeTicks());
               this.craftTicksRemaining = this.craftTicksTotal;
               this.setChanged();
               return;
            }
         }
      }
   }

   private boolean canAcceptOutput(ItemStack result) {
      ItemStack current = this.outputHandler.getStackInSlot(0);
      return current.isEmpty() || ItemStack.isSameItemSameComponents(current, result) && current.getCount() + result.getCount() <= current.getMaxStackSize();
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, WorkbenchBlockEntity be) {
      if (be.craftingRecipeId != null) {
         be.craftTicksRemaining--;
         if (be.craftTicksRemaining > 0) {
            be.setChanged();
         } else {
            be.finishCraft(level);
         }
      }
   }

   private void finishCraft(Level level) {
      ResourceLocation id = this.craftingRecipeId;
      this.craftingRecipeId = null;
      if (id != null) {
         level.getRecipeManager().byKey(id).ifPresent(holder -> {
            if (holder.value() instanceof GunFabricationRecipe recipe) {
               PartsInput var5 = new PartsInput(this.partsInv);
               recipe.planConsumption(var5).ifPresent(amounts -> {
                  ItemStack result = recipe.getResultItem(level.registryAccess());
                  if (this.canAcceptOutput(result)) {
                     for (int i = 0; i < amounts.length; i++) {
                        if (amounts[i] > 0) {
                           this.partsInv.extractItem(i, amounts[i], false);
                        }
                     }

                     ItemStack current = this.outputHandler.getStackInSlot(0);
                     if (current.isEmpty()) {
                        this.outputHandler.setStackInSlot(0, result.copy());
                     } else {
                        current.grow(result.getCount());
                     }
                  }
               });
            }
         });
         this.setChanged();
      }
   }

   public Component getDisplayName() {
      return Component.translatable("block.createloveandwar." + this.workbenchName);
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new WorkbenchMenu(containerId, playerInventory, this);
   }

   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
      buf.writeBlockPos(this.getBlockPos());
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Parts", this.partsInv.serializeNBT(registries));
      tag.put("Output", this.outputHandler.serializeNBT(registries));
      tag.put("Gun", this.gunHandler.serializeNBT(registries));
      if (this.craftingRecipeId != null) {
         tag.putString("CraftingRecipe", this.craftingRecipeId.toString());
         tag.putInt("CraftTicksRemaining", this.craftTicksRemaining);
         tag.putInt("CraftTicksTotal", this.craftTicksTotal);
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.partsInv.deserializeNBT(registries, tag.getCompound("Parts"));
      this.outputHandler.deserializeNBT(registries, tag.getCompound("Output"));
      this.gunHandler.deserializeNBT(registries, tag.getCompound("Gun"));
      if (tag.contains("CraftingRecipe")) {
         this.craftingRecipeId = ResourceLocation.parse(tag.getString("CraftingRecipe"));
         this.craftTicksRemaining = tag.getInt("CraftTicksRemaining");
         this.craftTicksTotal = tag.getInt("CraftTicksTotal");
      } else {
         this.craftingRecipeId = null;
      }
   }
}
