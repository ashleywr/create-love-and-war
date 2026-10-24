package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.runtime.GunIdentity;
import net.pttheta.loveandwar.network.ModMessages;
import org.slf4j.LoggerFactory;

/** Controlled packet ordering plus the actual interaction/release handler, in a snapshot scenario. */
@EventBusSubscriber(modid="createloveandwar", value=Dist.CLIENT)
public final class GunInputIdentityClientScenario {
    private static void check(String name, boolean pass) {
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_IDENTITY_CHECK name={} pass={}", name, pass);
        if (!pass) throw new IllegalStateException(name);
    }
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_identity_client").then(Commands.literal("queued").executes(context -> run(false)))
            .then(Commands.literal("release").executes(context -> run(true))));
    }
    private static int run(boolean releaseScenario) {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.getConnection() == null) return 0;
        mc.setScreen(null);
        var a = GunIdentity.id(mc.player.getInventory().getItem(0)); var b = GunIdentity.id(mc.player.getInventory().getItem(1));
        check("synced_distinct_ids", !a.equals(GunIdentity.INVALID) && !b.equals(GunIdentity.INVALID) && !a.equals(b));
        check("initial_slot_zero", mc.player.getInventory().selected == 0);
        var first = CGunInput.capture(mc.player, InteractionHand.MAIN_HAND, "Fire1").orElseThrow();
        if (releaseScenario) {
            mc.options.keyAttack.setDown(true);
            GunLeftClickFire.onInteractionKey(new InputEvent.InteractionKeyMappingTriggered(0, mc.options.keyAttack, InteractionHand.MAIN_HAND));
        }
        mc.player.getInventory().selected = 1;
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(1));
        if (releaseScenario) {
            var second = CGunInput.capture(mc.player, InteractionHand.MAIN_HAND, "Fire1").orElseThrow();
            ModMessages.sendToServer(second);
            mc.options.keyAttack.setDown(false);
            GunLeftClickFire.onClientTick(new ClientTickEvent.Post());
            check("release_retains_original_reference", first.released().gunId().equals(a));
        } else {
            // The server receives slot B before this request, while the request still names A.
            ModMessages.sendToServer(first); ModMessages.sendToServer(first.released());
            check("queued_reference_survives_switch", first.gunId().equals(a) && first.inventorySlot() == 0);
        }
        return 1;
    }
}
