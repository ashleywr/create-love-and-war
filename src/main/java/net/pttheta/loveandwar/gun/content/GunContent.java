package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GunContent {
   private static final String MOD_ID = "createloveandwar";
   public static final ContentLoadReport GUNS = load("guns", GunDef.CODEC);
   public static final ContentLoadReport PARTS = load("parts", PartDef.CODEC);
   public static final ContentLoadReport BULLETS = load("bullets", BulletDef.CODEC);
   public static final ContentLoadReport MAGAZINES = load("magazines", MagazineDef.CODEC);
   public static final ContentLoadReport ATTACHMENTS = load("attachments", AttachmentDef.CODEC);
   public static final ContentLoadReport TRAITS = load("traits", TraitDef.CODEC);
   public static final ContentLoadReport ABILITIES = load("abilities", AbilityDef.CODEC);
   public static final ContentLoadReport MATERIALS = load("materials", MaterialDef.CODEC);

   private GunContent() {
   }

   public static Optional<GunDef> gun(String name) {
      return get(GUNS, name);
   }

   public static Optional<PartDef> part(String name) {
      return get(PARTS, name);
   }

   public static Optional<BulletDef> bullet(String name) {
      return get(BULLETS, name);
   }

   public static Optional<MagazineDef> magazine(String name) {
      return get(MAGAZINES, name);
   }

   public static Optional<AttachmentDef> attachment(String name) {
      return get(ATTACHMENTS, name);
   }

   public static Optional<TraitDef> trait(String name) {
      return get(TRAITS, name);
   }

   public static Optional<AbilityDef> ability(String name) {
      return get(ABILITIES, name);
   }

   public static Optional<MaterialDef> material(String name) {
      return get(MATERIALS, name);
   }

   public static Map<String, GunDef> guns() {
      return all(GUNS);
   }

   public static Map<String, PartDef> parts() {
      return all(PARTS);
   }

   public static Map<String, BulletDef> bullets() {
      return all(BULLETS);
   }

   public static Map<String, MagazineDef> magazines() {
      return all(MAGAZINES);
   }

   public static Map<String, AttachmentDef> attachments() {
      return all(ATTACHMENTS);
   }

   public static Map<String, TraitDef> traits() {
      return all(TRAITS);
   }

   public static Map<String, AbilityDef> abilities() {
      return all(ABILITIES);
   }

   public static Map<String, MaterialDef> materials() {
      return all(MATERIALS);
   }

   public static List<ContentLoadReport> allReports() {
      return List.of(GUNS, PARTS, BULLETS, MAGAZINES, ATTACHMENTS, TRAITS, ABILITIES, MATERIALS);
   }

   private static <T> Optional<T> get(ContentLoadReport report, String name) {
      return Optional.ofNullable((T)report.parsed().get(name));
   }

   private static <T> Map<String, T> all(ContentLoadReport report) {
      return (Map<String, T>)report.parsed();
   }

   private static <T> ContentLoadReport load(String kind, Codec<T> codec) {
      List<String> names = ClasspathDir.listJsonNames("/data/createloveandwar/" + kind);
      Map<String, T> parsed = new LinkedHashMap<>();
      List<String> failures = new ArrayList<>();

      for (String name : names) {
         String path = "/data/createloveandwar/" + kind + "/" + name + ".json";

         try (InputStream in = GunContent.class.getResourceAsStream(path)) {
            if (in != null) {
               JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
               DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
               Optional<T> value = result.resultOrPartial(msg -> ContentCodecs.LOGGER.error("Failed to parse {}/{}: {}", new Object[]{kind, name, msg}));
               if (value.isPresent()) {
                  parsed.put(name, value.get());
               } else {
                  failures.add(name);
               }
            } else {
               ContentCodecs.LOGGER.warn("Listed but could not open {}", path);
               failures.add(name);
            }
         } catch (Exception e) {
            ContentCodecs.LOGGER.error("Could not read gun content file {}", path, e);
            failures.add(name);
         }
      }

      if (!failures.isEmpty()) {
         ContentCodecs.LOGGER.warn("{} content pack: {}/{} files failed to parse: {}", new Object[]{kind, failures.size(), names.size(), failures});
      }

      return new ContentLoadReport(kind, names.size(), parsed, failures);
   }
}
