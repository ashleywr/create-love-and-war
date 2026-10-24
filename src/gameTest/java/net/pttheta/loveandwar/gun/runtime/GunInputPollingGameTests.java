package net.pttheta.loveandwar.gun.runtime;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pttheta.loveandwar.gun.network.*;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunInputPollingGameTests {
    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void controlPhaseCodecRetainsHeldAndClearsItOnRelease(GameTestHelper h) {
        var buf =
                new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var base = new CGunInput("Fire1", true, 40, UUID.randomUUID());
            for (var packet :
                    List.of(
                            base,
                            base.controlled(),
                            base.heldUpdate(),
                            base.heldUpdate().released())) {
                CGunInput.STREAM_CODEC.encode(buf, packet);
                h.assertTrue(
                        CGunInput.STREAM_CODEC.decode(buf).equals(packet),
                        "Control phase round trip");
                buf.clear();
            }
            for (int flags : new int[] {1, 4, -1}) {
                var invalid =
                        new CGunInput(
                                "Fire1",
                                true,
                                40,
                                base.gunId(),
                                GunPredictionClaim.NONE,
                                GunAuthoredInput.NONE,
                                flags);
                CGunInput.STREAM_CODEC.encode(buf, invalid);
                boolean rejected = false;
                try {
                    CGunInput.STREAM_CODEC.decode(buf);
                } catch (io.netty.handler.codec.DecoderException expected) {
                    rejected = true;
                }
                h.assertTrue(rejected, "Invalid/uncontrolled held metadata must be rejected");
                buf.clear();
            }
        } finally {
            buf.release();
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void ownerReplacementCannotReleaseOrReusePreviousPlayersPress(GameTestHelper h) {
        try (var a = new Fixture(h);
                var b = new Fixture(h)) {
            var gun = a.gun(0, "mars", 4);
            b.gun(0, "mars", 4);
            a.poll("Fire1", true, true);
            var before = a.sent.size();
            a.dispatch.poll(b.player, "Fire1", false, true, b);
            h.assertTrue(
                    a.sent.size() == before && b.sent.isEmpty(),
                    "New player cannot release the old player's input references");
            a.dispatch.poll(b.player, "Fire1", true, true, b);
            h.assertTrue(
                    b.sent.size() == 1 && GunAmmo.count(gun) == 3,
                    "New owner has a fresh press state");
        }
        h.succeed();
    }

    private static ItemStack item(String key) {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + key)));
    }

    private static final class Fixture implements AutoCloseable, GunInputDispatcher.Sink {
        final GunAudioGameTests.Listener owner;
        final Player player;
        final GunInputDispatcher dispatch = new GunInputDispatcher();
        final Map<UUID, LocalGunActions> local = new LinkedHashMap<>();
        final List<CGunInput> sent = new ArrayList<>();
        final List<UUID> callbacks = new ArrayList<>();
        int acknowledged;

        Fixture(GameTestHelper h) {
            try {
                owner = new GunAudioGameTests.Listener(h);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
            player =
                    new Player(
                            h.getLevel(),
                            h.absolutePos(new BlockPos(2, 3, 2)),
                            0,
                            new GameProfile(owner.player.getUUID(), "PollTest")) {
                        @Override
                        public boolean isSpectator() {
                            return false;
                        }

                        @Override
                        public boolean isCreative() {
                            return false;
                        }
                    };
            player.setPos(owner.player.position());
            owner.player.setXRot(-90);
        }

        ItemStack gun(int slot, String name, int rounds) {
            var gun = item(name);
            String bullet = name.equals("g11k2") ? "caseless_bullet" : "pistol_bullet";
            gun.set(
                    ModDataComponents.GUN_LOADED_ROUNDS,
                    new AmmoPouchContents(
                            java.util.stream.IntStream.range(0, rounds)
                                    .mapToObj(i -> item(bullet))
                                    .toList()));
            owner.player.getInventory().setItem(slot, gun);
            GunIdentity.inventoryTick(owner.player);
            sync();
            return gun;
        }

        void sync() {
            for (int i = 0; i < owner.player.getInventory().getContainerSize(); i++)
                player.getInventory().setItem(i, owner.player.getInventory().getItem(i).copy());
            player.getInventory().selected = owner.player.getInventory().selected;
        }

        ItemStack actual(UUID id) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (GunIdentity.id(stack).equals(id)) return stack;
            }
            return ItemStack.EMPTY;
        }

        LocalGunActions runtime(CGunInput packet) {
            return local.computeIfAbsent(
                    packet.gunId(),
                    id ->
                            new LocalGunActions(
                                    player,
                                    actual(id),
                                    (g, key, x, y, index) -> callbacks.add(id)));
        }

        void handle(Object packet) {
            try {
                var context =
                        (IPayloadContext)
                                Proxy.newProxyInstance(
                                        IPayloadContext.class.getClassLoader(),
                                        new Class[] {IPayloadContext.class},
                                        (p, m, a) -> {
                                            if (m.getName().equals("player")) return owner.player;
                                            if (m.getName().equals("enqueueWork")) {
                                                ((Runnable) a[0]).run();
                                                return CompletableFuture.completedFuture(null);
                                            }
                                            throw new UnsupportedOperationException(m.getName());
                                        });
                var handler =
                        GunNetworking.class.getDeclaredMethod(
                                packet instanceof CGunInput ? "handle" : "handleAuthored",
                                packet.getClass(),
                                IPayloadContext.class);
                handler.setAccessible(true);
                handler.invoke(null, packet, context);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }

        void flush() {
            for (var value : local.values())
                for (var packet : value.drainAuthored()) handle(packet);
        }

        void feedback() {
            while (acknowledged < owner.packets.size()) {
                var packet = owner.packets.get(acknowledged++);
                if (packet instanceof ClientboundCustomPayloadPacket custom) {
                    if (custom.payload() instanceof SGunPresentationEnd end) {
                        var runtime = local.get(end.gun());
                        if (runtime != null) runtime.end(end);
                    }
                    if (custom.payload() instanceof SGunRecoil shot) {
                        var runtime = local.get(shot.receipt().gun());
                        if (runtime != null) runtime.acknowledge(shot.receipt());
                    }
                }
            }
        }

        @Override
        public CGunInput send(CGunInput input) {
            flush();
            var packet = runtime(input).input(input, actual(input.gunId()));
            sent.add(packet);
            handle(packet);
            flush();
            return packet;
        }

        @Override
        public Optional<CGunInput> held(CGunInput input) {
            flush();
            var result = runtime(input).held(input, actual(input.gunId()));
            result.ifPresent(
                    packet -> {
                        sent.add(packet);
                        handle(packet);
                    });
            flush();
            return result;
        }

        void poll(String input, boolean down, boolean continuous) {
            dispatch.poll(player, input, down, continuous, this);
        }

        void pre() {
            for (var entry : local.entrySet()) entry.getValue().tick(actual(entry.getKey()));
            flush();
        }

        List<SGunRecoil> shots() {
            return owner.packets.stream()
                    .filter(
                            p ->
                                    p instanceof ClientboundCustomPayloadPacket c
                                            && c.payload() instanceof SGunRecoil)
                    .map(p -> (SGunRecoil) ((ClientboundCustomPayloadPacket) p).payload())
                    .toList();
        }

        @Override
        public void close() {
            for (int i = 0; i < owner.player.getInventory().getContainerSize(); i++) {
                var stack = owner.player.getInventory().getItem(i);
                if (stack.getItem() instanceof GunItem)
                    GunFireScheduler.cancel(owner.player, stack);
            }
            owner.close();
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void rawPressVisitsMainThenOffhandAndReleasesBothCapturedIdentities(
            GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var main = f.gun(0, "mars", 4);
            var off = f.gun(40, "mars", 4);
            f.poll("Fire1", true, true);
            f.poll("Fire1", false, true);
            h.assertTrue(
                    f.callbacks.equals(List.of(GunIdentity.id(main), GunIdentity.id(off))),
                    "Raw press invokes main then offhand exactly once");
            h.assertTrue(
                    GunAmmo.count(main) == 3 && GunAmmo.count(off) == 3,
                    "Both authenticated inventory guns consume one round");
            h.assertTrue(
                    f.sent.size() == 4
                            && f.sent.get(2).gunId().equals(GunIdentity.id(main))
                            && f.sent.get(3).gunId().equals(GunIdentity.id(off)),
                    "Release uses both saved references in order");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void rawOffhandOnlyAndTwoHandedRestrictionsRemainIndependent(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var off = f.gun(40, "mars", 4);
            f.poll("Fire1", true, true);
            f.poll("Fire1", false, true);
            h.assertTrue(GunAmmo.count(off) == 3, "Offhand gun fires with an empty main hand");
        }
        try (var f = new Fixture(h)) {
            var off = f.gun(40, "mars", 4);
            var main = f.gun(0, "g11k2", 4);
            f.poll("Fire1", true, true);
            f.poll("Fire1", false, true);
            h.assertTrue(
                    GunAmmo.count(main) == 4 && GunAmmo.count(off) == 3,
                    "Two-handed G11 refuses while independent one-handed Mars still fires");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void missedRawTapAndHeldSemiCannotFabricatePresses(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var gun = f.gun(0, "mars", 4);
            f.poll("Fire1", false, true);
            h.assertTrue(
                    f.sent.isEmpty() && GunAmmo.count(gun) == 4,
                    "Unsampled key changes produce no press");
            f.poll("Fire1", true, true);
            for (int i = 0; i < 8; i++) {
                f.pre();
                f.poll("Fire1", true, true);
            }
            h.assertTrue(
                    GunAmmo.count(gun) == 3 && f.sent.size() == 1,
                    "Held Semi updates cannot restart or consume again");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void heldBurstRetainsItsThreeCallbacksWithoutRestartingInitialTrigger(
            GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var gun = f.gun(0, "g11k2", 12);
            f.poll("Fire1", true, true);
            for (int i = 0; i < 8; i++) {
                f.pre();
                f.poll("Fire1", true, true);
            }
            h.assertTrue(
                    GunAmmo.count(gun) == 9 && f.shots().size() == 3 && f.sent.size() == 1,
                    "Pressed then held preserves a single three-shot burst");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void heldSelectionCanStartNewFullAutoThenReleaseBothCapturedGuns(
            GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var a = f.gun(0, "mat49", 6);
            var b = f.gun(1, "mat49", 6);
            f.poll("Fire1", true, true);
            f.owner.player.getInventory().selected = 1;
            f.player.getInventory().selected = 1;
            f.poll("Fire1", true, true);
            h.assertTrue(
                    f.sent.size() == 2 && f.sent.get(1).isHeldUpdate(),
                    "Equipped FullAuto starts from held input after selection");
            h.assertTrue(
                    GunAmmo.count(a) == 5 && GunAmmo.count(b) == 5,
                    "Each start targets its logical gun");
            f.pre();
            f.poll("Fire1", false, true);
            f.pre();
            h.assertTrue(
                    GunAmmo.count(a) == 4 && GunAmmo.count(b) == 4,
                    "Release stops both captured identities after final eligible callback");
        }
        h.succeed();
    }

    @GameTest(
            template = "projectile_arena",
            templateNamespace = "createloveandwar",
            timeoutTicks = 140)
    public static void heldFullAutoAfterReloadRestartsFromOwnerWithoutServerFallback(
            GameTestHelper h) {
        var f = new Fixture(h);
        var gun = f.gun(0, "mat49", 4);
        f.owner
                .player
                .getInventory()
                .setItem(8, new ItemStack(item("pistol_bullet").getItem(), 12));
        f.sync();
        f.poll("Fire1", true, true);
        f.poll("Reload1", true, false);
        f.poll("Reload1", false, false);
        h.assertTrue(
                gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE)
                        .activeGroup()
                        .startsWith("reload"),
                "Real reload input starts reload stage");
        for (int tick = 1; tick <= 94; tick++) {
            int phase = tick;
            h.runAtTickTime(
                    tick,
                    () -> {
                        try {
                            GunEngine.tick(f.owner.player, gun);
                            f.feedback();
                            f.sync();
                            f.pre();
                            f.poll("Fire1", true, true);
                            if (phase == 94) {
                                h.assertTrue(
                                        f.sent.stream().anyMatch(CGunInput::isHeldUpdate),
                                        "Held physical input authors restart after reload");
                                h.assertTrue(
                                        f.shots().size() > 1
                                                && f.shots().stream()
                                                        .allMatch(p -> p.receipt().clientOwned()),
                                        "Reload restart cannot synthesize server-owned fallback"
                                            + " shots");
                                f.poll("Fire1", false, true);
                                f.close();
                                h.succeed();
                            }
                        } catch (RuntimeException ex) {
                            f.close();
                            throw ex;
                        }
                    });
        }
    }
}
