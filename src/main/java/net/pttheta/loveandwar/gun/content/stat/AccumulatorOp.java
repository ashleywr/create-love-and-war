package net.pttheta.loveandwar.gun.content.stat;

public enum AccumulatorOp {
   BASE_ADD("BaseAdd"),
   INDEPENDENT_PERCENTAGE("IndependentPercentage"),
   STACKABLE_PERCENTAGE("StackablePercentage"),
   UNKNOWN("");

   private final String key;

   AccumulatorOp(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }
}
