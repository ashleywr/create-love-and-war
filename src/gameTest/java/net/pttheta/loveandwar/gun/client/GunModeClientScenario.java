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
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.gun.runtime.GunModes;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

/** Opt-in mode checks drive existing Use input and special-key poll, then observe synchronized state/render FOV. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunModeClientScenario {
    private static int fovSamples;
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_modes_client")
                .then(Commands.literal("status").executes(context -> {
                    var mc = Minecraft.getInstance(); if (mc.player == null) return 0;
                    var gun = mc.player.getMainHandItem();
                    var state = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    String mode = GunLookup.gunDef(gun).map(def -> GunModes.value(def, state, "iron_sight")).orElse("missing");
                    LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_MODE_CLIENT gun={} mode={} initialized={} stored={} toggles={} adsBlend={}",
                            GunLookup.contentKey(gun), mode, state.modesInitialized(), state.modeValues(), state.activeToggles(), AdsBlendState.blend());
                    fovSamples = 1; return 1;
                }))
                .then(Commands.literal("aim").executes(context -> {
                    var mc = Minecraft.getInstance(); if (mc.player == null) return 0;
                    mc.setScreen(null); mc.options.keyUse.setDown(true);
                    GunLeftClickFire.onInteractionKey(new InputEvent.InteractionKeyMappingTriggered(1, mc.options.keyUse, InteractionHand.MAIN_HAND));
                    mc.options.keyUse.setDown(false); return 1;
                }))
                .then(Commands.literal("switch").executes(context -> {
                    var mc = Minecraft.getInstance(); if (mc.player == null) return 0;
                    mc.setScreen(null); GunKeyMappings.SPECIAL_KEY_2.setDown(true);
                    GunClientInput.onClientTick(new ClientTickEvent.Post());
                    GunKeyMappings.SPECIAL_KEY_2.setDown(false); GunClientInput.onClientTick(new ClientTickEvent.Post());
                    return 1;
                })));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void fov(ViewportEvent.ComputeFov event) {
        if (fovSamples <= 0 || !Boolean.getBoolean("law.enableClientScenario")) return;
        fovSamples--;
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_MODE_FOV value={} adsBlend={}", event.getFOV(), AdsBlendState.blend());
    }
}
