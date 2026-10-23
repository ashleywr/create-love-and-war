package net.pttheta.loveandwar;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

@EventBusSubscriber(modid = "createloveandwar")
public class Config {
   private static final Builder BUILDER = new Builder();
   private static final BooleanValue NO_OIL = BUILDER.comment("Whether to disable oil generation on world creation")
      .define("addNoOilDatapackAutomatically", false);
   private static final BooleanValue SIMPLE_BRASS = BUILDER.comment("Automatically add simple brass datapack on world creation")
      .define("addSimpleBrassDatapackAutomatically", true);
   private static final BooleanValue VENDER_DATA = BUILDER.comment("Automatically add Vender's game Data compat").define("vendersGameDataCompat", true);
   private static final BooleanValue VENDER_RESOURCE = BUILDER.comment("Automatically add Vender's game Resource compat").define("vendersGameArtCompat", true);
   public static final EnumValue<Config.CrosshairMode> CROSSHAIR = BUILDER.comment("When to hide the vanilla crosshair: VANILLA, HIDE_WITH_GUN, HIDE_ALWAYS.")
      .defineEnum("crosshair", Config.CrosshairMode.HIDE_WITH_GUN);
   public static final DoubleValue ADS_ZOOM = BUILDER.comment("Aim zoom strength. 1.0 = as authored (1.20.1), 0 = no zoom.")
      .defineInRange("ads_zoom", 0.5, 0.0, 1.0);
   public static final DoubleValue MUZZLE_SMOKE = BUILDER.comment("Muzzle smoke amount. 1.0 = as in 1.20.1, 0 = none.")
      .defineInRange("muzzle_smoke", 1.0, 0.0, 1.0);
   public static final BooleanValue SMOOTH_RECOIL_KICK = BUILDER.comment("Slide the gun back on firing instead of snapping. false = as in 1.20.1.")
      .define("smooth_recoil_kick", true);
   public static final DoubleValue AUTO_RECOIL = BUILDER.comment("Recoil animation strength for automatic/burst guns. 1.0 = as in 1.20.1.")
      .defineInRange("auto_recoil", 0.5, 0.0, 1.0);
   public static final BooleanValue REALISTIC_DAMAGE = BUILDER.comment("Gun damage from real calibre and muzzle velocity. false = as in 1.20.1.")
      .define("realistic_damage", true);
   public static final IntValue DRAWING_PROCESSING_DURATION = BUILDER.comment("Ticks a drawing recipe takes to process")
      .defineInRange("drawing_processing_duration", 100, 0, Integer.MAX_VALUE);
   public static final IntValue STAMPING_PROCESSING_DURATION = BUILDER.comment("Ticks a stamping recipe takes to process")
      .defineInRange("stamping_processing_duration", 100, 0, Integer.MAX_VALUE);
   public static final BooleanValue GUN_DIAGNOSTICS = BUILDER.comment("Log why a gun did not fire/reload at INFO level (default DEBUG, which is hidden).")
      .define("gun_diagnostics", false);
   public static final ModConfigSpec SPEC = BUILDER.build();
   public static int drawing_processing_time;
   public static boolean no_oil;
   public static boolean simple_brass;
   public static boolean vender_data;
   public static boolean vender_art;

   @SubscribeEvent
   static void onLoad(ModConfigEvent event) {
      drawing_processing_time = (Integer)DRAWING_PROCESSING_DURATION.get();
      no_oil = (Boolean)NO_OIL.get();
      simple_brass = (Boolean)SIMPLE_BRASS.get();
      vender_data = (Boolean)VENDER_DATA.get();
      vender_art = (Boolean)VENDER_RESOURCE.get();
   }

   public enum CrosshairMode {
      VANILLA,
      HIDE_WITH_GUN,
      HIDE_ALWAYS;
   }
}
