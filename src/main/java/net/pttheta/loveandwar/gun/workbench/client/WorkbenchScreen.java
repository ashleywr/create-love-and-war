package net.pttheta.loveandwar.gun.workbench.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.pttheta.loveandwar.gun.workbench.WorkbenchMenu;

public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
   private static final int PANEL_COLOR = -1071636448;
   private static final int BORDER_COLOR = -7631989;
   private static final int SLOT_COLOR = -13158601;

   public WorkbenchScreen(WorkbenchMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 176;
      this.imageHeight = 210;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   protected void init() {
      super.init();
      int craftButtonY = 18 + (((WorkbenchMenu)this.menu).blockEntity().partsInv().getSlots() + 4) / 5 * 18 + 2;
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("gui.createloveandwar.workbench.craft"),
               b -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(((WorkbenchMenu)this.menu).containerId, 0)
            )
            .bounds(this.leftPos + 116, this.topPos + craftButtonY, 50, 16)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("gui.createloveandwar.workbench.paint"),
               b -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(((WorkbenchMenu)this.menu).containerId, 1)
            )
            .bounds(this.leftPos + 116, this.topPos + 72, 50, 16)
            .build()
      );
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -1071636448);
      graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, -7631989);
      graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -7631989);

      for (Slot slot : ((WorkbenchMenu)this.menu).slots) {
         graphics.fill(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, this.leftPos + slot.x + 17, this.topPos + slot.y + 17, -13158601);
      }

      if (((WorkbenchMenu)this.menu).isCrafting()) {
         int total = Math.max(1, ((WorkbenchMenu)this.menu).craftTicksTotal());
         int remaining = ((WorkbenchMenu)this.menu).craftTicksRemaining();
         int width = (int)(34.0 * (total - remaining) / total);
         graphics.fill(this.leftPos + 116, this.topPos + 54, this.leftPos + 116 + Math.max(0, width), this.topPos + 58, -12603585);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 16777215, false);
      graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 16777215, false);
   }
}
