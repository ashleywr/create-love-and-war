package net.pttheta.loveandwar.gun.content;

import java.util.List;
import net.pttheta.loveandwar.gun.content.stat.Modifier;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;

public final class ContentSelfCheck {
   private ContentSelfCheck() {
   }

   public static void main(String[] args) {
      int totalFound = 0;
      int totalParsed = 0;
      System.out.println("Gun content self-check");
      System.out.println("=======================");

      for (ContentLoadReport report : GunContent.allReports()) {
         totalFound += report.filesFound();
         totalParsed += report.parsedCount();
         System.out
            .printf(
               "%-12s found=%-4d parsed=%-4d failed=%d%s%n",
               report.kind(),
               report.filesFound(),
               report.parsedCount(),
               report.failures().size(),
               report.failures().isEmpty() ? "" : " " + report.failures()
            );
      }

      System.out.println("-----------------------");
      System.out.printf("TOTAL found=%d parsed=%d%n%n", totalFound, totalParsed);
      if (totalParsed == 0) {
         throw new IllegalStateException("Parsed 0 content files - the loader is broken, this is not a pass.");
      }

      for (ContentLoadReport report : GunContent.allReports()) {
         if (!report.allParsed()) {
            throw new IllegalStateException(report.kind() + " had parse failures: " + report.failures());
         }
      }

      checkStatResolution();
      System.out.println();
      System.out.println("PASS: all " + totalParsed + " content files parsed; stat engine smoke test passed.");
   }

   private static void checkStatResolution() {
      GunDef gun = GunContent.gun("1887").orElseThrow(() -> new IllegalStateException("expected guns/1887.json to be loaded"));
      AttachmentDef barrel = GunContent.attachment("attachment_extended_barrel")
         .orElseThrow(() -> new IllegalStateException("expected attachments/attachment_extended_barrel.json to be loaded"));
      List<Modifier> gunModifiers = gun.actionGroups()
         .stream()
         .filter(g -> g.key().equals("primary_fire"))
         .findFirst()
         .orElseThrow(() -> new IllegalStateException("expected 1887 to have a primary_fire action group"))
         .modifiers();
      List<ModifierSource> sources = List.of(
         ModifierSource.of("gun:1887:primary_fire", gunModifiers), ModifierSource.of("attachment:attachment_extended_barrel", barrel.modifiers())
      );
      expectNear("spread", 15.0F, StatResolver.resolve("spread", 0.0F, sources));
      expectNear("impact_damage", 18.0F, StatResolver.resolve("impact_damage", 0.0F, sources));
      expectEquals("flash_model", "createloveandwar:effects/muzzle_flash", StatResolver.resolveOverride("flash_model", "", sources));
      expectNear("nonexistent_stat", 42.0F, StatResolver.resolve("nonexistent_stat", 42.0F, sources));
   }

   private static void expectNear(String label, float expected, float actual) {
      if (Math.abs(expected - actual) > 0.001F) {
         throw new IllegalStateException(label + ": expected " + expected + " but got " + actual);
      }

      System.out.printf("  ok: %s = %.3f%n", label, actual);
   }

   private static void expectEquals(String label, String expected, String actual) {
      if (!expected.equals(actual)) {
         throw new IllegalStateException(label + ": expected '" + expected + "' but got '" + actual + "'");
      }

      System.out.printf("  ok: %s = %s%n", label, actual);
   }
}
