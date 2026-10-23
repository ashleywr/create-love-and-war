package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
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
            });
         }
      }

      // Original ActionStack never dispatches AlwaysOn; declared abilities are not unconditional stat sources.
      return sources;
   }

   public static List<ModifierSource> forImpact(List<ModifierSource> firingSources, List<Modifier> effectModifiers) {
      List<ModifierSource> sources = new ArrayList<>(firingSources);
      sources.add(ModifierSource.of("impact-effect", effectModifiers));
      return sources;
   }

}
