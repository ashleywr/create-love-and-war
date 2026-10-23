package net.pttheta.loveandwar.gun.workbench;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.pttheta.loveandwar.gun.WorkbenchBlockEntity;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.screen.ModMenuTypes;

public class WorkbenchMenu extends AbstractContainerMenu {
   public static final String[] ATTACHMENT_CATEGORIES = new String[]{"barrel", "grip", "stock", "scope", "generic"};
   public static final int CRAFT_BUTTON = 0;
   public static final int PAINT_BUTTON = 1;
   private final WorkbenchBlockEntity blockEntity;
   private final int partsCount;
   private final int gunSlotIndex;
   private final int playerInvStart;

   public WorkbenchMenu(int containerId, Inventory playerInventory, final WorkbenchBlockEntity blockEntity) {
      super((MenuType)ModMenuTypes.WORKBENCH.get(), containerId);
      this.blockEntity = blockEntity;
      this.partsCount = blockEntity.partsInv().getSlots();
      this.gunSlotIndex = this.partsCount + 1;
      this.playerInvStart = this.partsCount + 2 + ATTACHMENT_CATEGORIES.length;

      for (int i = 0; i < this.partsCount; i++) {
         int col = i % 5;
         int row = i / 5;
         this.addSlot(new SlotItemHandler(blockEntity.partsInv(), i, 8 + col * 18, 18 + row * 18));
      }

      this.addSlot(new SlotItemHandler(blockEntity.outputHandler(), 0, 116, 36) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
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

      this.addDataSlots(new ContainerData() {
         public int get(int index) {
            return switch (index) {
               case 0 -> blockEntity.isCrafting() ? 1 : 0;
               case 1 -> blockEntity.craftTicksRemaining();
               case 2 -> blockEntity.craftTicksTotal();
               default -> 0;
            };
         }

         public void set(int index, int value) {
         }

         public int getCount() {
            return 3;
         }
      });
   }

   public static WorkbenchMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
      BlockPos pos = buf.readBlockPos();
      if (playerInventory.player.level().getBlockEntity(pos) instanceof WorkbenchBlockEntity be) {
         return new WorkbenchMenu(containerId, playerInventory, be);
      } else {
         throw new IllegalStateException("No workbench block entity at " + pos);
      }
   }

   public WorkbenchBlockEntity blockEntity() {
      return this.blockEntity;
   }

   public boolean stillValid(Player player) {
      return this.blockEntity.getLevel() != null
         && this.blockEntity.getLevel().getBlockEntity(this.blockEntity.getBlockPos()) == this.blockEntity
         && player.distanceToSqr(
               this.blockEntity.getBlockPos().getX() + 0.5, this.blockEntity.getBlockPos().getY() + 0.5, this.blockEntity.getBlockPos().getZ() + 0.5
            )
            <= 64.0;
   }

   public boolean clickMenuButton(Player player, int id) {
      Level level = player.level();
      if (level.isClientSide) {
         return true;
      } else if (id == 0) {
         this.blockEntity.tryStartCraft(level);
         return true;
      } else if (id == 1) {
         this.cyclePaint();
         return true;
      } else {
         return super.clickMenuButton(player, id);
      }
   }

   private void cyclePaint() {
      ItemStack gunStack = this.gunStack();
      if (!gunStack.isEmpty()) {
         GunLookup.gunDef(gunStack).ifPresent(def -> {
            List<GunDef.Paintjob> jobs = def.paints().paintjobs();
            if (!jobs.isEmpty()) {
               String current = (String)gunStack.getOrDefault(ModDataComponents.GUN_PAINTJOB, "");
               int idx = -1;

               for (int i = 0; i < jobs.size(); i++) {
                  if (jobs.get(i).textureName().equals(current)) {
                     idx = i;
                     break;
                  }
               }

               gunStack.set(ModDataComponents.GUN_PAINTJOB, jobs.get((idx + 1) % jobs.size()).textureName());
               this.blockEntity.setChanged();
            }
         });
      }
   }

   private ItemStack gunStack() {
      return this.blockEntity.gunHandler().getStackInSlot(0);
   }

   public ItemStack quickMoveStack(Player player, int index) {
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
