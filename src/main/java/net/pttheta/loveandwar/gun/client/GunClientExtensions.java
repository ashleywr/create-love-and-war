package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.item.ModItems;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunClientExtensions {
   private static final IClientItemExtensions GUN = new IClientItemExtensions() {
      public ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
         return stack.getItem() instanceof GunItem ? ArmPose.BOW_AND_ARROW : ArmPose.ITEM;
      }
   };

   private GunClientExtensions() {
   }

   @SubscribeEvent
   static void register(RegisterClientExtensionsEvent event) {
      for (DeferredHolder<Item, ? extends Item> holder : ModItems.ITEMS.getEntries()) {
         Item item = (Item)holder.get();
         if (item instanceof GunItem) {
            event.registerItem(GUN, new Item[]{item});
         }
      }
   }
}
