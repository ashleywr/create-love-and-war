package net.pttheta.loveandwar.item.custom;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.pttheta.loveandwar.item.ModArmorMaterials;

@EventBusSubscriber(modid = "createloveandwar")
public class ModArmorItem extends ArmorItem {
   private static final Map<Holder<ArmorMaterial>, MobEffectInstance> MATERIAL_TO_EFFECT_MAP = ImmutableMap.builder()
      .put(ModArmorMaterials.TUNGSTEN, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, false, true))
      .build();

   public ModArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
      super(material, type, properties);
   }

   @SubscribeEvent
   static void onPlayerTick(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide() && hasFullSuitOfArmorOn(player)) {
         if (player.getInventory().getArmor(3).getItem() instanceof ModArmorItem armor) {
            armor.evaluateArmorEffects(player);
         }
      }
   }

   private void evaluateArmorEffects(Player player) {
      for (Entry<Holder<ArmorMaterial>, MobEffectInstance> entry : MATERIAL_TO_EFFECT_MAP.entrySet()) {
         if (this.hasCorrectArmorOn(entry.getKey(), player)) {
            this.addStatusEffectForMaterial(player, entry.getValue());
         }
      }
   }

   private void addStatusEffectForMaterial(Player player, MobEffectInstance effect) {
      if (!player.hasEffect(effect.getEffect())) {
         player.addEffect(new MobEffectInstance(effect));
      }
   }

   private boolean hasCorrectArmorOn(Holder<ArmorMaterial> material, Player player) {
      for (ItemStack armorStack : player.getInventory().armor) {
         if (!(armorStack.getItem() instanceof ArmorItem)) {
            return false;
         }
      }

      for (int slot = 0; slot < 4; slot++) {
         if (!(player.getInventory().getArmor(slot).getItem() instanceof ArmorItem piece) || !piece.getMaterial().equals(material)) {
            return false;
         }
      }

      return true;
   }

   private static boolean hasFullSuitOfArmorOn(Player player) {
      for (int slot = 0; slot < 4; slot++) {
         if (player.getInventory().getArmor(slot).isEmpty()) {
            return false;
         }
      }

      return true;
   }
}
