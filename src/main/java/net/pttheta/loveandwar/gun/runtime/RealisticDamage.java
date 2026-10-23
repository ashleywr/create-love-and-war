package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.stat.Accumulator;
import net.pttheta.loveandwar.gun.content.stat.AccumulatorOp;
import net.pttheta.loveandwar.gun.content.stat.Modifier;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;

public final class RealisticDamage {
   private static final float K = 15.0F / (float)Math.sqrt(3136.0);
   static final Map<String, RealisticDamage.Load> LOADS = Map.ofEntries(
      Map.entry("mat49", l(9.0F, 8.0F, 390.0F, "9x19mm Parabellum")),
      Map.entry("mat4954", l(9.0F, 8.0F, 390.0F, "9x19mm Parabellum")),
      Map.entry("thompson", l(11.43F, 15.0F, 285.0F, ".45 ACP")),
      Map.entry("mars", l(11.5F, 14.0F, 380.0F, ".45 Webley-Mars long")),
      Map.entry("walker", l(11.3F, 9.1F, 410.0F, ".44 ball, 60gr black powder (Colt Walker)")),
      Map.entry("navycarbine", l(9.1F, 5.6F, 330.0F, ".36 round ball, carbine barrel")),
      Map.entry("perrin", l(11.4F, 12.0F, 260.0F, "11mm revolver cartridge, carbine barrel (est.)")),
      Map.entry("dreysepistol", bp(11.0F, 12.0F, 210.0F, "needle pistol, est. 11mm ball")),
      Map.entry("gyrojet", new RealisticDamage.Load(13.0F, 12.0F, 380.0F, 1.1F, "13mm Gyrojet rocket at burnout")),
      Map.entry("gyrojet_carbine", new RealisticDamage.Load(13.0F, 12.0F, 380.0F, 1.1F, "13mm Gyrojet rocket at burnout")),
      Map.entry("arm_pistol", l(5.7F, 4.0F, 790.0F, "5.56x45mm, short barrel")),
      Map.entry("g11k2", l(4.92F, 3.25F, 930.0F, "4.73x33mm caseless")),
      Map.entry("aai_acr", l(1.6F, 0.66F, 1400.0F, "5.56mm flechette (AAI SCF)")),
      Map.entry("sa81", l(7.92F, 7.9F, 715.0F, "7.62x39mm")),
      Map.entry("em2", l(7.2F, 9.07F, 770.0F, ".280 British")),
      Map.entry("chauchat", l(8.3F, 12.8F, 700.0F, "8mm Lebel Balle D")),
      Map.entry("lebel", l(8.3F, 12.8F, 700.0F, "8mm Lebel Balle D")),
      Map.entry("lebel_scope", l(8.3F, 12.8F, 700.0F, "8mm Lebel Balle D")),
      Map.entry("rsc17", l(8.3F, 12.8F, 700.0F, "8mm Lebel Balle D")),
      Map.entry("berthier", l(8.3F, 12.8F, 640.0F, "8mm Lebel, carbine barrel")),
      Map.entry("krag", l(6.7F, 10.1F, 700.0F, "6.5x55mm (Norwegian Krag)")),
      Map.entry("krag_clean", l(6.7F, 10.1F, 700.0F, "6.5x55mm (Norwegian Krag)")),
      Map.entry("leemetford", l(7.92F, 13.9F, 610.0F, ".303 British Mk II, black powder")),
      Map.entry("mas62", l(7.8F, 9.0F, 820.0F, "7.5x54mm French")),
      Map.entry("monitor", l(7.8F, 10.9F, 760.0F, ".30-06, 18in barrel")),
      Map.entry("stg57", l(7.8F, 11.3F, 760.0F, "7.5x55mm Swiss GP11")),
      Map.entry("wf54", l(7.8F, 9.5F, 830.0F, "7.62x51mm NATO (nearest equivalent)")),
      Map.entry("wz38", l(8.2F, 12.8F, 845.0F, "7.92x57mm Mauser")),
      Map.entry("vetterli", l(10.8F, 20.4F, 435.0F, "10.4x38mmR Vetterli")),
      Map.entry("vetterlivitali", l(10.8F, 20.4F, 435.0F, "10.4x38mmR Vetterli")),
      Map.entry("martini_henry", l(11.6F, 31.1F, 400.0F, ".577/450 Martini-Henry")),
      Map.entry("martini_repeater", l(11.6F, 31.1F, 400.0F, ".577/450 Martini-Henry")),
      Map.entry("martinicarbine", l(11.6F, 26.0F, 335.0F, ".577/450 carbine load")),
      Map.entry("snider_enfield", l(14.7F, 31.1F, 380.0F, ".577 Snider")),
      Map.entry("snider_enfield_carbine", l(14.7F, 31.1F, 330.0F, ".577 Snider, carbine barrel")),
      Map.entry("chassepot", bp(11.0F, 25.0F, 410.0F, "11mm Chassepot paper cartridge")),
      Map.entry("dreyse", bp(15.4F, 31.0F, 295.0F, "15.4mm Dreyse needle-gun")),
      Map.entry("p53_enfield", bp(14.7F, 34.0F, 290.0F, ".577 Minie ball")),
      Map.entry("p61_enfield", bp(14.7F, 34.0F, 260.0F, ".577 Minie ball, musketoon barrel")),
      Map.entry("wz35", l(8.2F, 12.8F, 1275.0F, "7.92x107mm DS")),
      Map.entry("tankgewehr", l(13.5F, 51.5F, 785.0F, "13.2x92mmSR TuF")),
      Map.entry("gepard_m3", l(14.9F, 64.0F, 1000.0F, "14.5x114mm")),
      Map.entry("1887", l(18.5F, 32.0F, 370.0F, "12 gauge, black powder, 1 1/8 oz")),
      Map.entry("alof", l(18.5F, 32.0F, 370.0F, "12 gauge, black powder (est.)")),
      Map.entry("sjogren", l(18.5F, 32.0F, 400.0F, "12 gauge smokeless, 1 1/8 oz")),
      Map.entry("greener", l(17.0F, 28.0F, 400.0F, "14 bore Greener police cartridge")),
      Map.entry("caws", l(18.5F, 35.0F, 470.0F, "12 gauge 3in CAWS high-velocity"))
   );

