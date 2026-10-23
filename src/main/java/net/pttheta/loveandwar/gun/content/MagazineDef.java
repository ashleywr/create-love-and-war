package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;

public record MagazineDef(
   List<String> tags,
   List<Modifier> modifiers,
   String ammoLoadMode,
   String ammoConsumeMode,
   int upgradeCost,
   int numRounds,
   boolean allRoundsMustBeIdentical,
   MagazineDef.MatchingBullets matchingBullets,
   String spawnBulletCasing,
   boolean ejectCasings,
   String casingModelOverride
) {
   public static final Codec<MagazineDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(MagazineDef::tags),
            Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(MagazineDef::modifiers),
            Codec.STRING.optionalFieldOf("ammoLoadMode", "FullMag").forGetter(MagazineDef::ammoLoadMode),
            Codec.STRING.optionalFieldOf("ammoConsumeMode", "FirstNonEmpty").forGetter(MagazineDef::ammoConsumeMode),
            Codec.INT.optionalFieldOf("upgradeCost", 0).forGetter(MagazineDef::upgradeCost),
            Codec.INT.optionalFieldOf("numRounds", 1).forGetter(MagazineDef::numRounds),
            Codec.BOOL.optionalFieldOf("allRoundsMustBeIdentical", false).forGetter(MagazineDef::allRoundsMustBeIdentical),
            MagazineDef.MatchingBullets.CODEC.optionalFieldOf("matchingBullets", MagazineDef.MatchingBullets.NONE).forGetter(MagazineDef::matchingBullets),
            Codec.STRING.optionalFieldOf("spawnBulletCasing", "").forGetter(MagazineDef::spawnBulletCasing),
            Codec.BOOL.optionalFieldOf("ejectCasings", true).forGetter(MagazineDef::ejectCasings),
            Codec.STRING.optionalFieldOf("casingModelOverride", "").forGetter(MagazineDef::casingModelOverride)
         )
         .apply(instance, MagazineDef::new)
   );

   public record MatchingBullets(List<JsonElement> itemIDFilters, List<MagazineDef.TagFilter> itemTagFilters, List<JsonElement> materialFilters) {
      public static final MagazineDef.MatchingBullets NONE = new MagazineDef.MatchingBullets(List.of(), List.of(), List.of());
      public static final Codec<MagazineDef.MatchingBullets> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ContentCodecs.RAW_JSON.listOf().optionalFieldOf("itemIDFilters", List.of()).forGetter(MagazineDef.MatchingBullets::itemIDFilters),
               MagazineDef.TagFilter.CODEC.listOf().optionalFieldOf("itemTagFilters", List.of()).forGetter(MagazineDef.MatchingBullets::itemTagFilters),
               ContentCodecs.RAW_JSON.listOf().optionalFieldOf("materialFilters", List.of()).forGetter(MagazineDef.MatchingBullets::materialFilters)
            )
            .apply(instance, MagazineDef.MatchingBullets::new)
      );
   }

   public record TagFilter(String filterType, List<String> matchResourceLocations) {
      public static final Codec<MagazineDef.TagFilter> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("filterType", "Allow").forGetter(MagazineDef.TagFilter::filterType),
               Codec.STRING.listOf().optionalFieldOf("matchResourceLocations", List.of()).forGetter(MagazineDef.TagFilter::matchResourceLocations)
            )
            .apply(instance, MagazineDef.TagFilter::new)
      );
   }
}
