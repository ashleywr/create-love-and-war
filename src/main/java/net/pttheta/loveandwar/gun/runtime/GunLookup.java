package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.MagazineDef;

public final class GunLookup {
   private GunLookup() {
   }

   public static String contentKey(ItemStack stack) {
      return contentKey(stack.getItem());
   }

   public static String contentKey(Item item) {
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      return id.getPath();
   }

   public static Optional<GunDef> gunDef(ItemStack stack) {
      return stack.isEmpty() ? Optional.empty() : GunContent.gun(contentKey(stack));
   }

   public static Optional<BulletDef> bulletDef(ItemStack stack) {
      return stack.isEmpty() ? Optional.empty() : GunContent.bullet(contentKey(stack));
   }

   public static Optional<MagazineDef> magazineDef(ItemStack stack) {
      return stack.isEmpty() ? Optional.empty() : GunContent.magazine(contentKey(stack));
   }

   public static Optional<AttachmentDef> attachmentDef(ItemStack stack) {
      return stack.isEmpty() ? Optional.empty() : GunContent.attachment(contentKey(stack));
   }

   public static Optional<MagazineDef> resolveMagazineForSlot(GunDef.MagazineSlot slot) {
      for (String name : slot.matchByNames()) {
         Optional<MagazineDef> byName = GunContent.magazine(name);
         if (byName.isPresent()) {
            return byName;
         }
      }

      if (!slot.matchByTags().isEmpty()) {
         for (MagazineDef candidate : GunContent.magazines().values()) {
            if (containsAny(candidate.tags(), slot.matchByTags())) {
               return Optional.of(candidate);
            }
         }
      }

      return Optional.empty();
   }

   public static boolean matchesBullet(MagazineDef.MatchingBullets filters, ItemStack bullet) {
      if (!bullet.isEmpty() && !bulletDef(bullet).isEmpty()) {
         List<String> bulletTags = bulletDef(bullet).get().itemSettings().tags();
         if (filters.itemTagFilters().isEmpty()) {
            return true;
         }

         boolean allowed = false;
         boolean anyAllowRule = false;

         for (MagazineDef.TagFilter filter : filters.itemTagFilters()) {
            boolean matches = containsAny(bulletTags, filter.matchResourceLocations());
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
      } else {
         return false;
      }
   }

   public static boolean matchesAttachmentSlot(GunDef.AttachmentSlot slot, ItemStack attachment) {
      if (attachment.isEmpty()) {
         return false;
      } else {
         String key = contentKey(attachment);
         if (slot.matchNames().contains(key)) {
            return true;
         } else {
            return slot.matchTags().isEmpty()
               ? true
               : attachmentDef(attachment).map(def -> containsAny(def.itemSettings().tags(), slot.matchTags())).orElse(false);
         }
      }
   }

   public static GunDef.AttachmentSlot slotFor(GunDef.AttachmentSlots slots, String category) {
      return switch (category) {
         case "barrel" -> slots.barrel();
         case "grip" -> slots.grip();
         case "stock" -> slots.stock();
         case "scope" -> slots.scope();
         default -> slots.generic();
      };
   }

   private static boolean containsAny(List<String> haystack, List<String> needles) {
      for (String needle : needles) {
         if (haystack.contains(needle)) {
            return true;
         }
      }

      return false;
   }
}
