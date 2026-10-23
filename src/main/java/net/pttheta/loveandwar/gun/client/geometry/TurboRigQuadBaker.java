package net.pttheta.loveandwar.gun.client.geometry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TurboRigQuadBaker {
   private static final Map<Direction, int[]> FACE_VERTEX_ORDER = new EnumMap<>(Direction.class);

   private TurboRigQuadBaker() {
   }

   public static List<BakedQuad> applyMaterial(List<BakedQuad> quads, String material) {
      if ("Emissive".equalsIgnoreCase(material) && !quads.isEmpty()) {
         List<BakedQuad> lit = new ArrayList<>(quads);
         QuadTransformers.settingMaxEmissivity().processInPlace(lit);
         return lit;
      } else {
         return quads;
      }
   }

   public static List<BakedQuad> bakeElement(TurboRigModelData.TurboElement element, Function<String, TextureAtlasSprite> spriteByTexture) {
      return bakeElement(element, spriteByTexture, null);
   }

   public static List<BakedQuad> bakeElement(
      TurboRigModelData.TurboElement element, Function<String, TextureAtlasSprite> spriteByTexture, UnaryOperator<Vector3f> postTransform
   ) {
      Vector3f[] verts = placeVerts(element.verts(), element.eulerRotations(), element.rotationOrigin());
      List<BakedQuad> quads = new ArrayList<>(element.faces().size());

      for (Entry<Direction, TurboRigModelData.FaceUV> entry : element.faces().entrySet()) {
         TextureAtlasSprite sprite = spriteByTexture.apply(entry.getValue().textureRef());
         if (sprite != null) {
            quads.add(bakeFace(verts, entry.getKey(), entry.getValue(), sprite, postTransform));
         }
      }

      return quads;
   }

   private static Vector3f[] placeVerts(TurboRigModelData.Vec3[] verts, TurboRigModelData.Vec3 euler, TurboRigModelData.Vec3 origin) {
      Vector3f[] out = new Vector3f[8];
      boolean identity = euler.x() == 0.0F && euler.y() == 0.0F && euler.z() == 0.0F;
      Quaternionf rotation = identity
         ? null
         : new Quaternionf().rotationZYX((float)Math.toRadians(euler.z()), (float)Math.toRadians(euler.y()), (float)Math.toRadians(euler.x()));

      for (int i = 0; i < 8; i++) {
         Vector3f v = new Vector3f(verts[i].x(), verts[i].y(), verts[i].z());
         if (!identity) {
            rotation.transform(v);
         }

         v.add(origin.x(), origin.y(), origin.z());
         out[i] = v;
      }

      return out;
   }

   private static BakedQuad bakeFace(
      Vector3f[] verts, Direction direction, TurboRigModelData.FaceUV uv, TextureAtlasSprite sprite, UnaryOperator<Vector3f> postTransform
   ) {
      int[] order = FACE_VERTEX_ORDER.get(direction);
      Vector3f[] pos = new Vector3f[4];
      float[] u = new float[4];
      float[] v = new float[4];

      for (int i = 0; i < 4; i++) {
         Vector3f p = new Vector3f(verts[order[i]]).div(16.0F);
         if (postTransform != null) {
            p = postTransform.apply(p);
         }

         pos[i] = p;
         int shifted = Math.floorMod(i + uv.rotation() / 90, 4);
         float rawU = shifted != 0 && shifted != 1 ? uv.u2() : uv.u1();
         float rawV = shifted != 0 && shifted != 3 ? uv.v2() : uv.v1();
         u[i] = sprite.getU(rawU);
         v[i] = sprite.getV(rawV);
      }

      Vector3f normal = new Vector3f(pos[2]).sub(pos[1]).cross(new Vector3f(pos[0]).sub(pos[1]));
      if (normal.isFinite() && normal.lengthSquared() > 1.0E-12F) {
         normal.normalize();
      } else {
         normal = null;
      }

      Vector3f elementCentre = new Vector3f();

      for (int i = 0; i < 8; i++) {
         Vector3f p = new Vector3f(verts[i]).div(16.0F);
         if (postTransform != null) {
            p = postTransform.apply(p);
         }

         elementCentre.add(p);
      }

      elementCentre.mul(0.125F);
      Vector3f faceCentre = new Vector3f();

      for (Vector3f p : pos) {
         faceCentre.add(p);
      }

      faceCentre.mul(0.25F);
      Vector3f outward = faceCentre.sub(elementCentre, new Vector3f());
      boolean reverseWinding;
      if (normal == null) {
         reverseWinding = false;
      } else if (outward.lengthSquared() > 1.0E-12F) {
         reverseWinding = normal.dot(outward) < 0.0F;
      } else {
         reverseWinding = normal.dot(direction.getStepX(), direction.getStepY(), direction.getStepZ()) < 0.0F;
      }

      int packedNormal = 0;
      Vector3f d = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
      Vector3f n = d;
      if (postTransform != null) {
         Vector3f origin = postTransform.apply(new Vector3f(0.5F, 0.5F, 0.5F));
         n = postTransform.apply(new Vector3f(0.5F, 0.5F, 0.5F).add(d)).sub(origin);
      }

      if (n.lengthSquared() > 1.0E-12F) {
         n.normalize();
         packedNormal = (byte)Math.round(n.x() * 127.0F) & 255 | ((byte)Math.round(n.y() * 127.0F) & 255) << 8 | ((byte)Math.round(n.z() * 127.0F) & 255) << 16;
      }

      int[] data = new int[32];
      int[] outputOrder = reverseWinding ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};

      for (int slot = 0; slot < 4; slot++) {
         int i = outputOrder[slot];
         int base = slot * 8;
         data[base] = Float.floatToRawIntBits(pos[i].x());
         data[base + 1] = Float.floatToRawIntBits(pos[i].y());
         data[base + 2] = Float.floatToRawIntBits(pos[i].z());
         data[base + 3] = -1;
         data[base + 4] = Float.floatToRawIntBits(u[i]);
         data[base + 5] = Float.floatToRawIntBits(v[i]);
         data[base + 7] = packedNormal;
      }

      Direction bakedDirection = direction;
      if (normal != null) {
         Vector3f finalNormal = reverseWinding ? normal.negate() : normal;
         bakedDirection = nearestAxis(finalNormal, direction);
      }

      return new BakedQuad(data, -1, bakedDirection, sprite, true);
   }

   private static Direction nearestAxis(Vector3f normal, Direction fallback) {
      Direction best = null;
      float bestDot = 0.0F;

      for (Direction candidate : Direction.values()) {
         float dot = normal.dot(candidate.getStepX(), candidate.getStepY(), candidate.getStepZ());
         if (dot > bestDot) {
            bestDot = dot;
            best = candidate;
         }
      }

      return best == null ? fallback : best;
   }

   static {
      FACE_VERTEX_ORDER.put(Direction.UP, new int[]{7, 3, 2, 6});
      FACE_VERTEX_ORDER.put(Direction.DOWN, new int[]{5, 4, 0, 1});
      FACE_VERTEX_ORDER.put(Direction.NORTH, new int[]{0, 2, 3, 1});
      FACE_VERTEX_ORDER.put(Direction.SOUTH, new int[]{5, 7, 6, 4});
      FACE_VERTEX_ORDER.put(Direction.EAST, new int[]{1, 3, 7, 5});
      FACE_VERTEX_ORDER.put(Direction.WEST, new int[]{4, 6, 2, 0});
   }
}
