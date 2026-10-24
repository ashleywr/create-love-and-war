package net.pttheta.loveandwar.gun.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Initial triggers are atomic with the validated input, including legitimate zero-trigger
 * retriggers.
 */
public record GunAuthoredInput(int tick, long firstTrigger, int count) {
    public static final GunAuthoredInput NONE = new GunAuthoredInput(-1, 0, 0);

    public boolean present() {
        return tick >= 0;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(present());
        if (present()) {
            buf.writeVarInt(tick);
            buf.writeVarLong(firstTrigger);
            buf.writeVarInt(count);
        }
    }

    public static GunAuthoredInput read(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) return NONE;
        var value = new GunAuthoredInput(buf.readVarInt(), buf.readVarLong(), buf.readVarInt());
        if (value.tick() < 0 || value.firstTrigger() < 0 || value.count() < 0 || value.count() > 64)
            throw new io.netty.handler.codec.DecoderException("Invalid authored gun input");
        return value;
    }
}
