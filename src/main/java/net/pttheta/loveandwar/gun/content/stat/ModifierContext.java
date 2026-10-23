package net.pttheta.loveandwar.gun.content.stat;

import java.util.List;
import java.util.Map;

public record ModifierContext(Map<MultiplyPerKind, Integer> multipliers, List<String> activeGroupPaths) {
   public static final ModifierContext EMPTY = new ModifierContext(Map.of(), List.of());

   public static ModifierContext ofGroups(List<String> activeGroupPaths) {
      return new ModifierContext(Map.of(), activeGroupPaths);
   }

   float multiplierFor(List<MultiplyPerKind> kinds) {
      float result = 1.0F;

      for (MultiplyPerKind kind : kinds) {
         result *= this.multipliers.getOrDefault(kind, 1).intValue();
      }

      return result;
   }

   public boolean inScope(List<String> matchGroupPaths) {
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
