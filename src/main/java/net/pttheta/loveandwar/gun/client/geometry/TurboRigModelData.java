package net.pttheta.loveandwar.gun.client.geometry;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class TurboRigModelData {
   private final Map<String, TurboRigModelData.PartDef> parts;
   private final List<TurboRigModelData.AttachPoint> attachPoints;
   private final List<String> paintjobs;
   private final Map<String, ResourceLocation> icons;

   public TurboRigModelData(
      Map<String, TurboRigModelData.PartDef> parts,
      List<TurboRigModelData.AttachPoint> attachPoints,
      List<String> paintjobs,
      Map<String, ResourceLocation> icons
   ) {
      this.parts = Map.copyOf(parts);
      this.attachPoints = List.copyOf(attachPoints);
      this.paintjobs = List.copyOf(paintjobs);
      this.icons = Map.copyOf(icons);
   }

   public Map<String, TurboRigModelData.PartDef> parts() {
      return this.parts;
   }

   public List<TurboRigModelData.AttachPoint> attachPoints() {
      return this.attachPoints;
   }

   public List<String> paintjobs() {
      return this.paintjobs;
   }

   public Map<String, ResourceLocation> icons() {
      return this.icons;
   }

   public record AttachPoint(String name, String attachTo, TurboRigModelData.Vec3 offset, TurboRigModelData.Vec3 euler) {
   }

   public record FaceUV(float u1, float v1, float u2, float v2, int rotation, String textureRef) {
   }

   public record PartDef(String name, TurboRigModelData.Vec3 origin, List<TurboRigModelData.TurboElement> elements, String material) {
   }

   public record TurboElement(
      TurboRigModelData.Vec3[] verts,
      TurboRigModelData.Vec3 eulerRotations,
      TurboRigModelData.Vec3 rotationOrigin,
      Map<Direction, TurboRigModelData.FaceUV> faces
   ) {
   }

   public record Vec3(float x, float y, float z) {
      public static final TurboRigModelData.Vec3 ZERO = new TurboRigModelData.Vec3(0.0F, 0.0F, 0.0F);
   }
}
