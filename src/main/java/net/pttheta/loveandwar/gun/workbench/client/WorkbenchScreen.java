package net.pttheta.loveandwar.gun.workbench.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.workbench.WorkbenchMenu;

public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private static final int RECIPES_PER_PAGE = 8;
    private final Button[] choices = new Button[RECIPES_PER_PAGE];
    private Button previous;
    private Button next;
    private int page;
    private int displayedPage = -1;
    private int displayedSelection = -2;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 304;
        this.imageHeight = 210;
        this.inventoryLabelY = 96;
    }

    protected void init() {
        super.init();
        this.displayedPage = -1;
        for (int row = 0; row < RECIPES_PER_PAGE; row++) {
            final int offset = row;
            this.choices[row] = this.addRenderableWidget(Button.builder(Component.empty(),
                    button -> sendButton(WorkbenchMenu.SELECT_RECIPE_BUTTON + this.page * RECIPES_PER_PAGE + offset))
                    .bounds(this.leftPos + 178, this.topPos + 24 + row * 20, 120, 18).build());
        }
        this.previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> { this.page--; updateChoices(); })
                .bounds(this.leftPos + 178, this.topPos + 188, 24, 16).build());
        this.next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> { this.page++; updateChoices(); })
                .bounds(this.leftPos + 274, this.topPos + 188, 24, 16).build());
        updateChoices();
    }

    private void sendButton(int id) {
        if (this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private int pages() {
        return Math.max(1, (this.menu.recipes().size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
    }

    private void updateChoices() {
        this.page = Math.clamp(this.page, 0, pages() - 1);
        if (this.displayedPage == this.page && this.displayedSelection == this.menu.selectedRecipeIndex()) return;
        this.displayedPage = this.page;
        this.displayedSelection = this.menu.selectedRecipeIndex();
        for (int row = 0; row < RECIPES_PER_PAGE; row++) {
            int index = this.page * RECIPES_PER_PAGE + row;
            Button button = this.choices[row];
            button.visible = index < this.menu.recipes().size();
            if (button.visible) {
                ItemStack result = this.menu.recipes().get(index).value().getResultItem(this.minecraft.level.registryAccess());
                String name = result.getHoverName().getString();
                String label = this.font.width(name) > 112 ? this.font.plainSubstrByWidth(name, 105) + "…" : name;
                button.setMessage(Component.literal(label)
                        .withStyle(index == this.menu.selectedRecipeIndex() ? ChatFormatting.GOLD : ChatFormatting.WHITE));
                button.setTooltip(Tooltip.create(result.getHoverName()));
            }
        }
        this.previous.active = this.page > 0;
        this.next.active = this.page < pages() - 1;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateChoices();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.menu.selectedRecipe().ifPresent(recipe -> {
            for (int i = 0; i < recipe.ingredients().size() && i < this.menu.blockEntity().partsInv().getSlots(); i++) {
                Slot slot = this.menu.getSlot(i);
                if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    var options = recipe.ingredients().get(i).ingredient().getItems();
                    if (options.length > 0) graphics.renderComponentTooltip(this.font,
                            java.util.List.of(ghost(options).getHoverName(), Component.translatable("gui.createloveandwar.workbench.fill_hint")), mouseX, mouseY);
                }
            }
        });
    }

    private ItemStack ghost(ItemStack[] options) {
        return options[(int)(this.minecraft.level.getGameTime() / 20 % options.length)];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var selected = this.menu.selectedRecipe();
        if (selected.isPresent() && this.menu.getCarried().isEmpty()) {
            for (int i = 0; i < selected.get().ingredients().size() && i < this.menu.blockEntity().partsInv().getSlots(); i++) {
                Slot slot = this.menu.getSlot(i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)
                        && (button == 0 && !slot.hasItem() || button == 1 && hasShiftDown())) {
                    sendButton(WorkbenchMenu.AUTO_FILL_BUTTON + i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xE0202328);
        graphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF8B8B8B);
        graphics.fill(this.leftPos + 173, this.topPos + 1, this.leftPos + 174, this.topPos + this.imageHeight - 1, 0xFF8B8B8B);
        for (Slot slot : this.menu.slots) {
            graphics.fill(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, this.leftPos + slot.x + 17, this.topPos + slot.y + 17, 0xFF151515);
            graphics.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0xFF505050);
        }
        this.menu.selectedRecipe().ifPresent(recipe -> {
            for (int i = 0; i < recipe.ingredients().size() && i < this.menu.blockEntity().partsInv().getSlots(); i++) {
                Slot slot = this.menu.getSlot(i);
                var options = recipe.ingredients().get(i).ingredient().getItems();
                if (!slot.hasItem() && options.length > 0) {
                    graphics.renderFakeItem(ghost(options), this.leftPos + slot.x, this.topPos + slot.y);
                    graphics.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x80404040);
                }
            }
        });
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.font.plainSubstrByWidth(this.title.getString(), 160), this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.createloveandwar.workbench.recipes"), 178, 8, 0xFFFFFF, false);
        graphics.drawCenteredString(this.font, (this.page + 1) + " / " + pages(), 238, 192, 0xFFFFFF);
    }
}
