package net.pttheta.loveandwar.gun.content.stat;

import java.util.List;

public interface ModifierSource {
   String sourceId();

   List<Modifier> modifiers();

   static ModifierSource of(final String id, final List<Modifier> modifiers) {
      return new ModifierSource() {
         @Override
         public String sourceId() {
            return id;
         }

         @Override
         public List<Modifier> modifiers() {
            return modifiers;
         }
      };
   }
}
