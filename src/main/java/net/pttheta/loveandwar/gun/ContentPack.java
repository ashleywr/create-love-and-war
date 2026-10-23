package net.pttheta.loveandwar.gun;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.pttheta.loveandwar.LoveAndWarMod;

public final class ContentPack {
   private static final Map<String, JsonObject> CACHE = new ConcurrentHashMap<>();

   private ContentPack() {
   }

   public static JsonObject load(String kind, String name) {
      return CACHE.computeIfAbsent(kind + "/" + name, key -> {
         String path = "/data/createloveandwar/" + key + ".json";

         try (InputStream in = ContentPack.class.getResourceAsStream(path)) {
            if (in == null) {
               LoveAndWarMod.LOGGER.debug("No gun content file {}, using defaults", path);
               return new JsonObject();
            } else {
               return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
         } catch (Exception e) {
            LoveAndWarMod.LOGGER.error("Could not read gun content file {}", path, e);
            return new JsonObject();
         }
      });
   }

   public static int maxStackSize(String kind, String name, int fallback) {
      JsonObject root = load(kind, name);
      if (root.has("itemSettings")) {
         JsonObject settings = root.getAsJsonObject("itemSettings");
         if (settings.has("maxStackSize")) {
            int size = settings.get("maxStackSize").getAsInt();
            if (size > 0) {
               return size;
            }
         }
      }

      return fallback;
   }
}
