package net.pttheta.loveandwar.item.custom;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.pttheta.loveandwar.blocks.LAWBlocks;
import net.pttheta.loveandwar.blocks.petrochem.DistillationColumnBlockEntity;

public class ThermostatItem extends Item {
   public ThermostatItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof FluidTankBlockEntity ftbe)) {
         return super.useOn(context);
      } else {
         ItemStack var12 = context.getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
         BlockPos cPos = ftbe.getController();
         int width = ftbe.getControllerBE().getWidth();
         int height = ftbe.getControllerBE().getHeight();

         for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
               for (int y = 0; y < height && (var12.getCount() != 0 || context.getPlayer().isCreative()); y++) {
                  context.getLevel().setBlock(cPos.offset(x, y, z), LAWBlocks.DISTILLATION_COLUMN.getDefaultState(), 1);
                  context.getLevel().updateNeighborsAt(cPos.offset(x, y, z), LAWBlocks.DISTILLATION_COLUMN.getDefaultState().getBlock());
               }
            }
         }

         for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
               for (int y = 0; y < height; y++) {
                  if (context.getLevel().getBlockEntity(cPos.offset(x, y, z)) instanceof DistillationColumnBlockEntity dtbe) {
                     dtbe.updateVerticalMulti();
                     dtbe.updateConnectivity();
                  }
               }
            }
         }

         if (!context.getPlayer().isCreative()) {
            var12.shrink(1);
         }

         return InteractionResult.SUCCESS;
      }
   }
}
