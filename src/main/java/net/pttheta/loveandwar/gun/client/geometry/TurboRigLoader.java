package net.pttheta.loveandwar.gun.client.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class TurboRigLoader implements IGeometryLoader<TurboRigGeometry> {
   public TurboRigGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
      Map<String, TurboRigModelData.PartDef> parts = new LinkedHashMap<>();
      if (json.has("parts")) {
         JsonObject partsJson = GsonHelper.getAsJsonObject(json, "parts");

         for (Entry<String, JsonElement> entry : partsJson.entrySet()) {
            parts.put(entry.getKey(), readPart(entry.getKey(), entry.getValue().getAsJsonObject()));
         }
      }

      List<TurboRigModelData.AttachPoint> attachPoints = new ArrayList<>();
      if (json.has("attachPoints")) {
         for (JsonElement e : GsonHelper.getAsJsonArray(json, "attachPoints")) {
            attachPoints.add(readAttachPoint(e.getAsJsonObject()));
         }
      }

      Map<String, ResourceLocation> icons = new LinkedHashMap<>();
      if (json.has("icons")) {
         JsonObject iconsJson = GsonHelper.getAsJsonObject(json, "icons");

         for (Entry<String, JsonElement> entry : iconsJson.entrySet()) {
            icons.put(entry.getKey(), ResourceLocation.parse(entry.getValue().getAsString()));
         }
      }

      List<String> paintjobs = new ArrayList<>();
      List<String> skinKeys = new ArrayList<>();
      if (json.has("textures")) {
         JsonObject texturesJson = GsonHelper.getAsJsonObject(json, "textures");

         for (String key : texturesJson.keySet()) {
            skinKeys.add(key);
            if (!key.equals("layer0") && !key.equals("particle") && !key.equals("default")) {
               paintjobs.add(key);
            }
         }
      }

      return new TurboRigGeometry(new TurboRigModelData(parts, attachPoints, paintjobs, icons,skinKeys));
   }

   private static TurboRigModelData.PartDef readPart(String name, JsonObject partJson) {
      TurboRigModelData.Vec3 origin = partJson.has("origin") ? readVec3(GsonHelper.getAsJsonArray(partJson, "origin")) : TurboRigModelData.Vec3.ZERO;
      List<TurboRigModelData.TurboElement> elements = new ArrayList<>();
      if (partJson.has("turboelements")) {
         for (JsonElement e : GsonHelper.getAsJsonArray(partJson, "turboelements")) {
            elements.add(readElement(e.getAsJsonObject()));
         }
      }

      return new TurboRigModelData.PartDef(name, origin, elements, GsonHelper.getAsString(partJson, "material", ""));
   }

   private static TurboRigModelData.TurboElement readElement(JsonObject elementJson) {
      JsonArray vertsJson = GsonHelper.getAsJsonArray(elementJson, "verts");
      if (vertsJson.size() != 8) {
         throw new JsonParseException("turboelement 'verts' must have exactly 8 entries, found " + vertsJson.size());
      }

      TurboRigModelData.Vec3[] verts = new TurboRigModelData.Vec3[8];

      for (int i = 0; i < 8; i++) {
         verts[i] = readVec3(vertsJson.get(i).getAsJsonArray());
      }

      TurboRigModelData.Vec3 euler = elementJson.has("eulerRotations")
         ? readVec3(GsonHelper.getAsJsonArray(elementJson, "eulerRotations"))
         : TurboRigModelData.Vec3.ZERO;
      TurboRigModelData.Vec3 rotationOrigin = elementJson.has("rotationOrigin")
         ? readVec3(GsonHelper.getAsJsonArray(elementJson, "rotationOrigin"))
         : TurboRigModelData.Vec3.ZERO;
      Map<Direction, TurboRigModelData.FaceUV> faces = new LinkedHashMap<>();
      if (elementJson.has("faces")) {
         JsonObject facesJson = GsonHelper.getAsJsonObject(elementJson, "faces");

         for (Entry<String, JsonElement> entry : facesJson.entrySet()) {
            Direction direction = Direction.byName(entry.getKey());
            if (direction == null) {
               throw new JsonParseException("Unknown turboelement face direction '" + entry.getKey() + "'");
            }

            faces.put(direction, readFaceUV(entry.getValue().getAsJsonObject()));
         }
      }

      return new TurboRigModelData.TurboElement(verts, euler, rotationOrigin, faces);
   }

   private static TurboRigModelData.FaceUV readFaceUV(JsonObject faceJson) {
      JsonArray uv = GsonHelper.getAsJsonArray(faceJson, "uv");
      if (uv.size() != 4) {
         throw new JsonParseException("turboelement face 'uv' must have exactly 4 entries, found " + uv.size());
      }

      float u1 = uv.get(0).getAsFloat();
      float v1 = uv.get(1).getAsFloat();
      float u2 = uv.get(2).getAsFloat();
      float v2 = uv.get(3).getAsFloat();
      int rotation = GsonHelper.getAsInt(faceJson, "rotation", 0);
      String textureRef = GsonHelper.getAsString(faceJson, "texture", "#default");
      if (textureRef.startsWith("#")) {
         textureRef = textureRef.substring(1);
      }

      return new TurboRigModelData.FaceUV(u1, v1, u2, v2, rotation, textureRef);
   }

   private static TurboRigModelData.AttachPoint readAttachPoint(JsonObject json) {
      String name = GsonHelper.getAsString(json, "name");
      String attachTo = GsonHelper.getAsString(json, "attachTo", "");
      TurboRigModelData.Vec3 offset = json.has("offset") ? readVec3(GsonHelper.getAsJsonArray(json, "offset")) : TurboRigModelData.Vec3.ZERO;
      TurboRigModelData.Vec3 euler = json.has("euler") ? readVec3(GsonHelper.getAsJsonArray(json, "euler")) : TurboRigModelData.Vec3.ZERO;
      return new TurboRigModelData.AttachPoint(name, attachTo, offset, euler);
   }

   private static TurboRigModelData.Vec3 readVec3(JsonArray array) {
      if (array.size() != 3) {
         throw new JsonParseException("Expected a 3-element [x,y,z] array, found " + array.size() + " entries");
      } else {
         return new TurboRigModelData.Vec3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
      }
   }
}
