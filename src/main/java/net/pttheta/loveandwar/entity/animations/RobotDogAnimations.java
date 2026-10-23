package net.pttheta.loveandwar.entity.animations;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel.Interpolations;
import net.minecraft.client.animation.AnimationChannel.Targets;
import net.minecraft.client.animation.AnimationDefinition.Builder;

public class RobotDogAnimations {
   public static final AnimationDefinition ROBOT_DOG_IDLE = Builder.withLength(1.5F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "torso",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(3.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -0.2F, 0.2F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -0.2F, 0.2F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-17.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-17.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftForeArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-11.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -0.2F, 0.3F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -0.2F, 0.3F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightForeArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "TurretBase",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-14.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.degreeVec(-14.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition ROBOT_DOG_WALK = Builder.withLength(0.8343334F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.8F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.posVec(0.0F, -1.2F, -1.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.posVec(0.0F, -0.2F, -1.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -4.2F, -1.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.posVec(0.0F, -4.2F, -1.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.posVec(0.0F, 0.8F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(55.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "torso",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-28.75F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(72.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(47.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(72.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftKnee",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftAnkle",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(62.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(62.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-17.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(62.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(67.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-97.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-42.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4167667F, KeyframeAnimations.degreeVec(48.75F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(47.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(67.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftForeArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-77.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(32.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.2916767F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.375F, KeyframeAnimations.degreeVec(-53.04F, -2.43F, -2.15F), Interpolations.LINEAR),
               new Keyframe(0.4167667F, KeyframeAnimations.degreeVec(-73.58F, -4.86F, -4.3F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(75.35F, -9.72F, -8.61F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-77.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(-77.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftHand",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(57.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-32.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-59.78F, 4.02F, 6.34F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-29.78F, 4.02F, 6.34F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightKnee",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightAnkle",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(65.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-85.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-47.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4167667F, KeyframeAnimations.degreeVec(56.25F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(42.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(127.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(65.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightForeArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-75.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(37.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4167667F, KeyframeAnimations.degreeVec(-61.25F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(65.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(-75.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightHand",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-100.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "TurretBase",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.16766666F, KeyframeAnimations.degreeVec(-28.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3433333F, KeyframeAnimations.degreeVec(-54.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4167667F, KeyframeAnimations.degreeVec(-31.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-8.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.6766666F, KeyframeAnimations.degreeVec(-1.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8343334F, KeyframeAnimations.degreeVec(-7.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition ROBOT_DOG_SIT = Builder.withLength(0.0F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -10.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "body",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(-32.5F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "head",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(42.5F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(-36.66F, -17.27F, -21.74F), Interpolations.LINEAR)}
         )
      )
      .addAnimation(
         "LeftAnkle",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "LeftForeArm",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(-32.5F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(-46.59F, 20.72F, 18.5F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightAnkle",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(42.5F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightForeArm",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "TurretBase",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(32.0F, 0.0F, 0.0F), Interpolations.LINEAR)})
      )
      .build();
   public static final AnimationDefinition ROBOT_DOG_ATTACK = Builder.withLength(0.8343334F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -2.9F, -2.9F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "torso",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(-32.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.375F, KeyframeAnimations.degreeVec(-41.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(55.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftKnee",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(55.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftAnkle",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(-112.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-87.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftForeArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(57.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightLeg",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightKnee",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(-115.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-87.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightForeArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "TurretBase",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.20834334F, KeyframeAnimations.degreeVec(31.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(1.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7916766F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
}
