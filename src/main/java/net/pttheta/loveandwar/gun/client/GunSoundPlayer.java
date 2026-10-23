package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.pttheta.loveandwar.gun.network.SGunSound;

public final class GunSoundPlayer {
    private GunSoundPlayer() {}

    public static void play(SGunSound payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.cameraEntity == null) return;
        double distance = Math.sqrt(minecraft.cameraEntity.distanceToSqr(payload.position()));
        var sound = payload.sound().resolve(distance, minecraft.level.random.nextFloat(), payload.volumeMultiplier(), payload.pitchMultiplier());
        var id = ResourceLocation.tryParse(sound.sound());
        if (id == null || !Float.isFinite(sound.volume()) || !Float.isFinite(sound.pitch()) || !Float.isFinite(sound.range()) || sound.range() <= 0) return;
        var position = payload.position();
        minecraft.level.playLocalSound(position.x, position.y, position.z, SoundEvent.createFixedRangeEvent(id, sound.range()),
                SoundSource.HOSTILE, sound.volume(), sound.pitch(), true);
    }
}
