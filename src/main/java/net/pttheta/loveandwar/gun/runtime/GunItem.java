package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class GunItem extends Item {
   private final String gunName;

   public GunItem(Properties properties, String gunName) {
      super(properties);
      this.gunName = gunName;
   }

   public String gunName() {
      return this.gunName;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (!level.isClientSide && isSelected && entity instanceof ServerPlayer player) {
         GunEngine.tick(player, stack);
      }
   }

   public boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to, boolean slotChanged) {
      if (slotChanged) {
         return true;
      } else {
         return from.getItem() == to.getItem() && to.getItem() instanceof GunItem ? false : !ItemStack.isSameItemSameComponents(from, to);
      }
   }
}
