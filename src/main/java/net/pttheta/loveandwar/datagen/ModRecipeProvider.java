package net.pttheta.loveandwar.datagen;

import com.simibubi.create.AllItems;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.pttheta.loveandwar.blocks.ModBlocks;
import net.pttheta.loveandwar.item.ModItems;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
   private static final List<ItemLike> SULPHUR_SMELTABLES = List.of(
      (ItemLike)ModItems.RAW_SULPHUR.get(),
      (ItemLike)ModBlocks.SULPHUR_ORE.get(),
      (ItemLike)ModBlocks.DEEPSLATE_SULPHUR_ORE.get(),
      (ItemLike)ModBlocks.NETHER_SULPHUR_ORE.get()
   );
   private static final List<ItemLike> TUNGSTEN_SMELTABLES = List.of((ItemLike)ModItems.RAW_TUNGSTEN.get(), (ItemLike)ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());

   public ModRecipeProvider(PackOutput pOutput, CompletableFuture<Provider> registries) {
      super(pOutput, registries);
   }

   protected void buildRecipes(RecipeOutput pWriter) {
      this.sulphurRecipes(pWriter);
      this.tungstenRecipes(pWriter);
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.GUNPOWDER, 3)
         .requires((ItemLike)ModItems.SULPHUR.get())
         .requires(Items.COAL)
         .requires(Items.BONE_MEAL)
         .unlockedBy(getHasName((ItemLike)ModItems.SULPHUR.get()), has((ItemLike)ModItems.SULPHUR.get()))
         .save(pWriter);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get())
         .pattern(" RB")
         .pattern("BPB")
         .pattern("T T")
         .define('T', (ItemLike)ModItems.TUNGSTEN.get())
         .define('P', (ItemLike)AllItems.PRECISION_MECHANISM.get())
         .define('B', (ItemLike)AllItems.BRASS_INGOT.get())
         .define('R', Items.REDSTONE_BLOCK)
         .unlockedBy(getHasName((ItemLike)AllItems.PRECISION_MECHANISM.get()), has((ItemLike)AllItems.PRECISION_MECHANISM.get()))
         .save(pWriter);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)ModItems.ARMOR_UPGRADE.get())
         .pattern("   ")
         .pattern("TIT")
         .pattern("TIT")
         .define('T', (ItemLike)ModItems.TUNGSTEN.get())
         .define('I', Items.IRON_INGOT)
         .unlockedBy(getHasName((ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get()), has((ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get()))
         .save(pWriter);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)ModItems.SCANNER_UPGRADE.get())
         .pattern(" T ")
         .pattern("GRG")
         .pattern(" T ")
         .define('T', (ItemLike)ModItems.TUNGSTEN.get())
         .define('R', Items.REDSTONE)
         .define('G', Items.GLASS)
         .unlockedBy(getHasName((ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get()), has((ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get()))
         .save(pWriter);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)ModItems.TURRET_UPGRADE.get())
         .pattern("   ")
         .pattern("RII")
         .pattern(" T ")
         .define('T', (ItemLike)ModItems.TUNGSTEN.get())
         .define('R', Items.REDSTONE)
         .define('I', Items.IRON_INGOT)
         .unlockedBy(getHasName((ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get()), has((ItemLike)ModItems.ROBOT_DOG_SPAWN_EGG.get()))
         .save(pWriter);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)ModItems.GRENADE.get())
         .pattern("IRI")
         .pattern("IGI")
         .pattern("III")
         .define('R', Items.REDSTONE)
         .define('G', Items.GUNPOWDER)
         .define('I', Items.IRON_NUGGET)
         .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
         .save(pWriter);
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)ModItems.BALL_BEARINGS.get(), 1)
         .requires(Items.IRON_NUGGET)
         .requires(Items.IRON_NUGGET)
         .requires(Items.IRON_NUGGET)
         .unlockedBy(getHasName(Items.IRON_NUGGET), has(Items.IRON_NUGGET))
         .save(pWriter);
   }

   private void sulphurRecipes(RecipeOutput pWriter) {
      oreBlasting(pWriter, SULPHUR_SMELTABLES, RecipeCategory.MISC, (ItemLike)ModItems.SULPHUR.get(), 0.25F, 100, "sulphur");
      oreSmelting(pWriter, SULPHUR_SMELTABLES, RecipeCategory.MISC, (ItemLike)ModItems.SULPHUR.get(), 0.25F, 200, "sulphur");
      blockFromItem((Block)ModBlocks.RAW_SULPHUR_BLOCK.get(), (Item)ModItems.RAW_SULPHUR.get(), pWriter);
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)ModBlocks.SULPHUR_BLOCK.get())
         .pattern("SSS")
         .pattern("SSS")
         .pattern("SSS")
         .define('S', (ItemLike)ModItems.SULPHUR.get())
         .unlockedBy(getHasName((ItemLike)ModItems.SULPHUR.get()), has((ItemLike)ModItems.SULPHUR.get()))
         .save(pWriter);
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)ModItems.SULPHUR.get(), 9)
         .requires((ItemLike)ModBlocks.SULPHUR_BLOCK.get())
         .unlockedBy(getHasName((ItemLike)ModBlocks.SULPHUR_BLOCK.get()), has((ItemLike)ModBlocks.SULPHUR_BLOCK.get()))
         .save(pWriter);
   }

   private void tungstenRecipes(RecipeOutput pWriter) {
      oreBlasting(pWriter, TUNGSTEN_SMELTABLES, RecipeCategory.MISC, (ItemLike)ModItems.TUNGSTEN.get(), 0.25F, 100, "tungsten");
      oreSmelting(pWriter, TUNGSTEN_SMELTABLES, RecipeCategory.MISC, (ItemLike)ModItems.TUNGSTEN.get(), 0.25F, 200, "tungsten");
      blockFromItem((Block)ModBlocks.RAW_TUNGSTEN_BLOCK.get(), (Item)ModItems.RAW_TUNGSTEN.get(), pWriter);
      blockFromItem((Block)ModBlocks.TUNGSTEN_BLOCK.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      swordItem((Item)ModItems.TUNGSTEN_SWORD.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      pickaxeItem((Item)ModItems.TUNGSTEN_PICKAXE.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      axeItem((Item)ModItems.TUNGSTEN_AXE.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      shovelItem((Item)ModItems.TUNGSTEN_SHOVEL.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      hoeItem((Item)ModItems.TUNGSTEN_HOE.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      helmetItem((Item)ModItems.TUNGSTEN_HELMET.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      chestplateItem((Item)ModItems.TUNGSTEN_CHESTPLATE.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      leggingsItem((Item)ModItems.TUNGSTEN_LEGGINGS.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      bootsItem((Item)ModItems.TUNGSTEN_BOOTS.get(), (Item)ModItems.TUNGSTEN.get(), pWriter);
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)ModItems.TUNGSTEN_NUGGET.get(), 9)
         .requires((ItemLike)ModItems.TUNGSTEN.get())
         .unlockedBy(getHasName((ItemLike)ModItems.TUNGSTEN.get()), has((ItemLike)ModItems.TUNGSTEN.get()))
         .save(pWriter);
   }

   protected static void blockFromItem(Block block, Item item, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, block)
         .pattern("SSS")
         .pattern("SSS")
         .pattern("SSS")
         .define('S', item)
         .unlockedBy(getHasName(item), has(item))
         .save(pWriter);
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item, 9).requires(block).unlockedBy(getHasName(block), has(block)).save(pWriter);
   }

   protected static void swordItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern(" T ")
         .pattern(" T ")
         .pattern(" S ")
         .define('S', Items.STICK)
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void pickaxeItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("TTT")
         .pattern(" S ")
         .pattern(" S ")
         .define('S', Items.STICK)
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void axeItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("TT ")
         .pattern("TS ")
         .pattern(" S ")
         .define('S', Items.STICK)
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void shovelItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern(" T ")
         .pattern(" S ")
         .pattern(" S ")
         .define('S', Items.STICK)
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void hoeItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("TT ")
         .pattern(" S ")
         .pattern(" S ")
         .define('S', Items.STICK)
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void helmetItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("TTT")
         .pattern("T T")
         .pattern("   ")
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void chestplateItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("T T")
         .pattern("TTT")
         .pattern("TTT")
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void leggingsItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("TTT")
         .pattern("T T")
         .pattern("T T")
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }

   protected static void bootsItem(Item output, Item ingredient, RecipeOutput pWriter) {
      ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output)
         .pattern("   ")
         .pattern("T T")
         .pattern("T T")
         .define('T', ingredient)
         .unlockedBy(getHasName(ingredient), has(ingredient))
         .save(pWriter);
   }
}
