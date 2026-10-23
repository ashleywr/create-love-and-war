package net.pttheta.loveandwar.gun.content;

import java.util.List;
import java.util.Map;

public record ContentLoadReport(String kind, int filesFound, Map<String, ?> parsed, List<String> failures) {
   public int parsedCount() {
      return this.parsed.size();
   }

   public boolean allParsed() {
      return this.failures.isEmpty();
   }
}
