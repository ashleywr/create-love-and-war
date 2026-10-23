package net.pttheta.loveandwar.item.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Original L&W bundle-style pouch interactions, stored in synchronized 1.21 components. */
public class AmmoPouchItem extends Item {
   private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);
   private final int capacity;
   private final TagKey<Item> accepts;

   public AmmoPouchItem(Properties properties, int capacity, TagKey<Item> accepts) {
      super(properties);
      this.capacity = capacity;
      this.accepts = accepts;
   }

   public int capacity() {
      return this.capacity;
   }

   public TagKey<Item> accepts() {
      return this.accepts;
   }

   public boolean canFitItem(ItemStack stack) {
      return stack.is(accepts);
   }

   private static List<ItemStack> contents(ItemStack pouch) {
      return pouch.getOrDefault(ModDataComponents.AMMO_POUCH_CONTENTS, AmmoPouchContents.EMPTY).items();
   }

   private static void store(ItemStack pouch, List<ItemStack> items) {
      if (items.isEmpty()) pouch.remove(ModDataComponents.AMMO_POUCH_CONTENTS);
      else pouch.set(ModDataComponents.AMMO_POUCH_CONTENTS, new AmmoPouchContents(items));
   }

   private int weight(ItemStack stack) {
      if (stack.is(Items.BUNDLE)) {
         int nested = 4;
         for (var item : stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).items()) {
            nested += weight(item) * item.getCount();
         }
         return nested;
      }
      if ((stack.is(Items.BEEHIVE) || stack.is(Items.BEE_NEST)) && !stack.getOrDefault(DataComponents.BEES, List.of()).isEmpty()) return capacity;
      // Unlike a vanilla bundle, each ordinary round costs one, regardless of its stack limit.
      return 1;
   }

   private int contentWeight(ItemStack pouch) {
      int total = 0;
      for (var item : contents(pouch)) total += weight(item) * item.getCount();
      return total;
   }

   public float getFullnessDisplay(ItemStack pouch) {
      return (float)contentWeight(pouch) / capacity;
   }

   @Override
   public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
      if (pouch.getCount() != 1 || action != ClickAction.SECONDARY) return false;
      var stack = slot.getItem();
      if (stack.isEmpty()) {
         playRemoveSound(player);
         removeOne(pouch).ifPresent(removed -> add(pouch, slot.safeInsert(removed)));
      } else if (stack.canFitInsideContainerItems() && canFitItem(stack)) {
         int available = Math.max(0, capacity - contentWeight(pouch)) / weight(stack);
         int inserted = add(pouch, slot.safeTake(stack.getCount(), available, player));
         if (inserted > 0) playInsertSound(player);
      }
      return true;
   }

   @Override
   public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess cursor) {
      if (pouch.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
      if (carried.isEmpty()) {
         var previous = contents(pouch);
         removeOne(pouch).ifPresent(removed -> {
            if (cursor.set(removed)) playRemoveSound(player);
            else store(pouch, previous);
         });
      } else if (canFitItem(carried)) {
         int inserted = add(pouch, carried);
         if (inserted > 0) {
            playInsertSound(player);
            carried.shrink(inserted);
         }
      }
      return true;
   }

   private int add(ItemStack pouch, ItemStack input) {
      if (input.isEmpty() || !input.canFitInsideContainerItems()) return 0;
      int accepted = Math.min(input.getCount(), Math.max(0, capacity - contentWeight(pouch)) / weight(input));
      if (accepted == 0) return 0;
      var items = new ArrayList<ItemStack>(contents(pouch).size() + 1);
      for (var item : contents(pouch)) items.add(item.copy());
      int remaining = accepted;
      // Quirk kept: a full 64-item input starts a fresh entry even if matching entries exist.
      if (input.getCount() != 64 && !input.is(Items.BUNDLE)) {
         for (int index = 0; index < items.size(); index++) {
            var existing = items.get(index);
            if (!ItemStack.isSameItemSameComponents(input, existing)) continue;
            int merged = Math.min(remaining, Math.max(0, existing.getMaxStackSize() - existing.getCount()));
            if (merged > 0) {
               existing.grow(merged);
               items.remove(index);
               items.addFirst(existing);
               remaining -= merged;
            }
            break;
         }
      }
      // Safety repair: original partial merges could exceed stack limits and corrupt saved/cursor stacks.
      while (remaining > 0) {
         int count = Math.min(remaining, input.getMaxStackSize());
         items.addFirst(input.copyWithCount(count));
         remaining -= count;
      }
      store(pouch, items);
      return accepted;
   }

   private static Optional<ItemStack> removeOne(ItemStack pouch) {
      var items = contents(pouch);
      if (items.isEmpty()) return Optional.empty();
      var result = items.getFirst().copy();
      store(pouch, items.subList(1, items.size()));
      return Optional.of(result);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      var pouch = player.getItemInHand(hand);
      var items = contents(pouch);
      if (items.isEmpty()) return InteractionResultHolder.fail(pouch);
      store(pouch, List.of());
      if (player instanceof ServerPlayer) for (var item : items) player.drop(item.copy(), true);
      player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
      player.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResultHolder.sidedSuccess(pouch, level.isClientSide());
   }

   @Override public boolean isBarVisible(ItemStack pouch) { return contentWeight(pouch) > 0; }
   @Override public int getBarWidth(ItemStack pouch) { return Math.min(1 + 12 * contentWeight(pouch) / capacity, 13); }
   @Override public int getBarColor(ItemStack pouch) { return BAR_COLOR; }

   @Override
   public Optional<TooltipComponent> getTooltipImage(ItemStack pouch) {
      return Optional.of(new AmmoPouchTooltip(contents(pouch), contentWeight(pouch)));
   }

   @Override
   public void appendHoverText(ItemStack pouch, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
      tooltip.add(Component.translatable("item.minecraft.bundle.fullness", contentWeight(pouch), capacity).withStyle(ChatFormatting.GRAY));
   }

   @Override
   public void onDestroyed(ItemEntity entity) {
      var items = contents(entity.getItem());
      store(entity.getItem(), List.of());
      ItemUtils.onContainerDestroyed(entity, items.stream().map(ItemStack::copy).toList());
   }

   private static void playRemoveSound(Entity entity) {
      entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
   }

   private static void playInsertSound(Entity entity) {
      entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
   }
}
