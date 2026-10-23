package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record GunDef(
   ItemSettings itemSettings,
   GunDef.Paints paints,
   List<InputHandler> inputHandlers,
   List<ActionGroup> actionGroups,
   List<GunDef.MagazineSlot> magazines,
   List<GunDef.ReloadDef> reloads,
   List<GunDef.ModeEntry> modes,
   List<JsonElement> loopingSounds,
   GunDef.AttachmentSlots attachmentSlots,
   List<JsonElement> modelParts,
   String animationSet,
   int particleCount,
   String casingModel,
   List<InlineAbility> staticAbilities
) {
   public static final Codec<GunDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ItemSettings.CODEC.optionalFieldOf("itemSettings", ItemSettings.DEFAULT).forGetter(GunDef::itemSettings),
            GunDef.Paints.CODEC.optionalFieldOf("paints", GunDef.Paints.EMPTY).forGetter(GunDef::paints),
            InputHandler.CODEC.listOf().optionalFieldOf("inputHandlers", List.of()).forGetter(GunDef::inputHandlers),
            ActionGroup.CODEC.listOf().optionalFieldOf("actionGroups", List.of()).forGetter(GunDef::actionGroups),
            GunDef.MagazineSlot.CODEC.listOf().optionalFieldOf("magazines", List.of()).forGetter(GunDef::magazines),
            GunDef.ReloadDef.CODEC.listOf().optionalFieldOf("reloads", List.of()).forGetter(GunDef::reloads),
            GunDef.ModeEntry.CODEC.listOf().optionalFieldOf("modes", List.of()).forGetter(GunDef::modes),
            ContentCodecs.RAW_JSON.listOf().optionalFieldOf("loopingSounds", List.of()).forGetter(GunDef::loopingSounds),
            RecordCodecBuilder.of(GunDef::attachmentSlots, GunDef.AttachmentSlots.MAP_CODEC),
            ContentCodecs.RAW_JSON.listOf().optionalFieldOf("modelParts", List.of()).forGetter(GunDef::modelParts),
            Codec.STRING.optionalFieldOf("animationSet", "").forGetter(GunDef::animationSet),
            Codec.INT.optionalFieldOf("particleCount", 0).forGetter(GunDef::particleCount),
            Codec.STRING.optionalFieldOf("casingModel", "").forGetter(GunDef::casingModel),
            InlineAbility.CODEC.listOf().optionalFieldOf("staticAbilities", List.of()).forGetter(GunDef::staticAbilities)
         )
         .apply(instance, GunDef::new)
   );

   public record AttachmentSlot(List<String> matchNames, List<String> matchTags, int numAttachmentSlots, boolean hideDefaultMesh) {
      public static final GunDef.AttachmentSlot NONE = new GunDef.AttachmentSlot(List.of(), List.of(), 0, true);
      public static final Codec<GunDef.AttachmentSlot> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.listOf().optionalFieldOf("matchNames", List.of()).forGetter(GunDef.AttachmentSlot::matchNames),
               Codec.STRING.listOf().optionalFieldOf("matchTags", List.of()).forGetter(GunDef.AttachmentSlot::matchTags),
               Codec.INT.optionalFieldOf("numAttachmentSlots", 0).forGetter(GunDef.AttachmentSlot::numAttachmentSlots),
               Codec.BOOL.optionalFieldOf("hideDefaultMesh", true).forGetter(GunDef.AttachmentSlot::hideDefaultMesh)
            )
            .apply(instance, GunDef.AttachmentSlot::new)
      );
   }

   public record AttachmentSlots(
      GunDef.AttachmentSlot barrel, GunDef.AttachmentSlot grip, GunDef.AttachmentSlot stock, GunDef.AttachmentSlot scope, GunDef.AttachmentSlot generic
   ) {
      public static final GunDef.AttachmentSlots NONE = new GunDef.AttachmentSlots(
         GunDef.AttachmentSlot.NONE, GunDef.AttachmentSlot.NONE, GunDef.AttachmentSlot.NONE, GunDef.AttachmentSlot.NONE, GunDef.AttachmentSlot.NONE
      );
      public static final MapCodec<GunDef.AttachmentSlots> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               GunDef.AttachmentSlot.CODEC.optionalFieldOf("barrelAttachments", GunDef.AttachmentSlot.NONE).forGetter(GunDef.AttachmentSlots::barrel),
               GunDef.AttachmentSlot.CODEC.optionalFieldOf("gripAttachments", GunDef.AttachmentSlot.NONE).forGetter(GunDef.AttachmentSlots::grip),
               GunDef.AttachmentSlot.CODEC.optionalFieldOf("stockAttachments", GunDef.AttachmentSlot.NONE).forGetter(GunDef.AttachmentSlots::stock),
               GunDef.AttachmentSlot.CODEC.optionalFieldOf("scopeAttachments", GunDef.AttachmentSlot.NONE).forGetter(GunDef.AttachmentSlots::scope),
               GunDef.AttachmentSlot.CODEC.optionalFieldOf("genericAttachments", GunDef.AttachmentSlot.NONE).forGetter(GunDef.AttachmentSlots::generic)
            )
            .apply(instance, GunDef.AttachmentSlots::new)
      );
   }

   public record MagazineSlot(String key, List<String> matchByNames, List<String> matchByTags, int baseCostToSwap) {
      public static final Codec<GunDef.MagazineSlot> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("key", "primary").forGetter(GunDef.MagazineSlot::key),
               Codec.STRING.listOf().optionalFieldOf("matchByNames", List.of()).forGetter(GunDef.MagazineSlot::matchByNames),
               Codec.STRING.listOf().optionalFieldOf("matchByTags", List.of()).forGetter(GunDef.MagazineSlot::matchByTags),
               Codec.INT.optionalFieldOf("baseCostToSwap", 0).forGetter(GunDef.MagazineSlot::baseCostToSwap)
            )
            .apply(instance, GunDef.MagazineSlot::new)
      );
   }

   public record ModeEntry(String key, List<String> values) {
      public static final Codec<GunDef.ModeEntry> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("key").forGetter(GunDef.ModeEntry::key),
               Codec.STRING.listOf().optionalFieldOf("values", List.of()).forGetter(GunDef.ModeEntry::values)
            )
            .apply(instance, GunDef.ModeEntry::new)
      );
   }

   public record Paintjob(String textureName, int paintBucketsRequired, String entitlementKey) {
      public static final Codec<GunDef.Paintjob> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("textureName", "").forGetter(GunDef.Paintjob::textureName),
               Codec.INT.optionalFieldOf("paintBucketsRequired", 0).forGetter(GunDef.Paintjob::paintBucketsRequired),
               Codec.STRING.optionalFieldOf("entitlementKey", "").forGetter(GunDef.Paintjob::entitlementKey)
            )
            .apply(instance, GunDef.Paintjob::new)
      );
   }

   public record Paints(List<GunDef.Paintjob> paintjobs) {
      public static final GunDef.Paints EMPTY = new GunDef.Paints(List.of());
      public static final Codec<GunDef.Paints> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(GunDef.Paintjob.CODEC.listOf().optionalFieldOf("paintjobs", List.of()).forGetter(GunDef.Paints::paintjobs))
            .apply(instance, GunDef.Paints::new)
      );
   }

   public record ReloadDef(
      String key,
      boolean manualReloadAllowed,
      boolean autoReloadWhenEmpty,
      String startActionKey,
      String ejectActionKey,
      String loadOneActionKey,
      String endActionKey
   ) {
      public static final Codec<GunDef.ReloadDef> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("key", "primary_fire").forGetter(GunDef.ReloadDef::key),
               Codec.BOOL.optionalFieldOf("manualReloadAllowed", true).forGetter(GunDef.ReloadDef::manualReloadAllowed),
               Codec.BOOL.optionalFieldOf("autoReloadWhenEmpty", true).forGetter(GunDef.ReloadDef::autoReloadWhenEmpty),
               Codec.STRING.optionalFieldOf("startActionKey", "").forGetter(GunDef.ReloadDef::startActionKey),
               Codec.STRING.optionalFieldOf("ejectActionKey", "").forGetter(GunDef.ReloadDef::ejectActionKey),
               Codec.STRING.optionalFieldOf("loadOneActionKey", "").forGetter(GunDef.ReloadDef::loadOneActionKey),
               Codec.STRING.optionalFieldOf("endActionKey", "").forGetter(GunDef.ReloadDef::endActionKey)
            )
            .apply(instance, GunDef.ReloadDef::new)
      );
   }
}
