package net.pttheta.loveandwar.datagen;

import java.util.LinkedHashMap;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.pttheta.loveandwar.blocks.ModBlocks;
import net.pttheta.loveandwar.item.ModItems;

public class ModItemModelProvider extends ItemModelProvider {
   private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();

   public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
      super(output, "createloveandwar", existingFileHelper);
   }

   protected void registerModels() {
      this.simpleItem(ModItems.CIGARETTE);
      this.simpleItem(ModItems.RAW_SULPHUR);
      this.simpleItem(ModItems.SULPHUR);
      this.simpleItem(ModItems.RAW_TUNGSTEN);
      this.simpleItem(ModItems.TUNGSTEN);
      this.simpleItem(ModItems.TUNGSTEN_NUGGET);
      this.simpleItem(ModItems.TUNGSTEN_SHEET);
      this.simpleBlockItem(ModBlocks.TUNGSTEN_DOOR);
      this.fenceItem(ModBlocks.TUNGSTEN_FENCE, ModBlocks.TUNGSTEN_BLOCK);
      this.buttonItem(ModBlocks.TUNGSTEN_BUTTON, ModBlocks.TUNGSTEN_BLOCK);
      this.wallItem(ModBlocks.TUNGSTEN_WALL, ModBlocks.TUNGSTEN_BLOCK);
      this.evenSimplerBlockItem(ModBlocks.TUNGSTEN_STAIRS);
      this.evenSimplerBlockItem(ModBlocks.TUNGSTEN_SLAB);
      this.evenSimplerBlockItem(ModBlocks.TUNGSTEN_PRESSURE_PLATE);
      this.evenSimplerBlockItem(ModBlocks.TUNGSTEN_GATE);
      this.trapdoorItem(ModBlocks.TUNGSTEN_TRAPDOOR);
      this.handHeldItem(ModItems.TUNGSTEN_SWORD);
      this.handHeldItem(ModItems.TUNGSTEN_PICKAXE);
      this.handHeldItem(ModItems.TUNGSTEN_AXE);
      this.handHeldItem(ModItems.TUNGSTEN_SHOVEL);
      this.handHeldItem(ModItems.TUNGSTEN_HOE);
      this.trimmedArmorItem(ModItems.TUNGSTEN_HELMET);
      this.trimmedArmorItem(ModItems.TUNGSTEN_CHESTPLATE);
      this.trimmedArmorItem(ModItems.TUNGSTEN_LEGGINGS);
      this.trimmedArmorItem(ModItems.TUNGSTEN_BOOTS);
      this.simpleItem(ModItems.TURRET_UPGRADE);
      this.simpleItem(ModItems.ARMOR_UPGRADE);
      this.simpleItem(ModItems.SCANNER_UPGRADE);
      this.simpleItem(ModItems.ROBOT_DOG_SPAWN_EGG);
      this.simpleItem(ModItems.BRASS_CUP);
      this.simpleItem(ModItems.WIDE_BRASS_CUP);
      this.simpleItem(ModItems.PISTOL_CASING);
      this.simpleItem(ModItems.RIFLE_CASING);
      this.simpleItem(ModItems.GRENADE_CASING);
      this.simpleItem(ModItems.ARTILLERY_CASING);
      this.simpleItem(ModItems.SHOTGUN_CASING);
      this.simpleItem(ModItems.FILLED_PISTOL_CASING);
      this.simpleItem(ModItems.FILLED_RIFLE_CASING);
      this.simpleItem(ModItems.FILLED_GRENADE_CASING);
      this.simpleItem(ModItems.FILLED_ARTILLERY_CASING);
      this.simpleItem(ModItems.FILLED_SHOTGUN_CASING);
      this.simpleItem(ModItems.BALL_BEARINGS);
      this.simpleItem(ModItems.GRENADE);
      this.simpleItem(ModItems.TEMPLATE_UPPER_RECEIVER);
      this.simpleItem(ModItems.TEMPLATE_LOWER_RECEIVER);
      this.simpleItem(ModItems.TEMPLATE_STOCK);
      this.simpleItem(ModItems.TEMPLATE_BARREL);
      this.simpleItem(ModItems.TEMPLATE_GRIP);
      this.simpleItem(ModItems.TEMPLATE_HANDGUARD);
      this.simpleItem(ModItems.UPPER_RECEIVER_IRON);
      this.simpleItem(ModItems.LOWER_RECEIVER_IRON);
      this.simpleItem(ModItems.STOCK_IRON);
      this.simpleItem(ModItems.BARREL_IRON);
      this.simpleItem(ModItems.GRIP_IRON);
      this.simpleItem(ModItems.UPPER_RECEIVER_BRASS);
      this.simpleItem(ModItems.LOWER_RECEIVER_BRASS);
      this.simpleItem(ModItems.STOCK_BRASS);
      this.simpleItem(ModItems.BARREL_BRASS);
      this.simpleItem(ModItems.GRIP_BRASS);
      this.simpleItem(ModItems.UPPER_RECEIVER_TUNGSTEN);
      this.simpleItem(ModItems.LOWER_RECEIVER_TUNGSTEN);
      this.simpleItem(ModItems.STOCK_TUNGSTEN);
      this.simpleItem(ModItems.BARREL_TUNGSTEN);
      this.simpleItem(ModItems.GRIP_TUNGSTEN);
      this.simpleItem(ModItems.UPPER_RECEIVER_STURDY);
      this.simpleItem(ModItems.LOWER_RECEIVER_STURDY);
      this.simpleItem(ModItems.STOCK_STURDY);
      this.simpleItem(ModItems.BARREL_STURDY);
      this.simpleItem(ModItems.GRIP_STURDY);
      this.simpleItem(ModItems.STOCK_WOOD);
      this.simpleItem(ModItems.GRIP_WOOD);
   }

   private ItemModelBuilder simpleItem(DeferredItem<Item> item) {
      return (ItemModelBuilder)((ItemModelBuilder)this.withExistingParent(item.getId().getPath(), ResourceLocation.withDefaultNamespace("item/generated")))
         .texture("layer0", ResourceLocation.fromNamespaceAndPath("createloveandwar", "item/" + item.getId().getPath()));
   }

   public void evenSimplerBlockItem(DeferredBlock<Block> block) {
      this.withExistingParent("createloveandwar:" + block.getId().getPath(), this.modLoc("block/" + block.getId().getPath()));
   }

   public void trapdoorItem(DeferredBlock<Block> block) {
      this.withExistingParent(block.getId().getPath(), this.modLoc("block/" + block.getId().getPath() + "_bottom"));
   }

   public void fenceItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
      ((ItemModelBuilder)this.withExistingParent(block.getId().getPath(), this.mcLoc("block/fence_inventory")))
         .texture("texture", ResourceLocation.fromNamespaceAndPath("createloveandwar", "block/" + baseBlock.getId().getPath()));
   }

   public void buttonItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
      ((ItemModelBuilder)this.withExistingParent(block.getId().getPath(), this.mcLoc("block/button_inventory")))
         .texture("texture", ResourceLocation.fromNamespaceAndPath("createloveandwar", "block/" + baseBlock.getId().getPath()));
   }

   public void wallItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
      ((ItemModelBuilder)this.withExistingParent(block.getId().getPath(), this.mcLoc("block/wall_inventory")))
         .texture("wall", ResourceLocation.fromNamespaceAndPath("createloveandwar", "block/" + baseBlock.getId().getPath()));
   }

   private ItemModelBuilder simpleBlockItem(DeferredBlock<Block> item) {
      return (ItemModelBuilder)((ItemModelBuilder)this.withExistingParent(item.getId().getPath(), ResourceLocation.withDefaultNamespace("item/generated")))
         .texture("layer0", ResourceLocation.fromNamespaceAndPath("createloveandwar", "item/" + item.getId().getPath()));
   }

   private ItemModelBuilder handHeldItem(DeferredItem<Item> item) {
      return (ItemModelBuilder)((ItemModelBuilder)this.withExistingParent(item.getId().getPath(), ResourceLocation.withDefaultNamespace("item/handheld")))
         .texture("layer0", ResourceLocation.fromNamespaceAndPath("createloveandwar", "item/" + item.getId().getPath()));
   }

   private void trimmedArmorItem(DeferredItem<Item> itemRegistryObject) {
      String MOD_ID = "createloveandwar";
      if (itemRegistryObject.get() instanceof ArmorItem armorItem) {
         trimMaterials.forEach(
            (trimMaterial, trimValue) -> {
               String armorType = switch (armorItem.getEquipmentSlot()) {
                  case HEAD -> "helmet";
                  case CHEST -> "chestplate";
                  case LEGS -> "leggings";
                  case FEET -> "boots";
                  default -> "";
               };
               String armorItemPath = "item/" + armorItem;
               String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterial.location().getPath();
               String currentTrimName = armorItemPath + "_" + trimMaterial.location().getPath() + "_trim";
               ResourceLocation armorItemResLoc = ResourceLocation.fromNamespaceAndPath("createloveandwar", armorItemPath);
               ResourceLocation trimResLoc = ResourceLocation.withDefaultNamespace(trimPath);
               ResourceLocation trimNameResLoc = ResourceLocation.fromNamespaceAndPath("createloveandwar", currentTrimName);
               this.existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");
               ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)this.getBuilder(currentTrimName)).parent(new UncheckedModelFile("item/generated")))
                     .texture("layer0", armorItemResLoc))
                  .texture("layer1", trimResLoc);
               ((ItemModelBuilder)this.withExistingParent(itemRegistryObject.getId().getPath(), this.mcLoc("item/generated")))
                  .override()
                  .model(new UncheckedModelFile(trimNameResLoc))
                  .predicate(this.mcLoc("trim_type"), trimValue)
                  .end()
                  .texture("layer0", ResourceLocation.fromNamespaceAndPath("createloveandwar", "item/" + itemRegistryObject.getId().getPath()));
            }
         );
      }
   }

   static {
      trimMaterials.put(TrimMaterials.QUARTZ, 0.1F);
      trimMaterials.put(TrimMaterials.IRON, 0.2F);
      trimMaterials.put(TrimMaterials.NETHERITE, 0.3F);
      trimMaterials.put(TrimMaterials.REDSTONE, 0.4F);
      trimMaterials.put(TrimMaterials.COPPER, 0.5F);
      trimMaterials.put(TrimMaterials.GOLD, 0.6F);
      trimMaterials.put(TrimMaterials.EMERALD, 0.7F);
      trimMaterials.put(TrimMaterials.DIAMOND, 0.8F);
      trimMaterials.put(TrimMaterials.LAPIS, 0.9F);
      trimMaterials.put(TrimMaterials.AMETHYST, 1.0F);
   }
}
