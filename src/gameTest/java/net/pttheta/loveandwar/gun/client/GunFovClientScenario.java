package net.pttheta.loveandwar.gun.client;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.SightFov;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.StatSources;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

/** Opt-in client oracle; synthetic numeric fixtures never replace shared content or server inventory. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunFovClientScenario {
    private static int samples;
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_fov")
                .then(Commands.literal("check").executes(context -> check()))
                .then(Commands.literal("sample").executes(context -> { samples = 12; return 1; })));
    }
    private static GunActionState active(String... keys) {
        return new GunActionState("", 0, 0, List.of(), Map.of(), List.of(keys), 0, 0, false);
    }
    private static void result(String name, double actual, double expected) {
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_FOV_CHECK name={} actual={} expected={} pass={}",
                name, actual, expected, Double.isFinite(actual) && Math.abs(actual - expected) < 0.0001);
    }
    private static float factor(GunDef def, GunActionState state) {
        var sights = SightFov.EMPTY;
        for (var group : def.actionGroups()) if (state.activeToggles().contains(group.key())) sights = sights.add(group, StatSources.forFiring(def, group, GunAttachments.EMPTY));
        return sights.mean();
    }
    private static int check() {
        result("krag_scope", factor(GunContent.gun("krag").orElseThrow(), active("ads_0")), 4);
        result("krag_iron", factor(GunContent.gun("krag").orElseThrow(), active("ads_1")), 1.25);
        var json = GunDef.CODEC.encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow()).getOrThrow().getAsJsonObject();
        json.add("actionGroups", JsonParser.parseString("""
            [{"key":"renamed","actions":[{"actionType":"AimDownSights","fovFactor":99},{"actionType":"Scope","fovFactor":99}],
              "modifiers":[{"stat":"fov_factor","accumulators":[{"operation":"BaseAdd","value":2}]}]},
             {"key":"second","actions":[{"actionType":"Scope"}],
              "modifiers":[{"stat":"fov_factor","accumulators":[{"operation":"BaseAdd","value":6}]}]},
             {"key":"wide","actions":[{"actionType":"AimDownSights"}],
              "modifiers":[{"stat":"fov_factor","accumulators":[{"operation":"BaseAdd","value":0.5}]}]}]
            """));
        var def = GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        result("per_action_mean", factor(def, active("renamed", "second")), 10.0 / 3);
        result("factor_below_one", factor(def, active("wide")), 0.5);
        result("idle", factor(def, GunActionState.IDLE), 1);
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null) return 0;
        if (!"krag".equals(GunLookup.contentKey(player.getMainHandItem()))) throw new IllegalStateException("FOV oracle requires the Krag scenario");
        var main = player.getInventory().items.get(player.getInventory().selected);
        var off = player.getInventory().offhand.get(0);
        try {
            var first = main.copy(); first.set(ModDataComponents.GUN_ACTION_STATE, active("ads_0"));
            first.set(ModDataComponents.GUN_ACTIVE_GROUPS, new GunActiveGroups(List.of(new GunActiveGroups.Entry("ads_0", GunActiveGroups.Origin.base(first), UUID.randomUUID(), player.level().getGameTime(), player.getUUID()))));
            var second = main.copy(); second.set(ModDataComponents.GUN_ACTION_STATE, active("ads_1"));
            second.set(ModDataComponents.GUN_ACTIVE_GROUPS, new GunActiveGroups(List.of(new GunActiveGroups.Entry("ads_1", GunActiveGroups.Origin.base(second), UUID.randomUUID(), player.level().getGameTime(), player.getUUID()))));
            player.getInventory().items.set(player.getInventory().selected, first);
            player.getInventory().offhand.set(0, second);
            var modifier = new ComputeFovModifierEvent(player, 1.1F); modifier.setNewFovModifier(1.1F);
            NeoForge.EVENT_BUS.post(modifier);
            double strength = Config.ADS_ZOOM.get();
            result("both_hands_modifier", modifier.getNewFovModifier(), 1.1 / (1 + (2.625 - 1) * strength));
            var remote = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "FovObserver"));
            var remoteModifier = new ComputeFovModifierEvent(remote, 1.1F); remoteModifier.setNewFovModifier(1.1F);
            NeoForge.EVENT_BUS.post(remoteModifier);
            result("camera_player_keeps_local_sights", remoteModifier.getNewFovModifier(), 1.1 / (1 + (2.625 - 1) * strength));
            var hand = new ViewportEvent.ComputeFov(mc.gameRenderer, mc.gameRenderer.getMainCamera(), 0, 70, false);
            NeoForge.EVENT_BUS.post(hand);
            result("hand_projection_unchanged", hand.getFOV(), 70);
            player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
            modifier.setNewFovModifier(1.1F); NeoForge.EVENT_BUS.post(modifier);
            result("offhand_only_modifier", modifier.getNewFovModifier(), 1.1 / (1 + (1.25 - 1) * strength));
            player.getInventory().offhand.set(0, ItemStack.EMPTY);
            modifier.setNewFovModifier(1.1F); NeoForge.EVENT_BUS.post(modifier);
            result("released_sights_no_stale_factor", modifier.getNewFovModifier(), 1.1);
        } finally {
            player.getInventory().items.set(player.getInventory().selected, main);
            player.getInventory().offhand.set(0, off);
        }
        return 1;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void fov(ViewportEvent.ComputeFov event) {
        if (samples <= 0 || !Boolean.getBoolean("law.enableClientScenario")) return;
        samples--;
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_FOV_PROJECTION configured={} value={} blend={} strength={}",
                event.usedConfiguredFov(), event.getFOV(), AdsBlendState.blend(), Config.ADS_ZOOM.get());
    }
}
