package net.pttheta.loveandwar.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.gun.content.BulletDef;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.stat.Modifier;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.runtime.GunTargeting;
import net.pttheta.loveandwar.gun.runtime.GunEngine;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class ProjectileContextGameTests {
    private static final ResourceLocation BULLET = ResourceLocation.parse("createloveandwar:gyrojet_basic");

    private static FakePlayer shooter(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ContextTest"));
        var pos = helper.absoluteVec(new Vec3(2.5, 6.5, 2.5));
        player.setPos(pos); player.setYRot(0); player.setYHeadRot(0); player.setXRot(0);
        return player;
    }

    private static BulletDef.ProjectileDef definition(String json) {
        return BulletDef.ProjectileDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static GunProjectile projectile(GameTestHelper helper, FakePlayer owner, BulletDef.ProjectileDef def,
                                            List<ModifierSource> modifiers, Entity target, int progress) {
        var projectile = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
        projectile.setPos(helper.absoluteVec(new Vec3(2.5, 6.5, 2.5)));
        projectile.initialize(owner, BULLET, def, modifiers, "primary_fire", target, progress);
        projectile.setDeltaMovement(0, 0, 1);
        helper.getLevel().addFreshEntity(projectile);
        return projectile;
    }

    private static Pig target(GameTestHelper helper, Vec3 relative) {
        var pig = helper.spawn(EntityType.PIG, relative);
        pig.setNoAi(true); pig.setNoGravity(true);
        return pig;
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void bulletIdentityAndAuthoredCatalogAreRetained(GameTestHelper helper) {
        String[] names = {"artillery_shell", "artillery_shell_ap", "artillery_shell_he", "black_powder_cartridge", "grenade_round_he",
                "gyrojet_basic", "gyrojet_he", "gyrojet_heatseeker", "gyrojet_heatseeker_top", "gyrojet_pen"};
        double[] speeds = {3.2, 3.2, 3.2, 4.05, 1.8, 0.05, 0.05, 0.45, 0.45, 0.05};
        var owner = shooter(helper);
        for (int index = 0; index < names.length; index++) {
            String name = names[index];
            var bullet = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
            bullet.setPos(owner.getEyePosition());
            bullet.initialize(owner, ResourceLocation.parse("createloveandwar:" + name), GunContent.bullet(name).orElseThrow().projectiles().getFirst(),
                    List.of(), "primary_fire", null, 0);
            bullet.launch(new Vec3(0, 0, 1));
            helper.assertTrue(Math.abs(bullet.getDeltaMovement().length() - speeds[index]) < 0.00001, name + " launch magnitude");
            helper.assertTrue(bullet.bulletId().getPath().equals(name), "Projectile must retain its authored model identity");
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(bullet.getType()).equals(ResourceLocation.parse("flansmod:bullet")), "Original bullet entity ID");
            helper.assertTrue(bullet.getBbWidth() == 0.5F && ModEntities.SHOT_PROJECTILE.get().getWidth() == 0.25F,
                    "Gun bullet bounds must not change Robotdog's projectile");
            bullet.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void blockResponsesBounceStickPassAndDetonate(GameTestHelper helper) {
        var owner = shooter(helper);
        var wall = new BlockPos(2, 6, 3);
        helper.setBlock(wall, Blocks.STONE);
        try {
            for (String response : List.of("Bounce", "Stick", "PassThrough", "Detonate")) {
                var def = definition("{\"gravityFactor\":0,\"dragInAir\":0,\"responseToBlock\":\"" + response + "\"}");
                var bullet = projectile(helper, owner, def, List.of(), null, 0);
                try {
                    bullet.tick();
                    switch (response) {
                        case "Bounce" -> helper.assertTrue(bullet.getDeltaMovement().z == -1 && !bullet.isRemoved(), "Block-face bounce reflects velocity");
                        case "Stick" -> helper.assertTrue(bullet.stuck() && bullet.getDeltaMovement().equals(Vec3.ZERO), "Stick stops and persists");
                        case "PassThrough" -> helper.assertTrue(bullet.getDeltaMovement().z == 1 && !bullet.isRemoved(), "PassThrough retains velocity");
                        case "Detonate" -> helper.assertTrue(bullet.isRemoved(), "Detonate removes the server bullet");
                    }
                } finally { bullet.discard(); }
            }
        } finally { helper.setBlock(wall, Blocks.AIR); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void fuseAccountsForActionProgressAndExpiresOnce(GameTestHelper helper) {
        var bullet = projectile(helper, shooter(helper), definition("{\"gravityFactor\":0,\"dragInAir\":0,\"fuseTime\":0.2}"), List.of(), null, 2);
        try {
            bullet.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(bullet.fuseRemaining() == 2, "Four-tick fuse subtracts two ticks of action progress");
            bullet.tick();
            helper.assertTrue(!bullet.isRemoved() && bullet.fuseRemaining() == 1, "First fuse tick remains active");
            bullet.tick();
            helper.assertTrue(bullet.isRemoved(), "Second fuse tick detonates");
        } finally { bullet.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void saveReloadKeepsFuseMotionDefinitionAndLock(GameTestHelper helper) {
        var owner = shooter(helper);
        var pig = target(helper, new Vec3(8, 6.5, 8));
        var bullet = projectile(helper, owner, GunContent.bullet("gyrojet_pen").orElseThrow().projectiles().getFirst(), List.of(), pig, 0);
        var loaded = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
        try {
            bullet.setNoGravity(true); bullet.setDeltaMovement(Vec3.ZERO);
            for (int tick = 0; tick < 10; tick++) bullet.tick();
            var saved = new CompoundTag(); bullet.saveWithoutId(saved); loaded.load(saved);
            helper.assertTrue(loaded.fuseRemaining() == 7990 && loaded.lifeTime() == 10, "Reload must not reset the authored 400-second fuse or age");
            helper.assertTrue(loaded.definition().equals(bullet.definition()) && loaded.bulletId().equals(bullet.bulletId()), "Reload retains definition/model identity");
            helper.assertTrue(loaded.getDeltaMovement().lengthSqr() < 0.0000001 && loaded.lockId() == pig.getId(),
                    "Motion and target UUID resolve after reload; motion=" + loaded.getDeltaMovement() + " lock=" + loaded.lockId() + " target=" + pig.getId());
            loaded.tick();
            helper.assertTrue(loaded.fuseRemaining() == 7989, "Loaded fuse continues counting down");
        } finally { bullet.discard(); loaded.discard(); pig.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void savedProjectileRetainsScopedImpactModifiers(GameTestHelper helper) {
        var owner = shooter(helper);
        var modifier = Modifier.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"stat":"impact_damage","matchGroupPaths":["primary_fire"],"accumulators":[{"operation":"BaseAdd","value":3}]}
                """)).getOrThrow();
        var def = definition("""
                {"impacts":[{"targetType":"ShotEntity","impactEffects":[{"effectType":"ApplyDamage","modifiers":[
                  {"stat":"impact_damage","matchGroupPaths":["other_group"],"accumulators":[{"operation":"BaseAdd","value":2}]}]}]}]}
                """);
        var bullet = projectile(helper, owner, def, List.of(ModifierSource.of("test", List.of(modifier))), null, 0);
        var loaded = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
        var pig = target(helper, new Vec3(8, 6.5, 8));
        try {
            var saved = new CompoundTag(); bullet.saveWithoutId(saved); loaded.load(saved); loaded.setOwner(owner);
            float initial = pig.getHealth();
            loaded.detonate(new EntityHitResult(pig, pig.position().add(0, 0.01, 0)));
            helper.assertTrue(Math.abs(pig.getHealth() - (initial - 5)) < 0.00001, "Saved projectile must retain scoped damage modifiers");
            loaded.detonate(new EntityHitResult(pig, pig.position()));
            helper.assertTrue(Math.abs(pig.getHealth() - (initial - 5)) < 0.00001, "Repeated detonation must not apply impacts twice");
        } finally { bullet.discard(); loaded.discard(); pig.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void guidanceTracksWithinConeAndTopAttackClimbs(GameTestHelper helper) {
        var owner = shooter(helper);
        var pig = target(helper, new Vec3(18, 6.5, 18));
        try {
            for (String type : List.of("lock_on", "lock_on_top")) {
                var bullet = projectile(helper, owner, definition("{\"guidanceType\":\"" + type + "\",\"gravityFactor\":0,\"dragInAir\":0,\"trackCone\":5,\"turnRate\":0.1,\"responseToBlock\":\"PassThrough\"}"), List.of(), pig, 0);
                try {
                    bullet.tick();
                    if (type.equals("lock_on")) helper.assertTrue(bullet.getDeltaMovement().distanceTo(new Vec3(0, 0, 1)) < 0.00001,
                            "Simple lock does not track outside its cone; got " + bullet.getDeltaMovement());
                    else helper.assertTrue(bullet.getDeltaMovement().y > 0 && bullet.getDeltaMovement().x > 0,
                            "Original top attack climbs toward the elevated waypoint without a track-cone check");
                } finally { bullet.discard(); }
            }
        } finally { pig.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void lockAcquisitionUsesOriginalTickThresholdAndRetention(GameTestHelper helper) {
        var owner = shooter(helper);
        var pig = target(helper, new Vec3(2.5, 6.5, 10.5));
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:chassepot")));
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:gyrojet_heatseeker"))))));
        owner.setItemInHand(InteractionHand.MAIN_HAND, gun);
        var wall = new BlockPos(2, 7, 6);
        try {
            for (int tick = 0; tick < 20; tick++) GunTargeting.tick(owner, gun);
            helper.assertTrue(GunTargeting.target(owner, gun) == null, "Default lockTime 20 uses strict greater-than");
            GunTargeting.tick(owner, gun);
            helper.assertTrue(GunTargeting.target(owner, gun) == pig, "Target locks on tick 21");
            helper.setBlock(wall, Blocks.STONE);
            GunTargeting.tick(owner, gun);
            helper.assertTrue(GunTargeting.target(owner, gun) == pig, "Original retained lock does not recheck line of sight");
            owner.setYRot(180); owner.setYHeadRot(180);
            GunTargeting.tick(owner, gun);
            helper.assertTrue(GunTargeting.target(owner, gun) == null, "Leaving the cone clears the lock");
        } finally { pig.discard(); helper.setBlock(wall, Blocks.AIR); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void flyingLockTiesPreferOriginalExpandedClassOrder(GameTestHelper helper) {
        // ProjectileDefinition.Targets: FlyingMob, EnderDragon, WitherBoss, Blaze.
        // Spawn the lower-priority Blaze first so an unordered union selects the wrong target.
        assertFlyingLockTie(helper, EntityType.GHAST);
        assertFlyingLockTie(helper, EntityType.WITHER);
        helper.succeed();
    }

    private static void assertFlyingLockTie(GameTestHelper helper, EntityType<? extends net.minecraft.world.entity.Mob> preferredType) {
        var owner = shooter(helper);
        var first = helper.spawn(EntityType.BLAZE, new Vec3(2.75, 6.5, 10.5));
        var preferred = helper.spawn(preferredType, new Vec3(2.25, 6.5, 10.5));
        first.setNoAi(true); first.setNoGravity(true);
        preferred.setNoAi(true); preferred.setNoGravity(true);
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:chassepot")));
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:gyrojet_heatseeker"))))));
        owner.setItemInHand(InteractionHand.MAIN_HAND, gun);
        try {
            helper.assertTrue(owner.distanceTo(first) == owner.distanceTo(preferred), "Fixture targets must be exactly equidistant");
            var candidates = helper.getLevel().getEntities(owner, first.getBoundingBox().minmax(preferred.getBoundingBox()).inflate(1));
            helper.assertTrue(candidates.indexOf(first) >= 0 && candidates.indexOf(first) < candidates.indexOf(preferred),
                    "Fixture must present the lower-priority Blaze first in the world query");
            for (int tick = 0; tick < 21; tick++) GunTargeting.tick(owner, gun);
            helper.assertTrue(GunTargeting.target(owner, gun) == preferred,
                    "Original expanded flying classes prioritize " + preferred.getType() + " over an equidistant Blaze");
        } finally { first.discard(); preferred.discard(); }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void burstProjectilesSubtractCurrentCycleProgressFromFuse(GameTestHelper helper) {
        var owner = shooter(helper);
        owner.setXRot(-90);
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:g11k2")));
        var round = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:gyrojet_pen")));
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(round.copy(), round.copy(), round.copy())));
        owner.setItemInHand(InteractionHand.MAIN_HAND, gun);
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Fire1", true);
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Fire1", false);
        for (int tick = 1; tick <= 2; tick++) {
            final int progress = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(owner, gun);
                var bullets = helper.getLevel().getEntitiesOfClass(GunProjectile.class, owner.getBoundingBox().inflate(32), bullet -> bullet.getOwner() == owner);
                try {
                    var latest = bullets.stream().max(java.util.Comparator.comparingInt(Entity::getId)).orElseThrow();
                    helper.assertTrue(latest.fuseRemaining() == 8000 - progress,
                            "New burst projectile must use current group progress, not last tick's synchronized item state; got " + latest.fuseRemaining());
                    if (progress == 2) helper.succeed();
                } finally { if (progress == 2) bullets.forEach(GunProjectile::discard); }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void fuseMissDoesNotRunBlockOnlyImpacts(GameTestHelper helper) {
        var owner = shooter(helper);
        var bullet = projectile(helper, owner, definition("""
                {"splashRadius":3,"impacts":[{"targetType":"ShotBlock","impactEffects":[{"effectType":"ApplyDamage","modifiers":[
                {"stat":"impact_damage","accumulators":[{"operation":"BaseAdd","value":3}]}]}]}]}
                """), List.of(), null, 0);
        var pig = target(helper, new Vec3(3.5, 6.5, 2.5));
        try {
            float health = pig.getHealth();
            bullet.detonate(BlockHitResult.miss(bullet.position(), Direction.DOWN, bullet.blockPosition()));
            helper.assertTrue(pig.getHealth() == health, "A fuse MISS must not dispatch ShotBlock effects");
        } finally { bullet.discard(); pig.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void projectileImpactsStayInTheirDimensionAfterOwnerTravel(GameTestHelper helper) {
        var otherLevel = helper.getLevel().getServer().getLevel(Level.NETHER);
        var owner = new FakePlayer(otherLevel, new GameProfile(UUID.randomUUID(), "TravelTest"));
        var bullet = projectile(helper, owner, definition("""
                {"splashRadius":3,"impacts":[{"targetType":"ShotPosition","impactEffects":[{"effectType":"ApplyDamage","modifiers":[
                {"stat":"impact_damage","accumulators":[{"operation":"BaseAdd","value":3}]}]}]}]}
                """), List.of(), null, 0);
        var pig = target(helper, new Vec3(3.5, 6.5, 2.5));
        try {
            float health = pig.getHealth();
            bullet.detonate(BlockHitResult.miss(bullet.position(), Direction.DOWN, bullet.blockPosition()));
            helper.assertTrue(pig.getHealth() < health, "Projectile-world effects must not move to the owner's new dimension");
        } finally { bullet.discard(); pig.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void compactTrackingRetainsVisualPhysicsAndFullServerSave(GameTestHelper helper) {
        var owner = shooter(helper);
        var authored = GunContent.bullet("gyrojet_heatseeker_top").orElseThrow().projectiles().getFirst();
        var gun = GunContent.gun("g11k2").orElseThrow();
        var group = gun.actionGroups().getFirst();
        var sources = net.pttheta.loveandwar.gun.runtime.StatSources.forFiring(gun, group, net.pttheta.loveandwar.gun.runtime.GunAttachments.EMPTY);
        var bullet = projectile(helper, owner, authored, sources, null, 0);
        var receiver = new GunProjectile(ModEntities.BULLET.get(), helper.getLevel());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var packet = new ClientboundSetEntityDataPacket(bullet.getId(), bullet.getEntityData().getNonDefaultValues());
            ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
            receiver.getEntityData().assignValues(decoded.packedItems());
            helper.assertTrue(receiver.definition().motion().equals(bullet.definition().motion())
                    && receiver.definition().behavior().equals(bullet.definition().behavior())
                    && receiver.definition().particles().equals(bullet.definition().particles())
                    && receiver.bulletId().equals(bullet.bulletId()), "Tracking must preserve every client physics, response and visual input");
            helper.assertTrue(receiver.definition().core().impacts().isEmpty(), "Server impact definitions are unnecessary in tracking metadata");
            for (var value : decoded.packedItems()) if (value.value() instanceof CompoundTag payload && payload.contains("Bullet")) {
                helper.assertTrue(!payload.contains("Modifiers") && !payload.contains("AttachmentCount"), "Server calculation inputs must not be sent to observers");
            }
            var saved = new CompoundTag(); bullet.saveWithoutId(saved);
            helper.assertTrue(!saved.getCompound("GunContext").getCompound("Definition").getList("impacts", 10).isEmpty()
                    && saved.getCompound("GunContext").contains("Modifiers"), "Disk context retains full authoritative impacts and modifiers");
            saved.getCompound("GunContext").getCompound("Definition").remove("impacts");
            var secondSave = new CompoundTag(); bullet.saveWithoutId(secondSave);
            helper.assertTrue(!secondSave.getCompound("GunContext").getCompound("Definition").getList("impacts", 10).isEmpty(),
                    "Mutating an emitted save must not corrupt the cached authoritative context");
        } finally { buffer.release(); bullet.discard(); receiver.discard(); }
        helper.succeed();
    }
}
