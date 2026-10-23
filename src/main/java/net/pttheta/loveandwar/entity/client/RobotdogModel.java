package net.pttheta.loveandwar.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.pttheta.loveandwar.entity.animations.RobotDogAnimations;
import net.pttheta.loveandwar.entity.custom.RobotdogEntity;

public class RobotdogModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart base;
   private final ModelPart head;
   private final ModelPart chest;
   private final ModelPart turret;
   private final ModelPart gun;
   private final ModelPart scanner;

   public RobotdogModel(ModelPart root) {
      this.base = root.getChild("root");
      this.head = this.base.getChild("body").getChild("torso").getChild("head");
      this.chest = this.base.getChild("body").getChild("chest");
      this.turret = this.base.getChild("body").getChild("TurretBase").getChild("turret");
      this.gun = this.turret.getChild("gun");
      this.scanner = this.base.getChild("body").getChild("torso").getChild("Scanner");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -15.0F, 5.0F));
      PartDefinition armor_r1 = body.addOrReplaceChild(
         "armor_r1",
         CubeListBuilder.create()
            .texOffs(34, 54)
            .addBox(-4.0F, -3.0F, -5.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(34, 54)
            .addBox(3.0F, -3.0F, -5.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(18, 25)
            .addBox(-3.0F, -3.0F, -7.0F, 6.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0)
            .addBox(-3.0F, -3.0F, -1.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(19, 6)
            .addBox(-2.0F, -2.5F, -6.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F)
      );
      PartDefinition torso = body.addOrReplaceChild(
         "torso",
         CubeListBuilder.create()
            .texOffs(0, 11)
            .addBox(-2.5F, -3.5F, -3.0F, 5.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(26, 15)
            .addBox(-1.0F, 0.5F, -7.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.0F, -6.0F)
      );
      PartDefinition cube_r1 = torso.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, -2.5F, -6.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.6545F, 0.0F, 0.0F)
      );
      PartDefinition cube_r2 = torso.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(13, 16).addBox(-2.0F, 2.5F, -3.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, -0.3491F, 0.0F, 0.0F)
      );
      PartDefinition shield = torso.addOrReplaceChild(
         "shield",
         CubeListBuilder.create().texOffs(37, 7).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -4.1F, -3.4F, -0.8727F, 0.0F, 0.0F)
      );
      PartDefinition cube_r3 = shield.addOrReplaceChild(
         "cube_r3",
         CubeListBuilder.create().texOffs(0, 41).addBox(-2.0F, -2.1F, 0.0F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-2.0F, 0.0F, -1.0F, 0.0F, 0.7854F, 0.0F)
      );
      PartDefinition cube_r4 = shield.addOrReplaceChild(
         "cube_r4",
         CubeListBuilder.create().texOffs(6, 41).addBox(0.0F, -2.1F, 0.0F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(2.0F, 0.0F, -1.0F, 0.0F, -0.7854F, 0.0F)
      );
      PartDefinition head = torso.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(31, 15)
            .addBox(-2.0F, -1.0F, -4.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(44, 35)
            .addBox(-1.5F, 2.0F, -5.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.5F, 1.2F, -7.0F)
      );
      PartDefinition LeftArm = torso.addOrReplaceChild("LeftArm", CubeListBuilder.create(), PartPose.offset(1.0F, 4.5F, -3.0F));
      PartDefinition armor_r2 = LeftArm.addOrReplaceChild(
         "armor_r2",
         CubeListBuilder.create()
            .texOffs(52, 48)
            .addBox(4.0F, -3.0F, -2.0F, 1.0F, 11.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(0, 29)
            .addBox(1.0F, -2.0F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2182F, 0.0F, 0.0F)
      );
      PartDefinition LeftForeArm = LeftArm.addOrReplaceChild("LeftForeArm", CubeListBuilder.create(), PartPose.offset(2.5F, 6.0F, 2.0F));
      PartDefinition cube_r5 = LeftForeArm.addOrReplaceChild(
         "cube_r5",
         CubeListBuilder.create().texOffs(22, 33).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
      );
      PartDefinition LeftHand = LeftForeArm.addOrReplaceChild("LeftHand", CubeListBuilder.create(), PartPose.offset(0.0F, 5.0F, -5.0F));
      PartDefinition cube_r6 = LeftHand.addOrReplaceChild(
         "cube_r6",
         CubeListBuilder.create().texOffs(0, 21).addBox(-1.5F, -1.0F, -5.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F)
      );
      PartDefinition RightArm = torso.addOrReplaceChild("RightArm", CubeListBuilder.create(), PartPose.offset(-1.0F, 4.5F, -3.0F));
      PartDefinition cube_r7 = RightArm.addOrReplaceChild(
         "cube_r7",
         CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-4.0F, -2.0F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2182F, 0.0F, 0.0F)
      );
      PartDefinition RightForeArm = RightArm.addOrReplaceChild("RightForeArm", CubeListBuilder.create(), PartPose.offset(-2.5F, 6.0F, 2.0F));
      PartDefinition armor_r3 = RightForeArm.addOrReplaceChild(
         "armor_r3",
         CubeListBuilder.create().texOffs(52, 48).addBox(-7.0F, -3.0F, -2.0F, 1.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(4.5F, -6.0F, -2.0F, 0.2182F, 0.0F, 0.0F)
      );
      PartDefinition cube_r8 = RightForeArm.addOrReplaceChild(
         "cube_r8",
         CubeListBuilder.create().texOffs(22, 33).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
      );
      PartDefinition RightHand = RightForeArm.addOrReplaceChild("RightHand", CubeListBuilder.create(), PartPose.offset(0.0F, 5.0F, -5.0F));
      PartDefinition cube_r9 = RightHand.addOrReplaceChild(
         "cube_r9",
         CubeListBuilder.create().texOffs(0, 21).mirror().addBox(-1.5F, -1.0F, -5.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F)
      );
      PartDefinition Scanner = torso.addOrReplaceChild(
         "Scanner",
         CubeListBuilder.create()
            .texOffs(56, 40)
            .addBox(-1.0F, -3.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(52, 22)
            .addBox(-1.5F, -6.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.0F, -1.0F)
      );
      PartDefinition LeftLeg = body.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offset(3.0F, 0.0F, 1.0F));
      PartDefinition armor_r4 = LeftLeg.addOrReplaceChild(
         "armor_r4",
         CubeListBuilder.create()
            .texOffs(40, 43)
            .addBox(2.0F, -4.0F, -1.5F, 1.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(33, 23)
            .addBox(0.5F, 4.0F, 0.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(32, 29)
            .addBox(0.0F, -3.0F, -1.0F, 2.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.6545F, 0.0F, 0.0F)
      );
      PartDefinition LeftKnee = LeftLeg.addOrReplaceChild("LeftKnee", CubeListBuilder.create(), PartPose.offset(1.0F, 5.4F, -3.0F));
      PartDefinition cube_r10 = LeftKnee.addOrReplaceChild(
         "cube_r10",
         CubeListBuilder.create().texOffs(40, 23).addBox(-1.0F, 0.6F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -0.4F, -2.0F, 0.9599F, 0.0F, 0.0F)
      );
      PartDefinition LeftAnkle = LeftKnee.addOrReplaceChild(
         "LeftAnkle",
         CubeListBuilder.create().texOffs(18, 0).addBox(-1.4F, 3.0F, -4.6F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 3.9F, 3.5F)
      );
      PartDefinition cube_r11 = LeftAnkle.addOrReplaceChild(
         "cube_r11",
         CubeListBuilder.create().texOffs(12, 33).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F)
      );
      PartDefinition RightLeg = body.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offset(-3.0F, 0.0F, 1.0F));
      PartDefinition cube_r12 = RightLeg.addOrReplaceChild(
         "cube_r12",
         CubeListBuilder.create()
            .texOffs(33, 23)
            .mirror()
            .addBox(-1.5F, 4.0F, 0.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .mirror(false)
            .texOffs(40, 43)
            .addBox(-3.0F, -4.0F, -1.5F, 1.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(32, 29)
            .mirror()
            .addBox(-2.0F, -3.0F, -1.0F, 2.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.6545F, 0.0F, 0.0F)
      );
      PartDefinition RightKnee = RightLeg.addOrReplaceChild("RightKnee", CubeListBuilder.create(), PartPose.offset(-1.0F, 5.4F, -3.0F));
      PartDefinition cube_r13 = RightKnee.addOrReplaceChild(
         "cube_r13",
         CubeListBuilder.create().texOffs(40, 23).mirror().addBox(-1.0F, 0.6F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(0.0F, -0.4F, -2.0F, 0.9599F, 0.0F, 0.0F)
      );
      PartDefinition RightAnkle = RightKnee.addOrReplaceChild(
         "RightAnkle",
         CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-1.6F, 3.0F, -4.6F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 3.9F, 3.5F)
      );
      PartDefinition cube_r14 = RightAnkle.addOrReplaceChild(
         "cube_r14",
         CubeListBuilder.create().texOffs(12, 33).mirror().addBox(-1.5F, -2.0F, -1.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F)
      );
      PartDefinition TurretBase = body.addOrReplaceChild("TurretBase", CubeListBuilder.create(), PartPose.offset(-3.0F, -2.0F, -5.0F));
      PartDefinition turret = TurretBase.addOrReplaceChild(
         "turret",
         CubeListBuilder.create()
            .texOffs(46, 1)
            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(47, 4)
            .addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-1.0F, -1.0F, 0.0F)
      );
      PartDefinition gun = turret.addOrReplaceChild(
         "gun",
         CubeListBuilder.create()
            .texOffs(45, 13)
            .addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(49, 29)
            .addBox(-0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -6.0F, 0.0F)
      );
      PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition cube_r15 = chest.addOrReplaceChild(
         "cube_r15",
         CubeListBuilder.create().texOffs(2, 43).addBox(-2.0F, -5.0F, -7.0F, 4.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.root().getAllParts().forEach(ModelPart::resetPose);
      this.applyTurretRotation(netHeadYaw, headPitch);
      this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);
      this.scanner.yRot = ((RobotdogEntity)entity).scannerAngle;
      this.animateWalk(RobotDogAnimations.ROBOT_DOG_WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
      this.animate(((RobotdogEntity)entity).idleAnimationState, RobotDogAnimations.ROBOT_DOG_IDLE, ageInTicks, 1.0F);
      this.animate(((RobotdogEntity)entity).attackAnimationState, RobotDogAnimations.ROBOT_DOG_ATTACK, ageInTicks, 1.0F);
      this.animate(((RobotdogEntity)entity).sitAnimationState, RobotDogAnimations.ROBOT_DOG_SIT, ageInTicks, 1.0F);
      if (((RobotdogEntity)entity).hasChest()) {
         this.chest.visible = true;
      } else {
         this.chest.visible = false;
      }

      if (((RobotdogEntity)entity).hasScanner()) {
         this.scanner.visible = true;
      } else {
         this.scanner.visible = false;
      }

      if (((RobotdogEntity)entity).hasTurret()) {
         this.turret.visible = true;
         this.gun.visible = true;
      } else {
         this.turret.visible = false;
         this.gun.visible = false;
      }
   }

   private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks) {
      pNetHeadYaw = Mth.clamp(pNetHeadYaw, -30.0F, 30.0F);
      pHeadPitch = Mth.clamp(pHeadPitch, -25.0F, 45.0F);
      this.head.yRot = pNetHeadYaw * (float) (Math.PI / 180.0);
      this.head.xRot = pHeadPitch * (float) (Math.PI / 180.0);
   }

   private void applyTurretRotation(float yaw, float pitch) {
      this.gun.xRot = pitch * (float) (Math.PI / 180.0);
      this.turret.yRot = yaw * (float) (Math.PI / 180.0);
      this.turret.xRot = 0.0F;
      this.gun.yRot = 0.0F;
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public ModelPart root() {
      return this.base;
   }
}
