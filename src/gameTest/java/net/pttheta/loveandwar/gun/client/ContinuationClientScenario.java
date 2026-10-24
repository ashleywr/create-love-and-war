package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.gun.content.stat.SightFov;
import net.pttheta.loveandwar.gun.runtime.*;
import net.pttheta.loveandwar.item.ModDataComponents;

import org.slf4j.LoggerFactory;

import java.util.List;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class ContinuationClientScenario {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        var check = Commands.literal("law_test_continuation_client");
        for (boolean on : List.of(true, false))
            check.then(
                    Commands.literal(on ? "on" : "off")
                            .executes(
                                    context -> {
                                        var mc = Minecraft.getInstance();
                                        if (mc.player == null) return 0;
                                        var gun = mc.player.getMainHandItem();
                                        var groups = GunActiveGroups.of(gun);
                                        var keys =
                                                groups.entries().stream()
                                                        .map(GunActiveGroups.Entry::key)
                                                        .toList();
                                        result(
                                                "groups_" + on,
                                                keys.equals(
                                                        on
                                                                ? List.of("first", "second")
                                                                : List.of()));
                                        var sights =
                                                GunAimZoom.addSights(
                                                        SightFov.EMPTY,
                                                        GunLookup.gunDef(gun).orElseThrow(),
                                                        gun.getOrDefault(
                                                                ModDataComponents.GUN_ATTACHMENTS,
                                                                GunAttachments.EMPTY),
                                                        groups,
                                                        mc.player.getUUID());
                                        result(
                                                "mean_" + on,
                                                sights.count() == (on ? 2 : 0)
                                                        && sights.mean() == (on ? 3 : 1));
                                        var modifier = new ComputeFovModifierEvent(mc.player, 1);
                                        modifier.setNewFovModifier(1);
                                        GunAimZoom.onComputeFov(modifier);
                                        float expected =
                                                1
                                                        / ((1
                                                                        - net.pttheta.loveandwar
                                                                                .Config.ADS_ZOOM
                                                                                .get()
                                                                                .floatValue())
                                                                + (on ? 3 : 1)
                                                                        * net.pttheta.loveandwar
                                                                                .Config.ADS_ZOOM
                                                                                .get()
                                                                                .floatValue());
                                        result(
                                                "fov_" + on,
                                                Math.abs(modifier.getNewFovModifier() - expected)
                                                        < .00001F);
                                        return 1;
                                    }));
        event.getDispatcher().register(check);
    }

    private static void result(String name, boolean pass) {
        LoggerFactory.getLogger("createloveandwar/scenario")
                .info("LAW_CONTINUATION_CHECK name={} pass={}", name, pass);
        if (!pass) throw new IllegalStateException(name);
    }
}
