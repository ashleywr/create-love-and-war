package net.pttheta.loveandwar.gun.client.geometry;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.client.AdsBlendState;
import net.pttheta.loveandwar.gun.client.MuzzleFlash;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.LoggerFactory;

public final class TurboRigBakedModel implements BakedModel {
   private final Map<String, Map<String, List<BakedQuad>>> quadsByVariantThenPart;
   private final List<TurboRigModelData.AttachPoint> attachPoints;
   private final Map<String, TurboRigModelData.AttachPoint> attachPointsByName;
   private final Map<String, ResourceLocation> icons;
   private final List<String> paintjobs;
   private final TurboRigModelData data;
   private final Map<String, Matrix4f> partTransforms;
   public static final ThreadLocal<Matrix4f> ADS_EYE_LINE_FOR_FRAME = new ThreadLocal<>();
   private static Vector3f muzzleViewSpace;
   private static long muzzleFrame = Long.MIN_VALUE;
   private static double muzzleForward;
   private static Matrix4f lastEyeLine;
   private static Item lastEyeLineItem;
   public static final ThreadLocal<ItemDisplayContext> CONTEXT_FOR_FRAME = new ThreadLocal<>();
   private UnaryOperator<Vector3f> rootTransform = p -> p;
   private final boolean ambientOcclusion;
   private final boolean gui3d;
   private final boolean blockLight;
   private final TextureAtlasSprite particleIcon;
   private final ItemTransforms transforms;
   private final ItemOverrides overrides;
   private final List<BakedQuad> defaultFlatQuads;
   private final BakedModel projectileMesh;
   private final BakedModel flatIconModel;
   private BakedModel heldIconModel;

   private TurboRigBakedModel(
      Map<String, Map<String, List<BakedQuad>>> quadsByVariantThenPart,
      TurboRigModelData data,
      Map<String, Matrix4f> partTransforms,
      boolean ambientOcclusion,
      boolean gui3d,
      boolean blockLight,
      TextureAtlasSprite particleIcon,
      ItemTransforms transforms,
      ItemOverrides overrides,
      TextureAtlasSprite iconSprite
   ) {
      this.quadsByVariantThenPart = quadsByVariantThenPart;
      this.attachPoints = data.attachPoints();
      this.icons = data.icons();
      this.paintjobs = data.paintjobs();
      this.data = data;
      this.partTransforms = partTransforms;
      this.ambientOcclusion = ambientOcclusion;
      this.gui3d = gui3d;
      this.blockLight = blockLight;
      this.particleIcon = particleIcon;
      this.transforms = transforms;
      this.overrides = overrides;
      Map<String, TurboRigModelData.AttachPoint> byName = new LinkedHashMap<>();

      for (TurboRigModelData.AttachPoint attachPoint : this.attachPoints) {
         byName.put(attachPoint.name(), attachPoint);
      }

      this.attachPointsByName = Map.copyOf(byName);
      List<BakedQuad> flat = new ArrayList<>();

      for (List<BakedQuad> partQuads : quadsByVariantThenPart.get("default").values()) {
         flat.addAll(partQuads);
      }

      this.defaultFlatQuads = List.copyOf(flat);
      this.projectileMesh = new FlatIconModel(this.defaultFlatQuads, particleIcon);
      this.flatIconModel = iconSprite == null ? null : new TurboRigBakedModel.FlatIconModel(bakeFlatIcon(iconSprite), iconSprite);
   }

