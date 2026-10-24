package net.pttheta.loveandwar.gun.client;

import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pttheta.loveandwar.gun.content.stat.SightFov;
import net.pttheta.loveandwar.gun.runtime.*;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

/** Opt-in integrated-client checks; /law_test_timing_setup snapshots and cleanup restores the inventory. */
@EventBusSubscriber(modid="createloveandwar",value=Dist.CLIENT)
public final class GunSightClientScenario {
    private static String providers(ItemStack gun) {
        return GunActiveGroups.of(gun).entries().stream().map(entry -> entry.origin().attachmentType()+"/"+entry.origin().attachmentIndex()+"/"+entry.origin().definitionKey()+"#"+entry.key()).collect(Collectors.joining(","));
    }
    private static String activations(ItemStack gun) {
        return GunActiveGroups.of(gun).entries().stream().map(entry -> entry.activation().toString()).collect(Collectors.joining(","));
    }
    @SubscribeEvent public static void server(RegisterCommandsEvent event) {
        if(!Boolean.getBoolean("law.enableClientScenario"))return;
        var command=Commands.literal("law_test_sight_attachment").requires(source->source.hasPermission(2));
        for(String key:new String[]{"clear","attachment_red_dot","attachment_suit_sight","attachment_telescopic_sight"}) {
            command.then(Commands.literal(key).executes(context->{
                var player=context.getSource().getPlayerOrException();var gun=player.getMainHandItem();
                if(!java.util.Set.of("g11k2","krag").contains(GunLookup.contentKey(gun)))throw new IllegalStateException("Use G11 or Krag timing setup before the sight fixture");
                var attachments=gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY);
                var sight=key.equals("clear")?java.util.List.<ItemStack>of():java.util.List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:"+key))));
                gun.set(ModDataComponents.GUN_ATTACHMENTS,attachments.with("scope",sight));
                player.inventoryMenu.broadcastChanges();
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SIGHT_INSTALL attachment={}",key);return 1;
            }));
        }
        event.getDispatcher().register(command);
        event.getDispatcher().register(Commands.literal("law_test_sight_status").requires(source->source.hasPermission(2)).executes(context->{
            var player=context.getSource().getPlayerOrException();var gun=player.getMainHandItem();
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SIGHT_SERVER providers=[{}] activations=[{}] toggles={}",providers(gun),activations(gun),gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE,GunActionState.IDLE).activeToggles());return 1;
        }));
    }
    @SubscribeEvent public static void client(RegisterClientCommandsEvent event) {
        if(!Boolean.getBoolean("law.enableClientScenario"))return;
        event.getDispatcher().register(Commands.literal("law_test_sight_check").executes(context -> check()));
        var pose=Commands.literal("law_test_ads_pose").executes(context -> pose("g11_tele"));
        for(String name:new String[]{"g11_tele","g11_mount_only","g11_base","krag_0","krag_1","krag_tele","gyrojet"})
            pose.then(Commands.literal(name).executes(context -> pose(name)));
        event.getDispatcher().register(pose);
        event.getDispatcher().register(Commands.literal("law_test_ads_render_stack").executes(context -> renderStack()));
        event.getDispatcher().register(Commands.literal("law_test_ads_icons").executes(context -> icons()));
        var arm=Commands.literal("law_test_ads_arm");
        for(var side:net.minecraft.world.entity.HumanoidArm.values())arm.then(Commands.literal(side.name().toLowerCase(java.util.Locale.ROOT)).executes(context->{
            var mc=Minecraft.getInstance();mc.options.mainHand().set(side);mc.options.broadcastOptions();return 1;
        }));
        event.getDispatcher().register(arm);
        event.getDispatcher().register(Commands.literal("law_test_sight_client").executes(context->{
            var player=Minecraft.getInstance().player;if(player==null)return 0;var gun=player.getMainHandItem();
            var def=GunLookup.gunDef(gun).orElseThrow();
            var factor=GunAimZoom.addSights(SightFov.EMPTY,def,gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY),GunActiveGroups.of(gun),player.getUUID()).mean();
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SIGHT_CLIENT providers=[{}] activations=[{}] factor={} overlay={} toggles={}",providers(gun),activations(gun),factor,GunScopeOverlay.scopeOverlay(gun,player.getUUID()),gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE,GunActionState.IDLE).activeToggles());return 1;
        }));
    }

    private static int pose(String name) {
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return 0;
        var gun=player.getMainHandItem();
        var model=mc.getItemRenderer().getItemModelShaper().getItemModel(gun);
        if(model instanceof net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel animated)model=animated.baseRig();
        if(!(model instanceof net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel rig))throw new IllegalStateException("No gun rig");
        var side=mc.options.mainHand().get()==net.minecraft.world.entity.HumanoidArm.RIGHT
                ?net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        var eye=net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.adsEyeLine(rig,side,gun);
        var pos=eye==null?null:eye.getTranslation(new org.joml.Vector3f());
        var expected=switch(name) {
            case "g11_tele"->new org.joml.Vector3f(0,.53125F,.03125F);
            case "g11_mount_only"->new org.joml.Vector3f(0,.375F,.15625F);
            case "g11_base"->new org.joml.Vector3f(0,.515625F,.234375F);
            case "krag_0"->new org.joml.Vector3f(0,8.75F/16,-2F/16);
            case "krag_1"->new org.joml.Vector3f(4F/16,8.5F/16,0);
            case "krag_tele"->new org.joml.Vector3f(0,2.5F/16,-2F/16);
            case "gyrojet"->new org.joml.Vector3f(-6.5F/16,5F/16,0);
            default->throw new IllegalArgumentException(name);
        };
        String expectedGun=name.startsWith("g11")?"g11k2":name.startsWith("krag")?"krag":"gyrojet";
        if(!expectedGun.equals(GunLookup.contentKey(gun))||GunActiveGroups.of(gun).entries().isEmpty())throw new IllegalStateException("Setup and aim expected gun first: "+name);
        boolean pass=pos!=null&&pos.distance(expected)<.00001;
        var poses=new com.mojang.blaze3d.vertex.PoseStack();
        var previous=net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.get();
        try {
            net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.set(eye);
            net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.applyRigTransform(rig.getTransforms(),side,side==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND,poses);
        } finally {
            if(previous==null)net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.remove();
            else net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.ADS_EYE_LINE_FOR_FRAME.set(previous);
        }
        float scale=poses.last().pose().getScale(new org.joml.Vector3f()).x;
        pass&=Math.abs(scale-1)<.00001;
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_ADS_POSE name={} side={} eye={} expected={} scale={} pass={} blend={}",name,side,pos,expected,scale,pass,AdsBlendState.blend());
        return pass?1:0;
    }

    private static int renderStack() {
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return 0;
        var slot=player.getInventory().selected;var held=player.getInventory().items.get(slot);
        if(!"g11k2".equals(GunLookup.contentKey(held))||AdsBlendState.poseBlend(AdsBlendState.displayHand(net.minecraft.world.InteractionHand.MAIN_HAND,mc.options.mainHand().get()))<.99F
                ||GunActiveGroups.of(held).entries().stream().noneMatch(entry->entry.origin().definitionKey().equals("attachment_telescopic_sight")))
            throw new IllegalStateException("Aim G11 + telescope first");
        var side=AdsBlendState.displayHand(net.minecraft.world.InteractionHand.MAIN_HAND,mc.options.mainHand().get());
        // A render-local copy models ItemInHandRenderer's retained old stack during the re-equip animation.
        var rendered=held.copy();
        var model=mc.getItemRenderer().getModel(rendered,mc.level,player,0);
        var before=new com.mojang.blaze3d.vertex.PoseStack();model.applyTransform(side,before,side==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        var priorContext=net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.CONTEXT_FOR_FRAME.get();
        try {
            for(boolean empty:new boolean[]{true,false}) {
                var replacement=empty?ItemStack.EMPTY:held.copy();
                if(!empty)replacement.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY);
                player.getInventory().items.set(slot,replacement);
                var after=new com.mojang.blaze3d.vertex.PoseStack();model.applyTransform(side,after,side==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                result(empty?"render_old_stack_after_empty_hand":"render_old_stack_after_attachment_change",before.last().pose().equals(after.last().pose(),.00001F));
            }
        } finally {
            player.getInventory().items.set(slot,held);
            if(priorContext==null)net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.CONTEXT_FOR_FRAME.remove();
            else net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.CONTEXT_FOR_FRAME.set(priorContext);
        }
        return 1;
    }

    private static int icons() {
        var mc=Minecraft.getInstance();if(mc.player==null)return 0;
        for(String name:new String[]{"attachment_telescopic_sight","attachment_red_dot"}) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:"+name)));
            var raw=mc.getItemRenderer().getItemModelShaper().getItemModel(stack);
            if(!(raw instanceof net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel animated)||!animated.baseRig().drawsAsIcon())
                throw new IllegalStateException("Expected held-icon fixture: "+name);
            var resolved=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            for(var side:new net.minecraft.world.item.ItemDisplayContext[]{net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND}) {
                var actualPose=new com.mojang.blaze3d.vertex.PoseStack();var expectedPose=new com.mojang.blaze3d.vertex.PoseStack();
                var actual=resolved.applyTransform(side,actualPose,side==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                var expected=animated.baseRig().applyTransform(side,expectedPose,side==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                result("held_icon_"+name+"_"+side,actual==expected&&actualPose.last().pose().equals(expectedPose.last().pose(),.00001F));
            }
        }
        return 1;
    }

    private static void result(String name, boolean pass) {
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SIGHT_CHECK name={} pass={}",name,pass);
    }
    private static GunActiveGroups.Entry entry(String key,GunActiveGroups.Origin origin,java.util.UUID owner) {
        return new GunActiveGroups.Entry(key,origin,java.util.UUID.randomUUID(),0,owner);
    }
    private static float factor(ItemStack gun,java.util.UUID owner) {
        return GunAimZoom.addSights(SightFov.EMPTY,GunLookup.gunDef(gun).orElseThrow(),GunAttachments.EMPTY,GunActiveGroups.of(gun),owner).mean();
    }
    private static int check() {
        var player=Minecraft.getInstance().player;if(player==null)return 0;
        var gun=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:krag")));
        var owner=player.getUUID();
        var tele=new GunActiveGroups.Origin("Sights",0,"attachment_telescopic_sight");
        gun.set(ModDataComponents.GUN_ACTIVE_GROUPS,new GunActiveGroups(java.util.List.of(entry("ads",tele,java.util.UUID.randomUUID()))));
        result("wrong_owner_fov",factor(gun,owner)==1);
        result("wrong_owner_overlay",GunScopeOverlay.scopeOverlay(gun,owner)==null);
        gun.set(ModDataComponents.GUN_ACTIVE_GROUPS,new GunActiveGroups(java.util.List.of(entry("ads_0",new GunActiveGroups.Origin("Sights",0,"missing_fixture_provider"),owner))));
        result("unresolved_provider_no_base_fov",factor(gun,owner)==1);
        result("unresolved_provider_no_base_overlay",GunScopeOverlay.scopeOverlay(gun,owner)==null);
        gun.set(ModDataComponents.GUN_ACTIVE_GROUPS,new GunActiveGroups(java.util.List.of(entry("ads",tele,owner))));
        result("metadata_sight_without_toggle_state",factor(gun,owner)==6&&gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE,GunActionState.IDLE).activeToggles().isEmpty());
        var base=entry("ads_0",GunActiveGroups.Origin.base(gun),owner);
        var suit=entry("ads",new GunActiveGroups.Origin("Sights",1,"attachment_suit_sight"),owner);
        gun.set(ModDataComponents.GUN_ACTIVE_GROUPS,new GunActiveGroups(java.util.List.of(base,suit)));
        result("overlay_base_created_first",ResourceLocation.parse("createloveandwar:textures/gui/krag_scope.png").equals(GunScopeOverlay.scopeOverlay(gun,owner)));
        gun.set(ModDataComponents.GUN_ACTIVE_GROUPS,new GunActiveGroups(java.util.List.of(suit,base)));
        result("overlay_attachment_created_first",ResourceLocation.parse("createloveandwar:textures/gui/suit_scope.png").equals(GunScopeOverlay.scopeOverlay(gun,owner)));
        return 1;
    }
}
