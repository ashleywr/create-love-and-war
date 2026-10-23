package net.pttheta.loveandwar.gun.client.anim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FlanimationLoader {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-anim");
   private static final Map<ResourceLocation, Optional<ResolvedAnimationSet>> CACHE = new ConcurrentHashMap<>();

   private FlanimationLoader() {
   }

   static Optional<ResolvedAnimationSet> get(String animationSet) {
      Optional<ResourceLocation> id = parseId(animationSet);
      return id.isEmpty() ? Optional.empty() : CACHE.computeIfAbsent(id.get(), FlanimationLoader::load);
   }

   private static Optional<ResourceLocation> parseId(String raw) {
      if (raw != null && !raw.isEmpty()) {
         ResourceLocation id = raw.indexOf(58) >= 0 ? ResourceLocation.tryParse(raw) : ResourceLocation.tryBuild("minecraft", raw);
         if (id == null) {
            LOGGER.warn("Invalid animationSet id '{}'", raw);
         }

         return Optional.ofNullable(id);
      } else {
         return Optional.empty();
      }
   }

   private static Optional<ResolvedAnimationSet> load(ResourceLocation id) {
      String path = "/assets/" + id.getNamespace() + "/flanimations/" + id.getPath() + ".json";

      try (InputStream in = FlanimationLoader.class.getResourceAsStream(path)) {
         if (in == null) {
            return Optional.empty();
         }

         JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
         DataResult<Flanimation> result = Flanimation.CODEC.parse(JsonOps.INSTANCE, json);
         Optional<Flanimation> parsed = result.resultOrPartial(msg -> LOGGER.error("Failed to parse flanimation {}: {}", path, msg));
         return parsed.map(ResolvedAnimationSet::resolve);
      } catch (Exception e) {
         LOGGER.error("Could not read flanimation file {}", path, e);
         return Optional.empty();
      }
   }
}
