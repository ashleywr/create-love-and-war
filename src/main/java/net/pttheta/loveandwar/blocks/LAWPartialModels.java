package net.pttheta.loveandwar.blocks;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.pttheta.loveandwar.LoveAndWarMod;

public class LAWPartialModels {
   public static final PartialModel DRAWING_PRESS_HEAD = block("drawing_press/head");
   public static final PartialModel STAMPING_PRESS_HEAD = block("stamping_press/head");
   public static final PartialModel DISTILLATION_GAUGE = block("distillation_column/gauge");
   public static final PartialModel DISTILLATION_GAUGE_DIAL = block("distillation_column/gauge_dial");

   private static PartialModel block(String path) {
      return PartialModel.of(LoveAndWarMod.asResource("block/" + path));
   }

   public static void init() {
   }
}
