package net.pttheta.loveandwar.gametest;

import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.entity.ModEntities;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.LoggerFactory;

/** Opt-in scenarios for the disposable development world. */
@EventBusSubscriber(modid = "createloveandwar")
public final class ProjectileClientScenarioSetup {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        var setup = Commands.literal("law_test_projectile_setup").requires(source -> source.hasPermission(2));
        for (String name : List.of("black_powder_cartridge", "gyrojet_basic", "gyrojet_pen", "gyrojet_heatseeker_top")) {
            setup.then(Commands.literal(name).executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:chassepot")));
                gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name))))));
                player.setItemInHand(InteractionHand.MAIN_HAND, gun);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), 0, -80);
                context.getSource().sendSuccess(() -> Component.literal("LAW_PROJECTILE_SETUP bullet=" + name + " rounds=1"), false);
                return 1;
            }));
        }
        event.getDispatcher().register(setup);
        event.getDispatcher().register(Commands.literal("law_test_projectile_status").requires(source -> source.hasPermission(2)).executes(context -> {
            var player = context.getSource().getPlayerOrException();
            for (var bullet : player.serverLevel().getEntitiesOfClass(GunProjectile.class, player.getBoundingBox().inflate(128))) {
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_PROJECTILE_SERVER id={} bullet={} age={} fuse={} pos={} velocity={}",
                        bullet.getId(), bullet.bulletId(), bullet.lifeTime(), bullet.fuseRemaining(), bullet.position(), bullet.getDeltaMovement());
            }
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("law_test_projectile_showcase").requires(source -> source.hasPermission(2)).executes(context -> {
            var player = context.getSource().getPlayerOrException();
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.connection.teleport(0.5, 220, -10, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            String[] names = {"artillery_shell", "grenade_round_he", "gyrojet_heatseeker_top", "gyrojet_pen"};
            for (int i = 0; i < names.length; i++) {
                var bullet = new GunProjectile(ModEntities.BULLET.get(), player.serverLevel());
                bullet.initialize(player, ResourceLocation.parse("createloveandwar:" + names[i]), GunContent.bullet(names[i]).orElseThrow().projectiles().getFirst(), List.of(), "primary_fire", null, 0);
                bullet.setPos(player.getEyePosition().add((i - 1.5) * 0.8, 0, 3));
                bullet.setNoGravity(true); bullet.setDeltaMovement(Vec3.ZERO);
                player.serverLevel().addFreshEntity(bullet);
            }
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("law_test_projectile_cleanup").requires(source -> source.hasPermission(2)).executes(context -> {
            var player = context.getSource().getPlayerOrException();
            var bullets = player.serverLevel().getEntitiesOfClass(GunProjectile.class, player.getBoundingBox().inflate(256));
            bullets.stream().filter(bullet -> bullet.getOwner() == player).forEach(GunProjectile::discard);
            return 1;
        }));
    }
}
