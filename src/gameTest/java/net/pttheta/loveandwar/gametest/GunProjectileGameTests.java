package net.pttheta.loveandwar.gametest;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.runtime.GunBallistics;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunProjectileGameTests {
    private static Projectile fire(GameTestHelper helper, String bullet) {
        var level = helper.getLevel();
        var shooter = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ProjectileTest"));
        var pos = helper.absolutePos(new BlockPos(2, 5, 2));
        shooter.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        shooter.setYRot(0); shooter.setXRot(0);
        GunBallistics.fire(shooter, GunContent.bullet(bullet).orElseThrow(), List.of());
        var projectiles = level.getEntitiesOfClass(Projectile.class, shooter.getBoundingBox().inflate(2));
        helper.assertTrue(projectiles.size() == 1, "One authored projectile must spawn; got " + projectiles.size());
        return projectiles.getFirst();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void gyrojetRetainsOriginalLaunchTrajectory(GameTestHelper helper) {
        var projectile = fire(helper, "gyrojet_basic");
        try {
            helper.assertTrue(Math.abs(projectile.getDeltaMovement().length() - 0.05) < 0.00001,
                    "Original ShootAction scales launch speed twice: launchSpeed 1 produces 0.05 blocks/tick; got " + projectile.getDeltaMovement());
        } finally { projectile.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void blackPowderProjectileAppliesAuthoredGravity(GameTestHelper helper) {
        var projectile = fire(helper, "black_powder_cartridge");
        try {
            projectile.tick();
            helper.assertTrue(Math.abs(projectile.getDeltaMovement().y + 0.002) < 0.00001,
                    "Authored black-powder gravity factor 0.1 subtracts 0.002 each tick; got " + projectile.getDeltaMovement());
        } finally { projectile.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void gyrojetAcceleratesEachTick(GameTestHelper helper) {
        var projectile = fire(helper, "gyrojet_basic");
        try {
            projectile.tick();
            helper.assertTrue(Math.abs(projectile.getDeltaMovement().horizontalDistance() - 1.05) < 0.00001,
                    "Original acceleration 20 adds 1 block/tick to the initial 0.05; got " + projectile.getDeltaMovement());
        } finally { projectile.discard(); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 120)
    public static void longFuseDoesNotInheritRobotdogLifetime(GameTestHelper helper) {
        var projectile = fire(helper, "gyrojet_pen");
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(Vec3.ZERO);
        helper.runAfterDelay(82, () -> {
            try { helper.assertTrue(!projectile.isRemoved(), "400-second penetrating Gyrojet fuse must survive the Robotdog projectile's arbitrary 80-tick lifetime"); }
            finally { projectile.discard(); }
            helper.succeed();
        });
    }
}
