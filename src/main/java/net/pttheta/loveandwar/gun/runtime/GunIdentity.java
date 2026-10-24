package net.pttheta.loveandwar.gun.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Server-assigned FlanItem identity; a packet slot is only a locator, never the identity. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunIdentity {
    public static final UUID INVALID = new UUID(0, 0);
    private static final Map<ServerPlayer, Map<UUID, Binding>> BINDINGS = new WeakHashMap<>();

    private record Binding(int slot, Item item) {
        boolean valid(ServerPlayer player, UUID id) {
            var stack = player.getInventory().getItem(slot);
            return stack.getItem() == item && id.equals(GunIdentity.id(stack));
        }
    }

    public record Located(ItemStack stack, int slot) {
        public InteractionHand hand() { return slot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }
    }

    private GunIdentity() {}

    /** Reading on either side never creates an ID. Copies retain the synchronized component. */
    public static UUID id(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GunItem
                ? stack.getOrDefault(ModDataComponents.GUN_ID, INVALID) : INVALID;
    }

    static void inventoryTick(ServerPlayer player) {
        var inventory = player.getInventory();
        var bindings = BINDINGS.get(player);
        if (bindings != null) bindings.entrySet().removeIf(entry -> !entry.getValue().valid(player, entry.getKey()));
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) continue;
            // Like FlanItem.Server_GetOrSetNewGunID, preserve an existing ID, even a copied/zero ID.
            if (!stack.has(ModDataComponents.GUN_ID)) stack.set(ModDataComponents.GUN_ID, UUID.randomUUID());
            var id = id(stack);
            if (INVALID.equals(id)) continue;
            if (bindings == null) {
                bindings = new HashMap<>();
                BINDINGS.put(player, bindings);
            }
            if (!bindings.containsKey(id)) bindings.put(id, new Binding(slot, stack.getItem()));
        }
        if (bindings != null && bindings.isEmpty()) BINDINGS.remove(player);
    }

    public static Optional<Located> resolve(ServerPlayer player, UUID id) {
        if (INVALID.equals(id)) return Optional.empty();
        var bindings = BINDINGS.get(player);
        var binding = bindings == null ? null : bindings.get(id);
        if (binding != null && binding.valid(player, id)) {
            return Optional.of(new Located(player.getInventory().getItem(binding.slot()), binding.slot()));
        }
        if (bindings != null) bindings.remove(id);
        // ShooterContextPlayer.CreateContext scans ascending slots after its cached context goes stale.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (!id.equals(id(stack))) continue;
            if (bindings == null) {
                bindings = new HashMap<>();
                BINDINGS.put(player, bindings);
            }
            bindings.put(id, new Binding(slot, stack.getItem()));
            return Optional.of(new Located(stack, slot));
        }
        if (bindings != null && bindings.isEmpty()) BINDINGS.remove(player);
        return Optional.empty();
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Pre event) {
        // ServerActionManager.ServerTick scans player inventories at START in the pinned source.
        for (var player : event.getServer().getPlayerList().getPlayers()) inventoryTick(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { BINDINGS.remove(event.getEntity()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { BINDINGS.clear(); }
}
