package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.network.GunPredictionClaim;
import net.pttheta.loveandwar.gun.network.GunShotReceipt;
import net.pttheta.loveandwar.gun.network.SGunPresentationEnd;
import net.pttheta.loveandwar.gun.network.SGunRecoil;
import net.pttheta.loveandwar.gun.network.GunAuthoredInput;
import net.pttheta.loveandwar.network.ModMessages;

import java.util.UUID;

/** Server validates input/retained provider; a client claim controls only its own presentation. */
public final class GunPresentation {
    private static final ThreadLocal<Input> INPUT = new ThreadLocal<>();
    private static final ThreadLocal<Activation> CALLBACK = new ThreadLocal<>();

    private static final class Input {
        final CGunInput packet;
        boolean matched;

        Input(CGunInput packet) {
            this.packet = packet;
        }
    }

    static final class Activation {
        final UUID gun, server;
        final GunPredictionClaim claim;
        int consumed, action;
        long sequence;
        boolean retired;
        final boolean authored;

        Activation(ItemStack stack, UUID server, String group, GunActiveGroups.Origin origin) {
            gun = GunIdentity.id(stack);
            this.server = server;
            var input = INPUT.get();
            var wanted = input == null ? GunPredictionClaim.NONE : input.packet.prediction();
            claim =
                    wanted.present()
                                    && input.packet.gunId().equals(gun)
                                    && wanted.group().equals(group)
                                    && wanted.origin().equals(origin)
                            ? wanted
                            : GunPredictionClaim.NONE;
            authored=claim.present()&&input.packet.authored().present();
            confirm(this);
        }

        void run(int action, Runnable callback) {
            var previous = CALLBACK.get();
            int previousAction = this.action;
            this.action = action;
            CALLBACK.set(this);
            try {
                callback.run();
            } finally {
                this.action = previousAction;
                if (previous == null) CALLBACK.remove();
                else CALLBACK.set(previous);
            }
        }

        void retire(ServerPlayer player, ItemStack stack) {
            if (retired || !claim.present()) return;
            retired = true;
            ModMessages.sendToPlayer(
                    new SGunPresentationEnd(
                            gun,
                            claim.activation(),
                            consumed,
                            GunAmmo.revision(stack),
                            GunAmmo.count(stack)),
                    player);
        }
    }

    private GunPresentation() {}
    static GunAuthoredInput authored(){var input=INPUT.get();return input==null?GunAuthoredInput.NONE:input.packet.authored();}
    static GunPredictionClaim claim(){var input=INPUT.get();return input==null?GunPredictionClaim.NONE:input.packet.prediction();}

    public static void input(
            ServerPlayer player, ItemStack stack, CGunInput packet, Runnable handler) {
        var previous = INPUT.get();
        var input = new Input(packet);
        INPUT.set(input);
        try {
            handler.run();
        } finally {
            if (previous == null) INPUT.remove();
            else INPUT.set(previous);
            if (packet.pressed() && packet.prediction().present() && !input.matched)
                ModMessages.sendToPlayer(
                        new SGunPresentationEnd(
                                packet.gunId(),
                                packet.prediction().activation(),
                                0,
                                GunAmmo.revision(stack),
                                GunAmmo.count(stack)),
                        player);
        }
    }

    static boolean permits(String group, GunActiveGroups.Origin origin) {
        var input = INPUT.get();
        return input == null
                || !input.packet.prediction().present()
                || input.packet.prediction().group().equals(group)
                        && input.packet.prediction().origin().equals(origin);
    }

    static boolean permits(Activation existing) {
        var input = INPUT.get();
        if(existing.authored&&(input==null||!input.packet.authored().present()))return false;
        if(input!=null&&input.packet.authored().present()!=existing.authored)return false;
        return input == null || !input.packet.prediction().present() || existing.claim.present();
    }

    static void confirm(Activation activation) {
        var input = INPUT.get();
        if (input != null
                && input.packet.gunId().equals(activation.gun)
                && activation.claim.present()
                && input.packet.prediction().equals(activation.claim)) input.matched = true;
    }

    static void shot(
            ServerPlayer player,
            ItemStack stack,
            float horizontal,
            float vertical,
            long trigger,
            boolean consumed) {
        var activation = CALLBACK.get();
        GunShotReceipt receipt = GunShotReceipt.NONE;
        if (activation != null && !activation.gun.equals(GunIdentity.INVALID)) {
            if (consumed) activation.consumed++;
            receipt =
                    new GunShotReceipt(
                            activation.gun,
                            activation.server,
                            activation.action,
                            activation.sequence++,
                            activation.claim.activation(),
                            activation.consumed,
                            GunAmmo.revision(stack),
                            GunAmmo.count(stack),
                            activation.claim.present());
        }
        ModMessages.sendToPlayer(new SGunRecoil(horizontal, vertical, trigger, receipt), player);
    }
}
