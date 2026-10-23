package net.pttheta.loveandwar.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.pttheta.loveandwar.fluid.ModFluids;
import net.pttheta.loveandwar.gun.LAWGunItems;
import net.pttheta.loveandwar.gun.WorkbenchBlockEntity;
import net.pttheta.loveandwar.item.custom.AmmoPouchItem;
import net.pttheta.loveandwar.item.custom.FuelItem;
import net.pttheta.loveandwar.item.custom.ModArmorItem;
import net.pttheta.loveandwar.item.custom.RobotDogItem;
import net.pttheta.loveandwar.item.custom.ThermostatItem;
import net.pttheta.loveandwar.util.ModTags;

public class ModItems {
   public static final Items ITEMS = DeferredRegister.createItems("createloveandwar");
   public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "createloveandwar");
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("createloveandwar");
   public static final DeferredItem<Item> SULPHUR = ITEMS.register("sulphur", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_SULPHUR = ITEMS.register("raw_sulphur", () -> new Item(new Properties()));
   public static final DeferredItem<Item> PROPELLANT = ITEMS.register("propellant", () -> new Item(new Properties()));
   public static final DeferredItem<Item> PROPELLANT_SHEET = ITEMS.register("propellant_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> PROPELLANT_MOULDED = ITEMS.register("propellant_moulded", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TUNGSTEN = ITEMS.register("tungsten", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TUNGSTEN_NUGGET = ITEMS.register("tungsten_nugget", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TUNGSTEN_SHEET = ITEMS.register("tungsten_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> NETHERITE_SHEET = ITEMS.register("netherite_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> CUT_NETHERITE_SHEET = ITEMS.register("cut_netherite_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.register("raw_tungsten", () -> new Item(new Properties()));
   public static final DeferredItem<Item> CRUSHED_TUNGSTEN = ITEMS.register("crushed_tungsten", () -> new Item(new Properties()));
   public static final DeferredItem<Item> CIGARETTE = ITEMS.register("cigarette", () -> new Item(new Properties().food(ModFoods.CIGARETTE)));
   public static final DeferredItem<Item> FORGOTTEN_GUNSMITH_PARTS = ITEMS.register(
      "forgotten_gunsmith_parts", () -> new Item(new Properties().food(ModFoods.CIGARETTE))
   );
   public static final DeferredItem<Item> TUNGSTEN_SWORD = ITEMS.register(
      "tungsten_sword",
      () -> new SwordItem(ModToolTiers.TUNGSTEN, new Properties().stacksTo(1).attributes(SwordItem.createAttributes(ModToolTiers.TUNGSTEN, 3, 1.2F)))
   );
   public static final DeferredItem<Item> TUNGSTEN_PICKAXE = ITEMS.register(
      "tungsten_pickaxe",
      () -> new PickaxeItem(ModToolTiers.TUNGSTEN, new Properties().stacksTo(1).attributes(PickaxeItem.createAttributes(ModToolTiers.TUNGSTEN, 1.0F, 1.0F)))
   );
   public static final DeferredItem<Item> TUNGSTEN_AXE = ITEMS.register(
      "tungsten_axe",
      () -> new AxeItem(ModToolTiers.TUNGSTEN, new Properties().stacksTo(1).attributes(AxeItem.createAttributes(ModToolTiers.TUNGSTEN, 5.0F, 1.0F)))
   );
   public static final DeferredItem<Item> TUNGSTEN_SHOVEL = ITEMS.register(
      "tungsten_shovel",
      () -> new ShovelItem(ModToolTiers.TUNGSTEN, new Properties().stacksTo(1).attributes(ShovelItem.createAttributes(ModToolTiers.TUNGSTEN, 4.0F, 1.0F)))
   );
   public static final DeferredItem<Item> TUNGSTEN_HOE = ITEMS.register(
      "tungsten_hoe",
      () -> new HoeItem(ModToolTiers.TUNGSTEN, new Properties().stacksTo(1).attributes(HoeItem.createAttributes(ModToolTiers.TUNGSTEN, 1.0F, 1.0F)))
   );
   public static final DeferredItem<Item> TUNGSTEN_HELMET = ITEMS.register(
      "tungsten_helmet",
      () -> new ModArmorItem(ModArmorMaterials.TUNGSTEN, Type.HELMET, new Properties().stacksTo(1).durability(Type.HELMET.getDurability(26)))
   );
   public static final DeferredItem<Item> TUNGSTEN_CHESTPLATE = ITEMS.register(
      "tungsten_chestplate",
      () -> new ArmorItem(ModArmorMaterials.TUNGSTEN, Type.CHESTPLATE, new Properties().stacksTo(1).durability(Type.CHESTPLATE.getDurability(26)))
   );
   public static final DeferredItem<Item> TUNGSTEN_LEGGINGS = ITEMS.register(
      "tungsten_leggings",
      () -> new ArmorItem(ModArmorMaterials.TUNGSTEN, Type.LEGGINGS, new Properties().stacksTo(1).durability(Type.LEGGINGS.getDurability(26)))
   );
   public static final DeferredItem<Item> TUNGSTEN_BOOTS = ITEMS.register(
      "tungsten_boots", () -> new ArmorItem(ModArmorMaterials.TUNGSTEN, Type.BOOTS, new Properties().stacksTo(1).durability(Type.BOOTS.getDurability(26)))
   );
   public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Properties()));
   public static final DeferredItem<Item> STEEL_NUGGET = ITEMS.register("steel_nugget", () -> new Item(new Properties()));
   public static final DeferredItem<Item> STEEL_SHEET = ITEMS.register("steel_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> BRASS_MIX = ITEMS.register("brass_mix", () -> new Item(new Properties()));
   public static final DeferredItem<Item> ROBOT_DOG_SPAWN_EGG = ITEMS.register("robot_dog_spawn_egg", () -> new RobotDogItem(new Properties()));
   public static final DeferredItem<Item> TURRET_UPGRADE = ITEMS.register("turret", () -> new Item(new Properties()));
   public static final DeferredItem<Item> ARMOR_UPGRADE = ITEMS.register("robot_dog_armor", () -> new Item(new Properties()));
   public static final DeferredItem<Item> SCANNER_UPGRADE = ITEMS.register("scanner", () -> new Item(new Properties()));
   public static final DeferredItem<Item> ROBOT_DOG_VALKYRIE_SKIN = ITEMS.register("robot_dog_valkyrie_skin", () -> new Item(new Properties()));
   public static final DeferredItem<Item> ROBOT_DOG_BUBBLEGUM_SKIN = ITEMS.register("robot_dog_bubblegum_skin", () -> new Item(new Properties()));
   public static final DeferredItem<Item> ROBOT_DOG_RACING_SKIN = ITEMS.register("robot_dog_racing_skin", () -> new Item(new Properties()));
   public static final DeferredItem<Item> BITUMEN_BUCKET = ITEMS.register(
      "bitumen_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_BITUMEN.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> CRUDE_OIL_BUCKET = ITEMS.register(
      "crude_oil_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_CRUDE_OIL.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> DIESEL_BUCKET = ITEMS.register(
      "diesel_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_DIESEL.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> KEROSENE_BUCKET = ITEMS.register(
      "kerosene_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_KEROSENE.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> ETHYLENE_BUCKET = ITEMS.register(
      "ethylene_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_ETHYLENE.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> POLYMER_BUCKET = ITEMS.register(
      "polymer_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_POLYMER.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> DURAPLAS_BUCKET = ITEMS.register(
      "duraplas_bucket",
      () -> new BucketItem((Fluid)ModFluids.SOURCE_DURAPLAS.get(), new Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1))
   );
   public static final DeferredItem<Item> MAGAZINE_UPGRADE = ITEMS.register("magazine_upgrade", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TAR_CLUMP = ITEMS.register("tar_clump", () -> new Item(new Properties()));
   public static final DeferredItem<Item> THERMOSTAT = ITEMS.register("thermostat", () -> new ThermostatItem(new Properties()));
   public static final DeferredItem<Item> SEEKER_UNIT = ITEMS.register("seeker_unit", () -> new Item(new Properties()));
   public static final DeferredItem<Item> MAS62_BLUEPRINT = ITEMS.register("mas62blueprint", () -> new Item(new Properties()));
   public static final DeferredItem<Item> EM2_BLUEPRINT = ITEMS.register("em2blueprint", () -> new Item(new Properties()));
   public static final DeferredItem<Item> STG57_BLUEPRINT = ITEMS.register("stg57blueprint", () -> new Item(new Properties()));
   public static final DeferredItem<Item> SLIME_RESIN = ITEMS.register("slime_resin", () -> new Item(new Properties()));
   public static final DeferredItem<Item> SLIME_RESIN_SHEET = ITEMS.register("slime_resin_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> CATALYST = ITEMS.register("catalyst", () -> new Item(new Properties()));
   public static final DeferredItem<Item> GLASS_LENS = ITEMS.register("glass_lens", () -> new Item(new Properties()));
   public static final DeferredItem<Item> OPTICAL_SCOPE = ITEMS.register("optical_scope", () -> new Item(new Properties()));
   public static final DeferredItem<Item> LAUNCHER_BARREL = ITEMS.register("launcher_barrel", () -> new Item(new Properties()));
   public static final DeferredItem<Item> POLYMER_INGOT = ITEMS.register("polymer_ingot", () -> new Item(new Properties()));
   public static final DeferredItem<Item> POLYMER_SHEET = ITEMS.register("polymer_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> DURAPLAS_INGOT = ITEMS.register("duraplas_ingot", () -> new Item(new Properties()));
   public static final DeferredItem<Item> DURAPLAS_SHEET = ITEMS.register("duraplas_sheet", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FUEL_PELLET = ITEMS.register("fuel_pellet", () -> new FuelItem(new Properties(), 2000));
   public static final DeferredItem<Item> COPPER_CUP = ITEMS.register("copper_cup", () -> new Item(new Properties()));
   public static final DeferredItem<Item> GYROJET_CASE = ITEMS.register("gyrojet_case", () -> new Item(new Properties()));
   public static final DeferredItem<Item> STEEL_CUP = ITEMS.register("steel_cup", () -> new Item(new Properties()));
   public static final DeferredItem<Item> HEAVY_RIFLE_CASING = ITEMS.register("heavy_rifle_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FILLED_HEAVY_RIFLE_CASING = ITEMS.register("filled_heavy_rifle_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> BRASS_CUP = ITEMS.register("brass_cup", () -> new Item(new Properties()));
   public static final DeferredItem<Item> WIDE_BRASS_CUP = ITEMS.register("wide_brass_cup", () -> new Item(new Properties()));
   public static final DeferredItem<Item> PISTOL_CASING = ITEMS.register("pistol_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RIFLE_CASING = ITEMS.register("rifle_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> GRENADE_CASING = ITEMS.register("grenade_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> ARTILLERY_CASING = ITEMS.register("artillery_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> SHOTGUN_CASING = ITEMS.register("shotgun_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FILLED_PISTOL_CASING = ITEMS.register("filled_pistol_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FILLED_RIFLE_CASING = ITEMS.register("filled_rifle_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FILLED_GRENADE_CASING = ITEMS.register("filled_grenade_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FILLED_ARTILLERY_CASING = ITEMS.register("filled_artillery_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> FILLED_SHOTGUN_CASING = ITEMS.register("filled_shotgun_casing", () -> new Item(new Properties()));
   public static final DeferredItem<Item> PISTOL_BULLET = LAWGunItems.Bullet(ITEMS, "createloveandwar", "pistol_bullet");
   public static final DeferredItem<Item> PISTOL_BULLET_SOFT = LAWGunItems.Bullet(ITEMS, "createloveandwar", "pistol_bullet_soft");
   public static final DeferredItem<Item> PISTOL_BULLET_AP = LAWGunItems.Bullet(ITEMS, "createloveandwar", "pistol_bullet_ap");
   public static final DeferredItem<Item> RIFLE_BULLET = LAWGunItems.Bullet(ITEMS, "createloveandwar", "rifle_bullet");
   public static final DeferredItem<Item> RIFLE_BULLET_SOFT = LAWGunItems.Bullet(ITEMS, "createloveandwar", "rifle_bullet_soft");
   public static final DeferredItem<Item> RIFLE_BULLET_AP = LAWGunItems.Bullet(ITEMS, "createloveandwar", "rifle_bullet_ap");
   public static final DeferredItem<Item> HEAVY_RIFLE_BULLET = LAWGunItems.Bullet(ITEMS, "createloveandwar", "heavy_rifle_bullet");
   public static final DeferredItem<Item> GYROJET_BASIC = LAWGunItems.Bullet(ITEMS, "createloveandwar", "gyrojet_basic");
   public static final DeferredItem<Item> GYROJET_SEEKER = LAWGunItems.Bullet(ITEMS, "createloveandwar", "gyrojet_heatseeker");
   public static final DeferredItem<Item> GYROJET_SEEKER_TOP = LAWGunItems.Bullet(ITEMS, "createloveandwar", "gyrojet_heatseeker_top");
   public static final DeferredItem<Item> GYROJET_PEN = LAWGunItems.Bullet(ITEMS, "createloveandwar", "gyrojet_pen");
   public static final DeferredItem<Item> GYROJET_HE = LAWGunItems.Bullet(ITEMS, "createloveandwar", "gyrojet_he");
   public static final DeferredItem<Item> CASELESS_BULLET = LAWGunItems.Bullet(ITEMS, "createloveandwar", "caseless_bullet");
   public static final DeferredItem<Item> CASELESS_BULLET_AP = LAWGunItems.Bullet(ITEMS, "createloveandwar", "caseless_bullet_ap");
   public static final DeferredItem<Item> CASELESS_BULLET_SOFT = LAWGunItems.Bullet(ITEMS, "createloveandwar", "caseless_bullet_soft");
   public static final DeferredItem<Item> GRENADE = ITEMS.register("grenade_he", () -> new Item(new Properties()));
   public static final DeferredItem<Item> GRENADE_ROUND = LAWGunItems.Bullet(ITEMS, "createloveandwar", "grenade_round_he");
   public static final DeferredItem<Item> GRENADE_ROUND_BUCKSHOT = LAWGunItems.Bullet(ITEMS, "createloveandwar", "grenade_round_buckshot");
   public static final DeferredItem<Item> BLACK_POWDER_CARTRIDGE = LAWGunItems.Bullet(ITEMS, "createloveandwar", "black_powder_cartridge");
   public static final DeferredItem<Item> FLECHETTES = ITEMS.register("flechettes", () -> new Item(new Properties()));
   public static final DeferredItem<Item> BALL_BEARINGS = ITEMS.register("ball_bearings", () -> new Item(new Properties()));
   public static final DeferredItem<Item> BUCKSHOT_SHELL = LAWGunItems.Bullet(ITEMS, "createloveandwar", "buckshot_shell");
   public static final DeferredItem<Item> SLUG_SHELL = LAWGunItems.Bullet(ITEMS, "createloveandwar", "slug_shell");
   public static final DeferredItem<Item> FLECHETTE_SHELL = LAWGunItems.Bullet(ITEMS, "createloveandwar", "flechette_shell");
   public static final DeferredItem<Item> EXPLOSIVE_SHELL = LAWGunItems.Bullet(ITEMS, "createloveandwar", "explosive_shell");
   public static final DeferredItem<Item> ARTILLERY_SHELL = LAWGunItems.Bullet(ITEMS, "createloveandwar", "artillery_shell");
   public static final DeferredItem<Item> ARTILLERY_SHELL_AP = LAWGunItems.Bullet(ITEMS, "createloveandwar", "artillery_shell_ap");
   public static final DeferredItem<Item> ARTILLERY_SHELL_HE = LAWGunItems.Bullet(ITEMS, "createloveandwar", "artillery_shell_he");
   public static final DeferredItem<Item> TEMPLATE_UPPER_RECEIVER = ITEMS.register("template_upper_receiver", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_LOWER_RECEIVER = ITEMS.register("template_lower_receiver", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_STOCK = ITEMS.register("template_stock", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_BARREL = ITEMS.register("template_barrel", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_GRIP = ITEMS.register("template_grip", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_HANDGUARD = ITEMS.register("template_handguard", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_BOLT_ACTION = ITEMS.register("template_bolt_action", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_REVOLVER_ACTION = ITEMS.register("template_revolver_action", () -> new Item(new Properties()));
   public static final DeferredItem<Item> TEMPLATE_PROPELLANT_BLOCK = ITEMS.register("template_propellant_block", () -> new Item(new Properties()));
   public static final DeferredItem<Item> AMMO_POUCH = ITEMS.register(
      "ammopouch", () -> new AmmoPouchItem(new Properties().stacksTo(1), 64, ModTags.Items.PAPER_CARTRIDGE)
   );
   public static final DeferredItem<Item> AMMO_POUCH_PISTOL = ITEMS.register(
      "ammopouch_pistol", () -> new AmmoPouchItem(new Properties().stacksTo(1), 192, ModTags.Items.PISTOL_BULLET)
   );
   public static final DeferredItem<Item> AMMO_POUCH_RIFLE = ITEMS.register(
      "ammopouch_rifle", () -> new AmmoPouchItem(new Properties().stacksTo(1), 96, ModTags.Items.RIFLE_BULLET)
   );
   public static final DeferredItem<Item> AMMO_POUCH_SHOTGUN = ITEMS.register(
      "ammopouch_shotgun", () -> new AmmoPouchItem(new Properties().stacksTo(1), 64, ModTags.Items.SHOTGUN_SHELL)
   );
   public static final DeferredItem<Item> UPPER_RECEIVER_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_iron");
   public static final DeferredItem<Item> LOWER_RECEIVER_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_iron");
   public static final DeferredItem<Item> STOCK_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_iron");
   public static final DeferredItem<Item> BARREL_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_iron");
   public static final DeferredItem<Item> GRIP_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_iron");
   public static final DeferredItem<Item> HANDGUARD_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_iron");
   public static final DeferredItem<Item> REVOLVER_ACTION_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_iron");
   public static final DeferredItem<Item> BOLT_ACTION_IRON = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_iron");
   public static final DeferredItem<Item> INCOMPLETE_IRON_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_iron_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_brass");
   public static final DeferredItem<Item> LOWER_RECEIVER_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_brass");
   public static final DeferredItem<Item> STOCK_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_brass");
   public static final DeferredItem<Item> BARREL_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_brass");
   public static final DeferredItem<Item> GRIP_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_brass");
   public static final DeferredItem<Item> HANDGUARD_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_brass");
   public static final DeferredItem<Item> REVOLVER_ACTION_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_brass");
   public static final DeferredItem<Item> BOLT_ACTION_BRASS = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_brass");
   public static final DeferredItem<Item> INCOMPLETE_BRASS_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_brass_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_tungsten");
   public static final DeferredItem<Item> LOWER_RECEIVER_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_tungsten");
   public static final DeferredItem<Item> STOCK_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_tungsten");
   public static final DeferredItem<Item> BARREL_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_tungsten");
   public static final DeferredItem<Item> GRIP_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_tungsten");
   public static final DeferredItem<Item> HANDGUARD_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_tungsten");
   public static final DeferredItem<Item> REVOLVER_ACTION_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_tungsten");
   public static final DeferredItem<Item> BOLT_ACTION_TUNGSTEN = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_tungsten");
   public static final DeferredItem<Item> INCOMPLETE_TUNGSTEN_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_tungsten_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_sturdy");
   public static final DeferredItem<Item> LOWER_RECEIVER_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_sturdy");
   public static final DeferredItem<Item> STOCK_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_sturdy");
   public static final DeferredItem<Item> BARREL_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_sturdy");
   public static final DeferredItem<Item> GRIP_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_sturdy");
   public static final DeferredItem<Item> HANDGUARD_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_sturdy");
   public static final DeferredItem<Item> REVOLVER_ACTION_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_sturdy");
   public static final DeferredItem<Item> BOLT_ACTION_STURDY = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_sturdy");
   public static final DeferredItem<Item> INCOMPLETE_STURDY_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_sturdy_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_netherite");
   public static final DeferredItem<Item> LOWER_RECEIVER_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_netherite");
   public static final DeferredItem<Item> STOCK_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_netherite");
   public static final DeferredItem<Item> BARREL_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_netherite");
   public static final DeferredItem<Item> GRIP_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_netherite");
   public static final DeferredItem<Item> HANDGUARD_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_netherite");
   public static final DeferredItem<Item> REVOLVER_ACTION_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_netherite");
   public static final DeferredItem<Item> BOLT_ACTION_NETHERITE = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_netherite");
   public static final DeferredItem<Item> INCOMPLETE_NETHERITE_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_netherite_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_resin");
   public static final DeferredItem<Item> LOWER_RECEIVER_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_resin");
   public static final DeferredItem<Item> STOCK_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_resin");
   public static final DeferredItem<Item> BARREL_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_resin");
   public static final DeferredItem<Item> GRIP_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_resin");
   public static final DeferredItem<Item> HANDGUARD_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_resin");
   public static final DeferredItem<Item> REVOLVER_ACTION_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_resin");
   public static final DeferredItem<Item> BOLT_ACTION_RESIN = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_resin");
   public static final DeferredItem<Item> INCOMPLETE_RESIN_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_resin_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_polymer");
   public static final DeferredItem<Item> LOWER_RECEIVER_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_polymer");
   public static final DeferredItem<Item> STOCK_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_polymer");
   public static final DeferredItem<Item> BARREL_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_polymer");
   public static final DeferredItem<Item> GRIP_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_polymer");
   public static final DeferredItem<Item> HANDGUARD_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_polymer");
   public static final DeferredItem<Item> REVOLVER_ACTION_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_polymer");
   public static final DeferredItem<Item> BOLT_ACTION_POLYMER = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_polymer");
   public static final DeferredItem<Item> INCOMPLETE_POLYMER_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_polymer_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_duraplas");
   public static final DeferredItem<Item> LOWER_RECEIVER_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_duraplas");
   public static final DeferredItem<Item> STOCK_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_duraplas");
   public static final DeferredItem<Item> BARREL_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_duraplas");
   public static final DeferredItem<Item> GRIP_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_duraplas");
   public static final DeferredItem<Item> HANDGUARD_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_duraplas");
   public static final DeferredItem<Item> REVOLVER_ACTION_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_duraplas");
   public static final DeferredItem<Item> BOLT_ACTION_DURAPLAS = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_duraplas");
   public static final DeferredItem<Item> INCOMPLETE_DURAPLAS_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_duraplas_receiver");
   public static final DeferredItem<Item> UPPER_RECEIVER_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "upper_receiver_steel");
   public static final DeferredItem<Item> LOWER_RECEIVER_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "lower_receiver_steel");
   public static final DeferredItem<Item> STOCK_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_steel");
   public static final DeferredItem<Item> BARREL_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "barrel_steel");
   public static final DeferredItem<Item> GRIP_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_steel");
   public static final DeferredItem<Item> HANDGUARD_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_steel");
   public static final DeferredItem<Item> REVOLVER_ACTION_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "revolver_action_steel");
   public static final DeferredItem<Item> BOLT_ACTION_STEEL = LAWGunItems.Part(ITEMS, "createloveandwar", "bolt_action_steel");
   public static final DeferredItem<Item> INCOMPLETE_STEEL_RECEIVER = LAWGunItems.Part(ITEMS, "createloveandwar", "incomplete_steel_receiver");
   public static final DeferredItem<Item> STOCK_WOOD = LAWGunItems.Part(ITEMS, "createloveandwar", "stock_wood");
   public static final DeferredItem<Item> GRIP_WOOD = LAWGunItems.Part(ITEMS, "createloveandwar", "grip_wood");
   public static final DeferredItem<Item> HANDGUARD_WOOD = LAWGunItems.Part(ITEMS, "createloveandwar", "handguard_wood");
   public static final DeferredItem<Item> REDSTONE_CIRCUIT = LAWGunItems.Part(ITEMS, "createloveandwar", "redstone_circuit");
   public static final DeferredItem<Item> PRECISION_CIRCUIT = LAWGunItems.Part(ITEMS, "createloveandwar", "precision_circuit");
   public static final DeferredItem<Item> QUANTUM_CIRCUIT = LAWGunItems.Part(ITEMS, "createloveandwar", "quantum_circuit");
   public static final DeferredItem<Item> LEBEL = LAWGunItems.Gun(ITEMS, "createloveandwar", "lebel");
   public static final DeferredItem<Item> LEBEL_SCOPE = LAWGunItems.Gun(ITEMS, "createloveandwar", "lebel_scope");
   public static final DeferredItem<Item> BERTHIER = LAWGunItems.Gun(ITEMS, "createloveandwar", "berthier");
   public static final DeferredItem<Item> MODEL1887 = LAWGunItems.Gun(ITEMS, "createloveandwar", "1887");
   public static final DeferredItem<Item> ALOF = LAWGunItems.Gun(ITEMS, "createloveandwar", "alof");
   public static final DeferredItem<Item> WALKER = LAWGunItems.Gun(ITEMS, "createloveandwar", "walker");
   public static final DeferredItem<Item> NAVY_CARBINE = LAWGunItems.Gun(ITEMS, "createloveandwar", "navycarbine");
   public static final DeferredItem<Item> PERRIN = LAWGunItems.Gun(ITEMS, "createloveandwar", "perrin");
   public static final DeferredItem<Item> MARTINI = LAWGunItems.Gun(ITEMS, "createloveandwar", "martini_henry");
   public static final DeferredItem<Item> GREENER = LAWGunItems.Gun(ITEMS, "createloveandwar", "greener");
   public static final DeferredItem<Item> MARTINI_CARBINE = LAWGunItems.Gun(ITEMS, "createloveandwar", "martinicarbine");
   public static final DeferredItem<Item> RSC17 = LAWGunItems.Gun(ITEMS, "createloveandwar", "rsc17");
   public static final DeferredItem<Item> WZ38 = LAWGunItems.Gun(ITEMS, "createloveandwar", "wz38");
   public static final DeferredItem<Item> MARS = LAWGunItems.Gun(ITEMS, "createloveandwar", "mars");
   public static final DeferredItem<Item> GYROJET = LAWGunItems.Gun(ITEMS, "createloveandwar", "gyrojet");
   public static final DeferredItem<Item> GYROJET_CARBINE = LAWGunItems.Gun(ITEMS, "createloveandwar", "gyrojet_carbine");
   public static final DeferredItem<Item> KRAG = LAWGunItems.Gun(ITEMS, "createloveandwar", "krag");
   public static final DeferredItem<Item> KRAG_CLEAN = LAWGunItems.Gun(ITEMS, "createloveandwar", "krag_clean");
   public static final DeferredItem<Item> CHAUCHAT = LAWGunItems.Gun(ITEMS, "createloveandwar", "chauchat");
   public static final DeferredItem<Item> MONITOR = LAWGunItems.Gun(ITEMS, "createloveandwar", "monitor");
   public static final DeferredItem<Item> THOMPSON = LAWGunItems.Gun(ITEMS, "createloveandwar", "thompson");
   public static final DeferredItem<Item> MAT49 = LAWGunItems.Gun(ITEMS, "createloveandwar", "mat49");
   public static final DeferredItem<Item> MAT4954 = LAWGunItems.Gun(ITEMS, "createloveandwar", "mat4954");
   public static final DeferredItem<Item> SJOGREN = LAWGunItems.Gun(ITEMS, "createloveandwar", "sjogren");
   public static final DeferredItem<Item> WF54 = LAWGunItems.Gun(ITEMS, "createloveandwar", "wf54");
   public static final DeferredItem<Item> EM2 = LAWGunItems.Gun(ITEMS, "createloveandwar", "em2");
   public static final DeferredItem<Item> MAS62 = LAWGunItems.Gun(ITEMS, "createloveandwar", "mas62");
   public static final DeferredItem<Item> STG57 = LAWGunItems.Gun(ITEMS, "createloveandwar", "stg57");
   public static final DeferredItem<Item> G11K2 = LAWGunItems.Gun(ITEMS, "createloveandwar", "g11k2");
   public static final DeferredItem<Item> SA81 = LAWGunItems.Gun(ITEMS, "createloveandwar", "sa81");
   public static final DeferredItem<Item> AAI_ACR = LAWGunItems.Gun(ITEMS, "createloveandwar", "aai_acr");
   public static final DeferredItem<Item> CAWS = LAWGunItems.Gun(ITEMS, "createloveandwar", "caws");
   public static final DeferredItem<Item> ARM_PISTOL = LAWGunItems.Gun(ITEMS, "createloveandwar", "arm_pistol");
   public static final DeferredItem<Item> VETTERLI = LAWGunItems.Gun(ITEMS, "createloveandwar", "vetterli");
   public static final DeferredItem<Item> VETTERLIVITALI = LAWGunItems.Gun(ITEMS, "createloveandwar", "vetterlivitali");
   public static final DeferredItem<Item> CHASSEPOT = LAWGunItems.Gun(ITEMS, "createloveandwar", "chassepot");
   public static final DeferredItem<Item> DREYSE = LAWGunItems.Gun(ITEMS, "createloveandwar", "dreyse");
   public static final DeferredItem<Item> DREYSEPISTOL = LAWGunItems.Gun(ITEMS, "createloveandwar", "dreysepistol");
   public static final DeferredItem<Item> P53_ENFIELD = LAWGunItems.Gun(ITEMS, "createloveandwar", "p53_enfield");
   public static final DeferredItem<Item> P61_ENFIELD = LAWGunItems.Gun(ITEMS, "createloveandwar", "p61_enfield");
   public static final DeferredItem<Item> SNIDER_ENFIELD = LAWGunItems.Gun(ITEMS, "createloveandwar", "snider_enfield");
   public static final DeferredItem<Item> SNIDER_ENFIELD_CARBINE = LAWGunItems.Gun(ITEMS, "createloveandwar", "snider_enfield_carbine");
   public static final DeferredItem<Item> MARTINI_REPEATER = LAWGunItems.Gun(ITEMS, "createloveandwar", "martini_repeater");
   public static final DeferredItem<Item> PVG43 = LAWGunItems.Gun(ITEMS, "createloveandwar", "pvg43");
   public static final DeferredItem<Item> LEE_METFORD = LAWGunItems.Gun(ITEMS, "createloveandwar", "leemetford");
   public static final DeferredItem<Item> MARTINILAUNCHER = LAWGunItems.Gun(ITEMS, "createloveandwar", "martinilauncher");
   public static final DeferredItem<Item> TANKGEWEHR = LAWGunItems.Gun(ITEMS, "createloveandwar", "tankgewehr");
   public static final DeferredItem<Item> WZ35 = LAWGunItems.Gun(ITEMS, "createloveandwar", "wz35");
   public static final DeferredItem<Item> GEPARD_M3 = LAWGunItems.Gun(ITEMS, "createloveandwar", "gepard_m3");
   public static final DeferredItem<Item> ATTACHMENT_WOOD_STOCK = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_wood_stock");
   public static final DeferredItem<Item> ATTACHMENT_IMPACT_STOCK = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_impact_stock");
   public static final DeferredItem<Item> ATTACHMENT_RED_DOT = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_red_dot");
   public static final DeferredItem<Item> ATTACHMENT_TELESCOPIC_SIGHT = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_telescopic_sight");
   public static final DeferredItem<Item> ATTACHMENT_LASER_SCOPE = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_laser_scope");
   public static final DeferredItem<Item> ATTACHMENT_EXTENDED_BARREL = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_extended_barrel");
   public static final DeferredItem<Item> ATTACHMENT_SILENCER = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_silencer");
   public static final DeferredItem<Item> ATTACHMENT_TANK_BRAKE = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_tank_brake");
   public static final DeferredItem<Item> ATTACHMENT_WOOD_GRIP = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_wood_grip");
   public static final DeferredItem<Item> ATTACHMENT_POLYMER_GRIP = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_polymer_grip");
   public static final DeferredItem<Item> ATTACHMENT_SUIT_SIGHT = LAWGunItems.Attachment(ITEMS, "createloveandwar", "attachment_suit_sight");
   public static final DeferredBlock<Block> WORKBENCH_BLOCK_LAW = LAWGunItems.Workbench_Block(BLOCKS, "createloveandwar", "law_workbench");
   public static final DeferredItem<Item> WORKBENCH_ITEM_LAW = LAWGunItems.Workbench_Item(ITEMS, "createloveandwar", "law_workbench", WORKBENCH_BLOCK_LAW);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_TILE_ENTITY_LAW = LAWGunItems.Workbench_TileEntityType(
      TILE_ENTITIES, "createloveandwar", "law_workbench"
   );
   public static final DeferredBlock<Block> WORKBENCH_BLOCK_ADV = LAWGunItems.Workbench_Block(BLOCKS, "createloveandwar", "advanced_workbench");
   public static final DeferredItem<Item> WORKBENCH_ITEM_ADV = LAWGunItems.Workbench_Item(ITEMS, "createloveandwar", "advanced_workbench", WORKBENCH_BLOCK_ADV);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_TILE_ENTITY_ADV = LAWGunItems.Workbench_TileEntityType(
      TILE_ENTITIES, "createloveandwar", "advanced_workbench"
   );
   public static final DeferredBlock<Block> WORKBENCH_BLOCK_EXP = LAWGunItems.Workbench_Block(BLOCKS, "createloveandwar", "expert_workbench");
   public static final DeferredItem<Item> WORKBENCH_ITEM_EXP = LAWGunItems.Workbench_Item(ITEMS, "createloveandwar", "expert_workbench", WORKBENCH_BLOCK_EXP);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_TILE_ENTITY_EXP = LAWGunItems.Workbench_TileEntityType(
      TILE_ENTITIES, "createloveandwar", "expert_workbench"
   );
   public static final DeferredBlock<Block> WORKBENCH_BLOCK_EM2 = LAWGunItems.Workbench_Block(BLOCKS, "createloveandwar", "em2_workbench");
   public static final DeferredItem<Item> WORKBENCH_ITEM_EM2 = LAWGunItems.Workbench_Item(ITEMS, "createloveandwar", "em2_workbench", WORKBENCH_BLOCK_EM2);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_TILE_ENTITY_EM2 = LAWGunItems.Workbench_TileEntityType(
      TILE_ENTITIES, "createloveandwar", "em2_workbench"
   );
   public static final DeferredBlock<Block> WORKBENCH_BLOCK_MAS62 = LAWGunItems.Workbench_Block(BLOCKS, "createloveandwar", "mas62_workbench");
   public static final DeferredItem<Item> WORKBENCH_ITEM_MAS62 = LAWGunItems.Workbench_Item(ITEMS, "createloveandwar", "mas62_workbench", WORKBENCH_BLOCK_MAS62);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_TILE_ENTITY_MAS62 = LAWGunItems.Workbench_TileEntityType(
      TILE_ENTITIES, "createloveandwar", "mas62_workbench"
   );
   public static final DeferredBlock<Block> WORKBENCH_BLOCK_STG57 = LAWGunItems.Workbench_Block(BLOCKS, "createloveandwar", "stg57_workbench");
   public static final DeferredItem<Item> WORKBENCH_ITEM_STG57 = LAWGunItems.Workbench_Item(ITEMS, "createloveandwar", "stg57_workbench", WORKBENCH_BLOCK_STG57);
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_TILE_ENTITY_STG57 = LAWGunItems.Workbench_TileEntityType(
      TILE_ENTITIES, "createloveandwar", "stg57_workbench"
   );

   public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
      BLOCKS.register(eventBus);
      TILE_ENTITIES.register(eventBus);
   }
}
