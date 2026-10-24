package net.pttheta.loveandwar.gun.runtime;

import io.netty.buffer.Unpooled;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pttheta.loveandwar.gun.network.*;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunPresentationGameTests {
    private static ItemStack item(String key) {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + key)));
    }

    private static ItemStack prepare(GunAudioGameTests.Listener owner) {
        var gun = item("g11k2");
        var round = item("caseless_bullet");
        gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(List.of(round, round.copy(), round.copy())));
        owner.player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        owner.player.setXRot(-90);
        GunIdentity.inventoryTick(owner.player);
        return gun;
    }

    private static void handle(GunAudioGameTests.Listener owner, CGunInput input)
            throws ReflectiveOperationException {
        var context =
                (IPayloadContext)
                        Proxy.newProxyInstance(
                                IPayloadContext.class.getClassLoader(),
                                new Class[] {IPayloadContext.class},
                                (p, m, args) -> {
                                    if (m.getName().equals("player")) return owner.player;
                                    if (m.getName().equals("enqueueWork")) {
                                        ((Runnable) args[0]).run();
                                        return CompletableFuture.completedFuture(null);
                                    }
                                    throw new UnsupportedOperationException(m.getName());
                                });
        var method =
                GunNetworking.class.getDeclaredMethod(
                        "handle", CGunInput.class, IPayloadContext.class);
        method.setAccessible(true);
        method.invoke(null, input, context);
    }

    private static List<SGunRecoil> shots(GunAudioGameTests.Listener owner) {
        return owner.packets.stream()
                .filter(
                        p ->
                                p instanceof ClientboundCustomPayloadPacket c
                                        && c.payload() instanceof SGunRecoil)
                .map(p -> (SGunRecoil) ((ClientboundCustomPayloadPacket) p).payload())
                .toList();
    }

    private static List<SGunPresentationEnd> ends(GunAudioGameTests.Listener owner) {
        return owner.packets.stream()
                .filter(
                        p ->
                                p instanceof ClientboundCustomPayloadPacket c
                                        && c.payload() instanceof SGunPresentationEnd)
                .map(p -> (SGunPresentationEnd) ((ClientboundCustomPayloadPacket) p).payload())
                .toList();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void presentationAuthorityCannotChangeMidActivation(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner);
            var input =
                    CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1")
                            .orElseThrow();
            handle(owner, input);
            handle(owner, input.released());
            var claim =
                    new GunPredictionClaim(
                            UUID.randomUUID(), "primary_fire", GunActiveGroups.Origin.base(gun));
            handle(owner, input.predicted(claim));
            h.assertTrue(
                    shots(owner).size() == 1
                            && !shots(owner).getFirst().receipt().clientOwned()
                            && GunAmmo.count(gun) == 2,
                    "An existing authoritative-fallback activation cannot also become locally"
                            + " owned");
            h.assertTrue(
                    ends(owner).stream()
                            .anyMatch(end -> end.prediction().equals(claim.activation())),
                    "New unmatched claim is retired");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void freshSameTickActivationsKeepDistinctCallbackZeroIdentities(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner);
            var def = net.pttheta.loveandwar.gun.content.GunContent.gun("g11k2").orElseThrow();
            var group =
                    def.actionGroups().stream()
                            .filter(g -> g.key().equals("primary_fire"))
                            .findFirst()
                            .orElseThrow();
            long now = h.getLevel().getGameTime();
            for (int n = 0; n < 2; n++) {
                var claim =
                        new GunPredictionClaim(
                                UUID.randomUUID(), group.key(), GunActiveGroups.Origin.base(gun));
                var input = new CGunInput("Fire1", true, 0, GunIdentity.id(gun), claim);
                GunPresentation.input(
                        owner.player,
                        gun,
                        input,
                        () ->
                                GunEngine.triggerActions(
                                        owner.player, gun, def, group, GunAttachments.EMPTY));
                GunActionTimeline.cancelAll(owner.player, gun);
            }
            var packets = shots(owner);
            h.assertTrue(
                    packets.size() == 2
                            && packets.stream()
                                    .allMatch(
                                            p ->
                                                    p.triggerIndex() == 0
                                                            && p.receipt().sequence() == 0),
                    "Every fresh instance starts at zero");
            h.assertTrue(
                    !packets.get(0)
                                    .receipt()
                                    .activation()
                                    .equals(packets.get(1).receipt().activation())
                            && !packets.get(0)
                                    .receipt()
                                    .prediction()
                                    .equals(packets.get(1).receipt().prediction())
                            && h.getLevel().getGameTime() == now,
                    "Neither server nor local activation relies on StartTick or trigger ordinal for"
                            + " identity");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void oneGunInputCannotClaimOtherGunCallback(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var a = prepare(owner);
            var b = a.copy();
            b.set(ModDataComponents.GUN_ID, UUID.randomUUID());
            owner.player.getInventory().setItem(1, b);
            var def = net.pttheta.loveandwar.gun.content.GunContent.gun("g11k2").orElseThrow();
            var group =
                    def.actionGroups().stream()
                            .filter(g -> g.key().equals("primary_fire"))
                            .findFirst()
                            .orElseThrow();
            var claim =
                    new GunPredictionClaim(
                            UUID.randomUUID(), group.key(), GunActiveGroups.Origin.base(a));
            var input = new CGunInput("Fire1", true, 0, GunIdentity.id(a), claim);
            GunPresentation.input(
                    owner.player,
                    a,
                    input,
                    () ->
                            GunEngine.triggerActions(
                                    owner.player, b, def, group, GunAttachments.EMPTY));
            var receipt = shots(owner).getFirst().receipt();
            h.assertTrue(
                    receipt.gun().equals(GunIdentity.id(b))
                            && !receipt.clientOwned()
                            && GunAmmo.count(a) == 3
                            && GunAmmo.count(b) == 2,
                    "An independently server-authored callback for B cannot inherit A's"
                            + " presentation claim");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void validatedClaimCorrelatesCallbackAndAmmoWithoutAuthorizingDamage(
            GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner);
            var claim =
                    new GunPredictionClaim(
                            UUID.randomUUID(), "primary_fire", GunActiveGroups.Origin.base(gun));
            var input =
                    CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1")
                            .orElseThrow()
                            .predicted(claim);
            handle(owner, input);
            var shot = shots(owner).getFirst();
            var receipt = shot.receipt();
            h.assertTrue(
                    shot.triggerIndex() == 0
                            && receipt.clientOwned()
                            && receipt.gun().equals(input.gunId())
                            && receipt.prediction().equals(claim.activation()),
                    "Validated retained provider carries exact gun and local activation ownership");
            h.assertTrue(
                    receipt.action() == 0
                            && receipt.sequence() == 0
                            && receipt.consumed() == 1
                            && receipt.ammoRevision() == 1
                            && receipt.remainingRounds() == 2,
                    "Authoritative callback identifies Shoot slot and acknowledged consumption,"
                            + " with no client damage values");
            h.assertTrue(
                    GunAmmo.count(gun) == 2 && ends(owner).isEmpty(),
                    "Gameplay still consumes exactly one server round; accepted claim is not"
                            + " denied");
            GunFireScheduler.cancel(owner.player, gun);
            GunActionTimeline.cancelAll(owner.player, gun);
            h.assertTrue(
                    ends(owner)
                            .equals(
                                    List.of(
                                            new SGunPresentationEnd(
                                                    input.gunId(), claim.activation(), 1, 1, 2))),
                    "Retiring group sends final matching ammo acknowledgement");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void mismatchedProviderClaimCannotFireOrConsumeAnotherContext(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner);
            var claim =
                    new GunPredictionClaim(
                            UUID.randomUUID(),
                            "primary_fire",
                            new GunActiveGroups.Origin("", -1, "mars"));
            var input =
                    CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1")
                            .orElseThrow()
                            .predicted(claim);
            handle(owner, input);
            h.assertTrue(
                    shots(owner).isEmpty() && GunAmmo.count(gun) == 3 && GunAmmo.revision(gun) == 0,
                    "Claim must match independently resolved actual provider before gameplay");
            h.assertTrue(
                    ends(owner)
                            .equals(
                                    List.of(
                                            new SGunPresentationEnd(
                                                    input.gunId(), claim.activation(), 0, 0, 3))),
                    "Denial retires only the named local claim");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void gunPresentationCodecsKeepClaimsReceiptsAndReleaseIdentity(GameTestHelper h) {
        var gun = UUID.randomUUID();
        var claim =
                new GunPredictionClaim(
                        UUID.randomUUID(),
                        "primary_fire",
                        new GunActiveGroups.Origin("Sights", 2, "attachment_red_dot"));
        var input = new CGunInput("Fire1", true, 40, gun, claim);
        var receipt =
                new GunShotReceipt(
                        gun, UUID.randomUUID(), 3, 123, claim.activation(), 7, 50, 5, true);
        var shot = new SGunRecoil(9, 15, 33, receipt);
        var end = new SGunPresentationEnd(gun, claim.activation(), 7, 50, 5);
        var packet = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            CGunInput.STREAM_CODEC.encode(packet, input);
            CGunInput.STREAM_CODEC.encode(packet, input.released());
            SGunRecoil.STREAM_CODEC.encode(packet, shot);
            SGunPresentationEnd.STREAM_CODEC.encode(packet, end);
            h.assertTrue(
                    CGunInput.STREAM_CODEC.decode(packet).equals(input)
                            && CGunInput.STREAM_CODEC.decode(packet).equals(input.released()),
                    "Input press/release claim stays attached to gun/provider");
            h.assertTrue(
                    SGunRecoil.STREAM_CODEC.decode(packet).equals(shot)
                            && SGunPresentationEnd.STREAM_CODEC.decode(packet).equals(end)
                            && !packet.isReadable(),
                    "Every callback/ammo/authority field round-trips");
        } finally {
            packet.release();
        }
        h.succeed();
    }
}