   private static RealisticDamage.Load l(float mm, float g, float v, String what) {
      return new RealisticDamage.Load(mm, g, v, 1.0F, what);
   }

   private static RealisticDamage.Load bp(float mm, float g, float v, String what) {
      return new RealisticDamage.Load(mm, g, v, 1.4F, what);
   }

   private RealisticDamage() {
   }

   public static List<ModifierSource> apply(String gunKey, ActionGroup group, List<ModifierSource> sources) {
      if (Config.REALISTIC_DAMAGE != null && (Boolean)Config.REALISTIC_DAMAGE.get()) {
         RealisticDamage.Load load = LOADS.get(gunKey);
         if (load == null) {
            return sources;
         }

         float authored = StatResolver.resolve("impact_damage", 0.0F, List.of(ModifierSource.of("group", group.modifiers())));
         if (authored <= 0.0F) {
            return sources;
         }

         float percent = load.damage() / authored * 100.0F;
         Modifier m = new Modifier("impact_damage", List.of(), List.of(new Accumulator(AccumulatorOp.INDEPENDENT_PERCENTAGE, percent, List.of())), "");
         List<ModifierSource> out = new ArrayList<>(sources);
         out.add(ModifierSource.of("realistic-damage:" + gunKey, List.of(m)));
         return out;
      } else {
         return sources;
      }
   }

   public static float baseFor(String gunKey) {
      RealisticDamage.Load load = LOADS.get(gunKey);
      return load == null ? -1.0F : load.damage();
   }

   private record Load(float mm, float grams, float mps, float ammoBonus, String what) {
      float damage() {
         double joules = 0.5 * this.grams / 1000.0 * this.mps * this.mps;
         return (float)(RealisticDamage.K * Math.sqrt(joules) * Math.pow(this.mm / 7.8, 0.25) / this.ammoBonus);
      }
   }
}
