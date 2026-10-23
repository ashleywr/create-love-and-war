package net.pttheta.loveandwar.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.entity.GunProjectile;
import net.pttheta.loveandwar.gun.client.GunProjectileEffects;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;

public final class GunProjectileRenderer extends EntityRenderer<GunProjectile> {
    public GunProjectileRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(GunProjectile bullet) { return TextureAtlas.LOCATION_BLOCKS; }

    @Override public void render(GunProjectile bullet, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        var model = GunProjectileEffects.model(bullet);
        if (model instanceof TurboRigBakedModel rig) model = rig.projectileMesh();
        if (model != null) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, bullet.yRotO, bullet.getYRot()) - 90));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, bullet.xRotO, bullet.getXRot())));
            // Direct mesh rendering avoids the held-ammunition icon substitution and item display transforms.
            Minecraft.getInstance().getItemRenderer().renderModelLists(model, ItemStack.EMPTY, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)));
            pose.popPose();
        }
        super.render(bullet, yaw, partialTick, pose, buffers, light);
    }
}
