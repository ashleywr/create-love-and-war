package net.pttheta.loveandwar.gun.client;

import com.mojang.brigadier.CommandDispatcher;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.StatSources;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.content.stat.ModifierContext;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.LoggerFactory;

/** Reflective state observation keeps this same fixture compilable against the unchanged baseline. */
@EventBusSubscriber(modid="createloveandwar",value=Dist.CLIENT)
public final class RecoilClientScenario {
    private static Object watched;
    private static Map<Field,Object> saved;
    private static int age,roundsBefore,expectedRounds;
    private static float previousPending,previousPitch,total,expected,peak;
    private static boolean curve;
    private static void result(String name,boolean pass,Object detail){LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_RECOIL_CHECK name={} pass={} detail={}",name,pass,detail);}
    private static Object state() throws ReflectiveOperationException{
        var type=Class.forName("net.pttheta.loveandwar.gun.client.GunRecoil");var field=type.getDeclaredField("STATE");field.setAccessible(true);return field.get(null);
    }
    private static float read(Object state,String name) throws ReflectiveOperationException{var f=state.getClass().getDeclaredField(name);f.setAccessible(true);return f.getFloat(state);}
    private static Map<Field,Object> clear(Object state) throws ReflectiveOperationException{
        var result=new LinkedHashMap<Field,Object>();for(var field:state.getClass().getDeclaredFields()){field.setAccessible(true);result.put(field,field.get(state));field.setFloat(state,0);}return result;
    }
    private static void restore(Object state,Map<Field,Object> snapshot) throws ReflectiveOperationException{for(var entry:snapshot.entrySet())entry.getKey().set(state,entry.getValue());}
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event){
        if(!Boolean.getBoolean("law.enableClientScenario"))return;
        event.getDispatcher().register(Commands.literal("law_test_recoil_camera").executes(context->cameraCheck()));
        event.getDispatcher().register(Commands.literal("law_test_prediction_initial").executes(context->{
            var mc=Minecraft.getInstance();if(mc.player==null)return 0;
            var stack=mc.player.getMainHandItem();
            var group=net.pttheta.loveandwar.gun.runtime.GunEngine.predictStartedGroup(mc.player,stack,"Fire1",mc.level.getGameTime());
            if(group.isEmpty()||!group.get().key().equals("primary_fire")||stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS,AmmoPouchContents.EMPTY).isEmpty())
                throw new IllegalStateException("Prediction baseline requires ready loaded primary group");
            try{
                var state=state();var snapshot=clear(state);
                try{
                    event.getDispatcher().execute("law_test_fire_client tap",context.getSource());
                    // Client payload handlers enqueue on this same thread and cannot run until this command returns.
                    float pending=read(state,"pendingPitch");
                    LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_PREDICTION_CHECK name=initial_before_reply pass={} pending={}",pending>0,pending);
                }finally{restore(state,snapshot);}
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("law_test_recoil_burst").executes(context->{
            if(!arm())return 0;return event.getDispatcher().execute("law_test_fire_client tap",context.getSource());
        }));
    }
    private static int cameraCheck(){
        Object state;try{state=state();}catch(ReflectiveOperationException ex){result("camera_recoil_installed",false,ex.getClass().getSimpleName());return 0;}
        var mc=Minecraft.getInstance();var cameraType=mc.options.getCameraType();Map<Field,Object> snapshot=null;
        try{
            snapshot=clear(state);state.getClass().getMethod("add",float.class,float.class).invoke(state,4F,8F);
            NeoForge.EVENT_BUS.post(new ClientTickEvent.Post());
            float partial=mc.getTimer().getGameTimeDeltaPartialTick(false);
            for(var type:CameraType.values()){
                mc.options.setCameraType(type);var event=new ViewportEvent.ComputeCameraAngles(new Camera(),0,10,20,30);NeoForge.EVENT_BUS.post(event);
                result("camera_yaw_"+type,Math.abs(event.getYaw()-(10+1.5F*partial))<.00001,event.getYaw());
                result("camera_pitch_"+type,Math.abs(event.getPitch()-(20-3F*partial))<.00001,event.getPitch());
                result("camera_roll_"+type,event.getRoll()==30,event.getRoll());
            }
            NeoForge.EVENT_BUS.post(new ClientTickEvent.Post());
            result("end_tick_second_stack",read(state,"pitch")==3.75F,read(state,"pitch"));
            result("end_tick_previous_stack",read(state,"lastPitch")==3,read(state,"lastPitch"));
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        finally{mc.options.setCameraType(cameraType);if(snapshot!=null)try{restore(state,snapshot);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
        return 1;
    }
    private static boolean arm(){
        var mc=Minecraft.getInstance();if(mc.player==null)return false;
        try{
            watched=state();saved=clear(watched);var gun=mc.player.getMainHandItem();var def=GunLookup.gunDef(gun).orElseThrow();
            var group=def.actionGroups().stream().filter(g->g.key().equals("primary_fire")).findFirst().orElseThrow();
            var sources=StatSources.forFiring(def,group,gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY));
            float vertical=StatResolver.resolve("vertical_recoil",0,sources,ModifierContext.ofGroups(java.util.List.of(group.key())));
            roundsBefore=gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS,AmmoPouchContents.EMPTY).items().size();
            expectedRounds=group.repeatMode().equals("BurstFire")?group.repeatCount():1;
            expected=0;for(int i=0;i<expectedRounds;i++)expected+=Math.max(0,vertical)*(.125F+.375F*(float)Math.exp(-i*.125F));
            age=0;previousPending=previousPitch=total=peak=0;curve=true;return true;
        }catch(ReflectiveOperationException ex){watched=null;result("native_recoil_installed",false,ex.getClass().getSimpleName());return false;}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) static void tick(ClientTickEvent.Post event){
        if(watched==null)return;
        try{
            float pending=read(watched,"pendingPitch"),pitch=read(watched,"pitch"),yaw=read(watched,"yaw");
            float added=2*pending-previousPending;total+=added;peak=Math.max(peak,pitch);
            curve&=Math.abs(pitch-.75F*(previousPitch+pending))<.00001&&yaw==0;
            previousPending=pending;previousPitch=pitch;
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_RECOIL_TICK age={} added={} pending={} pitch={} total={} curve={}",++age,added,pending,pitch,total,curve);
            if(age>=30){
                int rounds=Minecraft.getInstance().player.getMainHandItem().getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS,AmmoPouchContents.EMPTY).items().size();
                result("native_burst_ammo",roundsBefore-rounds==expectedRounds,roundsBefore+"->"+rounds);
                result("native_received_pitch_total",Math.abs(total-expected)<.0001,total+" expected="+expected);
                result("native_post_tick_curve",curve&&peak>0,peak);
                restore(watched,saved);watched=null;saved=null;
            }
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
}
