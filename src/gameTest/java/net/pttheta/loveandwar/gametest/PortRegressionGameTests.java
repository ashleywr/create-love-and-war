package net.pttheta.loveandwar.gametest;

import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.WorkbenchBlockEntity;
import net.pttheta.loveandwar.gun.WorkbenchBlock;
import net.pttheta.loveandwar.gun.workbench.WorkbenchMenu;
import net.pttheta.loveandwar.item.ModArmorMaterials;
import net.pttheta.loveandwar.item.ModItems;
import net.pttheta.loveandwar.recipe.petrochemical.FractionalDistillationRecipe;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class PortRegressionGameTests {
    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void breakingBenchPreservesStoredItems(GameTestHelper helper) {
        var position = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createloveandwar:law_workbench"));
        level.setBlockAndUpdate(position, block.defaultBlockState());
        var bench = (WorkbenchBlockEntity) level.getBlockEntity(position);
        bench.partsInv().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        bench.outputHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        bench.gunHandler().setStackInSlot(0, new ItemStack(ModItems.MARTINI.get()));
        level.setBlockAndUpdate(position, block.defaultBlockState().setValue(WorkbenchBlock.FACING, Direction.EAST));
        helper.assertTrue(bench.partsInv().getStackInSlot(0).getCount() == 4,
                "Changing bench facing must preserve its inventory");
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2)).isEmpty(),
                "Changing bench facing must not duplicate inventory into drops");
        level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(count == 7, "Breaking a bench must drop all three inventories; got " + count + " items");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void menuRetainsReceivedLegacyCraftingProgress(GameTestHelper helper) {
        var position = helper.absolutePos(new BlockPos(2, 2, 2));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createloveandwar:law_workbench"));
        helper.getLevel().setBlockAndUpdate(position, block.defaultBlockState());
        var bench = (WorkbenchBlockEntity) helper.getLevel().getBlockEntity(position);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var menu = new WorkbenchMenu(42, player.getInventory(), bench);
        menu.setData(0, 1);
        menu.setData(1, 25);
        menu.setData(2, 100);
        helper.assertTrue(menu.isCrafting() && menu.craftTicksRemaining() == 25 && menu.craftTicksTotal() == 100,
                "Legacy pending-craft progress packets must retain their values in menu data");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void tungstenProtectionMatchesOfficialSlots(GameTestHelper helper) {
        var defense = ModArmorMaterials.TUNGSTEN.value().defense();
        helper.assertTrue(defense.get(ArmorItem.Type.HELMET) == 5, "Official tungsten helmet protection is 5");
        helper.assertTrue(defense.get(ArmorItem.Type.CHESTPLATE) == 7, "Official tungsten chestplate protection is 7");
        helper.assertTrue(defense.get(ArmorItem.Type.LEGGINGS) == 5, "Official tungsten leggings protection is 5");
        helper.assertTrue(defense.get(ArmorItem.Type.BOOTS) == 4, "Official tungsten boots protection is 4");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void oilDistillationRetainsHeatAndDuration(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(
                ResourceLocation.parse("createloveandwar:fractional_distillation/crude_oil")).orElseThrow();
        var recipe = (FractionalDistillationRecipe) holder.value();
        helper.assertTrue(recipe.getProcessingDuration() == 100, "Oil distillation must take 100 ticks, got " + recipe.getProcessingDuration());
        helper.assertTrue(recipe.getRequiredHeat() == HeatCondition.HEATED, "Oil distillation must require a heated burner");
        helper.succeed();
    }
}