   public static TurboRigBakedModel bake(
      TurboRigModelData data,
      IGeometryBakingContext context,
      Function<Material, TextureAtlasSprite> spriteGetter,
      ModelState modelState,
      ItemOverrides overrides
   ) {
      ModelState effectiveState = UnbakedGeometryHelper.composeRootTransformIntoModelState(modelState, context.getRootTransform());
      Matrix4f rotationMatrix = effectiveState.getRotation().getMatrix();
      UnaryOperator<Vector3f> rootTransform = p -> applyPivotedRotation(p, rotationMatrix);
      Map<String, Matrix4f> partTransforms = TurboRigAttachTransforms.resolvePartTransforms(data);
      Map<String, Map<String, List<BakedQuad>>> quadsByVariantThenPart = new LinkedHashMap<>();
      quadsByVariantThenPart.put("default", bakeAllParts(data, textureResolver(context, spriteGetter, null), partTransforms, rootTransform));

      for (String paintjob : data.paintjobs()) {
         quadsByVariantThenPart.put(paintjob, bakeAllParts(data, textureResolver(context, spriteGetter, paintjob), partTransforms, rootTransform));
      }

      TextureAtlasSprite particleIcon = resolveParticleIcon(context, spriteGetter);
      TextureAtlasSprite iconSprite = resolveIconSprite(data, spriteGetter);
      TurboRigBakedModel built = new TurboRigBakedModel(
         quadsByVariantThenPart,
         data,
         partTransforms,
         context.useAmbientOcclusion(),
         context.isGui3d(),
         context.useBlockLight(),
         particleIcon,
         context.getTransforms(),
         overrides,
         iconSprite
      );
      built.rootTransform = rootTransform;
      if (iconSprite != null && !isGunModel(context.getModelName())) {
         List<BakedQuad> extruded = UnbakedGeometryHelper.bakeElements(
            UnbakedGeometryHelper.createUnbakedItemElements(0, iconSprite), material -> iconSprite, BlockModelRotation.X0_Y0
         );
         built.heldIconModel = built.new FlatIconModel(extruded, iconSprite, true);
      }

      return built;
   }

