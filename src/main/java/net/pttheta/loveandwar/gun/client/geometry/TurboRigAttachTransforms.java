package net.pttheta.loveandwar.gun.client.geometry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TurboRigAttachTransforms {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-geometry");

   private TurboRigAttachTransforms() {
   }

   static Matrix4f attachPointMatrix(TurboRigModelData data, String name) {
      Map<String, TurboRigModelData.AttachPoint> attachPointsByName = new LinkedHashMap<>();

      for (TurboRigModelData.AttachPoint attachPoint : data.attachPoints()) {
         attachPointsByName.put(attachPoint.name(), attachPoint);
      }

      return !attachPointsByName.containsKey(name)
         ? null
         : new Matrix4f(resolve(name, attachPointsByName, data.parts(), ignored -> null, new HashMap<>(), new LinkedHashSet<>()));
   }

   static Matrix4f attachPointMatrix(TurboRigModelData data, String name, Function<String, Matrix4f> poseByNode) {
      Map<String, TurboRigModelData.AttachPoint> attachPointsByName = new LinkedHashMap<>();

      for (TurboRigModelData.AttachPoint attachPoint : data.attachPoints()) {
         attachPointsByName.put(attachPoint.name(), attachPoint);
      }

      return !attachPointsByName.containsKey(name)
         ? null
         : new Matrix4f(resolve(name, attachPointsByName, data.parts(), poseByNode, new HashMap<>(), new LinkedHashSet<>()));
   }

   static Map<String, Matrix4f> resolvePartTransforms(TurboRigModelData data) {
      return resolvePartTransforms(data, name -> null);
   }

   static Map<String, Matrix4f> resolvePartTransforms(TurboRigModelData data, Function<String, Matrix4f> poseByNode) {
      Map<String, TurboRigModelData.AttachPoint> attachPointsByName = new LinkedHashMap<>();

      for (TurboRigModelData.AttachPoint attachPoint : data.attachPoints()) {
         attachPointsByName.put(attachPoint.name(), attachPoint);
      }

      Map<String, Matrix4f> cache = new HashMap<>();
      Map<String, Matrix4f> result = new LinkedHashMap<>();

      for (String partName : data.parts().keySet()) {
         result.put(partName, new Matrix4f(resolve(partName, attachPointsByName, data.parts(), poseByNode, cache, new LinkedHashSet<>())));
      }

      return result;
   }

   private static Matrix4f resolve(
      String name,
      Map<String, TurboRigModelData.AttachPoint> attachPointsByName,
      Map<String, TurboRigModelData.PartDef> parts,
      Function<String, Matrix4f> poseByNode,
      Map<String, Matrix4f> cache,
      Set<String> path
   ) {
      Matrix4f cached = cache.get(name);
      if (cached != null) {
         return cached;
      }

      if (!path.add(name)) {
         LOGGER.warn("turborig attachPoint chain has a cycle involving '{}' - treating it as attached directly to the root", name);
         Matrix4f identity = new Matrix4f();
         cache.put(name, identity);
         return identity;
      }

      TurboRigModelData.AttachPoint self = attachPointsByName.get(name);
      TurboRigModelData.PartDef partDef = parts.get(name);
      Matrix4f local = localMatrix(self, partDef);
      Matrix4f pose = poseByNode.apply(name);
      boolean poseFirst = true;
      if (pose != null) {
         local.mul(pose);
      }

      local.mul(originMatrix(partDef));
      Matrix4f world;
      if (self == null) {
         world = local;
      } else {
         String parentName = self.attachTo();
         boolean parentExists = parentName != null && !parentName.isEmpty() && (attachPointsByName.containsKey(parentName) || parts.containsKey(parentName));
         if (!parentExists) {
            if (parentName != null && !parentName.isEmpty()) {
               LOGGER.debug(
                  "turborig attachPoint '{}' attaches to '{}', which isn't a part or attach point in this model - resolving it as root", name, parentName
               );
            }

            world = local;
         } else {
            Matrix4f parentWorld = resolve(parentName, attachPointsByName, parts, poseByNode, cache, path);
            world = new Matrix4f(parentWorld).mul(local);
         }
      }

      path.remove(name);
      cache.put(name, world);
      return world;
   }

   private static Matrix4f localMatrix(TurboRigModelData.AttachPoint attachPoint, TurboRigModelData.PartDef part) {
      Matrix4f m = new Matrix4f();
      if (attachPoint != null) {
         TurboRigModelData.Vec3 offset = attachPoint.offset();
         if (offset.x() != 0.0F || offset.y() != 0.0F || offset.z() != 0.0F) {
            m.translate(offset.x() / 16.0F, offset.y() / 16.0F, offset.z() / 16.0F);
         }

         TurboRigModelData.Vec3 euler = attachPoint.euler();
         if (euler.x() != 0.0F || euler.y() != 0.0F || euler.z() != 0.0F) {
            m.rotate(
               new Quaternionf()
                  .rotateY((float)Math.toRadians(-euler.y()))
                  .rotateX((float)Math.toRadians(-euler.x()))
                  .rotateZ((float)Math.toRadians(-euler.z()))
            );
         }
      }

      return m;
   }

   private static Matrix4f originMatrix(TurboRigModelData.PartDef part) {
      return new Matrix4f();
   }
}
