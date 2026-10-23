package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.pttheta.loveandwar.gun.content.ActionDef;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.stat.ModifierContext;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.network.SGunSound;
import net.pttheta.loveandwar.network.ModMessages;

public final class GunSounds {
    private GunSounds() {}

    public static void send(ServerPlayer player, ActionGroup group, ActionDef action, List<ModifierSource> sources) {
        var context = ModifierContext.ofGroups(List.of(group.key()));
        float volume = StatResolver.resolve("loudness", 1, sources, context);
        float pitch = StatResolver.resolve("pitch", 1, sources, context);
        // PlaySound/Animation actions inherit a 200-block propagation radius in the original engine.
        double radius = group.actions().stream().anyMatch(a -> "Shoot".equals(a.actionType()))
                ? Math.max(200, StatResolver.resolve("loudness", group.loudness(), sources, context)) : 200;
        var position = player.getEyePosition();
        for (var sound : action.sounds()) {
            if (!sound.sound().isEmpty()) ModMessages.sendToNearbyPlayers(new SGunSound(sound, position, volume, pitch),
                    player.serverLevel(), position.x, position.y, position.z, radius);
        }
    }
}
