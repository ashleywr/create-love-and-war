package net.pttheta.loveandwar.gun.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.client.MuzzleFlash;
import net.pttheta.loveandwar.gun.client.AdsBlendState;
import net.pttheta.loveandwar.gun.client.ScopeVisibility;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigRenderTree;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.joml.Matrix4f;
import org.slf4j.LoggerFactory;

public final class AnimatedTurboRigModel implements BakedModel {
   private final TurboRigBakedModel base;
   private final ItemOverrides overrides;

   public AnimatedTurboRigModel(TurboRigBakedModel base) {
      this.base = base;
      this.overrides = new AnimatedTurboRigModel.AnimatedOverrides();
   }

   private List<BakedQuad> animatedQuads(ItemStack renderStack,ItemStack contextStack,Function<String,Matrix4f> poses) {
      String paint=renderStack.getOrDefault(ModDataComponents.GUN_PAINTJOB,"default");
      var def=GunLookup.gunDef(contextStack);
      var attachments=contextStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY);
      List<BakedQuad> out = new ArrayList<>();
      this.base.renderTree().visit(poses,(name,placement)->{
         var mount=renderStack.getItem() instanceof GunItem?TurboRigRenderTree.mount(name):null;
         if(mount!=null&&def.isPresent()) {
            var slots=attachments.forCategory(mount.category());
            var installed=mount.index()<slots.size()?slots.get(mount.index()):ItemStack.EMPTY;
            if(GunLookup.attachmentDef(installed).isPresent()) {
               // Definition presence, not renderer success, controls replacement in GunItemRenderer.
               var model=Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(installed);
               if(model instanceof AnimatedTurboRigModel animated)model=animated.baseRig();
               if(model instanceof TurboRigBakedModel child) {
                  String childPaint=installed.getOrDefault(ModDataComponents.GUN_PAINTJOB,"default");
                  child.renderTree().visit(ignored->null,(childName,childPlacement)->{
                     out.addAll(child.meshPart(childName,childPaint,new Matrix4f(placement).mul(childPlacement),this.base));
                     return true;
                  });
               }
               if(GunLookup.slotFor(def.get().attachmentSlots(),mount.category()).hideDefaultMesh())return false;
            }
         }
         out.addAll(this.base.meshPart(name,paint,placement,this.base));return true;
      });
      return out;
   }

   public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
      return this.base.getQuads(state, face, random);
   }

   public boolean useAmbientOcclusion() {
      return this.base.useAmbientOcclusion();
   }

   public boolean isGui3d() {
      return this.base.isGui3d();
   }

   public boolean usesBlockLight() {
      return this.base.usesBlockLight();
   }

   public boolean isCustomRenderer() {
      return this.base.isCustomRenderer();
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.base.getParticleIcon();
   }

   public ItemTransforms getTransforms() {
      return this.base.getTransforms();
   }

   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
      if (this.base.drawsAsIcon()) {
         return this.base.applyTransform(transformType, poseStack, applyLeftHandTransform);
      }

      BakedModel icon = this.base.flatIconModel();
      if (transformType == ItemDisplayContext.GUI && icon != null) {
         return icon;
      }

      if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
         LoggerFactory.getLogger("createloveandwar/ctx").info("CTX outer={} left={}", transformType, applyLeftHandTransform);
      }

      TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.remove();
      TurboRigBakedModel.CONTEXT_FOR_FRAME.set(transformType);

      try {
         TurboRigBakedModel.applyRigTransform(this.getTransforms(), transformType, applyLeftHandTransform, poseStack);
         TurboRigBakedModel.captureMuzzleFor(this.base, transformType, poseStack);
      } finally {
         TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.remove();
      }

      return this;
   }

   public TurboRigBakedModel baseRig() {
      return this.base;
   }

   private final class AnimatedOverrides extends ItemOverrides {
      public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
         // Vanilla can still render the old stack during re-equip after the selected hand has changed.
         return AnimatedTurboRigModel.this.new PosedModel(stack);
      }
   }

   private final class PosedModel implements BakedModel {
      private List<BakedQuad> quads;
      private Matrix4f posedMuzzle;
      private final ItemStack renderStack;
      private ScopeVisibility.GunRender visibility=ScopeVisibility.GunRender.VISIBLE;

      PosedModel(ItemStack renderStack) {
         this.renderStack = renderStack;
      }

      public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
         if(this.visibility!=ScopeVisibility.GunRender.VISIBLE){
            // Original FlashEffectRenderer is independent of ScopeAction.ShouldRender's gun-mesh gate.
            return this.visibility==ScopeVisibility.GunRender.SCOPED&&face==null&&MuzzleFlash.active()
               ?MuzzleFlash.quadsFor(AnimatedTurboRigModel.this.base,this.posedMuzzle):List.of();
         }
         if (this.quads == null) return AnimatedTurboRigModel.this.base.getQuads(state, face, random);
         if (face != null) {
            return List.of();
         }

         if (MuzzleFlash.active() && TurboRigBakedModel.CONTEXT_FOR_FRAME.get() != null && TurboRigBakedModel.CONTEXT_FOR_FRAME.get().firstPerson()) {
            List<BakedQuad> flash = MuzzleFlash.quadsFor(AnimatedTurboRigModel.this.base, this.posedMuzzle);
            if (!flash.isEmpty()) {
               List<BakedQuad> combined = new ArrayList<>(this.quads.size() + flash.size());
               combined.addAll(this.quads);
               combined.addAll(flash);
               return combined;
            }
         }

         return this.quads;
      }

      public boolean useAmbientOcclusion() {
         return AnimatedTurboRigModel.this.base.useAmbientOcclusion();
      }

      public boolean isGui3d() {
         return AnimatedTurboRigModel.this.base.isGui3d();
      }

      public boolean usesBlockLight() {
         return AnimatedTurboRigModel.this.base.usesBlockLight();
      }

      public boolean isCustomRenderer() {
         return false;
      }

      public TextureAtlasSprite getParticleIcon() {
         return AnimatedTurboRigModel.this.base.getParticleIcon();
      }

      public ItemTransforms getTransforms() {
         return AnimatedTurboRigModel.this.base.getTransforms();
      }

      public ItemOverrides getOverrides() {
         return ItemOverrides.EMPTY;
      }

      public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
         this.visibility=this.renderStack.getItem() instanceof net.pttheta.loveandwar.gun.runtime.GunItem?ScopeVisibility.renderState(transformType):ScopeVisibility.GunRender.VISIBLE;
         BakedModel icon=AnimatedTurboRigModel.this.base.applySelectedIcon(this.renderStack.getOrDefault(ModDataComponents.GUN_PAINTJOB,"default"),transformType,poseStack);
         if(icon!=null)return icon;

         var mc=Minecraft.getInstance();var contextStack=this.renderStack;
         if(this.renderStack.getItem() instanceof GunItem&&transformType.firstPerson()&&mc.player!=null) {
            var hand=transformType==AdsBlendState.displayHand(InteractionHand.MAIN_HAND,mc.options.mainHand().get())?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            contextStack=mc.player.getItemInHand(hand);
         }
         // Ordinary first-person entry always uses the current local shooter, even across different gun items.
         // The existing prediction clock is main-hand only; offhand prediction remains a separate audit.
         boolean predicted=transformType.firstPerson()&&mc.player!=null&&contextStack==mc.player.getMainHandItem();
         var poses=GunPoseResolver.currentPose(contextStack,mc.getTimer().getGameTimeDeltaPartialTick(false),predicted);
         Function<String,Matrix4f> poseByNode=name->{var pose=poses.get(name);return pose==null||pose.isIdentity()?null:pose.toMatrix();};
         this.posedMuzzle=poses.isEmpty()?null:AnimatedTurboRigModel.this.base.attachPointMatrix(MuzzleFlash.activeAttachPoint(),poseByNode);
         this.quads=this.visibility==ScopeVisibility.GunRender.VISIBLE?AnimatedTurboRigModel.this.animatedQuads(this.renderStack,contextStack,poseByNode):List.of();

         if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
            LoggerFactory.getLogger("createloveandwar/ctx")
               .info(
                  "CTX posed={} left={} scale={}", new Object[]{transformType, applyLeftHandTransform, this.getTransforms().getTransform(transformType).scale}
               );
         }

         TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.set(TurboRigBakedModel.adsEyeLine(AnimatedTurboRigModel.this.base, transformType, this.renderStack));
         TurboRigBakedModel.CONTEXT_FOR_FRAME.set(transformType);

         try {
            TurboRigBakedModel.applyRigTransform(this.getTransforms(), transformType, applyLeftHandTransform, poseStack);
            TurboRigBakedModel.captureMuzzleFor(AnimatedTurboRigModel.this.base, transformType, poseStack);
         } finally {
            TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.remove();
         }

         return this;
      }
   }
}
