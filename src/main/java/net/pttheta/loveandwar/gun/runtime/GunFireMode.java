package net.pttheta.loveandwar.gun.runtime;

public enum GunFireMode {
   SEMI_AUTO,
   FULL_AUTO,
   BURST_FIRE,
   TOGGLE,
   UNKNOWN;

   public static GunFireMode of(String repeatMode) {
      return switch (repeatMode) {
         case "SemiAuto" -> SEMI_AUTO;
         case "FullAuto" -> FULL_AUTO;
         case "BurstFire" -> BURST_FIRE;
         case "Toggle" -> TOGGLE;
         default -> UNKNOWN;
      };
   }
}
