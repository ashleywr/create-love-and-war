package net.pttheta.loveandwar.gametest;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.StatSources;

/** Opt-in component benchmark in a real NeoForge server; never included in normal test counts or artifacts. */
@GameTestHolder("createloveandwar")
public final class ProjectileSetupProfile {
    private record Definition(ResourceLocation id, BulletDef.ProjectileDef projectile) {}
    private static final int WARMUP = 10000, BATCHES = 5, PER_BATCH = 10000;

    @GameTestGenerator public static List<TestFunction> profiles() {
        if (System.getProperty("law.projectileProfileDirectory", "").isBlank()) return List.of();
        return List.of(new TestFunction("law_profile", "createloveandwar.projectile_setup_profile", "createloveandwar:projectile_arena",
                100, 0, true, ProjectileSetupProfile::profile));
    }

    private static GunProjectile create(GameTestHelper helper, FakePlayer owner, Definition def, List<ModifierSource> sources) {
        var bullet = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
        bullet.setPos(owner.getEyePosition());
        bullet.initialize(owner, def.id(), def.projectile(), sources, "primary_fire", null, 0);
        bullet.launch(new Vec3(0, 0, 1));
        return bullet;
    }

    private static long batch(GameTestHelper helper, FakePlayer owner, List<Definition> defs, List<ModifierSource> sources, int count) {
        long checksum = 0;
        for (int index = 0; index < count; index++) {
            var bullet = create(helper, owner, defs.get(index % defs.size()), sources);
            checksum += Double.doubleToLongBits(bullet.getDeltaMovement().length()) ^ bullet.fuseRemaining();
            bullet.discard();
        }
        return checksum;
    }

    private static void profile(GameTestHelper helper) {
        try {
            var directory = Path.of(System.getProperty("law.projectileProfileDirectory"));
            Files.createDirectories(directory);
            var owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ProfileTest"));
            owner.setPos(helper.absoluteVec(new Vec3(2, 6, 2)));
            var defs = new ArrayList<Definition>();
            GunContent.bullets().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                for (var projectile : entry.getValue().projectiles()) defs.add(new Definition(ResourceLocation.parse("createloveandwar:" + entry.getKey()), projectile));
            });
            var gun = GunContent.gun("g11k2").orElseThrow();
            var group = gun.actionGroups().stream().filter(value -> value.key().equals("primary_fire")).findFirst().orElseThrow();
            var sources = StatSources.forFiring(gun, group, GunAttachments.EMPTY);
            var packetBytes = new java.util.LinkedHashMap<String, Integer>();
            for (var def : defs) {
                var bullet = create(helper, owner, def, sources);
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
                try {
                    ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,
                            new ClientboundSetEntityDataPacket(bullet.getId(), bullet.getEntityData().getNonDefaultValues()));
                    packetBytes.put(def.id().toString(), buffer.readableBytes());
                } finally { buffer.release(); bullet.discard(); }
            }
            long checksum = batch(helper, owner, defs, sources, WARMUP);
            var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
            helper.assertTrue(bean.isThreadAllocatedMemorySupported() && bean.isCurrentThreadCpuTimeSupported(), "Benchmark requires JVM allocation/CPU counters");
            bean.setThreadAllocatedMemoryEnabled(true); bean.setThreadCpuTimeEnabled(true);
            long thread = Thread.currentThread().threadId();
            var results = new ArrayList<Map<String, Long>>();
            try (var recording = new Recording(Configuration.getConfiguration("profile"))) {
                recording.start();
                for (int index = 0; index < BATCHES; index++) {
                    long allocation = bean.getThreadAllocatedBytes(thread), cpu = bean.getCurrentThreadCpuTime(), start = System.nanoTime();
                    checksum = checksum * 31 + batch(helper, owner, defs, sources, PER_BATCH);
                    long wall = System.nanoTime() - start;
                    results.add(Map.of("wallNanos", wall, "cpuNanos", bean.getCurrentThreadCpuTime() - cpu,
                            "allocatedBytes", bean.getThreadAllocatedBytes(thread) - allocation));
                }
                recording.stop(); recording.dump(directory.resolve("setup.jfr"));
            }
            var output = Map.of("scope", "Projectile construction, parameter resolution, context serialization/cache and launch; no entity insertion, tracking observers or world ticking",
                    "warmup", WARMUP, "batches", BATCHES, "perBatch", PER_BATCH, "definitions", defs.size(),
                    "samples", results, "metadataPacketBytes", packetBytes, "checksum", checksum);
            Files.writeString(directory.resolve("setup.json"), new GsonBuilder().setPrettyPrinting().create().toJson(output));
            System.out.println("LAW_PROJECTILE_PROFILE path=" + directory + " definitions=" + defs.size() + " checksum=" + checksum);
            helper.succeed();
        } catch (Exception error) { throw new RuntimeException("Projectile setup profile failed", error); }
    }
}
