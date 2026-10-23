package net.pttheta.loveandwar.gun.content.stat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StatResolver {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-content");

   private StatResolver() {
   }

   public static float resolve(String stat, float base, Collection<ModifierSource> sources) {
      return resolve(stat, base, sources, ModifierContext.EMPTY);
   }

   public static float resolve(String stat, float base, Collection<ModifierSource> sources, ModifierContext ctx) {
      float running = base;
      float stackablePercentSum = 0.0F;
      List<Float> independentPercents = new ArrayList<>();

      for (ModifierSource source : sources) {
         for (Modifier modifier : source.modifiers()) {
            if (modifier.stat().equals(stat) && !modifier.isOverride() && ctx.inScope(modifier.matchGroupPaths())) {
               for (Accumulator acc : modifier.accumulators()) {
                  float scaled = acc.value() * ctx.multiplierFor(acc.multiplyPer());
                  switch (acc.operation()) {
                     case BASE_ADD:
                        running += scaled;
                        break;
                     case STACKABLE_PERCENTAGE:
                        stackablePercentSum += scaled;
                        break;
                     case INDEPENDENT_PERCENTAGE:
                        independentPercents.add(scaled);
                        break;
                     case UNKNOWN:
                        LOGGER.debug("Skipping unknown operation on stat '{}' from source '{}'", stat, source.sourceId());
                  }
               }
            }
         }
      }

      if (stackablePercentSum != 0.0F) {
         running *= 1.0F + stackablePercentSum / 100.0F;
      }

      for (float pct : independentPercents) {
         running *= pct / 100.0F;
      }

      return running;
   }

   public static String resolveOverride(String stat, String base, Collection<ModifierSource> sources) {
      return resolveOverride(stat, base, sources, ModifierContext.EMPTY);
   }

   public static String resolveOverride(String stat, String base, Collection<ModifierSource> sources, ModifierContext ctx) {
      String result = base;

      for (ModifierSource source : sources) {
         for (Modifier modifier : source.modifiers()) {
            if (modifier.stat().equals(stat) && modifier.isOverride() && ctx.inScope(modifier.matchGroupPaths())) {
               result = modifier.setValue();
            }
         }
      }

      return result;
   }

   public static Set<String> statsOf(Collection<ModifierSource> sources) {
      LinkedHashSet<String> stats = new LinkedHashSet<>();

      for (ModifierSource source : sources) {
         for (Modifier modifier : source.modifiers()) {
            stats.add(modifier.stat());
         }
      }

      return stats;
   }
}
