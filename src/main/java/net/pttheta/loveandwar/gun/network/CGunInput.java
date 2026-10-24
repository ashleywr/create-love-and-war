package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;

import java.util.Optional;
import java.util.UUID;

public record CGunInput(
        String inputType,
        boolean pressed,
        int inventorySlot,
        UUID gunId,
        GunPredictionClaim prediction,
        GunAuthoredInput authored,
        int controlFlags)
        implements CustomPacketPayload {
    public CGunInput(
            String inputType,
            boolean pressed,
            int inventorySlot,
            UUID gunId,
            GunPredictionClaim prediction,
            GunAuthoredInput authored) {
        this(inputType, pressed, inventorySlot, gunId, prediction, authored, 0);
    }

    public CGunInput(String inputType, boolean pressed, int inventorySlot, UUID gunId) {
        this(
                inputType,
                pressed,
                inventorySlot,
                gunId,
                GunPredictionClaim.NONE,
                GunAuthoredInput.NONE);
    }

    public CGunInput(
            String inputType,
            boolean pressed,
            int inventorySlot,
            UUID gunId,
            GunPredictionClaim prediction) {
        this(inputType, pressed, inventorySlot, gunId, prediction, GunAuthoredInput.NONE);
    }

    public static final Type<CGunInput> TYPE = new Type<>(LoveAndWarMod.asResource("gun_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CGunInput> STREAM_CODEC =
            StreamCodec.of(
                    (buf, input) -> {
                        buf.writeUtf(input.inputType(), 64);
                        buf.writeBoolean(input.pressed());
                        buf.writeVarInt(input.inventorySlot());
                        buf.writeUUID(input.gunId());
                        input.prediction().write(buf);
                        input.authored().write(buf);
                        buf.writeVarInt(input.controlFlags());
                    },
                    buf -> {
                        var input =
                                new CGunInput(
                                        buf.readUtf(64),
                                        buf.readBoolean(),
                                        buf.readVarInt(),
                                        buf.readUUID(),
                                        GunPredictionClaim.read(buf),
                                        GunAuthoredInput.read(buf),
                                        buf.readVarInt());
                        if (input.controlFlags() < 0
                                || input.controlFlags() > 3
                                || input.isHeldUpdate()
                                        && (!input.pressed() || !input.clientControlled()))
                            throw new io.netty.handler.codec.DecoderException(
                                    "Invalid gun control phase");
                        return input;
                    });

    public static Optional<CGunInput> capture(
            Player player, InteractionHand hand, String inputType) {
        var id = GunIdentity.id(player.getItemInHand(hand));
        return GunIdentity.INVALID.equals(id)
                ? Optional.empty()
                : Optional.of(
                        new CGunInput(
                                inputType,
                                true,
                                hand == InteractionHand.OFF_HAND
                                        ? 40
                                        : player.getInventory().selected,
                                id));
    }

    public CGunInput predicted(GunPredictionClaim claim) {
        return new CGunInput(
                inputType, pressed, inventorySlot, gunId, claim, authored, controlFlags);
    }

    public CGunInput authored(int tick, long first, int count) {
        return new CGunInput(
                inputType,
                pressed,
                inventorySlot,
                gunId,
                prediction,
                new GunAuthoredInput(tick, first, count),
                controlFlags);
    }

    public CGunInput controlled() {
        return new CGunInput(
                inputType, pressed, inventorySlot, gunId, prediction, authored, controlFlags | 2);
    }

    public CGunInput heldUpdate() {
        return new CGunInput(inputType, true, inventorySlot, gunId, prediction, authored, 3);
    }

    public boolean isHeldUpdate() {
        return (controlFlags & 1) != 0;
    }

    public boolean clientControlled() {
        return (controlFlags & 2) != 0;
    }

    public CGunInput released() {
        return new CGunInput(
                inputType, false, inventorySlot, gunId, prediction, authored, controlFlags & 2);
    }

    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
