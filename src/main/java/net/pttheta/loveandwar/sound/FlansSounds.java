package net.pttheta.loveandwar.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FlansSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, "flansmod");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_CLOSE_CLICKY = register("bolt_close_clicky");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_OPEN_CLICKY = register("bolt_open_clicky");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_PRIME_OILED = register("bolt_prime_oiled");
   public static final DeferredHolder<SoundEvent, SoundEvent> BREAK_ACTION_OPEN_CLACK = register("break_action_open_clack");
   public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_DROP_TINKLE = register("bullet_drop_tinkle");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_CLANG = register("gunshot_clang");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_FEATHERS = register("gunshot_feathers");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_JOLT = register("gunshot_jolt");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_KAPOW = register("gunshot_kapow");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_PTOO = register("gunshot_ptoo");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_SLAM = register("gunshot_slam");
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNSHOT_SNAP = register("gunshot_snap");
   public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_LOWER_HUP = register("item_lower_hup");
   public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_RAISE_HUP = register("item_raise_hup");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOAD_CHUNKY = register("magazine_load_chunky");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOAD_LATCH = register("magazine_load_latch");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOAD_SMOOTH = register("magazine_load_smooth");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_UNLOAD_CHUNKY = register("magazine_unload_chunky");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_UNLOAD_LATCH = register("magazine_unload_latch");
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_UNLOAD_SMOOTH = register("magazine_unload_smooth");
   public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_CLOSE_JINGLE = register("pump_close_jingle");
   public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_CLOSE_SLUGGISH = register("pump_close_sluggish");
   public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_OPEN_SLUGGISH = register("pump_open_sluggish");
   public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_CLOSE_RATTLE = register("revolver_close_rattle");
   public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_OPEN_RATTLE = register("revolver_open_rattle");
   public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_TURN_RATTLE = register("revolver_turn_rattle");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_INSERT_PLOP = register("shell_insert_plop");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_INSERT_SHLOOP = register("shell_insert_shloop");
   public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_INSERT_SHLOOP_DOUBLE = register("shell_insert_shloop_double");
   public static final DeferredHolder<SoundEvent, SoundEvent> SLIDE_OPEN_RATTLE = register("slide_open_rattle");

   private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("flansmod", name)));
   }

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }

   private FlansSounds() {
   }
}
