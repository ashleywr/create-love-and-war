package net.pttheta.loveandwar.gun.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class ShotTrailRenderer {
   private static final float WIDTH = 0.05F;
   private static final float LENGTH = 10.0F;
   private static final double SPEED = 10.0;
   private static final float R = 1.0F;
   private static final float G = 1.0F;
   private static final float B = 0.5F;
   private static final float A = 1.0F;
   private static final List<ShotTrailRenderer.Trail> TRAILS = new ArrayList<>();
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/tracer");

   private ShotTrailRenderer() {
   }

   public static void add(Vec3 origin, Vec3 end) {
      synchronized (TRAILS) {
         if (TRAILS.size() < 256) {
            ShotTrailRenderer.Trail trail = new ShotTrailRenderer.Trail(origin, end);
            TRAILS.add(trail);
            if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
               LOGGER.info(
                  "TRACER add origin={} end={} dist={} lifetime={}t", new Object[]{origin, end, String.format("%.1f", origin.distanceTo(end)), trail.lifetime}
               );
            }
         }
      }
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      synchronized (TRAILS) {
         TRAILS.removeIf(ShotTrailRenderer.Trail::tickAndCheckFinished);
      }
   }

   @SubscribeEvent
   static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         List<ShotTrailRenderer.Trail> snapshot;
         synchronized (TRAILS) {
            if (TRAILS.isEmpty()) {
               return;
            }

            snapshot = List.copyOf(TRAILS);
         }

         Camera camera = event.getCamera();
         Vec3 cameraPos = camera.getPosition();
         float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         PoseStack poseStack = event.getPoseStack();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);

         for (ShotTrailRenderer.Trail trail : snapshot) {
            trail.render(poseStack, cameraPos, partial);
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void muzzleParticles(Vec3 origin, Vec3 direction, int count) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && count > 0) {
         double scale = Config.MUZZLE_SMOKE == null ? 1.0 : (Double)Config.MUZZLE_SMOKE.get();
         if (!(scale <= 0.0)) {
            count = Math.max(1, (int)Math.round(count * scale));
            Vec3 look = direction.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
            ParticleOptions smoke = count > 1 ? ParticleTypes.POOF : ParticleTypes.SMOKE;

            for (int i = 0; i < count; i++) {
               mc.level
                  .addParticle(
                     smoke,
                     origin.x + look.x * 0.1,
                     origin.y + look.y * 0.1,
                     origin.z + look.z * 0.1,
                     look.x * 0.3 + jitter(count),
                     look.y * 0.3 + jitter(count),
                     look.z * 0.3 + jitter(count)
                  );
               if (i == 1) {
                  mc.level.addParticle(ParticleTypes.FLAME, origin.x, origin.y, origin.z, look.x, look.y, look.z);
               }
            }
         }
      }
   }

   private static double jitter(int strength) {
      return (Math.random() * 0.6 - 0.3) * strength / 20.0;
   }

   public static Vec3 localMuzzle(Vec3 fallback) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
         float hip = 1.0F - AdsBlendState.blend();
         Vec3 look = mc.player.getLookAngle();
         Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
         return mc.player.getEyePosition().add(look.scale(forwardToMuzzle())).add(right.scale(0.18 * hip)).subtract(0.0, 0.12 * hip, 0.0);
      } else {
         return fallback;
      }
   }

   private static double forwardToMuzzle() {
      double captured = TurboRigBakedModel.muzzleForwardDistance();
      return captured > 0.2 ? captured : 1.1;
   }

   private static final class Trail {
      private final Vec3 origin;
      private final Vec3 direction;
      private final int lifetime;
      private int ticks;

      Trail(Vec3 origin, Vec3 end) {
         this.origin = origin;
         Vec3 delta = end.subtract(origin);
         this.direction = delta.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : delta.normalize();
         this.lifetime = Math.max(1, Mth.floor((delta.length() - 10.0) / 10.0));
      }

      boolean tickAndCheckFinished() {
         return ++this.ticks >= this.lifetime;
      }

      void render(PoseStack poseStack, Vec3 cameraPos, float partial) {
         double centerT = 5.0 + (this.ticks + partial) * 10.0;
         Vec3 center = this.origin.add(this.direction.scale(centerT));
         Vec3 toCamera = center.subtract(cameraPos);
         if (!(toCamera.lengthSqr() < 1.0E-8)) {
            Vec3 yAxis = this.direction.cross(toCamera.normalize());
            if (!(yAxis.lengthSqr() < 1.0E-8)) {
               yAxis = yAxis.normalize().scale(0.025F);
               Vec3 xAxis = this.direction.scale(5.0);
               poseStack.pushPose();
               poseStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
               Matrix4f matrix = poseStack.last().pose();
               BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               vertex(buffer, matrix, yAxis.add(xAxis));
               vertex(buffer, matrix, yAxis.subtract(xAxis));
               vertex(buffer, matrix, yAxis.scale(-1.0).subtract(xAxis));
               vertex(buffer, matrix, yAxis.scale(-1.0).add(xAxis));
               BufferUploader.drawWithShader(buffer.buildOrThrow());
               poseStack.popPose();
            }
         }
      }

      private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 v) {
         buffer.addVertex(matrix, (float)v.x, (float)v.y, (float)v.z).setColor(1.0F, 1.0F, 0.5F, 1.0F);
      }
   }
}
