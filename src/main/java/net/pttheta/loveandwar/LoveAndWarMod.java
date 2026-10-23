package net.pttheta.loveandwar;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.pttheta.loveandwar.blocks.LAWBlockEntityTypes;
import net.pttheta.loveandwar.blocks.LAWBlocks;
import net.pttheta.loveandwar.blocks.ModBlocks;
import net.pttheta.loveandwar.blocks.ModWoodTypes;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.fluid.ModFluidTypes;
import net.pttheta.loveandwar.fluid.ModFluids;
import net.pttheta.loveandwar.gun.workbench.ModWorkbenchIngredientTypes;
import net.pttheta.loveandwar.gun.workbench.ModWorkbenchRecipes;
import net.pttheta.loveandwar.item.ModArmorMaterials;
import net.pttheta.loveandwar.item.ModCreativeModeTabs;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.ModItems;
import net.pttheta.loveandwar.loot.ModLootModifiers;
import net.pttheta.loveandwar.recipe.ModRecipes;
import net.pttheta.loveandwar.recipe.RecipeRegister;
import net.pttheta.loveandwar.screen.ModMenuTypes;
import net.pttheta.loveandwar.sound.FlansSounds;
import net.pttheta.loveandwar.sound.ModSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("createloveandwar")
public class LoveAndWarMod {
   public static final String MOD_ID = "createloveandwar";
   public static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar");
   public static final CreateRegistrate LAWREGISTRATE = CreateRegistrate.create("createloveandwar");

   public LoveAndWarMod(IEventBus modEventBus, ModContainer modContainer) {
      ModWoodTypes.init();
      ModItems.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModSounds.register(modEventBus);
      FlansSounds.register(modEventBus);
      ModFluidTypes.register(modEventBus);
      ModFluids.register(modEventBus);
      ModArmorMaterials.register(modEventBus);
      ModDataComponents.register(modEventBus);
      ModEntities.register(modEventBus);
      ModCreativeModeTabs.register(modEventBus);
      ModLootModifiers.register(modEventBus);
      ModMenuTypes.register(modEventBus);
      LAWREGISTRATE.setCreativeTab(ModCreativeModeTabs.LOVE_AND_WAR_TAB);
      LAWREGISTRATE.registerEventListeners(modEventBus);
      LAWBlocks.register();
      LAWBlockEntityTypes.register();
      ModWorkbenchIngredientTypes.register(modEventBus);
      ModWorkbenchRecipes.register(modEventBus);
      ModRecipes.register(modEventBus);
      RecipeRegister.register(modEventBus);
      modContainer.registerConfig(Type.COMMON, Config.SPEC);
      LOGGER.info("Create: Love and War loading on 1.21.1 NeoForge");
   }

   public static ResourceLocation asResource(String path) {
      return ResourceLocation.fromNamespaceAndPath("createloveandwar", path);
   }

   static {
      LAWREGISTRATE.setTooltipModifierFactory(item -> new Modifier(item, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));
   }
}
