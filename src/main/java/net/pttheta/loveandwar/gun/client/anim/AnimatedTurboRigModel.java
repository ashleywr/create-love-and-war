package net.pttheta.loveandwar.gun.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.client.MuzzleFlash;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigModelData;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigQuadBaker;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;

public final class AnimatedTurboRigModel implements BakedModel {
   private final TurboRigBakedModel base;
   private final Map<String, List<TextureAtlasSprite>> spritesByPartInFaceOrder;
   private final ItemOverrides overrides;
   private Matrix4f lastPosedMuzzle;

   public AnimatedTurboRigModel(TurboRigBakedModel base) {
      this.base = base;
      Map<String, List<TextureAtlasSprite>> sprites = new LinkedHashMap<>();

      for (String partName : base.partNames()) {
         sprites.put(partName, base.partQuads(partName).stream().<TextureAtlasSprite>map(BakedQuad::getSprite).toList());
      }

      this.spritesByPartInFaceOrder = Map.copyOf(sprites);
      this.overrides = new AnimatedTurboRigModel.AnimatedOverrides();
   }

   private List<BakedQuad> animatedQuads(ItemStack stack, boolean localPlayer) {
      float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      Map<String, PartPose> poses = GunPoseResolver.currentPose(stack, partialTick, localPlayer);
      if (poses.isEmpty()) {
         return null;
      }

      TurboRigModelData data = this.base.rawData();
      this.lastPosedMuzzle = this.base.attachPointMatrix(MuzzleFlash.activeAttachPoint(), nodeName -> {
         PartPose nodePose = poses.get(nodeName);
         return nodePose != null && !nodePose.isIdentity() ? nodePose.toMatrix() : null;
      });
      Map<String, Matrix4f> posed = this.base.posedPartTransforms(nodeName -> {
         PartPose nodePose = poses.get(nodeName);
         return nodePose != null && !nodePose.isIdentity() ? nodePose.toMatrix() : null;
      });
      List<BakedQuad> out = new ArrayList<>();

      for (TurboRigModelData.PartDef part : data.parts().values()) {
         Matrix4f placement = posed.getOrDefault(part.name(), this.base.partTransform(part.name()));
         if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
            Matrix4f st = this.base.partTransform(part.name());
            Vector3f a = st.transformPosition(new Vector3f());
            Vector3f b = placement.transformPosition(new Vector3f());
            float d = a.distance(b);
            LoggerFactory.getLogger("createloveandwar/mtx")
               .info("PART {} reuse={} originShift={}", new Object[]{part.name(), placement.equals(st, 1.0E-6F), String.format("%.4f", d)});
         }

         if (placement.equals(this.base.partTransform(part.name()), 1.0E-6F)) {
            out.addAll(this.base.partQuads(part.name()));
         } else {
            List<TextureAtlasSprite> sprites = this.spritesByPartInFaceOrder.getOrDefault(part.name(), List.of());
            int expectedFaces = 0;

            for (TurboRigModelData.TurboElement element : part.elements()) {
               expectedFaces += element.faces().size();
            }

            if (sprites.size() != expectedFaces) {
               out.addAll(this.base.partQuads(part.name()));
            } else {
               int[] cursor = new int[]{0};
               Function<String, TextureAtlasSprite> spriteByFaceOrder = ignoredTextureRef -> cursor[0] < sprites.size()
                  ? sprites.get((int)(cursor[0]++))
                  : null;
               UnaryOperator<Vector3f> root = this.base.rootTransform();
               UnaryOperator<Vector3f> postTransform = v -> root.apply(placement.transformPosition(v));
               List<BakedQuad> partQuads = new ArrayList<>();

               for (TurboRigModelData.TurboElement element : part.elements()) {
                  partQuads.addAll(TurboRigQuadBaker.bakeElement(element, spriteByFaceOrder, postTransform));
               }

               out.addAll(TurboRigQuadBaker.applyMaterial(partQuads, part.material()));
            }
         }
      }

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

      TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.set(TurboRigBakedModel.adsEyeLine(this.base, transformType));
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
         LocalPlayer me = Minecraft.getInstance().player;
         boolean localPlayer = me != null && entity == me && ItemStack.isSameItem(stack, me.getMainHandItem());
         List<BakedQuad> quads = AnimatedTurboRigModel.this.animatedQuads(localPlayer ? me.getMainHandItem() : stack, localPlayer);
         return (BakedModel)(quads == null
            ? AnimatedTurboRigModel.this
            : AnimatedTurboRigModel.this.new PosedModel(quads, AnimatedTurboRigModel.this.lastPosedMuzzle));
      }
   }

   private final class PosedModel implements BakedModel {
      private final List<BakedQuad> quads;
      private final Matrix4f posedMuzzle;

      PosedModel(List<BakedQuad> quads, Matrix4f posedMuzzle) {
         this.quads = quads;
         this.posedMuzzle = posedMuzzle;
      }

      public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
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
         BakedModel icon = AnimatedTurboRigModel.this.base.flatIconModel();
         if (transformType == ItemDisplayContext.GUI && icon != null) {
            return icon;
         }

         if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
            LoggerFactory.getLogger("createloveandwar/ctx")
               .info(
                  "CTX posed={} left={} scale={}", new Object[]{transformType, applyLeftHandTransform, this.getTransforms().getTransform(transformType).scale}
               );
         }

         TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.set(TurboRigBakedModel.adsEyeLine(AnimatedTurboRigModel.this.base, transformType));
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
