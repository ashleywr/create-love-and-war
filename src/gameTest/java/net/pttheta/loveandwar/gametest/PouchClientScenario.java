package net.pttheta.loveandwar.gametest;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import net.pttheta.loveandwar.item.custom.AmmoPouchItem;
import org.slf4j.LoggerFactory;

/** Opt-in inventory packet/tooltip fixture, excluded from release artifacts. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class PouchClientScenario {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_pouch_client")
                .then(Commands.literal("status").executes(context -> {
                    var client = Minecraft.getInstance();
                    if (client.player == null) return 0;
                    LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_POUCH_CLIENT held={} contents={} slot1={} carried={}",
                            client.player.getMainHandItem(), client.player.getMainHandItem().getOrDefault(ModDataComponents.AMMO_POUCH_CONTENTS, AmmoPouchContents.EMPTY),
                            client.player.getInventory().getItem(1), client.player.containerMenu.getCarried());
                    return 1;
                }))
                .then(Commands.literal("click").then(Commands.argument("slot", IntegerArgumentType.integer(0, 45))
                        .then(Commands.argument("button", IntegerArgumentType.integer(0, 1)).executes(context -> {
                            var client = Minecraft.getInstance();
                            if (client.player == null || client.gameMode == null || client.screen == null) return 0;
                            client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId,
                                    IntegerArgumentType.getInteger(context, "slot"), IntegerArgumentType.getInteger(context, "button"), ClickType.PICKUP, client.player);
                            return 1;
                        }))))
                .then(Commands.literal("use").executes(context -> {
                    var client = Minecraft.getInstance();
                    if (client.player == null || client.gameMode == null || client.screen != null) return 0;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    return 1;
                }))
                .then(Commands.literal("tooltip").executes(context -> {
                    var client = Minecraft.getInstance();
                    if (client.player == null || !(client.player.getMainHandItem().getItem() instanceof AmmoPouchItem)) return 0;
                    var pouch = client.player.getMainHandItem().copy();
                    client.setScreen(new Screen(Component.literal("Ammo pouch tooltip inspection")) {
                        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                            graphics.fill(0, 0, width, height, 0xE0202328);
                            graphics.drawString(font, "Ammo pouch contents", 20, 16, 0xFFFFFF);
                            graphics.renderItem(pouch, 20, 38);
                            graphics.renderItemDecorations(font, pouch, 20, 38);
                            graphics.renderTooltip(font, pouch, 64, 64);
                        }
                    });
                    return 1;
                })));
    }
}
