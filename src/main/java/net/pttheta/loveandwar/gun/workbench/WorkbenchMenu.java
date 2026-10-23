package net.pttheta.loveandwar.gun.workbench;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.pttheta.loveandwar.gun.WorkbenchBlockEntity;
import net.pttheta.loveandwar.gun.WorkbenchBlock;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.screen.ModMenuTypes;

public class WorkbenchMenu extends AbstractContainerMenu {
   public static final String[] ATTACHMENT_CATEGORIES = new String[]{"barrel", "grip", "stock", "scope", "generic"};
   public static final int SELECT_RECIPE_BUTTON = 16;
   public static final int AUTO_FILL_BUTTON = 128;
   private final WorkbenchBlockEntity blockEntity;
   private final ContainerData craftingData = new SimpleContainerData(4);
   private final List<RecipeHolder<GunFabricationRecipe>> recipes;
   private final Player owner;
   private final int partsCount;
   private final int gunSlotIndex;
   private final int playerInvStart;

   public WorkbenchMenu(int containerId, Inventory playerInventory, final WorkbenchBlockEntity blockEntity) {
      super((MenuType)ModMenuTypes.WORKBENCH.get(), containerId);
      this.blockEntity = blockEntity;
      this.owner = playerInventory.player;
      // Build the choice catalog once per open menu, not on every bench tick or output query.
      this.recipes = owner.level().getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get()).stream()
         .filter(r -> blockEntity.def().map(def -> def.canCraft(r.value().getResultItem(owner.level().registryAccess()))).orElse(false))
         .sorted(Comparator.comparing(r -> r.id().toString())).toList();
      this.craftingData.set(3, -1);
      this.partsCount = blockEntity.partsInv().getSlots();
      this.gunSlotIndex = this.partsCount + 1;
      this.playerInvStart = this.partsCount + 2 + ATTACHMENT_CATEGORIES.length;

      for (int i = 0; i < this.partsCount; i++) {
         final int ingredientIndex = i;
         int col = i % 5;
         int row = i / 5;
         this.addSlot(new SlotItemHandler(blockEntity.partsInv(), i, 8 + col * 18, 18 + row * 18) {
            public boolean mayPlace(ItemStack stack) {
               return selectedRecipe().map(r -> ingredientIndex < r.ingredients().size()
                  && r.ingredients().get(ingredientIndex).ingredient().test(stack)).orElse(false);
            }

            public void setChanged() {
               blockEntity.inputsChanged();
            }
         });
      }

      this.addSlot(new GunOutputSlot());
      this.addSlot(new SlotItemHandler(blockEntity.gunHandler(), 0, 8, 72));

      for (int i = 0; i < ATTACHMENT_CATEGORIES.length; i++) {
         this.addSlot(new WorkbenchMenu.InstalledAttachmentSlot(ATTACHMENT_CATEGORIES[i], 44 + i * 18, 72));
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
      }

