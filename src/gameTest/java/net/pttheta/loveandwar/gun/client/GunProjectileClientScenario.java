package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunProjectileClientScenario {
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_projectile_client").executes(context -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return 0;
            int count = 0;
            for (var entity : minecraft.level.entitiesForRendering()) if (entity instanceof GunProjectile bullet) {
                var model = GunProjectileEffects.model(bullet);
                if (model instanceof TurboRigBakedModel rig) model = rig.projectileMesh();
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_PROJECTILE_CLIENT id={} bullet={} age={} pos={} velocity={} mesh={} quads={}",
                        bullet.getId(), bullet.bulletId(), bullet.lifeTime(), bullet.position(), bullet.getDeltaMovement(),
                        model == null ? "null" : model.getClass().getSimpleName(), model == null ? 0 : model.getQuads(null, null, RandomSource.create(0)).size());
                count++;
            }
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_PROJECTILE_CLIENT_COUNT {}", count);
            return count;
        }));
    }
}
