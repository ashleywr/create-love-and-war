package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.pttheta.loveandwar.gun.client.anim.LocalGunAnim;
import net.pttheta.loveandwar.gun.network.CGunAuthored;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.network.GunShotReceipt;
import net.pttheta.loveandwar.gun.network.SGunPresentationEnd;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunEngine;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.gun.runtime.LocalGunActions;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.network.ModMessages;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunPrediction {
    private static Player owner;
    private static final Map<UUID, LocalGunActions> GUNS = new HashMap<>();
    private static final java.util.ArrayDeque<CGunAuthored> OUTGOING = new java.util.ArrayDeque<>();

    private record Callback(UUID activation, long sequence) {}

    private static final Map<Callback, Boolean> FALLBACK = new LinkedHashMap<>();

    private GunPrediction() {}

    private static void owner() {
        var current = Minecraft.getInstance().player;
        if (owner != current) {
            GUNS.clear();
            OUTGOING.clear();
            FALLBACK.clear();
            owner = current;
        }
    }

    private static ItemStack find(UUID id) {
        if (owner == null) return ItemStack.EMPTY;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            var gun = owner.getInventory().getItem(i);
            if (GunIdentity.id(gun).equals(id)) return gun;
        }
        return ItemStack.EMPTY;
    }

    public static CGunInput beforeSend(CGunInput input) {
        owner();
        var actual = find(input.gunId());
        if (actual.isEmpty()) return input;
        var result = runtime(input, actual).input(input, actual);
        if (input.pressed()) model(actual, input.inputType(), !result.prediction().present());
        return result;
    }

    private static LocalGunActions runtime(CGunInput input, ItemStack actual) {
        var previous = GUNS.get(input.gunId());
        if (previous != null && !previous.matches(actual)) {
            previous.cancel();
            OUTGOING.addAll(previous.drainAuthored());
            GUNS.remove(input.gunId());
        }
        return GUNS.computeIfAbsent(
                input.gunId(),
                ignored ->
                        new LocalGunActions(
                                owner,
                                actual,
                                (gun, group, h, v, index) -> {
                                    GunRecoil.local(h, v, index);
                                    MuzzleFlash.onPredicted(gun, group);
                                }));
    }

    public static java.util.Optional<CGunInput> held(CGunInput input) {
        owner();
        var actual = find(input.gunId());
        if (actual.isEmpty()) return java.util.Optional.empty();
        var prepared = runtime(input, actual).held(input, actual);
        prepared.ifPresent(
                packet -> {
                    flushAuthored();
                    model(actual, packet.inputType(), false);
                    if (owner != null && Minecraft.getInstance().getConnection() != null)
                        ModMessages.sendToServer(packet);
                    flushAuthored();
                });
        return prepared;
    }

    /** Preserve START callback order before END input edges and flush final callbacks at END. */
    public static CGunInput send(CGunInput input) {
        flushAuthored();
        var prepared = beforeSend(input.controlled());
        if (owner != null && Minecraft.getInstance().getConnection() != null)
            ModMessages.sendToServer(prepared);
        flushAuthored();
        return prepared;
    }

    public static void flushAuthored() {
        owner();
        for (var runtime : GUNS.values()) OUTGOING.addAll(runtime.drainAuthored());
        if (owner == null || Minecraft.getInstance().getConnection() == null) return;
        while (!OUTGOING.isEmpty()) ModMessages.sendToServer(OUTGOING.removeFirst());
    }

    public static void end(SGunPresentationEnd packet) {
        owner();
        var runtime = GUNS.get(packet.gun());
        if (runtime != null) runtime.end(packet);
    }

    /**
     * Ammo is acknowledged even when the owning runtime already owns every presentation callback.
     */
    public static boolean authoritative(GunShotReceipt receipt) {
        owner();
        if (!receipt.present()) return true;
        var runtime = GUNS.get(receipt.gun());
        if (runtime != null) runtime.acknowledge(receipt);
        if (receipt.clientOwned()) return false;
        var key = new Callback(receipt.activation(), receipt.sequence());
        if (FALLBACK.putIfAbsent(key, true) != null) return false;
        // Normal play payloads are reliable/ordered; retain a bounded duplicate window for
        // unpredicted callbacks.
        while (FALLBACK.size() > 4096) FALLBACK.remove(FALLBACK.keySet().iterator().next());
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(ClientTickEvent.Pre event) {
        owner();
        if (owner == null) return;
        var iterator = GUNS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var actual = find(entry.getKey());
            if (actual.isEmpty()
                    || !entry.getValue().matches(actual)
                    || !owner.isAlive()
                    || owner.isSpectator()) {
                entry.getValue().cancel();
                OUTGOING.addAll(entry.getValue().drainAuthored());
                iterator.remove();
            } else {
                entry.getValue().tick(actual);
                if (entry.getValue().idle()) iterator.remove();
            }
        }
    }

    public static void onInputSent(String inputType) {
        model(inputType, true);
    }

    private static void model(String inputType, boolean flash) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        model(player.getMainHandItem(), inputType, flash);
    }

    private static void model(ItemStack stack, String inputType, boolean flash) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!(stack.getItem() instanceof GunItem)) return;
        String gunKey = GunLookup.contentKey(stack);
        GunEngine.predictStartedGroup(player, stack, inputType, player.level().getGameTime())
                .ifPresent(
                        group -> {
                            if (LocalGunAnim.wouldPredict(gunKey, group)) {
                                LocalGunAnim.predict(
                                        gunKey,
                                        stack.getOrDefault(
                                                ModDataComponents.GUN_ACTION_STATE,
                                                GunActionState.IDLE),
                                        group);
                                if (flash) MuzzleFlash.onPredicted(stack, group.key());
                            }
                        });
    }
}
