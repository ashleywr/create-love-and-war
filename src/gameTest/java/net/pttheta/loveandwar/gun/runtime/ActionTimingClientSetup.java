package net.pttheta.loveandwar.gun.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.LoggerFactory;

/** Opt-in disposable-world scenario with exact inventory/mode/view restoration. */
@EventBusSubscriber(modid = "createloveandwar")
public final class ActionTimingClientSetup {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        final List<ItemStack> items, armor, offhand;
        final int selected;
        final float yaw, pitch;
        final GameType mode;
        final Vec3 position;
        final boolean flying;
        final String gun;
        final Set<UUID> initialItems = new HashSet<>(), spawned = new HashSet<>();
        long started = -1;
        String previous = "";
        Session(ServerPlayer player, String gun) {
            items = player.getInventory().items.stream().map(ItemStack::copy).toList();
            armor = player.getInventory().armor.stream().map(ItemStack::copy).toList();
            offhand = player.getInventory().offhand.stream().map(ItemStack::copy).toList();
            selected = player.getInventory().selected; yaw = player.getYRot(); pitch = player.getXRot();
            mode = player.gameMode.getGameModeForPlayer(); position = player.position(); flying = player.getAbilities().flying;
            this.gun = gun;
            player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(12)).forEach(item -> initialItems.add(item.getUUID()));
        }
    }
    private static ItemStack item(String name, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name)), count);
    }
    private static void cleanup(ServerPlayer player) {
        var session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        GunFireScheduler.releaseAll(player);
        var stack = player.getMainHandItem();
        GunFireScheduler.cancel(player, stack); GunActionTimeline.cancelAll(player, stack);
        for (var id : session.spawned) {
            var entity = player.serverLevel().getEntity(id);
            if (entity != null) entity.discard();
        }
        for (int i = 0; i < session.items.size(); i++) player.getInventory().items.set(i, session.items.get(i).copy());
        for (int i = 0; i < session.armor.size(); i++) player.getInventory().armor.set(i, session.armor.get(i).copy());
        for (int i = 0; i < session.offhand.size(); i++) player.getInventory().offhand.set(i, session.offhand.get(i).copy());
        player.getInventory().selected = session.selected;
        player.setGameMode(session.mode); player.getAbilities().flying = session.flying; player.onUpdateAbilities();
        player.connection.teleport(session.position.x, session.position.y, session.position.z, session.yaw, session.pitch);
        player.inventoryMenu.broadcastChanges();
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TIMING_CLEANUP restored inventory/mode/view gun={}", session.gun);
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        var command = Commands.literal("law_test_timing_setup").requires(source -> source.hasPermission(2));
        for (String name : new String[]{"1887", "g11k2", "wz38", "krag", "mat49"}) {
            command.then(Commands.literal(name).executes(context -> {
                var player = context.getSource().getPlayerOrException(); cleanup(player);
                SESSIONS.put(player.getUUID(), new Session(player, name));
                var stack = item(name, 1);
                String bullet = name.equals("1887") ? "buckshot_shell" : name.equals("g11k2") ? "caseless_bullet" : name.equals("mat49") ? "pistol_bullet" : "rifle_bullet";
                if (name.equals("krag") || name.equals("mat49")) {
                    stack.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(item(bullet, 1))));
                }
                player.getInventory().selected = 0;
                player.getInventory().setItem(0, stack);
                player.getInventory().setItem(8, item(bullet, name.equals("1887") ? 5 : 12));
                player.setGameMode(GameType.SURVIVAL);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), -80);
                player.inventoryMenu.broadcastChanges();
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TIMING_SETUP gun={} time={}", name, player.level().getGameTime());
                return 1;
            }));
        }
        event.getDispatcher().register(command);
        event.getDispatcher().register(Commands.literal("law_test_timing_cleanup").requires(source -> source.hasPermission(2)).executes(context -> {
            cleanup(context.getSource().getPlayerOrException()); return 1;
        }));
    }
    @SubscribeEvent public static void observe(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        var stack = player.getMainHandItem();
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        long now = player.level().getGameTime();
        if (session.started < 0 && !state.activeGroup().isEmpty()) session.started = now - state.groupTicksElapsed();
        int rounds = stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size();
        String frame = state.activeGroup() + ":" + state.groupTotalTicks() + ":" + rounds;
        if (!frame.equals(session.previous)) {
            session.previous = frame;
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TIMING_SERVER gun={} time={} relative={} group={} elapsed={} total={} rounds={}",
                    session.gun, now, session.started < 0 ? -1 : now - session.started, state.activeGroup(), state.groupTicksElapsed(), state.groupTotalTicks(), rounds);
        }
        if (session.started < 0 || now - session.started > 180) return;
        for (var entity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(12))) {
            if (entity.getItem().getItem().builtInRegistryHolder().key().location().getPath().endsWith("_casing")
                    && !session.initialItems.contains(entity.getUUID()) && session.spawned.add(entity.getUUID())) {
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_TIMING_CASING gun={} time={} relative={} item={}", session.gun, now, now - session.started, entity.getItem());
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { SESSIONS.clear(); }
}
