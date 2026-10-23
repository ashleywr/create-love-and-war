package net.pttheta.loveandwar.fluid.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.pttheta.loveandwar.fluid.BaseFluidType;
import net.pttheta.loveandwar.fluid.ModFluidTypes;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class LAWFluidClientExtensions {
   private LAWFluidClientExtensions() {
   }

   @SubscribeEvent
   static void registerClientExtensions(RegisterClientExtensionsEvent event) {
      for (DeferredHolder<FluidType, FluidType> holder : ModFluidTypes.all()) {
         if (holder.get() instanceof BaseFluidType type) {
            event.registerFluidType(new LAWFluidClientExtensions.Extensions(type), new FluidType[]{type});
         }
      }
   }

   private record Extensions(BaseFluidType type) implements IClientFluidTypeExtensions {
      public ResourceLocation getStillTexture() {
         return this.type.getStillTexture();
      }

      public ResourceLocation getFlowingTexture() {
         return this.type.getFlowingTexture();
      }

      @Nullable
      public ResourceLocation getOverlayTexture() {
         return this.type.getOverlayTexture();
      }

      public int getTintColor() {
         return this.type.getTintColor();
      }

      public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
         return this.type.getFogColor();
      }

      public void modifyFogRender(Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
         RenderSystem.setShaderFogStart(1.0F);
         RenderSystem.setShaderFogEnd(6.0F);
      }
   }
}
