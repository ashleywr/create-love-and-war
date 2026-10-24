package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;

import java.util.UUID;

/**
 * Retained server activation/callback identity plus compact authoritative ammunition
 * acknowledgement.
 */
public record GunShotReceipt(
        UUID gun,
        UUID activation,
        int action,
        long sequence,
        UUID prediction,
        int consumed,
        long ammoRevision,
        int remainingRounds,
        boolean clientOwned) {
    public static final GunShotReceipt NONE =
            new GunShotReceipt(
                    GunIdentity.INVALID,
                    GunIdentity.INVALID,
                    0,
                    0,
                    GunIdentity.INVALID,
                    0,
                    0,
                    0,
                    false);

    public boolean present() {
        return !gun.equals(GunIdentity.INVALID);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(present());
        if (present()) {
            buf.writeUUID(gun);
            buf.writeUUID(activation);
            buf.writeVarInt(action);
            buf.writeVarLong(sequence);
            buf.writeUUID(prediction);
            buf.writeVarInt(consumed);
            buf.writeVarLong(ammoRevision);
            buf.writeVarInt(remainingRounds);
            buf.writeBoolean(clientOwned);
        }
    }

    public static GunShotReceipt read(RegistryFriendlyByteBuf buf) {
        return !buf.readBoolean()
                ? NONE
                : new GunShotReceipt(
                        buf.readUUID(),
                        buf.readUUID(),
                        buf.readVarInt(),
                        buf.readVarLong(),
                        buf.readUUID(),
                        buf.readVarInt(),
                        buf.readVarLong(),
                        buf.readVarInt(),
                        buf.readBoolean());
    }
}
