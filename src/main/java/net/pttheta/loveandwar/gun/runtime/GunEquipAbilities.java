package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Original ActionStack.UpdateEquipped and its active ApplyAttribute path. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunEquipAbilities {
    private static final Map<ServerPlayer, Equipped> EQUIPPED = new WeakHashMap<>();
    private record Equipped(ItemStack main, ItemStack off) {}

    private GunEquipAbilities() {}

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        Equipped previous = EQUIPPED.get(player);
        boolean hasGun = main.getItem() instanceof GunItem || off.getItem() instanceof GunItem;
        if (previous == null && !hasGun) return;
        if (previous != null && previous.main() == main && previous.off() == off) return;
        update(player, previous == null ? ItemStack.EMPTY : previous.main(), main);
        update(player, previous == null ? ItemStack.EMPTY : previous.off(), off);
        if (!hasGun) EQUIPPED.remove(player);
        else EQUIPPED.put(player, new Equipped(main, off));
    }

    private static void update(ServerPlayer player, ItemStack previous, ItemStack current) {
        if (previous == current) return;
        dispatch(player, previous, "Unequip");
        dispatch(player, current, "Equip");
    }

    private static void dispatch(ServerPlayer player, ItemStack gun, String trigger) {
        if (!(gun.getItem() instanceof GunItem)) return;
        var attachments = gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
        var inputs = ModifierSource.gunInputs((int)attachments.all().stream().filter(stack -> !stack.isEmpty()).count());
        for (var ability : GunTraits.abilities(gun)) {
            var def = ability.definition();
            if (def.effects().isEmpty() || def.targets().isEmpty()
                    || def.startTriggers().stream().noneMatch(t -> t.triggerType().equals(trigger) && t.triggerConditions().isEmpty())) continue;
            if (def.targets().stream().noneMatch(t -> "Shooter".equals(t.targetType()) || "Owner".equals(t.targetType()))) continue;
            for (int i = 0; i < def.effects().size(); i++) {
                var effect = def.effects().get(i);
                if (!"ApplyAttribute".equals(effect.effectType())) continue;
                var sources = List.of(ModifierSource.of(ability.id().toString(), effect.modifiers()), inputs);
                var attributeId = ResourceLocation.tryParse(StatResolver.resolveOverride("attribute_id", "", sources));
                if (attributeId == null) continue;
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
                if (attribute.isEmpty()) continue;
                var instance = player.getAttribute(attribute.get());
                if (instance == null) continue;
                var modifierId = ability.id().withSuffix("/" + i);
                // Original nonstackable effects receive null AbilityStack: PerLevel defaults to one.
                double amount = StatResolver.resolve("attribute_multiplier", 0, sources);
                instance.removeModifier(modifierId);
                instance.addTransientModifier(new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_VALUE));
            }
            // Quirk kept: ActionStack does not evaluate endTriggers and ApplyAttribute.EndServer is empty.
        }
    }
}
