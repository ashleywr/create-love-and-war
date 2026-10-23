package net.pttheta.loveandwar.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import org.joml.Vector3f;

public class BaseFluidType extends FluidType {
   private final ResourceLocation stillTexture;
   private final ResourceLocation flowingTexture;
   private final ResourceLocation overlayTexture;
   private final int tintColor;
   private final Vector3f fogColor;

   public BaseFluidType(
      ResourceLocation stillTexture, ResourceLocation flowingTexture, ResourceLocation overlayTexture, int tintColor, Vector3f fogColor, Properties properties
   ) {
      super(properties);
      this.stillTexture = stillTexture;
      this.flowingTexture = flowingTexture;
      this.overlayTexture = overlayTexture;
      this.tintColor = tintColor;
      this.fogColor = fogColor;
   }

   public ResourceLocation getStillTexture() {
      return this.stillTexture;
   }

   public ResourceLocation getFlowingTexture() {
      return this.flowingTexture;
   }

   public ResourceLocation getOverlayTexture() {
      return this.overlayTexture;
   }

   public int getTintColor() {
      return this.tintColor;
   }

   public Vector3f getFogColor() {
      return this.fogColor;
   }
}
