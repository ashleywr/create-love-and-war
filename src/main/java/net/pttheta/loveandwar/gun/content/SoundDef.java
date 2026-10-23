package net.pttheta.loveandwar.gun.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SoundDef(
   String sound, float length, float minPitchMultiplier, float maxPitchMultiplier, float minVolume, float maxVolume, float maxRange, List<SoundDef.Lod> lods
) {
   public SoundDef { lods = List.copyOf(lods); }

   public static final Codec<SoundDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.optionalFieldOf("sound", "flansmod:null").forGetter(SoundDef::sound),
            Codec.FLOAT.optionalFieldOf("length", 1.0F).forGetter(SoundDef::length),
            Codec.FLOAT.optionalFieldOf("minPitchMultiplier", 1.0F).forGetter(SoundDef::minPitchMultiplier),
            Codec.FLOAT.optionalFieldOf("maxPitchMultiplier", 1.0F).forGetter(SoundDef::maxPitchMultiplier),
            Codec.FLOAT.optionalFieldOf("minVolume", 1.0F).forGetter(SoundDef::minVolume),
            Codec.FLOAT.optionalFieldOf("maxVolume", 1.0F).forGetter(SoundDef::maxVolume),
            Codec.FLOAT.optionalFieldOf("maxRange", 100.0F).forGetter(SoundDef::maxRange),
            Lod.CODEC.listOf().optionalFieldOf("LODs", List.of()).forGetter(SoundDef::lods)
         )
         .apply(instance, SoundDef::new)
   );

   private static final StreamCodec<ByteBuf, List<Lod>> LODS_STREAM_CODEC = Lod.STREAM_CODEC.apply(ByteBufCodecs.list());
   public static final StreamCodec<ByteBuf, SoundDef> STREAM_CODEC = StreamCodec.of((buf, value) -> {
      ByteBufCodecs.STRING_UTF8.encode(buf, value.sound());
      buf.writeFloat(value.length());
      buf.writeFloat(value.minPitchMultiplier());
      buf.writeFloat(value.maxPitchMultiplier());
      buf.writeFloat(value.minVolume());
      buf.writeFloat(value.maxVolume());
      buf.writeFloat(value.maxRange());
      LODS_STREAM_CODEC.encode(buf, value.lods());
   }, buf -> new SoundDef(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readFloat(), buf.readFloat(), buf.readFloat(),
         buf.readFloat(), buf.readFloat(), buf.readFloat(), LODS_STREAM_CODEC.decode(buf)));

   /** Original SoundLODManager chooses a variant and ranged volume separately for each camera. */
   public Resolved resolve(double distance, float randomPitch, float volumeMultiplier, float pitchMultiplier) {
      String selected = this.sound;
      float best = 0;
      for (var lod : this.lods) {
         if (distance >= lod.minDistance() && lod.minDistance() > best) {
            best = lod.minDistance();
            selected = lod.sound();
         }
      }
      float volume = this.minVolume + (this.maxVolume - this.minVolume) * (1 - (float)distance / this.maxRange);
      float pitch = this.minPitchMultiplier + randomPitch * (this.maxPitchMultiplier - this.minPitchMultiplier);
      return new Resolved(selected, volumeMultiplier * volume, pitchMultiplier * pitch, this.maxRange);
   }

   public record Resolved(String sound, float volume, float pitch, float range) {}

   public record Lod(String sound, float minDistance) {
      public static final Codec<Lod> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.STRING.optionalFieldOf("sound", "flansmod:null").forGetter(Lod::sound),
         Codec.FLOAT.optionalFieldOf("minDistance", 100.0F).forGetter(Lod::minDistance)
      ).apply(instance, Lod::new));
      public static final StreamCodec<ByteBuf, Lod> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8, Lod::sound, ByteBufCodecs.FLOAT, Lod::minDistance, Lod::new);
   }
}
