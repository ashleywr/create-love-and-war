package net.pttheta.loveandwar.fluid;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import org.joml.Vector3f;

public class ModFluidTypes {
   public static final ResourceLocation WATER_STILL_RL = ResourceLocation.withDefaultNamespace("block/water_still");
   public static final ResourceLocation WATER_FLOWING_RL = ResourceLocation.withDefaultNamespace("block/water_flow");
   public static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace("misc/in_oil");
   public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(Keys.FLUID_TYPES, "createloveandwar");
   public static final DeferredHolder<FluidType, FluidType> BITUMEN_FLUID_TYPE = register(
      "bitumen_fluid", 2, 2, -12505297, new Vector3f(0.25490198F, 0.18431373F, 0.18431373F)
   );
   public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = register(
      "crude_oil", 5, 5, -14673896, new Vector3f(0.1254902F, 0.09411765F, 0.09411765F)
   );
   public static final DeferredHolder<FluidType, FluidType> DIESEL_TYPE = register(
      "diesel", 10, 10, -11654357, new Vector3f(0.30588236F, 0.16862746F, 0.16862746F)
   );
   public static final DeferredHolder<FluidType, FluidType> KEROSENE_TYPE = register(
      "kerosene", 15, 15, -10068929, new Vector3f(0.4F, 0.36078432F, 0.24705882F)
   );
   public static final DeferredHolder<FluidType, FluidType> ETHYLENE_TYPE = register(
      "ethylene", 5, 15, -4670822, new Vector3f(0.72156864F, 0.7294118F, 0.6039216F)
   );
   public static final DeferredHolder<FluidType, FluidType> POLYMER_TYPE = register(
      "polymer", 5, 15, -4547920, new Vector3f(0.7294118F, 0.6039216F, 0.6901961F)
   );
   public static final DeferredHolder<FluidType, FluidType> DURAPLAS_TYPE = register(
      "duraplas", 5, 15, -7769961, new Vector3f(0.5372549F, 0.4392157F, 0.5921569F)
   );

   private static DeferredHolder<FluidType, FluidType> register(String name, int density, int viscosity, int tintColor, Vector3f fogColor) {
      return FLUID_TYPES.register(
         name,
         () -> new BaseFluidType(
            WATER_STILL_RL,
            WATER_FLOWING_RL,
            OVERLAY,
            tintColor,
            fogColor,
            Properties.create().lightLevel(2).density(density).viscosity(viscosity).sound(SoundAction.get("drink"), SoundEvents.HONEY_DRINK)
         )
      );
   }

   public static List<DeferredHolder<FluidType, FluidType>> all() {
      return List.of(BITUMEN_FLUID_TYPE, CRUDE_OIL_TYPE, DIESEL_TYPE, KEROSENE_TYPE, ETHYLENE_TYPE, POLYMER_TYPE, DURAPLAS_TYPE);
   }

   public static void register(IEventBus eventBus) {
      FLUID_TYPES.register(eventBus);
   }
}
