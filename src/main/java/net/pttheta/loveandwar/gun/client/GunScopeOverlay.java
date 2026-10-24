package net.pttheta.loveandwar.gun.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.StatSources;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunScopeOverlay {
   private static final int WIDTH_PER_HEIGHT = 4;

   private GunScopeOverlay() {
   }

   @SubscribeEvent(receiveCanceled = true)
   static void onRenderCrosshair(Pre event) {
      if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
         ResourceLocation overlay = activeScopeOverlay();
         if (overlay != null) {
            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = graphics.guiWidth();
            int screenHeight = graphics.guiHeight();
            int overlayWidth = WIDTH_PER_HEIGHT * screenHeight;
            int x = screenWidth / 2 - overlayWidth / 2;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(overlay, x, 0, overlayWidth, screenHeight, 0.0F, 0.0F, 1, 1, 1, 1);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            event.setCanceled(true);
         }
         if (hasActiveSight()) event.setCanceled(true);
      }
   }

   private static ResourceLocation activeScopeOverlay() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         for (var hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            var def = GunLookup.gunDef(stack);
            if (!(stack.getItem() instanceof GunItem) || def.isEmpty()) continue;
            var texture = scopeOverlay(stack, mc.player.getUUID());
            if (texture != null) return texture;
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean hasActiveSight() {
      var player=Minecraft.getInstance().player;
      if(player==null)return false;
      for(var hand:InteractionHand.values()) {
         var stack=player.getItemInHand(hand);
         if(!(stack.getItem() instanceof GunItem))continue;
         for(var entry:GunActiveGroups.of(stack).entries())if(entry.owner().equals(player.getUUID())) {
            var group=entry.resolve();
            if(group.isPresent()&&group.get().actions().stream().anyMatch(action->"AimDownSights".equals(action.actionType())||"Scope".equals(action.actionType())))return true;
         }
      }
      return false;
   }

   static ResourceLocation scopeOverlay(ItemStack stack, java.util.UUID owner) {
      var def = GunLookup.gunDef(stack);
      if (def.isEmpty()) return null;
      var attachments = stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
      for (var entry : GunActiveGroups.of(stack).entries()) {
         if (!entry.owner().equals(owner)) continue;
         var group = entry.resolve();
         if (group.isEmpty()) continue;
         var sources = StatSources.forFiring(def.get(), group.get(), attachments);
         for (var action : group.get().actions()) if ("Scope".equals(action.actionType())) {
            String declared = StatResolver.resolveOverride("scope_overlay", action.scopeOverlay(), sources);
            return resolveOverlay(declared);
         }
      }
      return null;
   }

   static ResourceLocation resolveOverlay(String declared) {
      if (!declared.contains(":")) {
         return ResourceLocation.fromNamespaceAndPath("flansmod", "textures/gui/" + declared + ".png");
      }

      String namespace = declared.substring(0, declared.indexOf(58));
      String path = declared.substring(declared.indexOf(58) + 1);
      String full = path.contains("textures/gui/") ? path : (path.contains("gui/") ? "textures/" + path : "textures/gui/" + path);
      return ResourceLocation.tryBuild(namespace, full + ".png");
   }
}
