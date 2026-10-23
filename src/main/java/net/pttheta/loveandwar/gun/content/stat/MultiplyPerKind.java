package net.pttheta.loveandwar.gun.content.stat;

public enum MultiplyPerKind {
   PER_LEVEL("PerLevel"),
   PER_ATTACHMENT("PerAttachment"),
   PER_STACKS("PerStacks"),
   UNKNOWN("");

   private final String key;

   MultiplyPerKind(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }
}
