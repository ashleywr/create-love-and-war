package net.pttheta.loveandwar.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import net.pttheta.loveandwar.item.custom.AmmoPouchItem;
import net.pttheta.loveandwar.item.custom.AmmoPouchTooltip;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class AmmoPouchGameTests {
    private static ItemStack stack(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + id)), count);
    }
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PouchTest"));
        player.setPos(helper.absoluteVec(new Vec3(8, 6, 8)));
        return player;
    }
    private static List<ItemStack> contents(ItemStack pouch) {
        return pouch.getOrDefault(ModDataComponents.AMMO_POUCH_CONTENTS, AmmoPouchContents.EMPTY).items();
    }
    private static int count(ItemStack pouch) { return contents(pouch).stream().mapToInt(ItemStack::getCount).sum(); }
    private static void insert(GameTestHelper helper, FakePlayer player, ItemStack pouch, ItemStack carried) {
        var inventory = new SimpleContainer(pouch);
        helper.assertTrue(pouch.getItem().overrideOtherStackedOnMe(pouch, carried, new Slot(inventory, 0, 0, 0),
                ClickAction.SECONDARY, player, SlotAccess.NULL), "Pouch must handle secondary inventory insertion");
    }
    private static ItemStack extract(GameTestHelper helper, FakePlayer player, ItemStack pouch) {
        var cursor = new SimpleContainer(1);
        helper.assertTrue(pouch.getItem().overrideOtherStackedOnMe(pouch, ItemStack.EMPTY, new Slot(new SimpleContainer(pouch), 0, 0, 0),
                ClickAction.SECONDARY, player, SlotAccess.forContainer(cursor, 0)), "Pouch must handle secondary inventory extraction");
        return cursor.getItem(0);
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void allPouchTagsAndOriginalCapacitiesAreEnforced(GameTestHelper helper) {
        String[] names = {"ammopouch", "ammopouch_pistol", "ammopouch_rifle", "ammopouch_shotgun"};
        String[] ammo = {"black_powder_cartridge", "pistol_bullet", "rifle_bullet", "buckshot_shell"};
        int[] capacities = {64, 192, 96, 64};
        var player = player(helper);
        for (int i = 0; i < names.length; i++) {
            var pouch = stack(names[i], 1);
            helper.assertTrue(((AmmoPouchItem)pouch.getItem()).capacity() == capacities[i], "Original capacity " + names[i]);
            for (int other = 0; other < ammo.length; other++) if (other != i) {
                var rejected = stack(ammo[other], 7);
                insert(helper, player, pouch, rejected);
                helper.assertTrue(rejected.getCount() == 7 && count(pouch) == 0, "Only the pouch's original ammunition tag is accepted");
            }
            for (int fill = 0; fill < 4; fill++) {
                var carried = stack(ammo[i], 1);
                int inputCount = carried.getMaxStackSize();
                carried.setCount(inputCount);
                int before = count(pouch);
                insert(helper, player, pouch, carried);
                helper.assertTrue(count(pouch) + carried.getCount() == before + inputCount, "Insertion must preserve every round");
            }
            helper.assertTrue(count(pouch) == capacities[i] && pouch.getItem().isBarVisible(pouch)
                    && pouch.getItem().getBarWidth(pouch) == 13, "Pouch fills to its own count capacity");
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void pouchOrderMergingAndSafeStackSizesArePreserved(GameTestHelper helper) {
        var player = player(helper);
        var pouch = stack("ammopouch_pistol", 1);
        insert(helper, player, pouch, stack("pistol_bullet", 20));
        insert(helper, player, pouch, stack("pistol_bullet_ap", 10));
        insert(helper, player, pouch, stack("pistol_bullet", 12));
        var newest = extract(helper, player, pouch);
        helper.assertTrue(newest.is(stack("pistol_bullet", 1).getItem()) && newest.getCount() == 32,
                "Matching partial stacks merge and move to the front before a different ammo type");
        var named = stack("pistol_bullet_ap", 4); named.set(DataComponents.CUSTOM_NAME, Component.literal("Separate rounds"));
        insert(helper, player, pouch, named);
        var distinct = extract(helper, player, pouch);
        helper.assertTrue(distinct.getCount() == 4 && distinct.has(DataComponents.CUSTOM_NAME), "Different components must not merge");
        helper.assertTrue(extract(helper, player, pouch).getCount() == 10, "Extract one whole stored entry in LIFO order");
        insert(helper, player, pouch, stack("pistol_bullet", 32));
        insert(helper, player, pouch, stack("pistol_bullet", 64));
        helper.assertTrue(extract(helper, player, pouch).getCount() == 64 && count(pouch) == 32,
                "Original full-64 input starts a new entry rather than merging");
        insert(helper, player, pouch, stack("pistol_bullet", 63));
        insert(helper, player, pouch, stack("pistol_bullet", 63));
        helper.assertTrue(count(pouch) == 158, "Safe splitting preserves the full accepted count");
        for (var item : contents(pouch)) helper.assertTrue(item.getCount() <= item.getMaxStackSize(),
                "Partial merges must not create oversized cursor or invalid saved stacks");
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void pouchSlotTransfersRespectCapacityAndRefusedSlots(GameTestHelper helper) {
        var player = player(helper);
        var pouch = stack("ammopouch_rifle", 1);
        var item = pouch.getItem();
        var inventory = new SimpleContainer(stack("rifle_bullet", 32));
        var slot = new Slot(inventory, 0, 0, 0);
        helper.assertTrue(item.overrideStackedOnOther(pouch, slot, ClickAction.SECONDARY, player), "Carried pouch handles slot insertion");
        helper.assertTrue(slot.getItem().isEmpty() && count(pouch) == 32, "Insertion takes real slot contents");
        insert(helper, player, pouch, stack("rifle_bullet", 32));
        insert(helper, player, pouch, stack("rifle_bullet", 16));
        slot.set(stack("rifle_bullet", 32));
        item.overrideStackedOnOther(pouch, slot, ClickAction.SECONDARY, player);
        helper.assertTrue(slot.getItem().getCount() == 16 && count(pouch) == 96,
                "Only remaining pouch capacity leaves the slot; slot=" + slot.getItem().getCount() + " pouch=" + count(pouch));
        var refused = new Slot(new SimpleContainer(1), 0, 0, 0) {
            @Override public boolean mayPlace(ItemStack input) { return false; }
        };
        item.overrideStackedOnOther(pouch, refused, ClickAction.SECONDARY, player);
        helper.assertTrue(refused.getItem().isEmpty() && count(pouch) == 96, "Refused extraction returns every item to the pouch");
        var small = new Slot(new SimpleContainer(1), 0, 0, 0) {
            @Override public int getMaxStackSize(ItemStack input) { return 8; }
        };
        item.overrideStackedOnOther(pouch, small, ClickAction.SECONDARY, player);
        helper.assertTrue(small.getItem().getCount() == 8 && count(pouch) == 88, "Partial slot insertion returns its remainder");
        item.overrideOtherStackedOnMe(pouch, ItemStack.EMPTY, new Slot(new SimpleContainer(pouch), 0, 0, 0),
                ClickAction.SECONDARY, player, SlotAccess.NULL);
        helper.assertTrue(count(pouch) == 88, "A refused cursor write cannot delete stored ammunition");
        var locked = new Slot(new SimpleContainer(pouch), 0, 0, 0) {
            @Override public boolean mayPickup(net.minecraft.world.entity.player.Player user) { return false; }
        };
        var carried = stack("rifle_bullet", 1);
        helper.assertTrue(!item.overrideOtherStackedOnMe(pouch, carried, locked, ClickAction.SECONDARY, player, SlotAccess.NULL)
                && count(pouch) == 88 && carried.getCount() == 1, "Nonmodifiable pouch slots refuse mutation");
        helper.assertTrue(!item.overrideStackedOnOther(pouch, slot, ClickAction.PRIMARY, player), "Primary click retains vanilla behavior");
        var two = pouch.copyWithCount(2);
        helper.assertTrue(!item.overrideStackedOnOther(two, slot, ClickAction.SECONDARY, player), "Malformed stacked pouches cannot transfer contents");
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void pouchContentsSurviveSavePacketsAndIndependentCopies(GameTestHelper helper) {
        var player = player(helper);
        var pouch = stack("ammopouch_pistol", 1);
        insert(helper, player, pouch, stack("pistol_bullet", 63));
        insert(helper, player, pouch, stack("pistol_bullet", 63));
        insert(helper, player, pouch, stack("pistol_bullet_ap", 14));
        var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag)pouch.save(helper.getLevel().registryAccess()));
        helper.assertTrue(count(restored) == 140, "All rounds survive item NBT save/reload");
        helper.assertTrue(ItemStack.matches(restored, pouch), "Saved pouch contents must compare by value for inventory synchronization");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var decoded = ItemStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(count(decoded) == 140 && contents(decoded).getFirst().is(stack("pistol_bullet_ap", 1).getItem()),
                    "Item packets preserve all rounds and extraction order");
            helper.assertTrue(ItemStack.matches(decoded, pouch)
                    && decoded.get(ModDataComponents.AMMO_POUCH_CONTENTS).hashCode() == pouch.get(ModDataComponents.AMMO_POUCH_CONTENTS).hashCode(),
                    "Independent packet contents must have equal values and hashes");
        } finally { buffer.release(); }
        var copy = restored.copy();
        insert(helper, player, copy, stack("pistol_bullet_ap", 2));
        helper.assertTrue(count(restored) == 140 && count(copy) == 142, "Mutating a pouch copy cannot change shared component stacks");
        helper.assertTrue(!ItemStack.matches(restored, copy), "Different stored counts must remain unequal");
        var namedCopy = restored.copy();
        var renamed = contents(namedCopy).stream().map(ItemStack::copy).toList();
        renamed.getFirst().set(DataComponents.CUSTOM_NAME, Component.literal("Different rounds"));
        namedCopy.set(ModDataComponents.AMMO_POUCH_CONTENTS, new AmmoPouchContents(renamed));
        helper.assertTrue(!ItemStack.matches(restored, namedCopy), "Different stored components must remain unequal");
        var tooltip = restored.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL).toString();
        helper.assertTrue(tooltip.contains("140") && tooltip.contains("192") && restored.getTooltipImage().orElse(null) instanceof AmmoPouchTooltip image && image.weight() == 140,
                "Original fullness text and inventory image are available");
        var riflePouch = stack("ammopouch_rifle", 1);
        insert(helper, player, riflePouch, stack("rifle_bullet", 32));
        helper.assertTrue(riflePouch.getTooltipImage().orElseThrow() instanceof AmmoPouchTooltip image && image.weight() == 32,
                "Tooltip fullness uses raw rounds, not vanilla bundle fractions based on an ammunition stack limit");
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void usingPouchDropsContentsOnceAndEmptyUseFails(GameTestHelper helper) {
        var player = player(helper);
        var pouch = stack("ammopouch", 1);
        insert(helper, player, pouch, stack("black_powder_cartridge", 31));
        player.setItemInHand(InteractionHand.MAIN_HAND, pouch);
        try {
            helper.assertTrue(pouch.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "Use drops stored ammunition");
            var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                    entity -> entity.getItem().is(stack("black_powder_cartridge", 1).getItem()));
            helper.assertTrue(count(pouch) == 0 && dropped.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 31,
                    "Use empties the pouch into real server item entities without loss");
            helper.assertTrue(!pouch.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "Empty pouch use must fail and never duplicate drops");
        } finally {
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void destroyedPouchReleasesStoredAmmunition(GameTestHelper helper) {
        var pouch = stack("ammopouch_rifle", 1);
        pouch.set(ModDataComponents.AMMO_POUCH_CONTENTS, new AmmoPouchContents(List.of(stack("rifle_bullet", 23), stack("rifle_bullet_ap", 7))));
        var position = helper.absoluteVec(new Vec3(8, 6, 8));
        var entity = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, pouch);
        helper.getLevel().addFreshEntity(entity);
        try {
            entity.hurt(helper.getLevel().damageSources().generic(), 10);
            var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
            helper.assertTrue(entity.isRemoved() && dropped.stream().mapToInt(item -> item.getItem().getCount()).sum() == 30,
                    "Actual item destruction releases both stored entries");
        } finally {
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2)).forEach(ItemEntity::discard);
            entity.discard();
        }
        helper.succeed();
    }
}
