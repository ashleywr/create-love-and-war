package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluids;
import net.pttheta.loveandwar.gun.content.ActionGroup;

/** Shared original CanStart gates; active group retriggers do not repeat these checks. */
public final class GunActionRestrictions {
    private GunActionRestrictions() {}

    public static boolean canStart(LivingEntity shooter, ActionGroup group) {
        if (group.twoHanded() && shooter.getMainHandItem().isEmpty() == shooter.getOffhandItem().isEmpty()) return false;
        var origin = BlockPos.containing(shooter.getEyePosition());
        if (!group.canActUnderwater() && shooter.level().isWaterAt(origin)) return false;
        var fluid = shooter.level().getFluidState(origin);
        // Source excludes only exact source WATER here, so flowing/tagged custom water can still be other liquid.
        return group.canActUnderOtherLiquid() || fluid.isEmpty() || fluid.isSourceOfType(Fluids.WATER);
    }
}
