package net.pttheta.loveandwar.gun.runtime;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.SoundDef;
import net.pttheta.loveandwar.gun.network.SGunSound;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.item.ModDataComponents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunAudioGameTests {
    static final class Listener implements AutoCloseable {
        final FakePlayer player;
        final List<Packet<?>> packets = new ArrayList<>();
        final List<ServerPlayer> players;

        @SuppressWarnings("unchecked")
        Listener(GameTestHelper helper) throws ReflectiveOperationException {
            this(helper, 0);
        }

        @SuppressWarnings("unchecked")
        Listener(GameTestHelper helper, double offset) throws ReflectiveOperationException {
            var level = helper.getLevel();
            this.player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "SoundTest"));
            var pos = helper.absolutePos(new BlockPos(2, 3, 2));
            this.player.setPos(pos.getX() + 0.5 + offset, pos.getY(), pos.getZ() + 0.5);
            this.player.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), this.player,
                    CommonListenerCookie.createInitial(this.player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { packets.add(packet); }
                // This in-process packet observer has no negotiated Netty channel (including time-sync probes).
                @Override public boolean hasChannel(ResourceLocation id) { return false; }
            };
            // A scoped observer in the already-loaded server, not a replacement registry/bootstrap.
            // PlayerList exposes an unmodifiable view, so the fixture temporarily accesses its backing list.
            var field = PlayerList.class.getDeclaredField("players");
            field.setAccessible(true);
            this.players = (List<ServerPlayer>)field.get(level.getServer().getPlayerList());
            this.players.add(this.player);
        }

        @Override public void close() { this.players.remove(this.player); }
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void audioUsesActualAttachmentInput(GameTestHelper helper) throws ReflectiveOperationException {
        try (var listener = new Listener(helper)) {
            var gun = GunContent.gun("g11k2").orElseThrow();
            var group = gun.actionGroups().stream().filter(g -> g.key().equals("primary_fire")).findFirst().orElseThrow();
            var action = group.actions().stream().filter(a -> a.actionType().equals("PlaySound")).findFirst().orElseThrow();
            var modifier = net.pttheta.loveandwar.gun.content.stat.Modifier.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString("{\"stat\":\"loudness\",\"accumulators\":[{\"operation\":\"BaseAdd\",\"value\":2,\"multiplyPer\":[\"PerAttachment\"]}]}" )).getOrThrow();
            for (int count : new int[]{0, 2}) {
                listener.packets.clear();
                var attachments = count == 0 ? GunAttachments.EMPTY : GunAttachments.EMPTY.with("generic", List.of(new ItemStack(net.minecraft.world.item.Items.APPLE), new ItemStack(net.minecraft.world.item.Items.STICK)));
                var sources = new ArrayList<>(StatSources.forFiring(gun, group, attachments));
                sources.add(net.pttheta.loveandwar.gun.content.stat.ModifierSource.of("test", List.of(modifier)));
                GunSounds.send(listener.player, group, action, sources);
                var packet = (SGunSound)((ClientboundCustomPayloadPacket)listener.packets.getFirst()).payload();
                helper.assertTrue(packet.volumeMultiplier() == 1 + 2 * count, "Playback loudness must use attachment input " + count);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void bundledSoundTransmitsAuthoredDefinitionAndOrigin(GameTestHelper helper) throws ReflectiveOperationException {
        try (var listener = new Listener(helper)) {
            var gun = GunContent.gun("g11k2").orElseThrow();
            var group = gun.actionGroups().stream().filter(g -> g.key().equals("primary_fire")).findFirst().orElseThrow();
            GunEngine.triggerSounds(listener.player, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:g11k2"))), gun, group);
            helper.assertTrue(listener.packets.size() == 1,
                    "A bundled sound must transmit one playback request; got " + listener.packets.size() + " packets");
            helper.assertTrue(listener.packets.getFirst() instanceof ClientboundCustomPayloadPacket packet && packet.payload() instanceof SGunSound,
                    "Client must receive a sound definition to resolve camera-specific playback");
            var request = (SGunSound)((ClientboundCustomPayloadPacket)listener.packets.getFirst()).payload();
            helper.assertTrue(request.sound().sound().equals("flansmod:gunshot_slam") && request.sound().maxRange() == 100,
                    "Playback request must retain the authored sound and range");
            helper.assertTrue(request.position().equals(listener.player.getEyePosition()), "Sound originates at the shooter's eye");
            helper.assertTrue(request.volumeMultiplier() == 1 && request.pitchMultiplier() == 1, "Bare gun has neutral sound multipliers");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void silencerModifiesPlaybackVolumeAndPitch(GameTestHelper helper) throws ReflectiveOperationException {
        try (var listener = new Listener(helper)) {
            var gun = GunContent.gun("g11k2").orElseThrow();
            var group = gun.actionGroups().stream().filter(g -> g.key().equals("primary_fire")).findFirst().orElseThrow();
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:g11k2")));
            var silencer = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:attachment_silencer")));
            stack.set(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY.with("barrel", List.of(silencer)));
            GunEngine.triggerSounds(listener.player, stack, gun, group);
            helper.assertTrue(listener.packets.size() == 1, "Silenced gun transmits exactly one sound request");
            var request = (SGunSound)((ClientboundCustomPayloadPacket)listener.packets.getFirst()).payload();
            helper.assertTrue(request.volumeMultiplier() == 0.25F, "Original silencer scales loudness to 25 percent");
            helper.assertTrue(request.pitchMultiplier() == 4, "Original silencer adds 300 percent pitch");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void soundRequestRoundTripsDistanceVariantsAndMultipliers(GameTestHelper helper) {
        var sound = new SoundDef("test:base", 2, 0.8F, 1.2F, 0.2F, 1, 100,
                List.of(new SoundDef.Lod("test:far", 50), new SoundDef.Lod("test:near", 10)));
        var original = new SGunSound(sound, new Vec3(1.25, -8, 70.5), 0.25F, 4);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            SGunSound.STREAM_CODEC.encode(buffer, original);
            helper.assertTrue(original.equals(SGunSound.STREAM_CODEC.decode(buffer)), "Packet must preserve every sound field and authored LOD order");
            helper.assertTrue(buffer.readableBytes() == 0, "Packet decoder must consume the complete request");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void playbackRequestsUseOriginalActionPropagationRadius(GameTestHelper helper) throws ReflectiveOperationException {
        try (var shooter = new Listener(helper); var near = new Listener(helper, 199); var far = new Listener(helper, 201)) {
            var gun = GunContent.gun("g11k2").orElseThrow();
            var group = gun.actionGroups().stream().filter(g -> g.key().equals("primary_fire")).findFirst().orElseThrow();
            GunEngine.triggerSounds(shooter.player,
                    new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:g11k2"))), gun, group);
            helper.assertTrue(shooter.packets.size() == 1 && near.packets.size() == 1 && far.packets.isEmpty(),
                    "PlaySound actions propagate within 200 blocks; this does not establish audible attenuation distance");
        }
        helper.succeed();
    }
}
