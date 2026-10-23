package net.pttheta.loveandwar.gun.content.stat;

import java.util.List;
import java.util.Map;
import java.util.Collection;

public record ModifierContext(Map<MultiplyPerKind, Integer> multipliers, List<String> activeGroupPaths) {
   public static final ModifierContext EMPTY = new ModifierContext(Map.of(), List.of());

   public static ModifierContext ofGroups(List<String> activeGroupPaths) {
      return new ModifierContext(Map.of(), activeGroupPaths);
   }

   public static int attachmentCount(Collection<ModifierSource> sources) {
      int count = 0;
      for (var source : sources) count += source.attachmentCount();
      return count;
   }

   float multiplierFor(List<MultiplyPerKind> kinds, int attachmentCount) {
      float result = 1.0F;
      int seen = 0;

      for (MultiplyPerKind kind : kinds) {
         // Original FloatFormula encodes an input set, not repeated multiplication of duplicate flags.
         int flag = 1 << kind.ordinal();
         if ((seen & flag) != 0) continue;
         seen |= flag;
         int fallback = switch (kind) {
            case PER_ATTACHMENT -> attachmentCount;
            // Original active IStatCalculatorContext has no fullness override, even for a loaded gun.
            case PER_MAG_FULLNESS, UNKNOWN -> 0;
            default -> 1;
         };
         result *= this.multipliers.getOrDefault(kind, fallback).intValue();
      }

      return result;
   }

   boolean inScope(List<String> matchGroupPaths) {
      if (matchGroupPaths.isEmpty()) {
         return true;
      }

      for (String path : matchGroupPaths) {
         if (this.activeGroupPaths.contains(path)) {
            return true;
         }
      }

      return false;
   }
}
