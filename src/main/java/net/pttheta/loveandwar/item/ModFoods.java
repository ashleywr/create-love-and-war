package net.pttheta.loveandwar.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.Builder;

public class ModFoods {
   public static final FoodProperties CIGARETTE = new Builder()
      .alwaysEdible()
      .nutrition(0)
      .saturationModifier(0.0F)
      .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 100), 1.0F)
      .build();
}
