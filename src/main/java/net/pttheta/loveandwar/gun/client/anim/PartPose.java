package net.pttheta.loveandwar.gun.client.anim;

import java.util.List;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record PartPose(Vector3f position, Vector3f rotationDeg, Vector3f scale) {
   public static final PartPose IDENTITY = new PartPose(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

   static PartPose from(Flanimation.RawPose raw) {
      return new PartPose(vec3(raw.position(), 0.0F), vec3(raw.rotation(), 0.0F), fvec3(raw.scale(), 1.0F));
   }

   private static Vector3f vec3(List<String> values, float fallback) {
      return new Vector3f(comp(values, 0, fallback), comp(values, 1, fallback), comp(values, 2, fallback));
   }

   private static float comp(List<String> values, int index, float fallback) {
      return index < values.size() ? Flanimation.parseNum(values.get(index)) : fallback;
   }

   private static Vector3f fvec3(List<Float> values, float fallback) {
      return new Vector3f(fcomp(values, 0, fallback), fcomp(values, 1, fallback), fcomp(values, 2, fallback));
   }

   private static float fcomp(List<Float> values, int index, float fallback) {
      return index < values.size() && values.get(index) != null ? values.get(index) : fallback;
   }

   public boolean isIdentity() {
      return this.position.lengthSquared() == 0.0F
         && this.rotationDeg.lengthSquared() == 0.0F
         && this.scale.x() == 1.0F
         && this.scale.y() == 1.0F
         && this.scale.z() == 1.0F;
   }

   public static PartPose lerp(PartPose a, PartPose b, float t) {
      if (t <= 0.0F) {
         return a;
      } else {
         return t >= 1.0F ? b : new PartPose(lerp(a.position, b.position, t), lerp(a.rotationDeg, b.rotationDeg, t), lerp(a.scale, b.scale, t));
      }
   }

   private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
      return new Vector3f(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t, a.z() + (b.z() - a.z()) * t);
   }

   public Matrix4f toMatrix() {
      Matrix4f m = new Matrix4f();
      float k = 1.0F;
      if (this.position.lengthSquared() != 0.0F) {
         float sgn = 1.0F;
         m.translate(1.0F * sgn * this.position.x() / 16.0F, 1.0F * sgn * this.position.y() / 16.0F, 1.0F * sgn * this.position.z() / 16.0F);
      }

      if (this.rotationDeg.lengthSquared() != 0.0F) {
         m.rotate(flansEuler(1.0F * this.rotationDeg.x(), 1.0F * this.rotationDeg.y(), 1.0F * this.rotationDeg.z()));
      }

      if (this.scale.x() != 1.0F || this.scale.y() != 1.0F || this.scale.z() != 1.0F) {
         m.scale(this.scale.x(), this.scale.y(), this.scale.z());
      }

      return m;
   }

   public Vector3f apply(Vector3f blockSpacePoint, TurboRigModelData.Vec3 originPixels) {
      if (this.isIdentity()) {
         return blockSpacePoint;
      }

      Vector3f origin = new Vector3f(originPixels.x(), originPixels.y(), originPixels.z()).div(16.0F);
      Vector3f local = new Vector3f(blockSpacePoint).sub(origin);
      local.mul(this.scale.x(), this.scale.y(), this.scale.z());
      if (this.rotationDeg.lengthSquared() != 0.0F) {
         flansEuler(this.rotationDeg.x(), this.rotationDeg.y(), this.rotationDeg.z()).transform(local);
      }

      local.add(origin);
      local.add(this.position.x() / 16.0F, this.position.y() / 16.0F, this.position.z() / 16.0F);
      return local;
   }

   private static Quaternionf flansEuler(float xDeg, float yDeg, float zDeg) {
      return new Quaternionf().rotateY((float)Math.toRadians(-yDeg)).rotateX((float)Math.toRadians(-xDeg)).rotateZ((float)Math.toRadians(-zDeg));
   }

   @Override
   public String toString() {
      return String.format(
         "pos(%.2f,%.2f,%.2f) rot(%.2f,%.2f,%.2f) scale(%.2f,%.2f,%.2f)",
         this.position.x(),
         this.position.y(),
         this.position.z(),
         this.rotationDeg.x(),
         this.rotationDeg.y(),
         this.rotationDeg.z(),
         this.scale.x(),
         this.scale.y(),
         this.scale.z()
      );
   }
}
