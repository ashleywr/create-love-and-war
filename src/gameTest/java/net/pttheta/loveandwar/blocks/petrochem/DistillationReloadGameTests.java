package net.pttheta.loveandwar.blocks.petrochem;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.blocks.LAWBlocks;
import net.pttheta.loveandwar.blocks.LAWBlockEntityTypes;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class DistillationReloadGameTests {
    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void reloadedMemberUsesControllerTank(GameTestHelper helper) {
        var level = helper.getLevel();
        var controllerPosition = helper.absolutePos(new BlockPos(1, 2, 2));
        var memberPosition = controllerPosition.east();
        var state = LAWBlocks.DISTILLATION_COLUMN.getDefaultState();
        level.setBlockAndUpdate(controllerPosition, state);
        level.setBlockAndUpdate(memberPosition, state);
        var controller = (DistillationColumnBlockEntity) level.getBlockEntity(controllerPosition);
        var member = (DistillationColumnBlockEntity) level.getBlockEntity(memberPosition);
        member.setController(controllerPosition);
        var saved = new CompoundTag();
        member.write(saved, level.registryAccess(), false);
        var reloaded = new DistillationColumnBlockEntity(LAWBlockEntityTypes.DISTILLATION_COLUMN.get(), memberPosition, state);
        reloaded.setLevel(level);
        reloaded.read(saved, level.registryAccess(), false);
        level.setBlockEntity(reloaded);
        helper.assertTrue(reloaded.getFluidHandlerCapability() == controller.tankInventory,
                "A reloaded member must resolve its controller's tank, not the constructor's isolated tank");
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, memberPosition, null);
        helper.assertTrue(handler != null && handler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE) == 100,
                "The reloaded member must expose its fluid capability");
        helper.assertTrue(controller.tankInventory.getFluidAmount() == 100,
                "Filling through a reloaded member must fill the controller");
        handler.drain(40, FluidAction.EXECUTE);
        helper.assertTrue(controller.tankInventory.getFluidAmount() == 60,
                "Draining through a reloaded member must drain the controller");
        helper.succeed();
    }
}
