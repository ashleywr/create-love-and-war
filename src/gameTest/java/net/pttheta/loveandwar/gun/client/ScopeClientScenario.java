package net.pttheta.loveandwar.gun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

/** Opt-in actual GUI-event/model checks; temporary client copies never replace server content. */
@EventBusSubscriber(modid="createloveandwar",value=Dist.CLIENT)
public final class ScopeClientScenario {
    private static boolean watching;
    private static UUID watched;
    private static int age;
    private static final class Graphics extends GuiGraphics {
        int blits,x,y,width,height;float alpha=1,drawAlpha;ResourceLocation texture;
        Graphics(){super(Minecraft.getInstance(),Minecraft.getInstance().renderBuffers().bufferSource());}
        @Override public void setColor(float r,float g,float b,float a){alpha=a;}
        @Override public void blit(ResourceLocation texture,int x,int y,int width,int height,float u,float v,int uw,int uh,int tw,int th){
            blits++;this.texture=texture;this.x=x;this.y=y;this.width=width;this.height=height;drawAlpha=alpha;
        }
    }
    private static void result(String name,boolean pass){LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SCOPE_CHECK name={} pass={}",name,pass);}
    private static ItemStack sight(String gun,String group,UUID owner){
        var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:"+gun)));
        stack.set(ModDataComponents.GUN_ACTIVE_GROUPS,new GunActiveGroups(List.of(new GunActiveGroups.Entry(group,GunActiveGroups.Origin.base(stack),UUID.randomUUID(),0,owner))));return stack;
    }
    private static Graphics overlay(boolean expected,String name){
        var graphics=new Graphics();var event=new RenderGuiLayerEvent.Pre(graphics,Minecraft.getInstance().getTimer(),VanillaGuiLayers.CROSSHAIR,(g,t)->{});
        NeoForge.EVENT_BUS.post(event);
        result(name,event.isCanceled()==expected);
        return graphics;
    }
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event){
        if(!Boolean.getBoolean("law.enableClientScenario"))return;
        event.getDispatcher().register(Commands.literal("law_test_scope_check").executes(context->check()));
        event.getDispatcher().register(Commands.literal("law_test_scope_sample").executes(context->sample()));
        event.getDispatcher().register(Commands.literal("law_test_scope_flash").executes(context->flash()));
        event.getDispatcher().register(Commands.literal("law_test_scope_watch").executes(context->{watching=true;watched=null;age=0;return 1;}));
    }
    private static int check(){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return 0;
        var slot=player.getInventory().selected;var main=player.getInventory().items.get(slot);var off=player.getOffhandItem();
        var camera=mc.options.getCameraType();var crosshair=Config.CROSSHAIR.get();
        try{
            Config.CROSSHAIR.set(Config.CrosshairMode.VANILLA);mc.options.setCameraType(CameraType.FIRST_PERSON);
            player.getInventory().items.set(slot,sight("gyrojet","ads",player.getUUID()));player.getInventory().offhand.set(0,ItemStack.EMPTY);
            var graphics=overlay(true,"plain_ads_hides_crosshair");result("plain_ads_has_no_overlay",graphics.blits==0);
            player.getInventory().items.set(slot,sight("g11k2","ads",player.getUUID()));
            graphics=overlay(true,"scope_immediate_crosshair");result("scope_immediate_draw",graphics.blits==1);
            result("scope_full_opacity",graphics.blits==1&&graphics.drawAlpha==1);
            result("scope_geometry",graphics.blits==1&&graphics.width==4*graphics.guiHeight()&&graphics.height==graphics.guiHeight()&&graphics.x==graphics.guiWidth()/2-graphics.width/2&&graphics.y==0);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);graphics=overlay(true,"third_person_crosshair");result("third_person_overlay",graphics.blits==1);
            mc.options.setCameraType(CameraType.FIRST_PERSON);player.getInventory().offhand.set(0,player.getMainHandItem());player.getInventory().items.set(slot,ItemStack.EMPTY);
            graphics=overlay(true,"offhand_crosshair");result("offhand_overlay",graphics.blits==1);
            player.getInventory().offhand.set(0,sight("g11k2","ads",UUID.randomUUID()));graphics=overlay(false,"foreign_owner_crosshair");result("foreign_owner_no_overlay",graphics.blits==0);
            player.getInventory().offhand.set(0,ItemStack.EMPTY);overlay(false,"idle_crosshair");
        }finally{
            player.getInventory().items.set(slot,main);player.getInventory().offhand.set(0,off);mc.options.setCameraType(camera);Config.CROSSHAIR.set(crosshair);
        }
        return 1;
    }
    private static int sample(){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return 0;
        var gun=player.getMainHandItem();
        var event=new RenderHandEvent(InteractionHand.OFF_HAND,new PoseStack(),mc.renderBuffers().bufferSource(),0,0,0,0,0,player.getOffhandItem());
        NeoForge.EVENT_BUS.post(event);result("opposite_hand_not_canceled",!event.isCanceled());
        var model=mc.getItemRenderer().getModel(gun,mc.level,player,0);
        var first=AdsBlendState.displayHand(InteractionHand.MAIN_HAND,mc.options.mainHand().get());
        for(var context:new ItemDisplayContext[]{first,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.GUI}){
            var drawn=model.applyTransform(context,new PoseStack(),false);
            result("settled_scope_visibility_"+context,drawn.getQuads(null,null,RandomSource.create(0)).isEmpty()==context.firstPerson());
        }
        var off=player.getOffhandItem();var slot=player.getInventory().selected;
        try{
            var plain=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:gyrojet")));
            player.getInventory().offhand.set(0,plain);
            var other=AdsBlendState.displayHand(InteractionHand.OFF_HAND,mc.options.mainHand().get());
            var otherModel=mc.getItemRenderer().getModel(plain,mc.level,player,0).applyTransform(other,new PoseStack(),other==ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            result("other_gun_mesh_remains_visible",!otherModel.getQuads(null,null,RandomSource.create(0)).isEmpty());
            player.getInventory().items.set(slot,ItemStack.EMPTY);
            var retained=model.applyTransform(first,new PoseStack(),first==ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            result("old_mesh_invalid_current_hand_hidden",retained.getQuads(null,null,RandomSource.create(0)).isEmpty());
            player.getInventory().items.set(slot,plain);
            retained=model.applyTransform(first,new PoseStack(),first==ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            result("old_mesh_valid_unscoped_current_hand_visible",!retained.getQuads(null,null,RandomSource.create(0)).isEmpty());
            player.getInventory().offhand.set(0,gun);
            var scoped=mc.getItemRenderer().getModel(gun,mc.level,player,0).applyTransform(other,new PoseStack(),other==ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            result("same_activation_moved_offhand_stays_hidden",scoped.getQuads(null,null,RandomSource.create(0)).isEmpty());
        }finally{player.getInventory().items.set(slot,gun);player.getInventory().offhand.set(0,off);}
        return 1;
    }

    private static int flash(){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return 0;
        var gun=player.getMainHandItem();var snapshots=new java.util.LinkedHashMap<java.lang.reflect.Field,Object>();
        try{
            for(var field:MuzzleFlash.class.getDeclaredFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers())&&!java.lang.reflect.Modifier.isFinal(field.getModifiers())){
                field.setAccessible(true);snapshots.put(field,field.get(null));
            }
            var raw=mc.getItemRenderer().getItemModelShaper().getItemModel(gun);
            var rig=((net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel)raw).baseRig();
            var side=AdsBlendState.displayHand(InteractionHand.MAIN_HAND,mc.options.mainHand().get());
            var model=mc.getItemRenderer().getModel(gun,mc.level,player,0).applyTransform(side,new PoseStack(),side==ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            result("flash_fixture_scope_mesh_hidden",model.getQuads(null,null,RandomSource.create(0)).isEmpty());
            MuzzleFlash.onPredicted(gun,"primary_fire");
            var expected=MuzzleFlash.quadsFor(rig);var actual=model.getQuads(null,null,RandomSource.create(0));
            result("scoped_flash_survives_without_gun_mesh",MuzzleFlash.active()&&!expected.isEmpty()&&actual.size()==expected.size());
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        finally{for(var snapshot:snapshots.entrySet())try{snapshot.getKey().set(null,snapshot.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
        return 1;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(ClientTickEvent.Pre event){
        if(!Boolean.getBoolean("law.enableClientScenario")||!watching)return;
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return;
        var stack=player.getMainHandItem();
        var scope=GunActiveGroups.of(stack).entries().stream().filter(entry->entry.owner().equals(player.getUUID())&&entry.resolve().map(group->group.actions().stream().anyMatch(action->action.actionType().equals("Scope"))).orElse(false)).findFirst();
        if(scope.isEmpty()){if(watched!=null){result("watch_unexpected_stop",false);watching=false;}return;}
        if(watched==null)watched=scope.get().activation();
        if(!watched.equals(scope.get().activation())){result("watch_unexpected_replacement",false);watching=false;return;}
        age++;
        var side=AdsBlendState.displayHand(InteractionHand.MAIN_HAND,mc.options.mainHand().get());
        var model=mc.getItemRenderer().getModel(stack,mc.level,player,0).applyTransform(side,new PoseStack(),side==ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        boolean hidden=model.getQuads(null,null,RandomSource.create(0)).isEmpty();
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SCOPE_TICK activation={} age={} hidden={} expected={} pass={}",watched,age,hidden,age>=3,hidden==(age>=3));
        if(age>=5)watching=false;
    }
}
