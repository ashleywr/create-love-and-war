package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;

import java.util.UUID;

/** Presentation ownership only; this does not authorize gameplay or supply damage/shot results. */
public record GunPredictionClaim(UUID activation, String group, GunActiveGroups.Origin origin) {
    public static final GunPredictionClaim NONE =
            new GunPredictionClaim(GunIdentity.INVALID, "", new GunActiveGroups.Origin("", -1, ""));

    public boolean present() {
        return !activation.equals(GunIdentity.INVALID);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(present());
        if (present()) {
            buf.writeUUID(activation);
            buf.writeUtf(group, 256);
            buf.writeUtf(origin.attachmentType(), 64);
            buf.writeVarInt(origin.attachmentIndex());
            buf.writeUtf(origin.definitionKey(), 256);
        }
    }

    public static GunPredictionClaim read(RegistryFriendlyByteBuf buf) {
        return !buf.readBoolean()
                ? NONE
                : new GunPredictionClaim(
                        buf.readUUID(),
                        buf.readUtf(256),
                        new GunActiveGroups.Origin(
                                buf.readUtf(64), buf.readVarInt(), buf.readUtf(256)));
    }
}
