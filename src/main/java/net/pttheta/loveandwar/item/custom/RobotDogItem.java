package net.pttheta.loveandwar.item.custom;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.entity.custom.RobotdogEntity;

public class RobotDogItem extends Item {
   public RobotDogItem(Properties pProperties) {
      super(pProperties);
   }

   public InteractionResult useOn(UseOnContext pContext) {
      Level level = pContext.getLevel();
      if (!(level instanceof ServerLevel)) {
         return InteractionResult.SUCCESS;
      }

      ItemStack itemstack = pContext.getItemInHand();
      BlockPos blockpos = pContext.getClickedPos();
      Direction direction = pContext.getClickedFace();
      BlockState blockstate = level.getBlockState(blockpos);
      BlockPos blockpos1;
      if (blockstate.getCollisionShape(level, blockpos).isEmpty()) {
         blockpos1 = blockpos;
      } else {
         blockpos1 = blockpos.relative(direction);
      }

      EntityType<?> entitytype = (EntityType<?>)ModEntities.ROBOT_DOG.get();
      RobotdogEntity e = (RobotdogEntity)entitytype.spawn(
         (ServerLevel)level,
         itemstack,
         pContext.getPlayer(),
         blockpos1,
         MobSpawnType.SPAWN_EGG,
         true,
         !Objects.equals(blockpos, blockpos1) && direction == Direction.UP
      );
      if (e != null) {
         itemstack.shrink(1);
         e.tame(pContext.getPlayer());
         level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
      }

      return InteractionResult.CONSUME;
   }
}
