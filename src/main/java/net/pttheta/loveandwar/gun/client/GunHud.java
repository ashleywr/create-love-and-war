package net.pttheta.loveandwar.gun.client;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunHud {
   private static final ResourceLocation LAYER_ID = LoveAndWarMod.asResource("gun_ammo_hud");
   private static final float ICON_SCALE = 0.5F;
   private static final int ICON_SIZE = 8;
   private static final int ICON_STEP = 9;
   private static final int MAX_ICONS = 20;
   private static final int OVERFLOW_LABEL_RESERVE = 24;

   private GunHud() {
   }

   @SubscribeEvent
   public static void registerLayers(RegisterGuiLayersEvent event) {
      event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, GunHud::render);
   }

   private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui) {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.getItem() instanceof GunItem) {
            Optional<GunDef> defOpt = GunLookup.gunDef(stack);
            if (!defOpt.isEmpty()) {
               GunDef def = defOpt.get();
               List<ItemStack> loadedRounds = ((AmmoPouchContents)stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY)).items();
               int loaded = loadedRounds.size();
               Font font = mc.font;
               int x = graphics.guiWidth() / 2 + 100;
               int hotbarTop = graphics.guiHeight() - 22;
               int rightMargin = graphics.guiWidth() - x;
               if (rightMargin >= 10) {
                  int nameY = hotbarTop - 11;
                  GunActionState hudState = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                  Iterator rowY = def.modes().iterator();
                  if (rowY.hasNext()) {
                     GunDef.ModeEntry mode = (GunDef.ModeEntry)rowY.next();
                     String value = hudState.modeValues().getOrDefault(mode.key(), "off");
                     String key = "tooltip.mode." + mode.key() + "." + value;
                     Component hint = Component.translatable(key);
                     if (!key.equals(hint.getString())) {
                        graphics.drawString(font, hint, x, nameY - 10, -5592406, true);
                     }
                  }

                  graphics.drawString(font, stack.getHoverName(), x, nameY, -1, true);
                  int rowYx = hotbarTop + 7;
                  if (loaded == 0) {
                     graphics.pose().pushPose();
                     graphics.pose().translate(x, rowYx, 0.0F);
                     graphics.pose().scale(0.5F, 0.5F, 1.0F);
                     graphics.renderItem(stack, 0, 0);
                     graphics.pose().popPose();
                  } else {
                     int iconBudget = Math.max(0, (rightMargin - 24) / 9);
                     int cap = Math.min(20, iconBudget);
                     int shown = Math.min(loaded, cap);

                     for (int i = 0; i < shown; i++) {
                        ItemStack round = loadedRounds.get(i);
                        int iconX = x + i * 9;
                        graphics.pose().pushPose();
                        graphics.pose().translate(iconX, rowYx, 0.0F);
                        graphics.pose().scale(0.5F, 0.5F, 1.0F);
                        graphics.renderItem(round, 0, 0);
                        graphics.pose().popPose();
                     }

                     int overflow = loaded - shown;
                     if (overflow > 0) {
                        int textX = x + shown * 9 + 1;
                        graphics.drawString(font, "x" + overflow, textX, rowYx, -5592406, true);
                     }
                  }

                  GunActionState state = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                  if (isReloading(def, state)) {
                     renderReloadBar(graphics, font, x, rowYx, rightMargin, state);
                  }
               }
            }
         }
      }
   }

   private static void renderReloadBar(GuiGraphics graphics, Font font, int x, int rowY, int rightMargin, GunActionState state) {
      float pct = state.groupTotalTicks() > 0 ? Math.min(1.0F, (float)state.groupTicksElapsed() / state.groupTotalTicks()) : 0.0F;
      int barY = rowY + 8 + 3;
      int barWidth = Math.min(60, Math.max(0, rightMargin - 2));
      if (barWidth > 4) {
         graphics.fill(x, barY, x + barWidth, barY + 2, Integer.MIN_VALUE);
         int fillWidth = Math.round(barWidth * pct);
         if (fillWidth > 0) {
            graphics.fill(x, barY, x + fillWidth, barY + 2, -22016);
         }

         if (rightMargin - barWidth > 34) {
            graphics.drawString(font, "RELOAD", x + barWidth + 4, barY - 3, -22016, true);
         }
      }
   }

   private static boolean isReloading(GunDef def, GunActionState state) {
      if (!state.pendingReloadGroups().isEmpty()) {
         return true;
      }

      if (state.activeGroup().isEmpty()) {
         return false;
      }

      for (GunDef.ReloadDef reload : def.reloads()) {
         if (state.activeGroup().equals(reload.startActionKey())
            || state.activeGroup().equals(reload.ejectActionKey())
            || state.activeGroup().equals(reload.loadOneActionKey())
            || state.activeGroup().equals(reload.endActionKey())) {
            return true;
         }
      }

      return false;
   }
}
