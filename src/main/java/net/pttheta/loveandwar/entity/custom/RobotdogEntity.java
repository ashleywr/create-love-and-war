package net.pttheta.loveandwar.entity.custom;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.entity.ai.RobotDogAttackGoal;
import net.pttheta.loveandwar.item.ModItems;
import net.pttheta.loveandwar.sound.ModSounds;
import net.pttheta.loveandwar.util.ModTags;
import org.jetbrains.annotations.Nullable;

public class RobotdogEntity extends TamableAnimal implements NeutralMob, ContainerListener {
   public static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_ID_CHEST = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_ID_TURRET = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Integer> PAINTJOB = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_ID_ARMOR = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_ID_SCANNER = SynchedEntityData.defineId(RobotdogEntity.class, EntityDataSerializers.BOOLEAN);
   public static final TagKey<Item> bulletTag = ModTags.Items.PISTOL_BULLET;
   public static final TagKey<Item> casingTag = TagKey.create(Registries.ITEM, LoveAndWarMod.asResource("bullet_casing"));
   public SimpleContainer dogInventory;
   public float scannerAngle = 0.0F;
   public final AnimationState idleAnimationState = new AnimationState();
   private int idleAnimationTimeout = 0;
   public final AnimationState attackAnimationState = new AnimationState();
   public int attackAnimationTimeout = 0;
   public int projectileAttackTick = 0;
   public final AnimationState sitAnimationState = new AnimationState();
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   @Nullable
   private UUID persistentAngerTarget;
   protected SimpleContainer inventory;

