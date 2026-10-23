package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.InlineAbility;
import net.pttheta.loveandwar.gun.content.stat.Modifier;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;

public final class StatSources {
   private StatSources() {
   }

   public static List<ModifierSource> forFiring(GunDef gun, ActionGroup group, GunAttachments attachments) {
      List<ModifierSource> sources = new ArrayList<>();
      sources.add(ModifierSource.of("group:" + group.key(), group.modifiers()));

      for (String category : new String[]{"barrel", "grip", "stock", "scope", "generic"}) {
         for (ItemStack stack : attachments.forCategory(category)) {
            GunLookup.attachmentDef(stack).ifPresent(def -> {
               String id = "attachment:" + GunLookup.contentKey(stack);
               sources.add(ModifierSource.of(id, def.modifiers()));
               addAttachmentAlwaysOn(sources, def, id);
            });
         }
      }

      for (InlineAbility ability : gun.staticAbilities()) {
         addAlwaysOn(sources, "gun-static", ability);
      }

      return sources;
   }

   public static List<ModifierSource> forImpact(List<ModifierSource> firingSources, List<Modifier> effectModifiers) {
      List<ModifierSource> sources = new ArrayList<>(firingSources);
      sources.add(ModifierSource.of("impact-effect", effectModifiers));
      return sources;
   }

   private static void addAlwaysOn(List<ModifierSource> sources, String idPrefix, InlineAbility ability) {
      boolean alwaysOn = ability.startTriggers().stream().anyMatch(t -> "AlwaysOn".equals(t.triggerType()));
      if (alwaysOn) {
         for (InlineAbility.EffectRef effect : ability.effects()) {
            if ("ApplyModifier".equals(effect.effectType())) {
               sources.add(ModifierSource.of(idPrefix + ":ability", effect.modifiers()));
            }
         }
      }
   }

   public static void addAttachmentAlwaysOn(List<ModifierSource> sources, AttachmentDef def, String id) {
      for (InlineAbility ability : def.abilities()) {
         addAlwaysOn(sources, id, ability);
      }
   }
}
