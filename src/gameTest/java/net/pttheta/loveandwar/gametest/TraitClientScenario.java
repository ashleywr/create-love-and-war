package net.pttheta.loveandwar.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunTraitTooltips;
import net.pttheta.loveandwar.gun.runtime.GunTraits;
import org.slf4j.LoggerFactory;

/** Explicitly gated visual fixture, excluded from both release artifacts. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class TraitClientScenario {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_traits").executes(context -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) return 0;
            ItemStack gun = minecraft.player.getMainHandItem().copy();
            if (!(gun.getItem() instanceof GunItem)) return 0;
            List<Component> collapsed = gun.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
            var expanded = new ArrayList<Component>();
            expanded.add(gun.getHoverName());
            expanded.addAll(GunTraitTooltips.lines(gun, true));
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TRAIT_CLIENT traits={} collapsed={} expanded={}",
                    GunTraits.resolve(gun), collapsed.stream().map(Component::getString).toList(), expanded.stream().map(Component::getString).toList());
            minecraft.setScreen(new Screen(Component.literal("Trait tooltip inspection")) {
                @Override
                public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    graphics.fill(0, 0, this.width, this.height, 0xE0202328);
                    graphics.drawString(this.font, "Item tooltip", 20, 16, 0xFFFFFF);
                    graphics.drawString(this.font, "Expanded crafting details", 250, 16, 0xFFFFFF);
                    graphics.renderComponentTooltip(this.font, collapsed, 20, 48);
                    graphics.renderComponentTooltip(this.font, expanded, 250, 48);
                }
            });
            return 1;
        }));
    }
}
