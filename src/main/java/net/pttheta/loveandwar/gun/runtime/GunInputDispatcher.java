package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.pttheta.loveandwar.gun.network.CGunInput;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Raw END samples, main then offhand; transport and presentation are supplied by the owning side.
 */
public final class GunInputDispatcher {
    public interface Sink {
        CGunInput send(CGunInput input);

        Optional<CGunInput> held(CGunInput input);
    }

    private static final class Button {
        boolean down;
        final Map<UUID, CGunInput> references = new LinkedHashMap<>();
    }

    private Player owner;
    private final Map<String, Button> buttons = new LinkedHashMap<>();

    public void reset(Player player) {
        if (owner != player) {
            buttons.clear();
            owner = player;
        }
    }

    public void poll(Player player, String input, boolean down, boolean continuous, Sink sink) {
        reset(player);
        if (player == null) return;
        var button = buttons.computeIfAbsent(input, ignored -> new Button());
        if (down && !button.down) {
            button.references.clear();
            for (var hand : InteractionHand.values())
                if (player.getItemInHand(hand).getItem() instanceof GunItem)
                    CGunInput.capture(player, hand, input)
                            .ifPresent(
                                    packet -> {
                                        var sent = sink.send(packet.controlled());
                                        button.references.put(sent.gunId(), sent);
                                    });
        }
        if (down && continuous) {
            for (var hand : InteractionHand.values())
                if (player.getItemInHand(hand).getItem() instanceof GunItem)
                    CGunInput.capture(player, hand, input)
                            .ifPresent(
                                    packet ->
                                            sink.held(packet.heldUpdate())
                                                    .ifPresent(
                                                            sent ->
                                                                    button.references.put(
                                                                            sent.gunId(), sent)));
        }
        if (!down && button.down) {
            // Retain captured identities across slot changes: releasing cannot stop the newly
            // selected gun instead.
            for (var reference : button.references.values()) sink.send(reference.released());
            button.references.clear();
        }
        button.down = down;
    }
}
