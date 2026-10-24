package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import org.slf4j.LoggerFactory;

/** The same raw-key END probe runs against the unchanged baseline and the repaired input poll. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class InputLifecycleClientScenario {
    private static int bothAge = -1;
    private static java.util.UUID mainId, offId;

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher()
                .register(
                        Commands.literal("law_test_input_interaction")
                                .executes(
                                        context -> {
                                            var mc = Minecraft.getInstance();
                                            if (mc.player == null) return 0;
                                            if (!(mc.player.getOffhandItem().getItem()
                                                    instanceof
                                                    net.pttheta.loveandwar.gun.runtime.GunItem))
                                                throw new IllegalStateException(
                                                        "Requires a prepared offhand gun");
                                            var main = mc.player.getMainHandItem();
                                            try {
                                                mc.player.setItemInHand(
                                                        net.minecraft.world.InteractionHand
                                                                .MAIN_HAND,
                                                        new net.minecraft.world.item.ItemStack(
                                                                net.minecraft.world.item.Items
                                                                        .IRON_SWORD));
                                                var attack =
                                                        new net.neoforged.neoforge.client.event
                                                                .InputEvent
                                                                .InteractionKeyMappingTriggered(
                                                                0,
                                                                mc.options.keyAttack,
                                                                net.minecraft.world.InteractionHand
                                                                        .MAIN_HAND);
                                                GunLeftClickFire.onInteractionKey(attack);
                                                boolean pass = !attack.isCanceled();
                                                LoggerFactory.getLogger("createloveandwar/scenario")
                                                        .info(
                                                                "LAW_INPUT_CHECK"
                                                                    + " name=offhand_preserves_main_interaction"
                                                                    + " pass={}",
                                                                pass);
                                                if (!pass)
                                                    throw new IllegalStateException(
                                                            "Offhand gun cannot cancel main-hand"
                                                                + " sword input");
                                            } finally {
                                                mc.player.setItemInHand(
                                                        net.minecraft.world.InteractionHand
                                                                .MAIN_HAND,
                                                        main);
                                            }
                                            return 1;
                                        }));
        event.getDispatcher()
                .register(
                        Commands.literal("law_test_input_raw")
                                .executes(
                                        context -> {
                                            var mc = Minecraft.getInstance();
                                            if (mc.player == null) return 0;
                                            mc.setScreen(null);
                                            try {
                                                var field =
                                                        GunRecoil.class.getDeclaredField("STATE");
                                                field.setAccessible(true);
                                                var state = field.get(null);
                                                var pending =
                                                        state.getClass()
                                                                .getDeclaredField("pendingPitch");
                                                pending.setAccessible(true);
                                                float before = pending.getFloat(state);
                                                mc.options.keyAttack.setDown(true);
                                                GunLeftClickFire.onClientTick(
                                                        new ClientTickEvent.Post());
                                                float added = pending.getFloat(state) - before;
                                                mc.options.keyAttack.setDown(false);
                                                GunLeftClickFire.onClientTick(
                                                        new ClientTickEvent.Post());
                                                LoggerFactory.getLogger("createloveandwar/scenario")
                                                        .info(
                                                                "LAW_INPUT_CHECK name=raw_end_press"
                                                                        + " pass={} added={}",
                                                                added > 0,
                                                                added);
                                            } catch (ReflectiveOperationException ex) {
                                                throw new IllegalStateException(ex);
                                            }
                                            return 1;
                                        }));
        event.getDispatcher()
                .register(
                        Commands.literal("law_test_input_short")
                                .executes(
                                        context -> {
                                            var mc = Minecraft.getInstance();
                                            if (mc.player == null) return 0;
                                            mc.setScreen(null);
                                            try {
                                                var field =
                                                        GunRecoil.class.getDeclaredField("STATE");
                                                field.setAccessible(true);
                                                var state = field.get(null);
                                                var pending =
                                                        state.getClass()
                                                                .getDeclaredField("pendingPitch");
                                                pending.setAccessible(true);
                                                float before = pending.getFloat(state);
                                                mc.options.keyAttack.setDown(true);
                                                GunLeftClickFire.onInteractionKey(
                                                        new net.neoforged.neoforge.client.event
                                                                .InputEvent
                                                                .InteractionKeyMappingTriggered(
                                                                0,
                                                                mc.options.keyAttack,
                                                                net.minecraft.world.InteractionHand
                                                                        .MAIN_HAND));
                                                mc.options.keyAttack.setDown(false);
                                                GunLeftClickFire.onClientTick(
                                                        new ClientTickEvent.Post());
                                                boolean pass = pending.getFloat(state) == before;
                                                LoggerFactory.getLogger("createloveandwar/scenario")
                                                        .info(
                                                                "LAW_INPUT_CHECK"
                                                                    + " name=unsampled_tap_ignored"
                                                                    + " pass={}",
                                                                pass);
                                                if (!pass)
                                                    throw new IllegalStateException(
                                                            "Interaction must not invent a raw END"
                                                                    + " press");
                                            } catch (ReflectiveOperationException ex) {
                                                throw new IllegalStateException(ex);
                                            }
                                            return 1;
                                        }));
        event.getDispatcher()
                .register(
                        Commands.literal("law_test_input_both")
                                .executes(
                                        context -> {
                                            var mc = Minecraft.getInstance();
                                            if (mc.player == null || bothAge >= 0) return 0;
                                            mc.setScreen(null);
                                            var main = mc.player.getMainHandItem();
                                            var off = mc.player.getOffhandItem();
                                            if (net.pttheta.loveandwar.gun.runtime.GunAmmo.count(
                                                                    main)
                                                            != 3
                                                    || net.pttheta.loveandwar.gun.runtime.GunAmmo
                                                                    .count(off)
                                                            != 3)
                                                throw new IllegalStateException(
                                                        "Requires prepared3round guns in both"
                                                                + " hands");
                                            mainId =
                                                    net.pttheta.loveandwar.gun.runtime.GunIdentity
                                                            .id(main);
                                            offId =
                                                    net.pttheta.loveandwar.gun.runtime.GunIdentity
                                                            .id(off);
                                            mc.options.keyAttack.setDown(true);
                                            GunLeftClickFire.onClientTick(
                                                    new ClientTickEvent.Post());
                                            mc.options.keyAttack.setDown(false);
                                            GunLeftClickFire.onClientTick(
                                                    new ClientTickEvent.Post());
                                            bothAge = 0;
                                            return 1;
                                        }));
        event.getDispatcher()
                .register(
                        Commands.literal("law_test_input_profile")
                                .executes(
                                        context -> {
                                            var mc = Minecraft.getInstance();
                                            if (mc.player == null) return 0;
                                            mc.setScreen(null);
                                            if (!net.pttheta.loveandwar.gun.runtime.GunLookup
                                                            .contentKey(mc.player.getMainHandItem())
                                                            .equals("mat49")
                                                    || net.pttheta.loveandwar.gun.runtime.GunAmmo
                                                                    .count(
                                                                            mc.player
                                                                                    .getMainHandItem())
                                                            < 3)
                                                throw new IllegalStateException(
                                                        "Requires a ready loaded MAT49");
                                            mc.options.keyAttack.setDown(true);
                                            GunLeftClickFire.onClientTick(
                                                    new ClientTickEvent.Post());
                                            var packet =
                                                    net.pttheta.loveandwar.gun.network.CGunInput
                                                            .capture(
                                                                    mc.player,
                                                                    net.minecraft.world
                                                                            .InteractionHand
                                                                            .MAIN_HAND,
                                                                    "Fire1")
                                                            .orElseThrow()
                                                            .heldUpdate();
                                            var bean =
                                                    (com.sun.management.ThreadMXBean)
                                                            java.lang.management.ManagementFactory
                                                                    .getThreadMXBean();
                                            bean.setThreadAllocatedMemoryEnabled(true);
                                            long thread = Thread.currentThread().threadId();
                                            try {
                                                for (int i = 0; i < 5000; i++)
                                                    if (GunPrediction.held(packet).isPresent())
                                                        throw new IllegalStateException(
                                                                "Active held callback cannot"
                                                                        + " restart");
                                                for (int batch = 0; batch < 7; batch++) {
                                                    long
                                                            bytes =
                                                                    bean.getThreadAllocatedBytes(
                                                                            thread),
                                                            started = System.nanoTime();
                                                    int sent = 0;
                                                    for (int i = 0; i < 10000; i++)
                                                        if (GunPrediction.held(packet).isPresent())
                                                            sent++;
                                                    long nanos = System.nanoTime() - started;
                                                    bytes =
                                                            bean.getThreadAllocatedBytes(thread)
                                                                    - bytes;
                                                    LoggerFactory.getLogger(
                                                                    "createloveandwar/scenario")
                                                            .info(
                                                                    "LAW_INPUT_PROFILE batch={}"
                                                                            + " iterations=10000"
                                                                            + " nsPerHold={}"
                                                                            + " bytesPerHold={}"
                                                                            + " starts={}",
                                                                    batch,
                                                                    nanos / 10000.0,
                                                                    bytes / 10000.0,
                                                                    sent);
                                                    if (sent != 0)
                                                        throw new IllegalStateException(
                                                                "Profile cannot author additional"
                                                                        + " starts");
                                                }
                                            } finally {
                                                mc.options.keyAttack.setDown(false);
                                                GunLeftClickFire.onClientTick(
                                                        new ClientTickEvent.Post());
                                            }
                                            return 1;
                                        }));
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void post(ClientTickEvent.Post event) {
        if (bothAge < 0 || ++bothAge < 25) return;
        bothAge = -1;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var main = mc.player.getMainHandItem();
        var off = mc.player.getOffhandItem();
        boolean client =
                net.pttheta.loveandwar.gun.runtime.GunIdentity.id(main).equals(mainId)
                        && net.pttheta.loveandwar.gun.runtime.GunIdentity.id(off).equals(offId)
                        && net.pttheta.loveandwar.gun.runtime.GunAmmo.count(main) == 2
                        && net.pttheta.loveandwar.gun.runtime.GunAmmo.count(off) == 2;
        LoggerFactory.getLogger("createloveandwar/scenario")
                .info("LAW_INPUT_CHECK name=both_hands_client pass={}", client);
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(
                () -> {
                    var owner = server.getPlayerList().getPlayer(id);
                    boolean pass =
                            owner != null
                                    && net.pttheta.loveandwar.gun.runtime.GunAmmo.count(
                                                    owner.getMainHandItem())
                                            == 2
                                    && net.pttheta.loveandwar.gun.runtime.GunAmmo.count(
                                                    owner.getOffhandItem())
                                            == 2;
                    LoggerFactory.getLogger("createloveandwar/scenario")
                            .info("LAW_INPUT_CHECK name=both_hands_server pass={}", pass);
                });
    }
}