   public RobotdogEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.initDogInventory();
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      } else {
         this.projectileAttackTick--;
      }

      this.scannerAngle += 0.5F;
   }

   private void setupAnimationStates() {
      if (this.idleAnimationTimeout <= 0 && !this.isInSittingPose()) {
         this.idleAnimationTimeout = this.random.nextInt(30) + 60;
         if (!this.idleAnimationState.isStarted()) {
            this.idleAnimationState.start(this.tickCount);
         }
      } else {
         this.idleAnimationTimeout--;
      }

      if (this.isAttacking() && this.attackAnimationTimeout <= 0) {
         this.idleAnimationState.stop();
         this.attackAnimationTimeout = 16;
         this.attackAnimationState.start(this.tickCount);
         this.setSpeed(0.0F);
      } else {
         this.attackAnimationTimeout--;
      }

      if (!this.isAttacking()) {
         this.attackAnimationState.stop();
         this.setSpeed(1.0F);
      }

      if (this.isInSittingPose()) {
         this.sitAnimationState.start(this.tickCount);
         this.idleAnimationState.stop();
      } else {
         this.sitAnimationState.stop();
      }
   }

   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide) {
         this.updatePersistentAnger((ServerLevel)this.level(), true);
      }
   }

   protected void updateWalkAnimation(float pPartialTick) {
      float f;
      if (this.getPose() == Pose.STANDING) {
         f = Math.min(pPartialTick * 6.0F, 1.0F);
      } else {
         f = 0.0F;
      }

      this.walkAnimation.update(f, 0.25F);
   }

   public void setAttacking(boolean attacking) {
      this.entityData.set(ATTACKING, attacking);
      this.attackAnimationTimeout = 16;
      this.attackAnimationState.start(0);
   }

   public boolean isAttacking() {
      return (Boolean)this.entityData.get(ATTACKING);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(DATA_ID_FLAGS, (byte)0);
      builder.define(DATA_REMAINING_ANGER_TIME, 0);
      builder.define(DATA_ID_CHEST, false);
      builder.define(DATA_ID_TURRET, false);
      builder.define(PAINTJOB, 0);
      builder.define(DATA_ID_ARMOR, false);
      builder.define(DATA_ID_SCANNER, false);
   }

   public void openGUI(Player playerEntity) {
      if (!this.level().isClientSide && !this.hasPassenger(playerEntity)) {
         ((ServerPlayer)playerEntity).openMenu(new MenuProvider() {
            public AbstractContainerMenu createMenu(int p_createMenu_1_, Inventory p_createMenu_2_, Player p_createMenu_3_) {
               return new DispenserMenu(p_createMenu_1_, p_createMenu_2_, RobotdogEntity.this.dogInventory);
            }

            public Component getDisplayName() {
               return Component.literal("Storage");
            }
         });
      }
   }

   private void initDogInventory() {
      SimpleContainer animalchest = this.dogInventory;
      this.dogInventory = new SimpleContainer(9) {
         public void stopOpen(Player player) {
            RobotdogEntity.this.resetSlots();
         }

         public boolean stillValid(Player player) {
            return RobotdogEntity.this.isAlive() && RobotdogEntity.this.portalProcess == null;
         }
      };
      this.dogInventory.addListener(this);
      if (animalchest != null) {
         int i = Math.min(animalchest.getContainerSize(), this.dogInventory.getContainerSize());

         for (int j = 0; j < i; j++) {
            ItemStack itemstack = animalchest.getItem(j);
            if (!itemstack.isEmpty()) {
               this.dogInventory.setItem(j, itemstack.copy());
            }
         }

         this.resetSlots();
      }
   }

   public void resetSlots() {
      if (!this.level().isClientSide) {
         for (int i = 0; i < this.dogInventory.getContainerSize(); i++) {
            ItemStack stack = this.dogInventory.getItem(i);
            if (!stack.isEmpty()) {
            }
         }

         this.updateClientInventory();
      }
   }

   private void updateClientInventory() {
      if (!this.level().isClientSide) {
         int i = 0;

         while (i < 9) {
            i++;
         }
      }
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new RobotDogAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 2.0F));
      this.goalSelector.addGoal(1, new TemptGoal(this, 1.2, Ingredient.of(new ItemLike[]{Items.REDSTONE_BLOCK}), false));
      this.goalSelector.addGoal(3, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(4, new RobotdogEntity.SearchForItemsGoal());
      this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.1));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, FlyingMob.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, AbstractSkeleton.class, false));
   }

   public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.4)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.1F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.5)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
      return null;
   }

   public boolean isFood(ItemStack pStack) {
      Item item = pStack.getItem();
      return item == Items.IRON_INGOT;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      if (this.isAngry()) {
         return (SoundEvent)ModSounds.ROBOT_DOG_AMBIENT.get();
      } else if (this.random.nextInt(3) != 0) {
         return SoundEvents.WOLF_AMBIENT;
      } else {
         return this.isTame() && this.getHealth() < 10.0F ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_PANT;
      }
   }

   protected void playStepSound(BlockPos pPos, BlockState pBlock) {
      this.playSound((SoundEvent)ModSounds.SERVO.get(), 0.08F, 1.0F);
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.IRON_GOLEM_DAMAGE;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      Item item = itemstack.getItem();
      if (this.level().isClientSide) {
         boolean flag = this.isOwnedBy(pPlayer) || this.isTame() || itemstack.is(Items.BONE) && !this.isTame() && !this.isAngry();
         return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
      }

      if (this.isTame()) {
         if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
            FoodProperties food = (FoodProperties)itemstack.get(DataComponents.FOOD);
            if (food != null) {
               this.heal(food.nutrition());
            }

            if (!pPlayer.getAbilities().instabuild) {
               itemstack.shrink(1);
            }

            this.gameEvent(GameEvent.EAT, this);
            return InteractionResult.SUCCESS;
         } else {
            if (!this.hasChest() && itemstack.is(Items.CHEST)) {
               this.equipChest(pPlayer, itemstack);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (this.hasChest() && !this.hasTurret() && itemstack.is((Item)ModItems.TURRET_UPGRADE.get())) {
               this.equipTurret(pPlayer, itemstack);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (!this.hasArmor() && itemstack.is((Item)ModItems.ARMOR_UPGRADE.get())) {
               this.setArmor(true);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (this.hasChest() && !this.hasScanner() && itemstack.is((Item)ModItems.SCANNER_UPGRADE.get())) {
               this.setScanner(true);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (!(item instanceof DyeItem)
               && item != ModItems.ROBOT_DOG_VALKYRIE_SKIN.get()
               && item != ModItems.ROBOT_DOG_BUBBLEGUM_SKIN.get()
               && item != ModItems.ROBOT_DOG_RACING_SKIN.get()) {
               InteractionResult interactionresult = super.mobInteract(pPlayer, pHand);
               if (interactionresult != InteractionResult.SUCCESS && pPlayer.isShiftKeyDown() && this.isOwnedBy(pPlayer) && this.hasChest()) {
                  if (!this.isBaby()) {
                     this.openGUI(pPlayer);
                     this.ejectPassengers();
                  }

                  return InteractionResult.SUCCESS;
               } else if ((!interactionresult.consumesAction() || this.isBaby()) && this.isOwnedBy(pPlayer)) {
                  this.setOrderedToSit(!this.isOrderedToSit());
                  this.jumping = false;
                  this.navigation.stop();
                  this.setTarget((LivingEntity)null);
                  return InteractionResult.SUCCESS;
               } else {
                  return interactionresult;
               }
            } else {
               this.SetPaintFromDye(item);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
         }
      } else if (itemstack.is(Items.REDSTONE_BLOCK) && !this.isAngry()) {
         if (!pPlayer.getAbilities().instabuild) {
            itemstack.shrink(1);
         }

         if (this.random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, pPlayer)) {
            this.tame(pPlayer);
            this.navigation.stop();
            this.setTarget((LivingEntity)null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
         } else {
            this.level().broadcastEntityEvent(this, (byte)6);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(pPlayer, pHand);
      }
   }

   public void SetPaintFromDye(Item item) {
      if (item == Items.YELLOW_DYE) {
         this.setPaintjob(0);
      } else if (item == Items.RED_DYE) {
         this.setPaintjob(1);
      } else if (item == Items.WHITE_DYE) {
         this.setPaintjob(2);
      } else if (item == Items.LIGHT_GRAY_DYE) {
         this.setPaintjob(3);
      } else if (item == Items.GRAY_DYE) {
         this.setPaintjob(4);
      } else if (item == Items.BLACK_DYE) {
         this.setPaintjob(5);
      } else if (item == Items.BROWN_DYE) {
         this.setPaintjob(6);
      } else if (item == Items.ORANGE_DYE) {
         this.setPaintjob(7);
      } else if (item == Items.LIME_DYE) {
         this.setPaintjob(8);
      } else if (item == Items.GREEN_DYE) {
         this.setPaintjob(9);
      } else if (item == Items.CYAN_DYE) {
         this.setPaintjob(10);
      } else if (item == Items.LIGHT_BLUE_DYE) {
         this.setPaintjob(11);
      } else if (item == Items.BLUE_DYE) {
         this.setPaintjob(12);
      } else if (item == Items.PURPLE_DYE) {
         this.setPaintjob(13);
      } else if (item == Items.MAGENTA_DYE) {
         this.setPaintjob(14);
      } else if (item == Items.PINK_DYE) {
         this.setPaintjob(15);
      } else if (item == ModItems.ROBOT_DOG_VALKYRIE_SKIN.get()) {
         this.setPaintjob(16);
      } else if (item == ModItems.ROBOT_DOG_BUBBLEGUM_SKIN.get()) {
         this.setPaintjob(17);
      } else if (item == ModItems.ROBOT_DOG_RACING_SKIN.get()) {
         this.setPaintjob(18);
      }
   }

   public void setTame(boolean pTamed, boolean pApplyTamingSideEffects) {
      super.setTame(pTamed, pApplyTamingSideEffects);
      if (pTamed) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
         this.setHealth(20.0F);
      } else {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
      }

      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
   }

   public int getRemainingPersistentAngerTime() {
      return (Integer)this.entityData.get(DATA_REMAINING_ANGER_TIME);
   }

   public void setRemainingPersistentAngerTime(int pTime) {
      this.entityData.set(DATA_REMAINING_ANGER_TIME, pTime);
   }

   public void startPersistentAngerTimer() {
      this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Nullable
   public UUID getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   public void setPersistentAngerTarget(@Nullable UUID pTarget) {
      this.persistentAngerTarget = pTarget;
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      if (this.isInvulnerableTo(pSource)) {
         return false;
      }

      Entity entity = pSource.getEntity();
      if (!this.level().isClientSide) {
         this.setOrderedToSit(false);
      }

      if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
         pAmount = (pAmount + 1.0F) / 2.0F;
      }

      return super.hurt(pSource, pAmount);
   }

   public void updatePersistentAnger(ServerLevel pServerLevel, boolean pUpdateAnger) {
      LivingEntity livingentity = this.getTarget();
      UUID uuid = this.getPersistentAngerTarget();
      if ((livingentity == null || livingentity.isDeadOrDying()) && uuid != null && pServerLevel.getEntity(uuid) instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if (livingentity != null && !Objects.equals(uuid, livingentity.getUUID())) {
            this.setPersistentAngerTarget(livingentity.getUUID());
            this.startPersistentAngerTimer();
         }

         if (this.getRemainingPersistentAngerTime() > 0 && (livingentity == null || livingentity.getType() != EntityType.PLAYER || !pUpdateAnger)) {
            this.setRemainingPersistentAngerTime(this.getRemainingPersistentAngerTime() - 1);
            if (this.getRemainingPersistentAngerTime() == 0) {
               this.stopBeingAngry();
            }
         }
      }
   }

   public boolean hasChest() {
      return (Boolean)this.entityData.get(DATA_ID_CHEST);
   }

   public void setChest(boolean pChested) {
      this.entityData.set(DATA_ID_CHEST, pChested);
   }

   public boolean hasArmor() {
      return (Boolean)this.entityData.get(DATA_ID_ARMOR);
   }

   public void setArmor(boolean pChested) {
      this.entityData.set(DATA_ID_ARMOR, pChested);
      if (pChested) {
         this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
      } else {
         this.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
      }
   }

   public int getPaintjob() {
      return (Integer)this.entityData.get(PAINTJOB);
   }

   public void setPaintjob(int id) {
      this.entityData.set(PAINTJOB, id);
   }

   public boolean hasTurret() {
      return (Boolean)this.entityData.get(DATA_ID_TURRET);
   }

   public boolean hasTurretAmmo() {
      for (int i = 0; i < this.dogInventory.getContainerSize(); i++) {
         ItemStack s = this.dogInventory.getItem(i);
         if (s.is(bulletTag)) {
            return true;
         }
      }

      return false;
   }

   public void ConsumeAmmo() {
      for (int i = 0; i < this.dogInventory.getContainerSize(); i++) {
         ItemStack s = this.dogInventory.getItem(i);
         if (s.is(bulletTag)) {
            s.shrink(1);
            ItemStack casing = new ItemStack((ItemLike)ModItems.PISTOL_CASING.get(), 1);
            ItemEntity created = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), casing);
            created.setDefaultPickUpDelay();
            created.setDefaultPickUpDelay();
            created.setDeltaMovement(VecHelper.offsetRandomly(new Vec3(0.0, 0.5, 0.0), this.level().random, 0.05F));
            this.level().addFreshEntity(created);
            return;
         }
      }
   }

   public void setTurret(boolean turret) {
      this.entityData.set(DATA_ID_TURRET, turret);
   }

   public boolean hasScanner() {
      return (Boolean)this.entityData.get(DATA_ID_SCANNER);
   }

   public void setScanner(boolean turret) {
      this.entityData.set(DATA_ID_SCANNER, turret);
   }

   protected void dropEquipment() {
      super.dropEquipment();
      if (this.hasChest()) {
         if (!this.level().isClientSide) {
            this.spawnAtLocation(Blocks.CHEST);
         }

         for (int i = 0; i < this.dogInventory.getContainerSize(); i++) {
            this.spawnAtLocation(this.dogInventory.getItem(i));
         }

         this.dogInventory.clearContent();
         this.setChest(false);
      }
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putBoolean("robotdog_chest", this.hasChest());
      pCompound.putBoolean("robotdog_turret", this.hasTurret());
      pCompound.putInt("robotdog_paint", this.getPaintjob());
      pCompound.putBoolean("robotdog_armor", this.hasArmor());
      pCompound.putBoolean("robotdog_scanner", this.hasScanner());
      if (this.hasChest()) {
         ListTag nbttaglist = new ListTag();

         for (int i = 0; i < this.dogInventory.getContainerSize(); i++) {
            ItemStack itemstack = this.dogInventory.getItem(i);
            if (!itemstack.isEmpty()) {
               CompoundTag CompoundNBT = new CompoundTag();
               CompoundNBT.putByte("Slot", (byte)i);
               itemstack.save(this.registryAccess(), CompoundNBT);
               nbttaglist.add(CompoundNBT);
            }
         }

         pCompound.put("Items", nbttaglist);
      }
   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setChest(pCompound.getBoolean("robotdog_chest"));
      this.setTurret(pCompound.getBoolean("robotdog_turret"));
      this.setPaintjob(pCompound.getInt("robotdog_paint"));
      this.setArmor(pCompound.getBoolean("robotdog_armor"));
      this.setScanner(pCompound.getBoolean("robotdog_scanner"));
      if (this.hasChest()) {
         if (this.dogInventory != null) {
            ListTag nbttaglist = pCompound.getList("Items", 10);
            this.initDogInventory();

            for (int i = 0; i < nbttaglist.size(); i++) {
               CompoundTag CompoundNBT = nbttaglist.getCompound(i);
               int j = CompoundNBT.getByte("Slot") & 255;
               this.dogInventory.setItem(j, ItemStack.parseOptional(this.registryAccess(), CompoundNBT));
            }
         } else {
            ListTag nbttaglist = pCompound.getList("Items", 10);
            this.initDogInventory();

            for (int i = 0; i < nbttaglist.size(); i++) {
               CompoundTag CompoundNBT = nbttaglist.getCompound(i);
               int j = CompoundNBT.getByte("Slot") & 255;
               this.initDogInventory();
               this.dogInventory.setItem(j, ItemStack.parseOptional(this.registryAccess(), CompoundNBT));
            }
         }

         this.resetSlots();
      }
   }

   public SlotAccess getSlot(int pSlot) {
      return pSlot == 499 ? new SlotAccess() {
         public ItemStack get() {
            return RobotdogEntity.this.hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
         }

         public boolean set(ItemStack p_149485_) {
            if (p_149485_.isEmpty()) {
               if (RobotdogEntity.this.hasChest()) {
                  RobotdogEntity.this.setChest(false);
               }

               return true;
            } else if (p_149485_.is(Items.CHEST)) {
               if (!RobotdogEntity.this.hasChest()) {
                  RobotdogEntity.this.setChest(true);
               }

               return true;
            } else {
               return false;
            }
         }
      } : super.getSlot(pSlot);
   }

   private void equipChest(Player pPlayer, ItemStack pChestStack) {
      this.setChest(true);
      if (!pPlayer.getAbilities().instabuild) {
         pChestStack.shrink(1);
      }
   }

   private void equipTurret(Player pPlayer, ItemStack pChestStack) {
      this.setTurret(true);
   }

   public void containerChanged(Container pContainer) {
   }

   protected void pickUpItem(ItemEntity pItemEntity) {
      if (this.hasScanner()) {
         ItemStack itemstack = pItemEntity.getItem();
         if (this.dogInventory.canAddItem(itemstack)) {
            int i = itemstack.getCount();
            this.dogInventory.addItem(itemstack);
            pItemEntity.discard();
         }
      }
   }

   public boolean canTakeItem(ItemStack pItemstack) {
      return this.hasScanner();
   }

   class SearchForItemsGoal extends Goal {
      public SearchForItemsGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         if (!RobotdogEntity.this.hasScanner()) {
            return false;
         } else if (RobotdogEntity.this.getTarget() == null) {
            List<ItemEntity> list = RobotdogEntity.this.level()
               .getEntitiesOfClass(ItemEntity.class, RobotdogEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0));
            return !list.isEmpty();
         } else {
            return false;
         }
      }

      public void tick() {
         List<ItemEntity> list = RobotdogEntity.this.level().getEntitiesOfClass(ItemEntity.class, RobotdogEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0));
         ArrayList<ItemEntity> bulletList = new ArrayList<>();

         for (ItemEntity e : list) {
            if (e.getItem().is(RobotdogEntity.casingTag)) {
               bulletList.add(e);
            }
         }

         if (!bulletList.isEmpty() && RobotdogEntity.this.dogInventory.canAddItem(bulletList.get(0).getItem())) {
            RobotdogEntity.this.getNavigation().moveTo((Entity)bulletList.get(0), 1.2F);
            if (RobotdogEntity.this.distanceToSqr((Entity)bulletList.get(0)) <= 2.0) {
               RobotdogEntity.this.pickUpItem(bulletList.get(0));
            }
         }
      }

      public void start() {
         List<ItemEntity> list = RobotdogEntity.this.level().getEntitiesOfClass(ItemEntity.class, RobotdogEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0));
         if (!list.isEmpty() && RobotdogEntity.this.dogInventory.canAddItem(list.get(0).getItem())) {
            RobotdogEntity.this.getNavigation().moveTo((Entity)list.get(0), 1.2F);
         }
      }
   }
}
