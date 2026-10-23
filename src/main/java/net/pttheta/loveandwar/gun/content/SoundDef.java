package net.pttheta.loveandwar.gun.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record SoundDef(
   String sound, float length, float minPitchMultiplier, float maxPitchMultiplier, float minVolume, float maxVolume, float maxRange, List<JsonElement> lods
) {
   public static final Codec<SoundDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.optionalFieldOf("sound", "").forGetter(SoundDef::sound),
            Codec.FLOAT.optionalFieldOf("length", 0.0F).forGetter(SoundDef::length),
            Codec.FLOAT.optionalFieldOf("minPitchMultiplier", 1.0F).forGetter(SoundDef::minPitchMultiplier),
            Codec.FLOAT.optionalFieldOf("maxPitchMultiplier", 1.0F).forGetter(SoundDef::maxPitchMultiplier),
            Codec.FLOAT.optionalFieldOf("minVolume", 1.0F).forGetter(SoundDef::minVolume),
            Codec.FLOAT.optionalFieldOf("maxVolume", 1.0F).forGetter(SoundDef::maxVolume),
            Codec.FLOAT.optionalFieldOf("maxRange", 16.0F).forGetter(SoundDef::maxRange),
            ContentCodecs.RAW_JSON.listOf().optionalFieldOf("LODs", List.of()).forGetter(SoundDef::lods)
         )
         .apply(instance, SoundDef::new)
   );
}
