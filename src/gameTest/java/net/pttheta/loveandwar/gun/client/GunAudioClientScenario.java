package net.pttheta.loveandwar.gun.client;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import org.slf4j.LoggerFactory;

/** Observes actual sound-engine attempts and non-streaming backend submissions. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunAudioClientScenario {
    private static final AtomicInteger attempts = new AtomicInteger();
    private static final AtomicInteger sources = new AtomicInteger();
    private static boolean accepts(SoundInstance sound) {
        return Boolean.getBoolean("law.enableClientScenario") && sound != null
                && sound.getLocation().getNamespace().equals("flansmod")
                && sound.getLocation().getPath().startsWith("gunshot_");
    }

    private static void log(String stage, SoundInstance sound, int count) {
        // Bound output even if a diagnostic world is left firing.
        if (count <= 32) LoggerFactory.getLogger("createloveandwar/scenario").info(
                "LAW_AUDIO_{} count={} id={} category={} volume={} pitch={} x={} y={} z={}",
                stage, count, sound.getLocation(), sound.getSource(), sound.getVolume(), sound.getPitch(), sound.getX(), sound.getY(), sound.getZ());
    }

    @SubscribeEvent public static void attempt(PlaySoundEvent event) {
        if (accepts(event.getOriginalSound())) {
            var sound = event.getOriginalSound();
            int count = attempts.incrementAndGet();
            // This hook runs before asset resolution; getVolume/getPitch require a resolved Sound.
            if (count <= 32) LoggerFactory.getLogger("createloveandwar/scenario").info(
                    "LAW_AUDIO_ATTEMPT count={} id={} category={}", count, sound.getLocation(), sound.getSource());
        }
    }

    @SubscribeEvent public static void submitted(PlaySoundSourceEvent event) {
        if (accepts(event.getSound())) log("SOURCE", event.getSound(), sources.incrementAndGet());
    }

    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_audio_client")
                .then(Commands.literal("reset").executes(context -> {
                    attempts.set(0); sources.set(0);
                    LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_AUDIO_RESET");
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_AUDIO_STATUS attempts={} sources={}", attempts.get(), sources.get());
                    return 1;
                })));
    }
}
