package net.pttheta.loveandwar.gametest;

import java.util.Comparator;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import io.netty.buffer.Unpooled;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.WorkbenchBlockEntity;
import net.pttheta.loveandwar.gun.workbench.ModWorkbenchRecipes;
import net.pttheta.loveandwar.gun.workbench.PartsInput;
import net.pttheta.loveandwar.gun.workbench.WorkbenchMenu;
import net.pttheta.loveandwar.gun.workbench.GunFabricationRecipe;
import net.pttheta.loveandwar.gun.workbench.GunFabricationRecipeSerializer;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.gun.runtime.CraftedParts;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunCraftingParityGameTests {
    private static WorkbenchBlockEntity bench(GameTestHelper helper, String name) {
        var position = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(position,
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createloveandwar:" + name)).defaultBlockState());
        return (WorkbenchBlockEntity) helper.getLevel().getBlockEntity(position);
    }

    private static void fill(WorkbenchBlockEntity bench,
            net.pttheta.loveandwar.gun.workbench.GunFabricationRecipe recipe) {
        for (int i = 0; i < bench.partsInv().getSlots(); i++) bench.partsInv().setStackInSlot(i, ItemStack.EMPTY);
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            var ingredient = recipe.ingredients().get(i);
            var options = ingredient.ingredient().getItems();
            if (options.length == 0) throw new IllegalStateException("Unresolved ingredient " + i);
            bench.partsInv().setStackInSlot(i, options[0].copyWithCount(ingredient.count()));
        }
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void gunIngredientsRetainOfficialSlotOrder(GameTestHelper helper) {
        var bench = bench(helper, "law_workbench");
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get())
                .stream().filter(r -> r.id().getPath().endsWith("/chassepot")).findFirst().orElseThrow().value();
        fill(bench, recipe);
        helper.assertTrue(recipe.planConsumption(new PartsInput(bench.partsInv())).isPresent(), "Valid ordered inputs must match");
        var first = bench.partsInv().getStackInSlot(0);
        bench.partsInv().setStackInSlot(0, bench.partsInv().getStackInSlot(1));
        bench.partsInv().setStackInSlot(1, first);
        helper.assertTrue(recipe.planConsumption(new PartsInput(bench.partsInv())).isEmpty(), "Swapped barrel and handguard must not match");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void assembledGunsKeepTheirCraftedParts(GameTestHelper helper) {
        var bench = bench(helper, "law_workbench");
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get())
                .stream().filter(r -> r.id().getPath().endsWith("/chassepot")).findFirst().orElseThrow().value();
        fill(bench, recipe);
        var crafted = recipe.assemble(new PartsInput(bench.partsInv()), helper.getLevel().registryAccess());
        helper.assertTrue(!ItemStack.isSameItemSameComponents(crafted, recipe.getResultItem(helper.getLevel().registryAccess())),
                "An assembled gun must retain part identities rather than equal the bare recipe display stack");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void everyAllowedGunRecipeIsSelectableAndCraftable(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var position = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        var visited = new HashSet<ResourceLocation>();
        for (String name : new String[]{"law_workbench", "advanced_workbench", "expert_workbench", "em2_workbench", "mas62_workbench", "stg57_workbench"}) {
            var bench = bench(helper, name);
            var recipes = level.getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get()).stream()
                    .filter(r -> bench.def().orElseThrow().canCraft(r.value().getResultItem(level.registryAccess())))
                    .sorted(Comparator.comparing(r -> r.id().toString())).toList();
            var menu = new WorkbenchMenu(42, player.getInventory(), bench);
            for (int index = 0; index < recipes.size(); index++) {
                var holder = recipes.get(index);
                fill(bench, holder.value());
                helper.assertTrue(menu.clickMenuButton(player, 16 + index), "Cannot select " + holder.id() + " at " + name);
                var output = menu.getSlot(bench.partsInv().getSlots());
                helper.assertTrue(output.getItem().is(holder.value().getResultItem(level.registryAccess()).getItem()),
                        "Selected recipe must preview its own gun immediately: " + holder.id());
                var result = output.remove(1);
                helper.assertTrue(result.is(holder.value().getResultItem(level.registryAccess()).getItem()), "Wrong selected gun crafted");
                for (int slot = 0; slot < holder.value().ingredients().size(); slot++) {
                    helper.assertTrue(bench.partsInv().getStackInSlot(slot).isEmpty(), "Craft did not consume slot " + slot);
                }
                helper.assertTrue(output.remove(1).isEmpty(), "A second take must not duplicate a gun without parts");
                visited.add(holder.id());
            }
        }
        var all = level.getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get());
        var unavailable = all.stream().map(r -> r.id()).filter(id -> !visited.contains(id)).toList();
        helper.assertTrue(all.size() == 46 && visited.size() == 45
                        && unavailable.equals(java.util.List.of(ResourceLocation.parse("createloveandwar:gun_fabrication/stg57_scope"))),
                "Expected 45 allowed recipes and the original orphaned optical-scope recipe; reached " + visited.size() + ", unavailable=" + unavailable);
        System.out.println("LAW_GUN_CRAFTING_REACHABLE recipes=" + visited.size());
        helper.succeed();
    }

    private record CraftingContext(WorkbenchBlockEntity bench, WorkbenchMenu menu,
            net.minecraft.world.entity.player.Player player, GunFabricationRecipe recipe) {}

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void menuPacketDoesNotRequireWorldBlockEntity(GameTestHelper helper) {
        var bench = bench(helper, "law_workbench");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            bench.writeClientSideData(null, buffer);
            helper.getLevel().setBlockAndUpdate(bench.getBlockPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            var menu = WorkbenchMenu.fromNetwork(42, player.getInventory(), buffer);
            helper.assertTrue(menu.blockEntity() != bench && menu.blockEntity().partsInv().getSlots() == 10,
                    "Menu must create its synchronized inventory before a world block entity exists");
            helper.assertTrue(menu.slots.size() == 53, "Network bench must expose all expected input/output/attachment/player slots");
        } finally { buffer.release(); }
        helper.succeed();
    }

    private static CraftingContext prepare(GameTestHelper helper) {
        var bench = bench(helper, "law_workbench");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = bench.getBlockPos();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var menu = new WorkbenchMenu(42, player.getInventory(), bench);
        int choice = -1;
        for (int i = 0; i < menu.recipes().size(); i++) if (menu.recipes().get(i).id().getPath().endsWith("/chassepot")) choice = i;
        helper.assertTrue(choice >= 0 && menu.clickMenuButton(player, WorkbenchMenu.SELECT_RECIPE_BUTTON + choice), "Chassepot must be selectable");
        var recipe = menu.selectedRecipe().orElseThrow();
        fill(bench, recipe);
        return new CraftingContext(bench, menu, player, recipe);
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void gunOutputClicksConsumeExactlyOnce(GameTestHelper helper) {
        var context = prepare(helper);
        var bench = context.bench();
        var menu = context.menu();
        var player = context.player();
        int output = bench.partsInv().getSlots();
        for (int i = 0; i < context.recipe().ingredients().size(); i++) bench.partsInv().getStackInSlot(i).setCount(2);
        bench.inputsChanged();
        menu.clicked(output, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(context.recipe().result().getItem()), "Normal pickup must produce the selected gun");
        for (int i = 0; i < context.recipe().ingredients().size(); i++) helper.assertTrue(bench.partsInv().getStackInSlot(i).getCount() == 1,
                "Normal pickup must consume one from slot " + i);
        menu.clicked(output + 7, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && player.getInventory().getItem(9).is(context.recipe().result().getItem()),
                "Crafted gun must be placeable into the player's inventory");
        menu.clicked(output, 0, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(0).is(context.recipe().result().getItem()), "Hotbar swap must take a gun");
        for (int i = 0; i < context.recipe().ingredients().size(); i++) helper.assertTrue(bench.partsInv().getStackInSlot(i).isEmpty(),
                "Hotbar swap must consume exactly once from slot " + i);
        helper.assertTrue(menu.getSlot(output).getItem().isEmpty(), "No output may remain without ingredients");
        menu.clicked(output, 1, ClickType.THROW, player);
        helper.assertTrue(menu.getSlot(output).getItem().isEmpty(), "Throwing empty output must not create another gun");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void shiftCraftRequiresInventorySpace(GameTestHelper helper) {
        var context = prepare(helper);
        var inventory = context.player().getInventory();
        for (int i = 0; i < 36; i++) inventory.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        int output = context.bench().partsInv().getSlots();
        helper.assertTrue(context.menu().quickMoveStack(context.player(), output).isEmpty(), "Full inventory must refuse shift craft");
        for (int i = 0; i < context.recipe().ingredients().size(); i++) helper.assertTrue(context.bench().partsInv().getStackInSlot(i).getCount() == 1,
                "Failed shift craft must not consume slot " + i);
        inventory.setItem(35, ItemStack.EMPTY);
        context.menu().clicked(output, 0, ClickType.QUICK_MOVE, context.player());
        helper.assertTrue(inventory.getItem(35).is(context.recipe().result().getItem()), "Shift craft must fill the available slot");
        helper.assertTrue(context.menu().getSlot(output).getItem().isEmpty(), "Shift craft must not leave an extra output");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void craftedPartsSurviveSaveAndNetwork(GameTestHelper helper) {
        var context = prepare(helper);
        var assembled = context.recipe().assemble(new PartsInput(context.bench().partsInv()), helper.getLevel().registryAccess());
        var parts = assembled.getOrDefault(ModDataComponents.CRAFTED_PARTS, CraftedParts.EMPTY);
        helper.assertTrue(parts.parts().size() == 5, "Chassepot must retain all five part IDs");
        for (int i = 0; i < 5; i++) helper.assertTrue(parts.parts().get(i).equals(BuiltInRegistries.ITEM.getKey(context.bench().partsInv().getStackInSlot(i).getItem())),
                "Crafted part order changed at " + i);
        var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)assembled.save(helper.getLevel().registryAccess()));
        helper.assertTrue(ItemStack.isSameItemSameComponents(loaded, assembled), "Crafted part identities must survive item NBT");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, assembled);
            helper.assertTrue(ItemStack.isSameItemSameComponents(ItemStack.STREAM_CODEC.decode(buffer), assembled), "Crafted parts must survive item packet encoding");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void gunPreviewIsNotStoredOrDropped(GameTestHelper helper) {
        var context = prepare(helper);
        helper.assertTrue(context.menu().getSlot(context.bench().partsInv().getSlots()).hasItem(), "Inputs must show a preview");
        helper.assertTrue(context.bench().outputHandler().getStackInSlot(0).isEmpty(), "A preview must not be a stored output");
        var pos = context.bench().getBlockPos();
        helper.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2));
        helper.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 5, "Breaking a bench must drop only the five real input parts");
        helper.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(context.recipe().result().getItem())), "Breaking a bench must not drop a preview gun");
        helper.assertTrue(context.menu().getSlot(context.bench().partsInv().getSlots()).remove(1).isEmpty(), "Removed bench must reject a stale output take");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void autoFillAndInvalidChoicesPreserveItems(GameTestHelper helper) {
        var context = prepare(helper);
        var inv = context.player().getInventory();
        var barrel = context.bench().partsInv().getStackInSlot(0).copy();
        context.bench().partsInv().setStackInSlot(0, ItemStack.EMPTY);
        inv.setItem(0, barrel.copyWithCount(2));
        helper.assertTrue(context.menu().clickMenuButton(context.player(), -128), "Signed-byte auto-fill request must be accepted");
        helper.assertTrue(inv.getItem(0).getCount() == 1 && context.bench().partsInv().getStackInSlot(0).getCount() == 1, "Auto-fill must transfer exactly one part");
        helper.assertTrue(!context.menu().clickMenuButton(context.player(), 127), "Out-of-catalog recipe index must be rejected");
        helper.assertTrue(!context.menu().clickMenuButton(context.player(), 1), "These benches must not permit the rewrite's free paint action");
        var selected = context.menu().selectedRecipe().orElseThrow();
        context.player().setPos(0, 200, 0);
        helper.assertTrue(!context.menu().clickMenuButton(context.player(), 16), "Distant player must not change the recipe");
        helper.assertTrue(context.menu().selectedRecipe().orElseThrow() == selected, "Invalid requests must preserve selection");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void unusedPartsKeepOriginalTraitContribution(GameTestHelper helper) {
        var context = prepare(helper);
        var extra = context.bench().partsInv().getStackInSlot(0).copyWithCount(7);
        context.bench().partsInv().setStackInSlot(9, extra);
        var crafted = context.menu().getSlot(context.bench().partsInv().getSlots()).remove(1);
        var parts = crafted.getOrDefault(ModDataComponents.CRAFTED_PARTS, CraftedParts.EMPTY).parts();
        helper.assertTrue(parts.size() == 6 && parts.get(5).equals(BuiltInRegistries.ITEM.getKey(extra.getItem())),
                "Original quirk: an unused part slot contributes one part identity, regardless of stack count");
        helper.assertTrue(context.bench().partsInv().getStackInSlot(9).getCount() == 7, "Unused contributing parts must not be consumed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void recoveredPendingCraftFinishesWithoutDuplicating(GameTestHelper helper) {
        var context = prepare(helper);
        var bench = context.bench();
        var tag = bench.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putString("CraftingRecipe", "createloveandwar:gun_fabrication/chassepot");
        tag.putInt("CraftTicksRemaining", 1);
        tag.putInt("CraftTicksTotal", 300);
        bench.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(bench.isCrafting(), "Recovered saved countdown must remain pending");
        helper.assertTrue(context.menu().getSlot(bench.partsInv().getSlots()).getItem().isEmpty(), "Pending legacy craft must not also expose a new preview");
        WorkbenchBlockEntity.serverTick(helper.getLevel(), bench.getBlockPos(), bench.getBlockState(), bench);
        var stored = bench.outputHandler().getStackInSlot(0);
        helper.assertTrue(!bench.isCrafting() && stored.is(context.recipe().result().getItem()), "Legacy countdown must finish into real output storage");
        helper.assertTrue(stored.getOrDefault(ModDataComponents.CRAFTED_PARTS, CraftedParts.EMPTY).parts().size() == 5,
                "Recovered pending craft must preserve its materials");
        var output = context.menu().getSlot(bench.partsInv().getSlots());
        helper.assertTrue(output.remove(1).is(context.recipe().result().getItem()) && output.remove(1).isEmpty(),
                "Stored legacy output must be retrievable exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void autoFillDoesNotPartiallyReturnAnExistingStack(GameTestHelper helper) {
        var context = prepare(helper);
        var inv = context.player().getInventory();
        var original = context.bench().partsInv().getStackInSlot(0).copyWithCount(64);
        context.bench().partsInv().setStackInSlot(0, original);
        for (int i = 0; i < 36; i++) inv.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        inv.setItem(0, original.copyWithCount(64));
        helper.assertTrue(!context.menu().clickMenuButton(context.player(), WorkbenchMenu.AUTO_FILL_BUTTON),
                "Auto-fill must refuse when the existing stack cannot be fully returned");
        helper.assertTrue(inv.getItem(0).getCount() == 64 && context.bench().partsInv().getStackInSlot(0).getCount() == 64,
                "Refused auto-fill must not insert part of the return or consume a source item");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void recipeResultComponentsAndDefaultDurationRoundTrip(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json = JsonParser.parseString("""
            {"ingredients":[{"item":"minecraft:iron_ingot"}],
             "result":{"id":"createloveandwar:chassepot","count":2,"components":{}}}
            """).getAsJsonObject();
        json.getAsJsonObject("result").getAsJsonObject("components")
                .addProperty("minecraft:custom_name", new com.google.gson.JsonPrimitive("Custom gun").toString());
        var recipe = GunFabricationRecipeSerializer.CODEC.codec().parse(ops, json).getOrThrow();
        helper.assertTrue(recipe.craftTimeTicks() == 20, "Original default craft_time is 20");
        helper.assertTrue(recipe.result().getCount() == 2 && recipe.result().getHoverName().getString().equals("Custom gun"), "Recipe result must keep count and components");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            GunFabricationRecipeSerializer.STREAM_CODEC.encode(buffer, recipe);
            var decoded = GunFabricationRecipeSerializer.STREAM_CODEC.decode(buffer);
            helper.assertTrue(ItemStack.isSameItemSameComponents(decoded.result(), recipe.result()) && decoded.result().getCount() == 2, "Recipe sync must preserve the full result");
        } finally { buffer.release(); }
        recipe.result().set(DataComponents.CUSTOM_NAME, Component.literal("Mutated"));
        helper.assertTrue(recipe.result().getHoverName().getString().equals("Custom gun"), "Recipe display result must not expose mutable state");
        helper.succeed();
    }
}
