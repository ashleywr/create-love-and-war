package net.pttheta.loveandwar.gun.client.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.pttheta.loveandwar.gun.client.GunClientInput;
import net.pttheta.loveandwar.gun.client.GunKeyMappings;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.LoggerFactory;

/** Drives the normal reload key poll and observes real sound-backend and pose resolution. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class ActionTimingClientScenario {
    private static boolean armed;
    private static int sounds;
    private static String previous = "";
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_timing_client")
                .then(Commands.literal("observe").executes(context -> { armed = true; sounds = 0; previous = ""; return 1; }))
                .then(Commands.literal("stop").executes(context -> { armed = false; GunKeyMappings.RELOAD1.setDown(false); return 1; }))
                .then(Commands.literal("reload").executes(context -> {
                    var minecraft = Minecraft.getInstance();
                    if (minecraft.player == null || minecraft.screen != null) return 0;
                    armed = true; sounds = 0; previous = "";
                    GunKeyMappings.RELOAD1.setDown(true); GunClientInput.onClientTick(new ClientTickEvent.Post());
                    GunKeyMappings.RELOAD1.setDown(false); GunClientInput.onClientTick(new ClientTickEvent.Post());
                    return 1;
                })));
    }
    @SubscribeEvent public static void source(PlaySoundSourceEvent event) {
        if (!armed || !Boolean.getBoolean("law.enableClientScenario") || event.getSound() == null
                || !event.getSound().getLocation().getNamespace().equals("flansmod") || ++sounds > 64) return;
        var level = Minecraft.getInstance().level;
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TIMING_SOUND time={} id={} category={}",
                level == null ? -1 : level.getGameTime(), event.getSound().getLocation(), event.getSound().getSource());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!armed || !Boolean.getBoolean("law.enableClientScenario")) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        var stack = minecraft.player.getMainHandItem();
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        int rounds = stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size();
        String frame = state.activeGroup() + ":" + state.groupTotalTicks() + ":" + rounds;
        if (frame.equals(previous)) return;
        previous = frame;
        int remoteAge = GunLookup.gunDef(stack).stream().flatMap(def -> def.actionGroups().stream())
                .filter(group -> group.key().equals(state.activeGroup())).findFirst().map(group -> state.reloadAnimationElapsed(group.durationTicks())).orElse(0);
        var pose = GunPoseResolver.currentPose(stack, 0, false);
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TIMING_CLIENT time={} group={} elapsed={} total={} rounds={} observerPhase={} poseParts={}",
                minecraft.level.getGameTime(), state.activeGroup(), state.groupTicksElapsed(), state.groupTotalTicks(), rounds, remoteAge, pose.size());
    }
}
