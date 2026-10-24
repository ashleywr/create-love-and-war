package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.gun.runtime.GunAmmo;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;
import net.pttheta.loveandwar.gun.runtime.GunLookup;

import org.slf4j.LoggerFactory;

/** END0 press -> START1 -> END1 release -> START2, using real transport and tick subscribers. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class AuthoredFireClientScenario {
    private static boolean active, started, released;
    private static int before, age;
    private static java.util.UUID gun;

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher()
                .register(
                        Commands.literal("law_test_authored_onepoll")
                                .executes(
                                        context -> {
                                            var mc = Minecraft.getInstance();
                                            if (active
                                                    || mc.player == null
                                                    || mc.getSingleplayerServer() == null) return 0;
                                            var stack = mc.player.getMainHandItem();
                                            if (!GunLookup.contentKey(stack).equals("mat49")
                                                    || GunAmmo.count(stack) < 3)
                                                throw new IllegalStateException(
                                                        "Requires a ready loaded MAT49");
                                            mc.setScreen(null);
                                            before = GunAmmo.count(stack);
                                            gun = GunIdentity.id(stack);
                                            mc.options.keyAttack.setDown(true);
                                            GunLeftClickFire.onInteractionKey(
                                                    new InputEvent.InteractionKeyMappingTriggered(
                                                            0,
                                                            mc.options.keyAttack,
                                                            InteractionHand.MAIN_HAND));
                                            GunLeftClickFire.onClientTick(
                                                    new ClientTickEvent.Post());
                                            age = 0;
                                            started = released = false;
                                            active = true;
                                            return 1;
                                        }));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pre(ClientTickEvent.Pre event) {
        if (active) started = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void post(ClientTickEvent.Post event) {
        if (!active || !started) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            active = false;
            return;
        }
        if (!released) {
            mc.options.keyAttack.setDown(false);
            GunLeftClickFire.onClientTick(new ClientTickEvent.Post());
            released = true;
        }
        if (++age < 30) return;
        active = false;
        var stack = mc.player.getMainHandItem();
        int remaining = GunAmmo.count(stack), expected = before - 2;
        boolean client = GunIdentity.id(stack).equals(gun) && remaining == expected;
        LoggerFactory.getLogger("createloveandwar/scenario")
                .info(
                        "LAW_AUTHORED_CHECK name=one_poll_mat49_client pass={} rounds={}->{}"
                            + " expected={}",
                        client,
                        before,
                        remaining,
                        expected);
        var id = mc.player.getUUID();
        var gunId = gun;
        mc.getSingleplayerServer()
                .execute(
                        () -> {
                            var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                            var actual =
                                    owner == null
                                            ? null
                                            : GunIdentity.resolve(owner, gunId).orElse(null);
                            boolean server =
                                    actual != null && GunAmmo.count(actual.stack()) == expected;
                            LoggerFactory.getLogger("createloveandwar/scenario")
                                    .info(
                                            "LAW_AUTHORED_CHECK name=one_poll_mat49_server pass={}"
                                                + " expected={}",
                                            server,
                                            expected);
                        });
    }
}
