package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContentCodecs {
   public static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-content");
   public static final Codec<JsonElement> RAW_JSON = Codec.PASSTHROUGH
      .xmap(dynamic -> (JsonElement)dynamic.convert(JsonOps.INSTANCE).getValue(), json -> new Dynamic(JsonOps.INSTANCE, json));

   private ContentCodecs() {
   }

   public static <E extends Enum<E>> Codec<E> lenientEnum(Class<E> type, Function<E, String> keyOf, E fallback) {
      Map<String, E> byKey = Arrays.stream(type.getEnumConstants()).collect(Collectors.toMap(keyOf, Function.identity(), (a, b) -> a));
      return Codec.STRING.xmap(key -> byKey.computeIfAbsent(key, unknown -> {
         LOGGER.debug("Unknown {} value '{}', treating as {}", new Object[]{type.getSimpleName(), unknown, fallback});
         return fallback;
      }), keyOf);
   }
}
