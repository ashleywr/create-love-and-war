package net.pttheta.loveandwar.gun.runtime;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.network.GunNetworking;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunInputIdentityGameTests {
    private static ItemStack gun() { return gun("g11k2", "caseless_bullet"); }
    private static ItemStack gun(String key, String ammo) {
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + key)));
        var round = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + ammo)));
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(round, round.copy(), round.copy())));
        return gun;
    }
    private static int rounds(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size();
    }
    private static void prepare(GunAudioGameTests.Listener owner, ItemStack... guns) {
        for (int i = 0; i < guns.length; i++) owner.player.getInventory().setItem(i, guns[i]);
        owner.player.getInventory().selected = 0;
        owner.player.setXRot(-90);
        GunIdentity.inventoryTick(owner.player);
    }
    private static CGunInput capture(GunAudioGameTests.Listener owner) {
        return CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1").orElseThrow();
    }
    private static void handle(GunAudioGameTests.Listener listener, CGunInput payload) throws ReflectiveOperationException {
        var context = (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class[]{IPayloadContext.class}, (p, m, args) -> {
            if (m.getName().equals("player")) return listener.player;
            if (m.getName().equals("enqueueWork")) { ((Runnable)args[0]).run(); return CompletableFuture.completedFuture(null); }
            throw new UnsupportedOperationException(m.getName());
        });
        var method = GunNetworking.class.getDeclaredMethod("handle", CGunInput.class, IPayloadContext.class);
        method.setAccessible(true);
        method.invoke(null, payload, context);
    }

    @GameTest(template="projectile_arena", templateNamespace="createloveandwar")
    public static void queuedInputCannotFireNewlySelectedSameItemGun(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var original = gun(); var replacement = gun(); prepare(owner, original, replacement);
            var captured = capture(owner);
            owner.player.getInventory().selected = 1;
            handle(owner, captured);
            h.assertTrue(rounds(replacement) == 3, "Input for A cannot fire newly selected same-item B");
            h.assertTrue(rounds(original) == 2, "Source UUID routing still executes owned A even when unequipped");
        }
        h.succeed();
    }

    @GameTest(template="projectile_arena", templateNamespace="createloveandwar")
    public static void queuedInputFollowsGunMovedOutOfClaimedSlot(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var original = gun(); var replacement = gun(); prepare(owner, original, replacement);
            var captured = capture(owner);
            owner.player.getInventory().setItem(9, original);
            owner.player.getInventory().setItem(0, replacement);
            owner.player.getInventory().setItem(1, ItemStack.EMPTY);
            handle(owner, captured);
            h.assertTrue(rounds(original) == 2 && rounds(replacement) == 3, "Stale cached slot must rebind by UUID, never write into B");
            h.assertTrue(owner.player.getInventory().getItem(0) == replacement && owner.player.getInventory().getItem(9) == original,
                "Moving A must preserve both actual inventory stacks");
        }
        h.succeed();
    }

    @GameTest(template="projectile_arena", templateNamespace="createloveandwar")
    public static void releaseStopsCapturedGunWithoutStoppingOtherHeldGun(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = gun("mat49", "pistol_bullet"); var b = gun("mat49", "pistol_bullet"); prepare(owner, a, b);
            var first = capture(owner); handle(owner, first);
            owner.player.getInventory().selected = 1;
            var second = capture(owner); handle(owner, second);
            owner.player.getInventory().setItem(9, a);
            owner.player.getInventory().setItem(0, ItemStack.EMPTY);
            owner.player.getInventory().selected = 0;
            handle(owner, first.released());
            h.assertTrue(!GunFireScheduler.owns(owner.player, a, "primary_fire") && GunFireScheduler.owns(owner.player, b, "primary_fire"),
                "Release with empty hand stops moved A while B keeps its independent held trigger");
            h.assertTrue(rounds(a) == 2 && rounds(b) == 2 && owner.player.getInventory().getItem(9) == a,
                "Release must consume no round and cannot overwrite the old slot");
            handle(owner, second.released());
        }
        h.succeed();
    }

    @GameTest(template="projectile_arena", templateNamespace="createloveandwar")
    public static void unownedAndMissingIdentitiesCannotFireCurrentGun(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h); var foreign = new GunAudioGameTests.Listener(h, 10)) {
            var a = gun(); var b = gun(); prepare(owner, a); prepare(foreign, b);
            for (UUID id : List.of(GunIdentity.INVALID, UUID.randomUUID(), GunIdentity.id(b))) {
                handle(owner, new CGunInput("Fire1", true, 0, id));
            }
            var captured = capture(owner);
            owner.player.getInventory().setItem(0, ItemStack.EMPTY);
            owner.player.getInventory().setItem(1, b);
            handle(owner, captured);
            h.assertTrue(rounds(a) == 3 && rounds(b) == 3, "Foreign, zero, unknown and removed IDs never fall back to current hand");
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="createloveandwar")
    public static void serverAssignsStableDistinctIdsAcrossWholeInventory(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = gun(); var b = gun(); var armorSlot = gun(); var offhand = gun();
            h.assertTrue(GunIdentity.id(a).equals(GunIdentity.INVALID), "Client-side/read-only ID lookup must never mint identity");
            h.assertTrue(CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1").isEmpty(), "Empty gun capture is absent");
            owner.player.getInventory().setItem(0, a); owner.player.getInventory().setItem(20, b);
            owner.player.getInventory().setItem(36, armorSlot); owner.player.getInventory().setItem(40, offhand);
            h.assertTrue(CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1").isEmpty(), "Unsynchronized gun cannot emit an unnamed request");
            GunIdentity.tick(new ServerTickEvent.Pre(() -> true, h.getLevel().getServer()));
            var ids = List.of(GunIdentity.id(a), GunIdentity.id(b), GunIdentity.id(armorSlot), GunIdentity.id(offhand));
            h.assertTrue(ids.stream().distinct().count() == 4 && !ids.contains(GunIdentity.INVALID), "Whole inventory receives distinct IDs, including offhand and armor compartments");
            GunIdentity.tick(new ServerTickEvent.Pre(() -> true, h.getLevel().getServer()));
            h.assertTrue(ids.equals(List.of(GunIdentity.id(a), GunIdentity.id(b), GunIdentity.id(armorSlot), GunIdentity.id(offhand))), "Subsequent ticks preserve IDs");
            var stick = new ItemStack(Items.STICK); stick.set(ModDataComponents.GUN_ID, ids.getFirst());
            h.assertTrue(GunIdentity.id(stick).equals(GunIdentity.INVALID), "A forged component does not turn an ordinary item into a gun");
            a.set(ModDataComponents.GUN_ID, GunIdentity.INVALID); GunIdentity.inventoryTick(owner.player);
            h.assertTrue(GunIdentity.id(a).equals(GunIdentity.INVALID), "Existing zero-ID source quirk is preserved, not silently rekeyed");
            owner.player.getInventory().setItem(10, stick);
            h.assertTrue(GunIdentity.resolve(owner.player, ids.getFirst()).isEmpty(), "Forged non-gun ID is ignored by inventory routing");
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="createloveandwar")
    public static void identitySurvivesCopySaveAndActualItemPacket(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = gun(); prepare(owner, a); var id = GunIdentity.id(a);
            var saved = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)a.save(h.getLevel().registryAccess()));
            var packet = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(packet, a); var received = ItemStack.STREAM_CODEC.decode(packet);
                h.assertTrue(id.equals(GunIdentity.id(saved)) && id.equals(GunIdentity.id(received)) && id.equals(GunIdentity.id(a.copy())),
                    "Logical copies, saves and real item packet preserve gun UUID");
                owner.player.getInventory().setItem(0, received);
                h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == received, "Same-slot synchronization copy remains the cached logical gun");
            } finally { packet.release(); }
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="createloveandwar")
    public static void duplicateIdsRetainValidCacheThenUseAscendingSlots(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = gun(); owner.player.getInventory().setItem(5, a); GunIdentity.inventoryTick(owner.player);
            var id = GunIdentity.id(a); var copy = a.copy(); owner.player.getInventory().setItem(0, copy);
            GunIdentity.inventoryTick(owner.player);
            h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == a, "Valid cached source slot wins over earlier same-ID copy");
            owner.player.getInventory().setItem(5, ItemStack.EMPTY);
            h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == copy, "Invalid cached context rebinds to first ascending match");
            owner.player.getInventory().setItem(7, copy); owner.player.getInventory().setItem(0, ItemStack.EMPTY);
            var differentItem = gun("mat49", "pistol_bullet"); differentItem.set(ModDataComponents.GUN_ID, id);
            owner.player.getInventory().setItem(0, differentItem);
            h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == differentItem, "Changed item invalidates cached context before ascending first-match lookup");
            owner.player.getInventory().setItem(0, ItemStack.EMPTY);
            h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == copy, "Moved valid copy binds at slot7");
            var earlierCopy = copy.copy(); owner.player.getInventory().setItem(0, earlierCopy);
            h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == copy, "Still-valid slot7 remains cached before logout");
            GunIdentity.logout(new PlayerEvent.PlayerLoggedOutEvent(owner.player));
            h.assertTrue(GunIdentity.resolve(owner.player, id).orElseThrow().stack() == earlierCopy, "Logout clears even a still-valid cached later slot");
            h.assertTrue(id.equals(GunIdentity.id(copy)), "Never silently rekey copied source IDs");
        }
        h.succeed();
    }

    @GameTest(template="projectile_arena", templateNamespace="createloveandwar")
    public static void offhandCaptureAndInputCodecKeepOriginalReference(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = gun(); owner.player.getInventory().setItem(40, a); GunIdentity.inventoryTick(owner.player);
            var input = CGunInput.capture(owner.player, InteractionHand.OFF_HAND, "Fire1").orElseThrow();
            h.assertTrue(input.inventorySlot() == 40 && input.gunId().equals(GunIdentity.id(a)), "Offhand capture records its own logical identity");
            for (CGunInput value : List.of(input, input.released())) {
                var packet = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
                try {
                    CGunInput.STREAM_CODEC.encode(packet, value);
                    h.assertTrue(value.equals(CGunInput.STREAM_CODEC.decode(packet)) && !packet.isReadable(), "Press/release codec preserves full gun reference");
                } finally { packet.release(); }
            }
            h.assertTrue(GunIdentity.resolve(owner.player, input.gunId()).orElseThrow().hand() == InteractionHand.OFF_HAND, "Router preserves offhand location");
            owner.player.setXRot(-90); handle(owner, input);
            h.assertTrue(rounds(a) == 2, "Named offhand request reaches the gun instead of empty main hand");
        }
        h.succeed();
    }

    @GameTest(template="projectile_arena", templateNamespace="createloveandwar")
    public static void transferredGunRejectsOldOwnerAndAcceptsCurrentOwner(GameTestHelper h) throws ReflectiveOperationException {
        try (var oldOwner = new GunAudioGameTests.Listener(h); var currentOwner = new GunAudioGameTests.Listener(h, 10)) {
            var a = gun(); prepare(oldOwner, a); var stale = capture(oldOwner);
            oldOwner.player.getInventory().setItem(0, ItemStack.EMPTY); prepare(currentOwner, a);
            handle(oldOwner, stale);
            h.assertTrue(rounds(a) == 3, "Original ownership does not authorize a packet after transfer");
            handle(currentOwner, capture(currentOwner));
            h.assertTrue(rounds(a) == 2 && stale.gunId().equals(GunIdentity.id(a)), "New owner can use transferred gun without rekeying it");
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="createloveandwar")
    public static void unknownInputsCannotGrowIdentityCacheAndRemovedGunsArePruned(GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = gun(); prepare(owner, a);
            var field = GunIdentity.class.getDeclaredField("BINDINGS"); field.setAccessible(true);
            var all = (java.util.Map<?, ?>)field.get(null);
            for (int i = 0; i < 256; i++) h.assertTrue(GunIdentity.resolve(owner.player, UUID.randomUUID()).isEmpty(), "Unknown ID rejected");
            h.assertTrue(((java.util.Map<?, ?>)all.get(owner.player)).size() == 1, "Unowned requests cannot allocate identity bindings");
            owner.player.getInventory().setItem(0, ItemStack.EMPTY); GunIdentity.inventoryTick(owner.player);
            h.assertTrue(!all.containsKey(owner.player), "Inventory lifecycle removes absent gun IDs and empty owner map");
        }
        h.succeed();
    }
}
