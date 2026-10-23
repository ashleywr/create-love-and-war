package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class AdsBlendState {
   private static final float RATE_IN = 8.0F;
   private static final float RATE_OUT = 10.0F;
   private static final boolean DIAG = Boolean.getBoolean("lw.adsdiag");
   private static float blend;
   private static long lastFrameNanos;

   private AdsBlendState() {
   }

   @SubscribeEvent
   static void onRenderFrame(Pre event) {
      boolean aiming = aiming();
      long now = System.nanoTime();
      float dt = lastFrameNanos == 0L ? 0.0F : (float)(now - lastFrameNanos) / 1.0E9F;
      lastFrameNanos = now;
      dt = Math.min(dt, 0.1F);
      float t = Mth.clamp(dt * (aiming ? 8.0F : 10.0F), 0.0F, 1.0F);
      if (DIAG) {
         LoggerFactory.getLogger("createloveandwar/ads").info("ADS dt={} t={} blend={} aiming={}", new Object[]{dt, t, blend, aiming});
      }

      blend = Mth.lerp(t, blend, aiming ? 1.0F : 0.0F);
      if (!aiming && blend < 0.01F) {
         blend = 0.0F;
      }
   }

   public static float blend() {
      return blend;
   }

   public static boolean fullyAimed() {
      return blend > 0.999F;
   }

   private static boolean aiming() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      }

      ItemStack stack = mc.player.getMainHandItem();
      if (!(stack.getItem() instanceof GunItem)) {
         return false;
      }

      GunActionState state = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);

      for (String toggle : state.activeToggles()) {
         if (toggle.startsWith("ads")) {
            return true;
         }
      }

      return false;
   }
}
