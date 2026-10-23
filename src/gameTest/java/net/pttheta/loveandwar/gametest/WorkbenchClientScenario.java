package net.pttheta.loveandwar.gametest;

import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pttheta.loveandwar.gun.WorkbenchBlockEntity;
import net.pttheta.loveandwar.gun.workbench.ModWorkbenchRecipes;
import net.pttheta.loveandwar.gun.workbench.WorkbenchMenu;
import net.pttheta.loveandwar.gun.runtime.CraftedParts;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Opt-in development scenario. This source set is excluded from release artifacts. */
@EventBusSubscriber(modid = "createloveandwar")
public final class WorkbenchClientScenario {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_bench").requires(source -> source.hasPermission(2))
            .executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var level = player.serverLevel();
                var pos = player.blockPosition().offset(2, 0, 0);
                level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createloveandwar:law_workbench")).defaultBlockState());
                var bench = (WorkbenchBlockEntity)level.getBlockEntity(pos);
                var recipe = level.getRecipeManager().getAllRecipesFor(ModWorkbenchRecipes.GUN_FABRICATION_TYPE.get()).stream()
                    .filter(r -> r.id().getPath().endsWith("/chassepot")).findFirst().orElseThrow().value();
                for (int i = 0; i < bench.partsInv().getSlots(); i++) bench.partsInv().setStackInSlot(i, ItemStack.EMPTY);
                for (int i = 0; i < recipe.ingredients().size(); i++) bench.partsInv().setStackInSlot(i,
                    recipe.ingredients().get(i).ingredient().getItems()[0].copyWithCount(1));
                bench.selectGunRecipe(null);
                player.openMenu(bench);
                return 1;
            }).then(Commands.literal("status").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                if (!(player.containerMenu instanceof WorkbenchMenu menu)) return 0;
                var carried = menu.getCarried();
                var parts = carried.getOrDefault(ModDataComponents.CRAFTED_PARTS, CraftedParts.EMPTY);
                int inputs = 0;
                for (int i = 0; i < menu.blockEntity().partsInv().getSlots(); i++) inputs += menu.blockEntity().partsInv().getStackInSlot(i).getCount();
                var message = "LAW_BENCH_SCENARIO selected=" + menu.selectedRecipeIndex() + " carried=" + carried + " parts=" + parts.parts() + " inputs=" + inputs;
                context.getSource().sendSuccess(() -> Component.literal(message), false);
                return 1;
            })));
    }
}
