package net.pttheta.loveandwar.gun.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.MagazineDef;

public record WorkbenchDef(WorkbenchDef.GunCrafting gunCrafting, WorkbenchDef.GunModifying gunModifying) {
   public static final Codec<WorkbenchDef> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            WorkbenchDef.GunCrafting.CODEC.optionalFieldOf("gunCrafting", WorkbenchDef.GunCrafting.INACTIVE).forGetter(WorkbenchDef::gunCrafting),
            WorkbenchDef.GunModifying.CODEC.optionalFieldOf("gunModifying", WorkbenchDef.GunModifying.INACTIVE).forGetter(WorkbenchDef::gunModifying)
         )
         .apply(instance, WorkbenchDef::new)
   );

   public boolean canCraft(ItemStack stack) {
      if (!this.gunCrafting.isActive()) {
         return false;
      }

      List<MagazineDef.TagFilter> filters = this.gunCrafting.craftableGuns().itemTagFilters();
      if (filters.isEmpty()) {
         return true;
      }

      boolean anyAllowRule = false;
      boolean allowed = false;

      for (MagazineDef.TagFilter filter : filters) {
         boolean matches = matchesAnyTag(stack, filter.matchResourceLocations());
         if ("Deny".equalsIgnoreCase(filter.filterType())) {
            if (matches) {
               return false;
            }
         } else {
            anyAllowRule = true;
            allowed |= matches;
         }
      }

      return !anyAllowRule || allowed;
   }

   public boolean isModAllowed(String modKey) {
      if (!this.gunModifying.isActive()) {
         return false;
      } else {
         return this.gunModifying.disallowedMods().contains(modKey)
            ? false
            : this.gunModifying.allowedMods().isEmpty() || this.gunModifying.allowedMods().contains(modKey);
      }
   }

   private static boolean matchesAnyTag(ItemStack stack, List<String> tagIds) {
      for (String tagId : tagIds) {
         TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
         if (stack.is(tag)) {
            return true;
         }
      }

      return false;
   }

   public record CraftableGuns(List<MagazineDef.TagFilter> itemTagFilters) {
      public static final WorkbenchDef.CraftableGuns NONE = new WorkbenchDef.CraftableGuns(List.of());
      public static final Codec<WorkbenchDef.CraftableGuns> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               MagazineDef.TagFilter.CODEC.listOf().optionalFieldOf("itemTagFilters", List.of()).forGetter(WorkbenchDef.CraftableGuns::itemTagFilters)
            )
            .apply(instance, WorkbenchDef.CraftableGuns::new)
      );
   }

   public record GunCrafting(boolean isActive, WorkbenchDef.CraftableGuns craftableGuns, int maxSlots) {
      public static final WorkbenchDef.GunCrafting INACTIVE = new WorkbenchDef.GunCrafting(false, WorkbenchDef.CraftableGuns.NONE, 10);
      public static final Codec<WorkbenchDef.GunCrafting> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.BOOL.optionalFieldOf("isActive", false).forGetter(WorkbenchDef.GunCrafting::isActive),
               WorkbenchDef.CraftableGuns.CODEC
                  .optionalFieldOf("craftableGuns", WorkbenchDef.CraftableGuns.NONE)
                  .forGetter(WorkbenchDef.GunCrafting::craftableGuns),
               Codec.INT.optionalFieldOf("maxSlots", 10).forGetter(WorkbenchDef.GunCrafting::maxSlots)
            )
            .apply(instance, WorkbenchDef.GunCrafting::new)
      );
   }

   public record GunModifying(boolean isActive, List<String> allowedMods, List<String> disallowedMods) {
      public static final WorkbenchDef.GunModifying INACTIVE = new WorkbenchDef.GunModifying(false, List.of(), List.of());
      public static final Codec<WorkbenchDef.GunModifying> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.BOOL.optionalFieldOf("isActive", false).forGetter(WorkbenchDef.GunModifying::isActive),
               Codec.STRING.listOf().optionalFieldOf("allowedMods", List.of()).forGetter(WorkbenchDef.GunModifying::allowedMods),
               Codec.STRING.listOf().optionalFieldOf("disallowedMods", List.of()).forGetter(WorkbenchDef.GunModifying::disallowedMods)
            )
            .apply(instance, WorkbenchDef.GunModifying::new)
      );
   }
}
