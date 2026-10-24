package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

/** Server ammunition mutations and their atomic item-snapshot revision. */
public final class GunAmmo {
    private GunAmmo() {}

    public static long revision(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.GUN_AMMO_REVISION, 0L);
    }

    public static int count(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY)
                .items()
                .size();
    }

    static void set(ItemStack gun, AmmoPouchContents rounds) {
        if (rounds.equals(
                gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY)))
            return;
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, rounds);
        gun.set(ModDataComponents.GUN_AMMO_REVISION, revision(gun) + 1);
    }
}
