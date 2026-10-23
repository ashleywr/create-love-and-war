package net.pttheta.loveandwar.gun.client.anim;

import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.runtime.GunActionState;

public final class LocalGunAnim {
   private static final long NANOS_PER_TICK = 50000000L;
   private static final float CONFIRM_WINDOW_TICKS = 8.0F;
   private static final int LATE_JOIN_TICKS = 3;
   private static float carry = -1.0F;
   private static String gun = "";
   private static String key = "";
   private static long startNanos;
   private static int lastElapsed;
   private static int lastGroupTotal;
   private static boolean predicted;
   private static String baseKey = "";
   private static int baseElapsed;
   private static String lockKey = "";
   private static long lockUntilNanos;

   private LocalGunAnim() {
   }

   public static void predict(String gunKey, GunActionState current, ActionGroup group) {
      long now = System.nanoTime();
      if (!group.key().equals(lockKey) || !gunKey.equals(gun) || now >= lockUntilNanos) {
         carry = group.key().equals(key) && gunKey.equals(gun) ? ticksSince(now) : -1.0F;
         gun = gunKey;
         key = group.key();
         startNanos = now;
         predicted = true;
         baseKey = current.activeGroup();
         baseElapsed = current.groupTicksElapsed();
         lockKey = group.key();
         lockUntilNanos = now + Math.max(50000000L, (long)(group.repeatDelay() * 20.0F) * 50000000L);
      }
   }

   public static boolean wouldPredict(String gunKey, ActionGroup group) {
      return !group.key().equals(lockKey) || !gunKey.equals(gun) || System.nanoTime() >= lockUntilNanos;
   }

   public static LocalGunAnim.Playing resolve(String gunKey, GunActionState state) {
      long now = System.nanoTime();
      if (!gunKey.equals(gun)) {
         clear();
         gun = gunKey;
      }

      String serverKey = state.activeGroup();
      int serverElapsed = state.groupTicksElapsed();
      boolean retriggered = serverKey.equals(key) && state.groupTotalTicks() > lastGroupTotal;
      lastGroupTotal = state.groupTotalTicks();
      if (predicted) {
         float age = ticksSince(now);
         boolean fresh = serverKey.equals(key) && (!key.equals(baseKey) || serverElapsed < baseElapsed);
         if (fresh) {
            predicted = false;
            lastElapsed = serverElapsed;
            return new LocalGunAnim.Playing(key, age, false);
         }

         if (age <= 8.0F) {
            return new LocalGunAnim.Playing(key, age, false);
         }

         predicted = false;
         key = "";
      }

      if (serverKey.isEmpty()) {
         return key.isEmpty() ? null : new LocalGunAnim.Playing(key, ticksSince(now), true);
      }

      if (!serverKey.equals(key) || serverElapsed < lastElapsed || retriggered) {
         carry = serverKey.equals(key) ? ticksSince(now) : -1.0F;
         key = serverKey;
         startNanos = now - (!retriggered && serverElapsed > 3 ? serverElapsed * 50000000L : 0L);
      }

      lastElapsed = serverElapsed;
      return new LocalGunAnim.Playing(key, ticksSince(now), false);
   }

   public static void clear() {
      key = "";
      carry = -1.0F;
      predicted = false;
      lastElapsed = 0;
      lastGroupTotal = 0;
   }

   private static float ticksSince(long now) {
      return (float)(now - startNanos) / 5.0E7F;
   }

   public record Playing(String groupKey, float ticks, boolean serverIdle, float carryTicks) {
      Playing(String groupKey, float ticks, boolean serverIdle) {
         this(groupKey, ticks, serverIdle, LocalGunAnim.carry);
      }
   }
}