   private static TextureAtlasSprite resolveIconSprite(TurboRigModelData data, Function<Material, TextureAtlasSprite> spriteGetter) {
      ResourceLocation iconLocation = data.icons().get("default");
      if (iconLocation == null) {
         iconLocation = data.icons().values().stream().findFirst().orElse(null);
      }

      return iconLocation == null ? null : spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, iconLocation));
   }

   private static List<BakedQuad> bakeFlatIcon(TextureAtlasSprite sprite) {
      float minZ = 0.46875F;
      float maxZ = 0.53125F;
      List<BakedQuad> quads = new ArrayList<>(2);
      quads.add(bakeFlatFace(Direction.SOUTH, maxZ, sprite, false));
      quads.add(bakeFlatFace(Direction.NORTH, minZ, sprite, true));
      return quads;
   }

   private static BakedQuad bakeFlatFace(Direction direction, float z, TextureAtlasSprite sprite, boolean mirrorU) {
      float[][] xy = direction == Direction.SOUTH
         ? new float[][]{{0.0F, 1.0F}, {0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}}
         : new float[][]{{1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}, {0.0F, 1.0F}};
      int[] data = new int[32];

      for (int i = 0; i < 4; i++) {
         float x = xy[i][0];
         float y = xy[i][1];
         float u = mirrorU ? 1.0F - x : x;
         float v = 1.0F - y;
         int base = i * 8;
         data[base] = Float.floatToRawIntBits(x);
         data[base + 1] = Float.floatToRawIntBits(y);
         data[base + 2] = Float.floatToRawIntBits(z);
         data[base + 3] = -1;
         data[base + 4] = Float.floatToRawIntBits(sprite.getU(u));
         data[base + 5] = Float.floatToRawIntBits(sprite.getV(v));
      }

      return new BakedQuad(data, -1, direction, sprite, true);
   }

   private static Map<String, List<BakedQuad>> bakeAllParts(
      TurboRigModelData data, Function<String, TextureAtlasSprite> textureResolver, Map<String, Matrix4f> partTransforms, UnaryOperator<Vector3f> rootTransform
   ) {
      Map<String, List<BakedQuad>> result = new LinkedHashMap<>();

      for (TurboRigModelData.PartDef part : data.parts().values()) {
         Matrix4f placement = partTransforms.getOrDefault(part.name(), new Matrix4f());
         UnaryOperator<Vector3f> postTransform = v -> rootTransform.apply(placement.transformPosition(v));
         List<BakedQuad> quads = new ArrayList<>();

         for (TurboRigModelData.TurboElement element : part.elements()) {
            quads.addAll(TurboRigQuadBaker.bakeElement(element, textureResolver, postTransform));
         }

         result.put(part.name(), List.copyOf(TurboRigQuadBaker.applyMaterial(quads, part.material())));
      }

      return result;
   }

   private static Function<String, TextureAtlasSprite> textureResolver(
      IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteGetter, String paintjobOverride
   ) {
      return textureKey -> {
         String resolvedKey = "default".equals(textureKey) && paintjobOverride != null ? paintjobOverride : textureKey;
         return context.hasMaterial(resolvedKey) ? spriteGetter.apply(context.getMaterial(resolvedKey)) : null;
      };
   }

   private static boolean isGunModel(String modelName) {
      int hash = modelName.indexOf(35);
      ResourceLocation rl = ResourceLocation.tryParse(hash >= 0 ? modelName.substring(0, hash) : modelName);
      if (rl == null) {
         return true;
      }

      String path = rl.getPath().startsWith("item/") ? rl.getPath().substring(5) : rl.getPath();
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), path));
      return item instanceof GunItem;
   }

   private static TextureAtlasSprite resolveParticleIcon(IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteGetter) {
      for (String candidate : new String[]{"particle", "default", "layer0"}) {
         if (context.hasMaterial(candidate)) {
            return spriteGetter.apply(context.getMaterial(candidate));
         }
      }

      throw new IllegalStateException("turborig model '" + context.getModelName() + "' has none of particle/default/layer0 textures to use as a particle icon");
   }

   private static Vector3f applyPivotedRotation(Vector3f p, Matrix4f rotationMatrix) {
      Vector4f v = new Vector4f(p.x() - 0.5F, p.y() - 0.5F, p.z() - 0.5F, 1.0F);
      rotationMatrix.transform(v);
      return new Vector3f(v.x() + 0.5F, v.y() + 0.5F, v.z() + 0.5F);
   }

   public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
      if (face != null) {
         return List.of();
      }

      if (MuzzleFlash.active() && isHeldFirstPerson()) {
         List<BakedQuad> flash = MuzzleFlash.quadsFor(this);
         if (!flash.isEmpty()) {
            List<BakedQuad> combined = new ArrayList<>(this.defaultFlatQuads.size() + flash.size());
            combined.addAll(this.defaultFlatQuads);
            combined.addAll(flash);
            return combined;
         }
      }

      return this.defaultFlatQuads;
   }

   /** Unposed authored geometry, without held-icon or local gun muzzle-flash state. */
   public BakedModel projectileMesh() { return this.projectileMesh; }

   private static boolean isHeldFirstPerson() {
      ItemDisplayContext ctx = CONTEXT_FOR_FRAME.get();
      return ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
   }

   private static void captureMuzzle(TurboRigBakedModel rig, ItemDisplayContext ctx, PoseStack poseStack) {
      if (ctx.firstPerson()) {
         Matrix4f attach = rig.attachPointMatrix("shoot_origin");
         if (attach != null) {
            Vector3f local = attach.getTranslation(new Vector3f());
            Vector3f view = new Matrix4f(poseStack.last().pose()).translate(-0.5F, -0.5F, -0.5F).transformPosition(local);
            muzzleViewSpace = view;
            muzzleForward = Math.abs(local.x());
            muzzleFrame = System.nanoTime();
         }
      }
   }

   public static double muzzleForwardDistance() {
      long ageNs = System.nanoTime() - muzzleFrame;
      return muzzleViewSpace != null && ageNs >= 0L && ageNs < 200000000L ? muzzleForward : 0.0;
   }

   public static void captureMuzzleFor(TurboRigBakedModel rig, ItemDisplayContext ctx, PoseStack poseStack) {
      captureMuzzle(rig, ctx, poseStack);
   }

   public static Vector3f muzzleViewSpace() {
      return muzzleViewSpace;
   }

   public Matrix4f attachPointMatrix(String name) {
      return TurboRigAttachTransforms.attachPointMatrix(this.rawData(), name);
   }

   public Matrix4f attachPointMatrix(String name, Function<String, Matrix4f> poseByNode) {
      return TurboRigAttachTransforms.attachPointMatrix(this.rawData(), name, poseByNode);
   }

   public boolean useAmbientOcclusion() {
      return this.ambientOcclusion;
   }

   public boolean isGui3d() {
      return this.gui3d;
   }

   public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
      if (transformType == ItemDisplayContext.GUI && this.flatIconModel != null) {
         return this.flatIconModel;
      }

      if (this.heldIconModel != null) {
         poseStack.translate(-0.5F, -0.5F, -0.5F);
         poseStack.scale(0.55F, 0.55F, 0.55F);
         poseStack.translate(0.4F, 0.5F, 0.5F);
         poseStack.translate(0.5F, 0.5F, 0.5F);
         return this.heldIconModel;
      }

      ADS_EYE_LINE_FOR_FRAME.set(adsEyeLine(this, transformType));
      CONTEXT_FOR_FRAME.set(transformType);

      try {
         applyRigTransform(this.getTransforms(), transformType, applyLeftHandTransform, poseStack);
         captureMuzzle(this, transformType, poseStack);
      } finally {
         ADS_EYE_LINE_FOR_FRAME.remove();
      }

      return this;
   }

   public boolean usesBlockLight() {
      return this.flatIconModel == null && this.blockLight;
   }

   public boolean isCustomRenderer() {
      return false;
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.particleIcon;
   }

   private static float yawCorrection() {
      return 0.0F;
   }

   public static void applyRigTransform(ItemTransforms transforms, ItemDisplayContext ctx, boolean leftHand, PoseStack poseStack) {
      if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
         LoggerFactory.getLogger("createloveandwar/art").info("ART ctx={}{}", ctx, "");
      }

      switch (ctx) {
         case FIRST_PERSON_RIGHT_HAND:
            poseStack.translate(-0.56F, 0.5F, 0.5F);
            break;
         case FIRST_PERSON_LEFT_HAND:
            poseStack.translate(0.56F, 0.5F, 0.5F);
            break;
         case THIRD_PERSON_RIGHT_HAND:
         case THIRD_PERSON_LEFT_HAND:
            poseStack.mulPose(new Quaternionf().rotateY((float)Math.toRadians(-90.0)));
         case GROUND:
            break;
         default:
            poseStack.translate(-0.5F, -0.5F, -0.5F);
      }

      ItemTransform t = transforms.getTransform(ctx);
      Matrix4f adsEye = ADS_EYE_LINE_FOR_FRAME.get();
      float blend = AdsBlendState.blend();
      if (adsEye != null && blend > 0.001F && t != ItemTransform.NO_TRANSFORM) {
         applyAdsPoseBlended(t, adsEye, blend, poseStack);
      } else if (t != ItemTransform.NO_TRANSFORM) {
         poseStack.translate(t.translation.x(), t.translation.y(), t.translation.z());
         poseStack.mulPose(flansEuler(t.rotation));
         poseStack.scale(t.scale.x(), t.scale.x(), t.scale.x());
      }

      if (yawCorrection() != 0.0F) {
         poseStack.mulPose(new Quaternionf().rotateY((float)Math.toRadians(yawCorrection())));
      }

      poseStack.translate(0.5F, 0.5F, 0.5F);
   }

   private static Quaternionf flansEuler(Vector3f euler) {
      return new Quaternionf().rotateY((float)Math.toRadians(-euler.y())).rotateX((float)Math.toRadians(-euler.x())).rotateZ((float)Math.toRadians(-euler.z()));
   }

   public ItemTransforms getTransforms() {
      return this.transforms;
   }

   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   public Set<String> partNames() {
      return this.quadsByVariantThenPart.get("default").keySet();
   }

   public List<BakedQuad> partQuads(String partName) {
      return this.partQuads(partName, "default");
   }

   public List<BakedQuad> partQuads(String partName, String paintjob) {
      return this.quadsByVariantThenPart.getOrDefault(paintjob, Map.of()).getOrDefault(partName, List.of());
   }

   public List<String> paintjobs() {
      return this.paintjobs;
   }

   public List<TurboRigModelData.AttachPoint> attachPoints() {
      return this.attachPoints;
   }

   public Optional<TurboRigModelData.AttachPoint> attachPoint(String name) {
      return Optional.ofNullable(this.attachPointsByName.get(name));
   }

   public Optional<ResourceLocation> icon(String paintjob) {
      return Optional.ofNullable(this.icons.get(paintjob));
   }

   public TurboRigModelData rawData() {
      return this.data;
   }

   public Matrix4f partTransform(String partName) {
      Matrix4f m = this.partTransforms.get(partName);
      return m == null ? new Matrix4f() : new Matrix4f(m);
   }

   public Map<String, Matrix4f> posedPartTransforms(Function<String, Matrix4f> poseByNode) {
      return TurboRigAttachTransforms.resolvePartTransforms(this.data, poseByNode);
   }

   public boolean drawsAsIcon() {
      return this.heldIconModel != null;
   }

   public BakedModel flatIconModel() {
      return this.flatIconModel;
   }

   public UnaryOperator<Vector3f> rootTransform() {
      return this.rootTransform;
   }

   public static void applyAdsEyeLine(TurboRigBakedModel rig, ItemDisplayContext ctx, PoseStack poseStack) {
   }

   public static Matrix4f adsEyeLine(TurboRigBakedModel rig, ItemDisplayContext ctx) {
      if (ctx != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
         return null;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return null;
      }

      GunActionState st = (GunActionState)mc.player.getMainHandItem().getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
      String adsKey = null;

      for (String t : st.activeToggles()) {
         if (t.startsWith("ads")) {
            adsKey = t;
            break;
         }
      }

      if (adsKey == null) {
         if (lastEyeLine != null && mc.player.getMainHandItem().getItem() == lastEyeLineItem && AdsBlendState.blend() > 0.001F) {
            return lastEyeLine;
         }

         lastEyeLine = null;
         lastEyeLineItem = null;
         return null;
      } else {
         String eyeName = adsKey.replaceFirst("^ads", "eye_line");
         Matrix4f resolved = rig.attachPointMatrix(eyeName);
         if (resolved != null) {
            lastEyeLine = resolved;
            lastEyeLineItem = mc.player.getMainHandItem().getItem();
         }

         if (resolved == null && Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
            LoggerFactory.getLogger("createloveandwar/ads")
               .info(
                  "ADS toggle={} wants '{}' but rig has {}",
                  new Object[]{adsKey, eyeName, rig.attachPoints().stream().map(TurboRigModelData.AttachPoint::name).toList()}
               );
         }

         return resolved;
      }
   }

   private static void applyAdsPoseBlended(ItemTransform display, Matrix4f eyeLine, float blend, PoseStack poseStack) {
      Matrix4f hip = hipMatrix(display);
      Matrix4f aim = adsMatrix(display, eyeLine);
      Vector3f pos = hip.getTranslation(new Vector3f()).lerp(aim.getTranslation(new Vector3f()), blend);
      Quaternionf rot = hip.getNormalizedRotation(new Quaternionf()).slerp(aim.getNormalizedRotation(new Quaternionf()), blend);
      poseStack.translate(pos.x, pos.y, pos.z);
      poseStack.mulPose(rot);
      poseStack.scale(display.scale.x(), display.scale.x(), display.scale.x());
   }

   private static Matrix4f hipMatrix(ItemTransform display) {
      return new Matrix4f().translate(display.translation.x(), display.translation.y(), display.translation.z()).rotate(flansEuler(display.rotation));
   }

   private static Matrix4f adsMatrix(ItemTransform display, Matrix4f eyeLine) {
      Vector3f eyePos = eyeLine.getTranslation(new Vector3f());
      Quaternionf eyeRot = eyeLine.getNormalizedRotation(new Quaternionf());
      return new Matrix4f()
         .translate(0.0F, 0.0F, -0.5F)
         .rotate(flansEuler(display.rotation))
         .rotate(eyeRot.conjugate())
         .translate(-eyePos.x, -eyePos.y, -eyePos.z);
   }

   private final class FlatIconModel implements BakedModel {
      private final List<BakedQuad> quads;
      private final TextureAtlasSprite sprite;
      private final boolean blockRendered;

      FlatIconModel(List<BakedQuad> quads, TextureAtlasSprite sprite) {
         this(quads, sprite, false);
      }

      FlatIconModel(List<BakedQuad> quads, TextureAtlasSprite sprite, boolean blockRendered) {
         this.quads = quads;
         this.sprite = sprite;
         this.blockRendered = blockRendered;
      }

      public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
         return this.blockRendered ? List.of(RenderType.cutout()) : BakedModel.super.getRenderTypes(stack, fabulous);
      }

      public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
         return face == null ? this.quads : List.of();
      }

      public boolean useAmbientOcclusion() {
         return TurboRigBakedModel.this.ambientOcclusion;
      }

      public boolean isGui3d() {
         return false;
      }

      public boolean usesBlockLight() {
         return false;
      }

      public boolean isCustomRenderer() {
         return false;
      }

      public TextureAtlasSprite getParticleIcon() {
         return this.sprite;
      }

      public ItemTransforms getTransforms() {
         return TurboRigBakedModel.this.transforms;
      }

      public ItemOverrides getOverrides() {
         return ItemOverrides.EMPTY;
      }
   }
}
