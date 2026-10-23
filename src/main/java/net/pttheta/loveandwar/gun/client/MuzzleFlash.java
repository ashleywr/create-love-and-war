package net.pttheta.loveandwar.gun.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel;
import net.pttheta.loveandwar.gun.content.ActionDef;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class MuzzleFlash {
   private static final String DEFAULT_FLASH_MODEL = "flansmod:effects/muzzle_flash_small";
   private static final String DEFAULT_ATTACH_POINT = "shoot_origin";
   private static final List<String> ALL_FLASH_MODELS = List.of(
      "flansmod:effects/muzzle_flash_small",
      "flansmod:effects/muzzle_flash_medium",
      "flansmod:effects/muzzle_flash_large",
      "createloveandwar:effects/muzzle_flash"
   );
   private static String activeModel = "flansmod:effects/muzzle_flash_small";
   private static String activeAttachPoint = "shoot_origin";
   private static final float FIRST_PERSON_SCALE = 2.0F;
   private static final float MUZZLE_NUDGE = -0.0625F;
   private static final long FLASH_NANOS = 50000000L;
   private static long flashStartNanos = Long.MIN_VALUE;
   private static String lastGroup = "";
   private static String predictedGroup = "";
   private static long predictedUntilNanos;
   private static final long PREDICTION_CONFIRM_NANOS = 400000000L;
   private static int lastGroupTotal = -1;

   private MuzzleFlash() {
   }

   @SubscribeEvent
   static void registerModels(RegisterAdditional event) {
      for (String id : ALL_FLASH_MODELS) {
         event.register(modelKey(id));
      }
   }

   private static ModelResourceLocation modelKey(String flashModel) {
      ResourceLocation id = ResourceLocation.parse(flashModel);
      return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath()));
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ItemStack stack = mc.player.getMainHandItem();
         if (!(stack.getItem() instanceof GunItem)) {
            lastGroup = "";
         } else {
            GunActionState state = (GunActionState)stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            String group = state.activeGroup();
            boolean started = !group.isEmpty() && (!group.equals(lastGroup) || state.groupTotalTicks() != lastGroupTotal);
            lastGroup = group;
            lastGroupTotal = state.groupTotalTicks();
            if (started && group.equals(predictedGroup) && System.nanoTime() < predictedUntilNanos) {
               predictedGroup = "";
            } else {
               if (started && hasAttachEffect(stack, group)) {
                  resolveEffect(stack, group);
                  startFlash();
               }
            }
         }
      }
   }

   static void onPredicted(ItemStack stack, String groupKey) {
      if (hasAttachEffect(stack, groupKey)) {
         resolveEffect(stack, groupKey);
         startFlash();
         predictedGroup = groupKey;
         predictedUntilNanos = System.nanoTime() + 400000000L;
      }
   }

   private static void resolveEffect(ItemStack stack, String groupKey) {
      activeModel = "flansmod:effects/muzzle_flash_small";
      activeAttachPoint = "shoot_origin";
      Optional<GunDef> def = GunLookup.gunDef(stack);
      if (!def.isEmpty()) {
         for (ActionGroup group : def.get().actionGroups()) {
            if (group.key().equals(groupKey)) {
               List<ModifierSource> sources = List.of(ModifierSource.of("group", group.modifiers()));
               activeModel = StatResolver.resolveOverride("flash_model", "flansmod:effects/muzzle_flash_small", sources);
               activeAttachPoint = StatResolver.resolveOverride("flash_attach_point", "shoot_origin", sources);
               return;
            }
         }
      }
   }

   private static boolean hasAttachEffect(ItemStack stack, String groupKey) {
      Optional<GunDef> def = GunLookup.gunDef(stack);
      if (def.isEmpty()) {
         return false;
      }

      for (ActionGroup group : def.get().actionGroups()) {
         if (group.key().equals(groupKey)) {
            for (ActionDef action : group.actions()) {
               if ("AttachEffect".equals(action.actionType())) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static String activeAttachPoint() {
      return activeAttachPoint;
   }

   private static void startFlash() {
      flashStartNanos = System.nanoTime();
   }

   public static boolean active() {
      long age = System.nanoTime() - flashStartNanos;
      return age >= 0L && age < 50000000L;
   }

   public static List<BakedQuad> quadsFor(TurboRigBakedModel rig) {
      return quadsFor(rig, null);
   }

   public static List<BakedQuad> quadsFor(TurboRigBakedModel rig, Matrix4f posedAttach) {
      Matrix4f attach = posedAttach != null ? posedAttach : rig.attachPointMatrix(activeAttachPoint);
      if (attach == null) {
         return List.of();
      }

      Matrix4f placement = new Matrix4f(attach).scale(2.0F).translate(0.0F, 0.0F, -0.0625F);
      BakedModel flash = Minecraft.getInstance().getModelManager().getModel(modelKey(activeModel));
      if (flash != null && flash != Minecraft.getInstance().getModelManager().getMissingModel()) {
         List<BakedQuad> source = flash.getQuads(null, null, RandomSource.create());
         List<BakedQuad> out = new ArrayList<>(source.size());

         for (BakedQuad quad : source) {
            out.add(transform(quad, placement));
         }

         return out;
      } else {
         return List.of();
      }
   }

   private static BakedQuad transform(BakedQuad quad, Matrix4f matrix) {
      int[] vertices = (int[])quad.getVertices().clone();
      int stride = vertices.length / 4;
      Vector3f position = new Vector3f();

      for (int i = 0; i < 4; i++) {
         int base = i * stride;
         position.set(Float.intBitsToFloat(vertices[base]), Float.intBitsToFloat(vertices[base + 1]), Float.intBitsToFloat(vertices[base + 2]));
         matrix.transformPosition(position);
         vertices[base] = Float.floatToRawIntBits(position.x);
         vertices[base + 1] = Float.floatToRawIntBits(position.y);
         vertices[base + 2] = Float.floatToRawIntBits(position.z);
         int packed = vertices[base + 7];
         if ((packed & 16777215) != 0) {
            Vector3f n = new Vector3f((byte)packed / 127.0F, (byte)(packed >> 8) / 127.0F, (byte)(packed >> 16) / 127.0F);
            matrix.transformDirection(n).normalize();
            vertices[base + 7] = packed & 0xFF000000
               | (byte)Math.round(n.x * 127.0F) & 255
               | ((byte)Math.round(n.y * 127.0F) & 255) << 8
               | ((byte)Math.round(n.z * 127.0F) & 255) << 16;
         }
      }

      Direction direction = quad.getDirection();
      return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade());
   }
}
