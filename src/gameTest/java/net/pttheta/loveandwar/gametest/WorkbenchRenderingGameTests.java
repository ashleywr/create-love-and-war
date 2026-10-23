package net.pttheta.loveandwar.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.WorkbenchBlock;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class WorkbenchRenderingGameTests {
    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void benchesKeepSurroundingTerrainVisible(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = helper.absolutePos(new BlockPos(2, 2, 2));
        int cases = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof WorkbenchBlock)) continue;
            for (var direction : Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState().setValue(WorkbenchBlock.FACING, direction);
                level.setBlockAndUpdate(position, state);
                var floorPosition = position.below();
                level.setBlockAndUpdate(floorPosition, Blocks.STONE.defaultBlockState());
                helper.assertTrue(Block.shouldRenderFace(Blocks.STONE.defaultBlockState(), level,
                        floorPosition, Direction.UP, position),
                        BuiltInRegistries.BLOCK.getKey(block) + " hides the floor when facing " + direction);
                cases++;
            }
        }
        helper.assertTrue(cases == 24, "Expected six workbenches in four directions; got " + cases);
        System.out.println("LAW_BENCH_TERRAIN_VISIBILITY cases=" + cases + " passed=true");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void benchesRetainTheOfficialInsetShape(GameTestHelper helper) {
        var position = helper.absolutePos(new BlockPos(2, 2, 2));
        int cases = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof WorkbenchBlock)) continue;
            var bounds = block.defaultBlockState().getShape(helper.getLevel(), position, CollisionContext.empty()).bounds();
            helper.assertTrue(bounds.minX == 1.0 / 16 && bounds.maxX == 15.0 / 16
                    && bounds.minY == 0 && bounds.maxY == 12.0 / 16
                    && bounds.minZ == 1.0 / 16 && bounds.maxZ == 15.0 / 16,
                    BuiltInRegistries.BLOCK.getKey(block) + " lost the official bench shape: " + bounds);
            cases++;
        }
        helper.assertTrue(cases == 6, "Expected six workbenches; got " + cases);
        System.out.println("LAW_BENCH_SHAPE cases=" + cases + " passed=true");
        helper.succeed();
    }
}
