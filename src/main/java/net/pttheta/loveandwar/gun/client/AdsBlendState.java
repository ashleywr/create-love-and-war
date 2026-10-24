package net.pttheta.loveandwar.gun.client;

import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.gun.runtime.StatSources;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class AdsBlendState {
    private static final AdsPoseState POSES = new AdsPoseState();
    private static long lastFrameNanos;

    private AdsBlendState() {}

    @SubscribeEvent
    static void onRenderFrame(Pre event) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0 : (float)(now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        var mc = Minecraft.getInstance();
        // Quirk kept: original RenderTick leaves the map untouched while no local player exists.
        if (mc.player == null) return;
        POSES.beginFrame();
        for (var hand : InteractionHand.values()) {
            var stack = mc.player.getItemInHand(hand);
            if (!(stack.getItem() instanceof GunItem)) continue;
            var gun = GunLookup.gunDef(stack);
            if (gun.isEmpty()) continue;
            var attachments = stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
            for (var entry : GunActiveGroups.of(stack).entries()) {
                if (!entry.owner().equals(mc.player.getUUID())) continue;
                var group = entry.resolve();
                if (group.isEmpty()) continue;
                for (var action : group.get().actions()) {
                    if (!"AimDownSights".equals(action.actionType()) && !"Scope".equals(action.actionType())) continue;
                    String eye = StatResolver.resolveOverride("eye_line_name", "eye_line", StatSources.forFiring(gun.get(), group.get(), attachments));
                    var origin = entry.origin();
                    String path = origin.attachmentIndex() >= 0 ? origin.attachmentType() + "/" + origin.attachmentIndex() + "/" + entry.key() : entry.key();
                    POSES.activate(displayHand(hand, mc.options.mainHand().get()), path,
                            new AdsPoseState.Selection(eye, origin.attachmentType(), origin.attachmentIndex()));
                }
            }
        }
        POSES.endFrame(dt);
    }

    public static ItemDisplayContext displayHand(InteractionHand hand, HumanoidArm mainArm) {
        return (hand == InteractionHand.MAIN_HAND) == (mainArm == HumanoidArm.RIGHT)
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    /** Legacy FX/overlay consumers use the full sight transition; cross-hand pose attenuation is separate. */
    public static float blend() {
        return Math.max(POSES.blend(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND), POSES.blend(ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
    }

    public static boolean fullyAimed() { return blend() > 0.999F; }
    public static float poseBlend(ItemDisplayContext hand) { return POSES.poseBlend(hand); }
    public static boolean hasOtherHand(ItemDisplayContext hand) { return POSES.hasOtherHand(hand); }
    public static Matrix4f eyeLine(ItemDisplayContext hand, Function<AdsPoseState.Selection, Matrix4f> lookup) { return POSES.eyeLine(hand, lookup); }
}
