package net.pttheta.loveandwar.gun.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.pttheta.loveandwar.gun.client.ShotTrailRenderer;
import net.pttheta.loveandwar.gun.client.GunSoundPlayer;
import net.pttheta.loveandwar.gun.runtime.GunEngine;
import net.pttheta.loveandwar.gun.runtime.GunLookup;

@EventBusSubscriber(modid = "createloveandwar")
public final class GunNetworking {
   private GunNetworking() {
   }

   @SubscribeEvent
   public static void registerPayloads(RegisterPayloadHandlersEvent event) {
      // Runtime active-group provenance adds a network-only component unknown to older clients.
      PayloadRegistrar registrar = event.registrar("3.0");
      registrar.playToServer(CGunInput.TYPE, CGunInput.STREAM_CODEC, GunNetworking::handle);
      registrar.playToClient(SShotTrail.TYPE, SShotTrail.STREAM_CODEC, GunNetworking::handleShotTrail);
      registrar.playToClient(SGunSound.TYPE, SGunSound.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> GunSoundPlayer.play(payload)));
   }

   private static void handleShotTrail(SShotTrail payload, IPayloadContext context) {
      context.enqueueWork(
         () -> {
            Vec3 origin = payload.origin();
            if (context.player() != null && context.player().getId() == payload.shooterId()) {
               origin = ShotTrailRenderer.localMuzzle(origin);
            }

            Vec3 muzzle = origin;
            Vec3 shotLine = payload.end().subtract(payload.origin());
            Vec3 trailEnd = shotLine.lengthSqr() < 1.0E-8 ? payload.end() : muzzle.add(shotLine);
            ShotTrailRenderer.add(muzzle, trailEnd);
            if ((context.player() == null ? null : context.player().level().getEntity(payload.shooterId())) instanceof LivingEntity living) {
               GunLookup.gunDef(living.getMainHandItem())
                  .ifPresent(def -> ShotTrailRenderer.muzzleParticles(muzzle, trailEnd.subtract(muzzle), def.particleCount()));
            }
         }
      );
   }

   private static void handle(CGunInput payload, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            GunEngine.handleInput(player, player.getMainHandItem(), InteractionHand.MAIN_HAND, payload.inputType(), payload.pressed());
         }
      });
   }
}
