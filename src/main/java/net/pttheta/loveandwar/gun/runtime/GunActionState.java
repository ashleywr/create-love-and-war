package net.pttheta.loveandwar.gun.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GunActionState(
   String activeGroup,
   int groupTicksElapsed,
   int groupTotalTicks,
   List<String> pendingReloadGroups,
   Map<String, String> modeValues,
   List<String> activeToggles,
   long cooldownUntilGameTime,
   int burstRemaining,
   boolean waitingForRelease
) {
   public static final GunActionState IDLE = new GunActionState("", 0, 0, List.of(), Map.of(), List.of(), 0L, 0, false);
   public static final Codec<GunActionState> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.optionalFieldOf("activeGroup", "").forGetter(GunActionState::activeGroup),
            Codec.INT.optionalFieldOf("groupTicksElapsed", 0).forGetter(GunActionState::groupTicksElapsed),
            Codec.INT.optionalFieldOf("groupTotalTicks", 0).forGetter(GunActionState::groupTotalTicks),
            Codec.STRING.listOf().optionalFieldOf("pendingReloadGroups", List.of()).forGetter(GunActionState::pendingReloadGroups),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("modeValues", Map.of()).forGetter(GunActionState::modeValues),
            Codec.STRING.listOf().optionalFieldOf("activeToggles", List.of()).forGetter(GunActionState::activeToggles),
            Codec.LONG.optionalFieldOf("cooldownUntilGameTime", 0L).forGetter(GunActionState::cooldownUntilGameTime),
            Codec.INT.optionalFieldOf("burstRemaining", 0).forGetter(GunActionState::burstRemaining),
            Codec.BOOL.optionalFieldOf("waitingForRelease", false).forGetter(GunActionState::waitingForRelease)
         )
         .apply(instance, GunActionState::new)
   );
   private static final StreamCodec<ByteBuf, List<String>> STRING_LIST = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
   private static final StreamCodec<ByteBuf, Map<String, String>> STRING_MAP = ByteBufCodecs.map(
      HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, GunActionState> STREAM_CODEC = StreamCodec.of(
      (buf, v) -> {
         ByteBufCodecs.STRING_UTF8.encode(buf, v.activeGroup());
         ByteBufCodecs.VAR_INT.encode(buf, v.groupTicksElapsed());
         ByteBufCodecs.VAR_INT.encode(buf, v.groupTotalTicks());
         STRING_LIST.encode(buf, v.pendingReloadGroups());
         STRING_MAP.encode(buf, v.modeValues());
         STRING_LIST.encode(buf, v.activeToggles());
         ByteBufCodecs.VAR_LONG.encode(buf, v.cooldownUntilGameTime());
         ByteBufCodecs.VAR_INT.encode(buf, v.burstRemaining());
         ByteBufCodecs.BOOL.encode(buf, v.waitingForRelease());
      },
      buf -> new GunActionState(
         (String)ByteBufCodecs.STRING_UTF8.decode(buf),
         (Integer)ByteBufCodecs.VAR_INT.decode(buf),
         (Integer)ByteBufCodecs.VAR_INT.decode(buf),
         (List<String>)STRING_LIST.decode(buf),
         (Map<String, String>)STRING_MAP.decode(buf),
         (List<String>)STRING_LIST.decode(buf),
         (Long)ByteBufCodecs.VAR_LONG.decode(buf),
         (Integer)ByteBufCodecs.VAR_INT.decode(buf),
         (Boolean)ByteBufCodecs.BOOL.decode(buf)
      )
   );

   public boolean busy() {
      return !this.activeGroup.isEmpty() && this.groupTicksElapsed < this.groupTotalTicks;
   }

   public GunActionState withGroup(String group, int totalTicks, List<String> pending) {
      return new GunActionState(
         group, 0, totalTicks, pending, this.modeValues, this.activeToggles, this.cooldownUntilGameTime, this.burstRemaining, this.waitingForRelease
      );
   }

   public GunActionState tickedGroup() {
      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed + 1,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         this.modeValues,
         this.activeToggles,
         this.cooldownUntilGameTime,
         this.burstRemaining,
         this.waitingForRelease
      );
   }

   public GunActionState idled() {
      return new GunActionState(
         "", 0, 0, List.of(), this.modeValues, this.activeToggles, this.cooldownUntilGameTime, this.burstRemaining, this.waitingForRelease
      );
   }

   public GunActionState withCooldown(long until) {
      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         this.modeValues,
         this.activeToggles,
         until,
         this.burstRemaining,
         this.waitingForRelease
      );
   }

   public GunActionState withMode(String key, String value) {
      Map<String, String> next = new LinkedHashMap<>(this.modeValues);
      next.put(key, value);
      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         next,
         this.activeToggles,
         this.cooldownUntilGameTime,
         this.burstRemaining,
         this.waitingForRelease
      );
   }

   public GunActionState withToggle(String key, boolean on) {
      List<String> next = new ArrayList<>(this.activeToggles);
      next.remove(key);
      if (on) {
         next.add(key);
      }

      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         this.modeValues,
         next,
         this.cooldownUntilGameTime,
         this.burstRemaining,
         this.waitingForRelease
      );
   }

   public GunActionState withBurstRemaining(int n) {
      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         this.modeValues,
         this.activeToggles,
         this.cooldownUntilGameTime,
         n,
         this.waitingForRelease
      );
   }

   public GunActionState withWaitingForRelease(boolean waiting) {
      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         this.modeValues,
         this.activeToggles,
         this.cooldownUntilGameTime,
         this.burstRemaining,
         waiting
      );
   }

   public GunActionState released() {
      return new GunActionState(
         this.activeGroup,
         this.groupTicksElapsed,
         this.groupTotalTicks,
         this.pendingReloadGroups,
         this.modeValues,
         this.activeToggles,
         this.cooldownUntilGameTime,
         0,
         false
      );
   }
}
