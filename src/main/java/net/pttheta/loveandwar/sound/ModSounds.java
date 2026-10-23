package net.pttheta.loveandwar.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pttheta.loveandwar.LoveAndWarMod;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, "createloveandwar");
   public static final DeferredHolder<SoundEvent, SoundEvent> SERVO = registerSoundEvent("servo");
   public static final DeferredHolder<SoundEvent, SoundEvent> ROBOT_DOG_AMBIENT = registerSoundEvent("robotdog_ambient");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT = registerSoundEvent("gunshot");
   public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_DROP_LARGE = registerSoundEvent("bullet_drop_large");
   public static final DeferredHolder<SoundEvent, SoundEvent> CYCLE_BOLT = registerSoundEvent("cycle_bolt");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_CYCLE = registerSoundEvent("gunshot_cycle");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_HUGE = registerSoundEvent("gunshot_huge");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_HUGE_CYCLE = registerSoundEvent("gunshot_huge_cycle");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_HUGE_CYCLE_2 = registerSoundEvent("gunshot_huge_cycle_2");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_REVOLVER = registerSoundEvent("gunshot_revolver");
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_LAUNCH = registerSoundEvent("rocket_launch");

   private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(LoveAndWarMod.asResource(name)));
   }

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }
}
