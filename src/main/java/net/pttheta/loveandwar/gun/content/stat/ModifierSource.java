package net.pttheta.loveandwar.gun.content.stat;

import java.util.List;

public interface ModifierSource {
   String sourceId();

   List<Modifier> modifiers();

   /** Calculation input, separate from whether a stack has a recognized modifier definition. */
   default int attachmentCount() { return 0; }

   static ModifierSource gunInputs(int attachmentCount) {
      return new ModifierSource() {
         @Override public String sourceId() { return "gun-inputs"; }
         @Override public List<Modifier> modifiers() { return List.of(); }
         @Override public int attachmentCount() { return Math.max(0, attachmentCount); }
      };
   }

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
