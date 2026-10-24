package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Network-only view of server-owned instances, in original activation order. Never a saved action stack. */
public record GunActiveGroups(List<Entry> entries) {
    public GunActiveGroups { entries = List.copyOf(entries); }
    public static final GunActiveGroups EMPTY = new GunActiveGroups(List.of());

    public record Origin(String attachmentType, int attachmentIndex, String definitionKey) {
        public static Origin base(ItemStack gun) { return new Origin("", -1, GunLookup.contentKey(gun)); }
        public Optional<ActionGroup> resolve(String key) {
            return attachmentType.isEmpty()
                    ? GunContent.gun(definitionKey).flatMap(def -> def.actionGroups().stream().filter(group -> group.key().equals(key)).findFirst())
                    : GunContent.attachment(definitionKey).flatMap(def -> def.actionOverrides().stream().filter(group -> group.key().equals(key)).findFirst());
        }
    }

    public record Entry(String key, Origin origin, UUID activation, long startedAt, UUID owner, long scopeTrigger) {
        public Entry(String key,Origin origin,UUID activation,long startedAt,UUID owner){this(key,origin,activation,startedAt,owner,0);}
        public Optional<ActionGroup> resolve() { return origin.resolve(key); }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.of((buf, entry) -> {
        buf.writeUtf(entry.key());
        buf.writeUtf(entry.origin().attachmentType());
        buf.writeVarInt(entry.origin().attachmentIndex());
        buf.writeUtf(entry.origin().definitionKey());
        buf.writeUUID(entry.activation());
        buf.writeLong(entry.startedAt());
        buf.writeUUID(entry.owner());
        buf.writeVarLong(entry.scopeTrigger());
    }, buf -> new Entry(buf.readUtf(), new Origin(buf.readUtf(), buf.readVarInt(), buf.readUtf()), buf.readUUID(), buf.readLong(), buf.readUUID(), buf.readVarLong()));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunActiveGroups> STREAM_CODEC = ENTRY_CODEC
            .apply(ByteBufCodecs.list()).map(GunActiveGroups::new, GunActiveGroups::entries);

    public static GunActiveGroups of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUN_ACTIVE_GROUPS, EMPTY);
    }

    public Optional<Entry> find(String key) { return entries.stream().filter(entry -> entry.key().equals(key)).findFirst(); }
}
