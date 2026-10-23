package net.pttheta.loveandwar.gun.client.anim;

final class Easing {
   private Easing() {
   }

   static float shape(String exit, String entry, float u) {
      return shape(exit, entry, u, false);
   }

   static float shape(String exit, String entry, float u, boolean smoothKick) {
      if (!smoothKick || !"instant".equals(exit) && !"instant".equals(entry)) {
         float out = u;
         if ("instant".equals(exit)) {
            out = 1.0F;
         }

         if ("instant".equals(entry)) {
            out = 0.0F;
         }

         if ("smooth".equals(exit) && u < 0.5F) {
            out = u * u * (3.0F - 2.0F * u);
         }

         if ("smooth".equals(entry) && u > 0.5F) {
            out = u * u * (3.0F - 2.0F * u);
         }

         return out;
      } else {
         return 1.0F - (1.0F - u) * (1.0F - u);
      }
   }
}