      this.addDataSlots(this.craftingData);
   }

   public static WorkbenchMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
      BlockPos pos = buf.readBlockPos();
      var block = BuiltInRegistries.BLOCK.get(buf.readResourceLocation());
      if (block instanceof WorkbenchBlock bench && bench.newBlockEntity(pos, block.defaultBlockState()) instanceof WorkbenchBlockEntity be) {
         // Slot packets own the client inventory. The world block update may arrive after the menu packet.
         be.setLevel(playerInventory.player.level());
         return new WorkbenchMenu(containerId, playerInventory, be);
      } else {
         throw new IllegalStateException("Invalid workbench menu block " + block);
      }
   }

   @Override
   public void broadcastChanges() {
      if (this.blockEntity.getLevel() != null && !this.blockEntity.getLevel().isClientSide) {
         this.craftingData.set(0, this.blockEntity.isCrafting() ? 1 : 0);
         this.craftingData.set(1, this.blockEntity.craftTicksRemaining());
         this.craftingData.set(2, this.blockEntity.craftTicksTotal());
         int selected = -1;
         for (int i = 0; i < this.recipes.size(); i++) {
            if (this.recipes.get(i).id().equals(this.blockEntity.selectedGunRecipeId())) { selected = i; break; }
         }
         this.craftingData.set(3, selected);
      }
      super.broadcastChanges();
   }

   public boolean isCrafting() {
      return this.craftingData.get(0) != 0;
   }

   public int craftTicksRemaining() {
      return this.craftingData.get(1);
   }

   public int craftTicksTotal() {
      return this.craftingData.get(2);
   }

   public WorkbenchBlockEntity blockEntity() {
      return this.blockEntity;
   }

   public List<RecipeHolder<GunFabricationRecipe>> recipes() {
      return this.recipes;
   }

   public int selectedRecipeIndex() {
      return this.craftingData.get(3);
   }

   public Optional<GunFabricationRecipe> selectedRecipe() {
      int index = selectedRecipeIndex();
      var id = this.owner.level().isClientSide
         ? (index >= 0 && index < this.recipes.size() ? this.recipes.get(index).id() : null)
         : this.blockEntity.selectedGunRecipeId();
      if (id == null) return Optional.empty();
      var holder = this.owner.level().getRecipeManager().byKey(id);
      return holder.filter(r -> r.value() instanceof GunFabricationRecipe)
         .map(r -> (GunFabricationRecipe)r.value())
         .filter(r -> this.blockEntity.def().map(def -> def.canCraft(r.getResultItem(this.owner.level().registryAccess()))).orElse(false));
   }

   public boolean stillValid(Player player) {
      if (player.level().isClientSide) return player == this.owner;
      return this.blockEntity.getLevel() != null
         && this.blockEntity.getLevel().getBlockEntity(this.blockEntity.getBlockPos()) == this.blockEntity
         && player.distanceToSqr(
               this.blockEntity.getBlockPos().getX() + 0.5, this.blockEntity.getBlockPos().getY() + 0.5, this.blockEntity.getBlockPos().getZ() + 0.5
            )
            <= 64.0;
   }

   public boolean clickMenuButton(Player player, int id) {
      if (player != this.owner || !this.stillValid(player)) return false;
      if (id < 0) id += 256; // Vanilla's menu-button packet uses a signed byte.
      Level level = player.level();
      if (level.isClientSide) {
         return true;
      } else if (id >= SELECT_RECIPE_BUTTON && id < SELECT_RECIPE_BUTTON + this.recipes.size()) {
         int index = id - SELECT_RECIPE_BUTTON;
         if (level.getRecipeManager().byKey(this.recipes.get(index).id())
            .filter(r -> r.value() instanceof GunFabricationRecipe recipe
               && this.blockEntity.def().map(def -> def.canCraft(recipe.getResultItem(level.registryAccess()))).orElse(false)).isEmpty()) return false;
         this.blockEntity.selectGunRecipe(this.recipes.get(index).id());
         this.broadcastChanges();
         return true;
      } else if (id >= AUTO_FILL_BUTTON && id < AUTO_FILL_BUTTON + this.partsCount) {
         return this.autoFill(player, id - AUTO_FILL_BUTTON);
      } else {
         return false;
      }
   }

   private boolean autoFill(Player player, int index) {
      var selected = this.selectedRecipe();
      if (selected.isEmpty() || index >= selected.get().ingredients().size()) return false;
      var ingredient = selected.get().ingredients().get(index);
      var current = this.blockEntity.partsInv().getStackInSlot(index);
      if (player.hasInfiniteMaterials()) {
         var options = ingredient.ingredient().getItems();
         if (options.length == 0) return false;
         int existing = -1;
         for (int i = 0; i < options.length; i++) if (ItemStack.isSameItemSameComponents(current, options[i])) existing = i;
         this.blockEntity.partsInv().setStackInSlot(index, options[(existing + 1) % options.length].copyWithCount(ingredient.count()));
      } else {
         var inventory = player.getInventory();
         int source = -1;
         for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (ingredient.test(inventory.getItem(i))) { source = i; break; }
         }
         if (source < 0) return false;
         // Simulate the return before modifying anything: Inventory.add may otherwise partially insert and fail.
         var simulation = new net.neoforged.neoforge.items.ItemStackHandler(36);
         for (int i = 0; i < 36; i++) simulation.setStackInSlot(i, inventory.getItem(i).copy());
         if (source < 36) simulation.extractItem(source, ingredient.count(), false);
         var toReturn = current.copy();
         for (int i = 0; i < 36 && !toReturn.isEmpty(); i++) toReturn = simulation.insertItem(i, toReturn, false);
         if (!toReturn.isEmpty()) return false;
         var replacement = inventory.removeItem(source, ingredient.count());
         if (!current.isEmpty()) inventory.add(current.copy());
         this.blockEntity.partsInv().setStackInSlot(index, replacement);
         inventory.setChanged();
      }
      this.broadcastChanges();
      return true;
   }

   private ItemStack gunStack() {
      return this.blockEntity.gunHandler().getStackInSlot(0);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (!stillValid(player) || index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
      if (index == this.partsCount) {
         Slot output = this.slots.get(index);
         ItemStack preview = output.getItem().copy();
         if (preview.isEmpty()) return ItemStack.EMPTY;
         int capacity = 0;
         for (int i = this.playerInvStart; i < this.playerInvStart + 36; i++) {
            Slot destination = this.slots.get(i);
            ItemStack stack = destination.getItem();
            if (stack.isEmpty()) capacity += destination.getMaxStackSize(preview);
            else if (ItemStack.isSameItemSameComponents(stack, preview)) capacity += Math.max(0, destination.getMaxStackSize(preview) - stack.getCount());
         }
         // Taking a preview consumes ingredients; never do so before checking that the result fits.
         if (capacity < preview.getCount()) return ItemStack.EMPTY;
         ItemStack crafted = output.remove(preview.getCount());
         if (crafted.isEmpty()) return ItemStack.EMPTY;
         ItemStack result = crafted.copy();
         this.moveItemStackTo(crafted, this.playerInvStart, this.playerInvStart + 36, true);
         output.onTake(player, result);
         return result;
      }
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack original = slot.getItem();
         ItemStack copy = original.copy();
         int playerInvEnd = this.playerInvStart + 36;
         if (index < this.playerInvStart) {
            if (!this.moveItemStackTo(original, this.playerInvStart, playerInvEnd, true)) {
               return ItemStack.EMPTY;
            }
         } else if (original.getItem() instanceof GunItem) {
            if (!this.moveItemStackTo(original, this.gunSlotIndex, this.gunSlotIndex + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(original, 0, this.partsCount, false)) {
            return ItemStack.EMPTY;
         }

         if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, original);
         return copy;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private final class GunOutputSlot extends Slot {
      private ItemStack preview = ItemStack.EMPTY;
      private long revision = -1;
      private GunFabricationRecipe cachedRecipe;

      GunOutputSlot() {
         super(new SimpleContainer(1), 0, 116, 36);
      }

      public ItemStack getItem() {
         if (owner.level().isClientSide) return this.preview;
         ItemStack stored = blockEntity.outputHandler().getStackInSlot(0);
         if (!stored.isEmpty()) return stored;
         var recipe = selectedRecipe().orElse(null);
         if (recipe != this.cachedRecipe || this.revision != blockEntity.inputsRevision()) {
            this.cachedRecipe = recipe;
            this.revision = blockEntity.inputsRevision();
            var input = new PartsInput(blockEntity.partsInv());
            this.preview = recipe != null && !blockEntity.isCrafting() && recipe.planConsumption(input).isPresent()
               ? recipe.assemble(input, owner.level().registryAccess()).copyWithCount(1) : ItemStack.EMPTY;
         }
         return this.preview;
      }

      public boolean hasItem() { return !this.getItem().isEmpty(); }
      public boolean mayPlace(ItemStack stack) { return false; }
      public boolean mayPickup(Player player) { return stillValid(player) && this.hasItem(); }

      public void set(ItemStack stack) {
         // Network slot updates are display-only. A preview is never saved or dropped with the bench.
         if (owner.level().isClientSide) this.preview = stack;
      }

      public ItemStack remove(int amount) {
         if (amount <= 0 || !stillValid(owner)) return ItemStack.EMPTY;
         if (owner.level().isClientSide) return this.preview.split(amount);
         if (!blockEntity.outputHandler().getStackInSlot(0).isEmpty()) return blockEntity.outputHandler().extractItem(0, amount, false);
         if (blockEntity.isCrafting()) return ItemStack.EMPTY;
         var recipe = selectedRecipe().orElse(null);
         if (recipe == null) return ItemStack.EMPTY;
         var input = new PartsInput(blockEntity.partsInv());
         var consumption = recipe.planConsumption(input);
         if (consumption.isEmpty()) return ItemStack.EMPTY;
         // Original GunCraftingOutputSlot yields one gun per take, regardless of result stack count.
         var result = recipe.assemble(input, owner.level().registryAccess()).copyWithCount(1);
         for (int i = 0; i < consumption.get().length; i++) {
            if (consumption.get()[i] > 0) blockEntity.partsInv().extractItem(i, consumption.get()[i], false);
         }
         return result;
      }

      protected void onSwapCraft(int amount) {
         this.remove(amount); // Vanilla hotbar swap bypasses Slot.remove.
      }
   }

   private final class InstalledAttachmentSlot extends Slot {
      private final String category;

      InstalledAttachmentSlot(String category, int x, int y) {
         super(new SimpleContainer(1), 0, x, y);
         this.category = category;
      }

      public ItemStack getItem() {
         ItemStack gun = WorkbenchMenu.this.gunStack();
         if (gun.isEmpty()) {
            return ItemStack.EMPTY;
         }

         List<ItemStack> installed = ((GunAttachments)gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY)).forCategory(this.category);
         return installed.isEmpty() ? ItemStack.EMPTY : installed.get(0);
      }

      public boolean hasItem() {
         return !this.getItem().isEmpty();
      }

      public void set(ItemStack stack) {
         ItemStack gun = WorkbenchMenu.this.gunStack();
         if (!gun.isEmpty()) {
            GunAttachments attachments = (GunAttachments)gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
            gun.set(ModDataComponents.GUN_ATTACHMENTS, attachments.with(this.category, stack.isEmpty() ? List.of() : List.of(stack.copyWithCount(1))));
            WorkbenchMenu.this.blockEntity.setChanged();
         }
      }

      public ItemStack remove(int amount) {
         ItemStack removed = this.getItem();
         if (!removed.isEmpty()) {
            this.set(ItemStack.EMPTY);
         }

         return removed;
      }

      public boolean mayPlace(ItemStack stack) {
         if (!this.hasItem() && !WorkbenchMenu.this.gunStack().isEmpty()) {
            Optional<GunDef> def = GunLookup.gunDef(WorkbenchMenu.this.gunStack());
            if (def.isPresent()) {
               GunDef.AttachmentSlot slotDef = GunLookup.slotFor(def.get().attachmentSlots(), this.category);
               return slotDef.numAttachmentSlots() > 0
                  && GunLookup.matchesAttachmentSlot(slotDef, stack)
                  && WorkbenchMenu.this.blockEntity.def().map(wb -> wb.isModAllowed(GunLookup.contentKey(stack))).orElse(true);
            }
         }

         return false;
      }

      public boolean mayPickup(Player player) {
         return !this.getItem().isEmpty();
      }

      public int getMaxStackSize() {
         return 1;
      }
   }
}
