package net.pttheta.loveandwar.gun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.item.ModDataComponents;
import org.slf4j.LoggerFactory;

/** Opt-in checks through real item overrides; all temporary inventory writes are restored. */
@EventBusSubscriber(modid="createloveandwar",value=Dist.CLIENT)
public final class AttachmentRenderClientScenario {
    private static ItemStack stack(String name){
        var id=ResourceLocation.fromNamespaceAndPath("createloveandwar",name);
        if(!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Missing fixture item "+id);
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }
    private static void result(String name,boolean pass,Object detail){
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_ATTACHMENT_CHECK name={} pass={} detail={}",name,pass,detail);
    }
    private static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> draw(ItemStack stack,ItemDisplayContext context){
        var mc=Minecraft.getInstance();
        return mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)
            .applyTransform(context,new PoseStack(),context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
            .getQuads(null,null,RandomSource.create(0));
    }
    private static void faces(String name,ItemStack stack,ItemDisplayContext context,int expected){
        var quads=draw(stack,context);result(name,quads.size()==expected,quads.size());
    }
    private static boolean skin(java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads,String path){
        return !quads.isEmpty()&&quads.stream().allMatch(q->q.getSprite().contents().name().equals(ResourceLocation.fromNamespaceAndPath("createloveandwar",path)));
    }
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event){
        if(!Boolean.getBoolean("law.enableClientScenario"))return;
        event.getDispatcher().register(Commands.literal("law_test_attachment_render").executes(context->check()));
        event.getDispatcher().register(Commands.literal("law_test_attachment_profile").executes(context->profile()));
    }
    private static int check(){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return 0;
        int selected=player.getInventory().selected;var main=player.getMainHandItem();var off=player.getOffhandItem();
        var oldContext=net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.CONTEXT_FOR_FRAME.get();
        try{
            player.getInventory().offhand.set(0,ItemStack.EMPTY);
            var g11=stack("g11k2");player.getInventory().items.set(selected,g11);
            var first=AdsBlendState.displayHand(InteractionHand.MAIN_HAND,mc.options.mainHand().get());
            faces("g11_bare",g11,ItemDisplayContext.GROUND,204);
            g11.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("scope",List.of(stack("attachment_telescopic_sight"))));
            faces("g11_telescope_replaces_sight",g11,ItemDisplayContext.GROUND,198);
            faces("g11_telescope_first_person",g11,first,198);
            var mounted=draw(g11,ItemDisplayContext.GROUND).stream().filter(q->q.getSprite().contents().name().getPath().equals("skins/attachment_telescopic_sight")).toList();
            var raw=mc.getItemRenderer().getItemModelShaper().getItemModel(stack("attachment_telescopic_sight"));
            var child=((net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel)raw).baseRig().partQuads("sights");
            boolean positioned=mounted.size()==36&&child.size()==36;
            for(int q=0;positioned&&q<child.size();q++)for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++){
                float expected=Float.intBitsToFloat(child.get(q).getVertices()[8*v+axis])+(axis==1?.375F:axis==2?.15625F:0);
                positioned&=Math.abs(Float.intBitsToFloat(mounted.get(q).getVertices()[8*v+axis])-expected)<.00001;
            }
            result("telescope_mount_and_child_rotation",positioned,mounted.size());
            g11.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("scope",List.of(stack("attachment_red_dot"))));
            faces("g11_red_dot_replaces_sight",g11,ItemDisplayContext.GROUND,216);
            g11.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("barrel",List.of(stack("attachment_extended_barrel"))));
            faces("g11_mount_without_section",g11,ItemDisplayContext.GROUND,228);
            g11.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("scope",List.of(stack("krag"))));
            faces("non_attachment_keeps_default",g11,ItemDisplayContext.GROUND,204);
            g11.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY);
            faces("removed_attachment_restores_default",g11,ItemDisplayContext.GROUND,204);

            var krag=stack("krag");krag.set(ModDataComponents.GUN_PAINTJOB,"krag_brass");
            var mesh=draw(krag,ItemDisplayContext.GROUND);result("painted_mesh",skin(mesh,"skins/krag_brass"),mesh.stream().map(q->q.getSprite().contents().name()).distinct().toList());
            var icon=draw(krag,ItemDisplayContext.GUI);result("painted_icon",skin(icon,"item/krag_brass"),icon.stream().map(q->q.getSprite().contents().name()).distinct().toList());
            krag.remove(ModDataComponents.GUN_PAINTJOB);result("absent_paint_default",skin(draw(krag,ItemDisplayContext.GROUND),"skins/krag"),"default");
            for(String unknown:List.of("","missing_paint")){
                krag.set(ModDataComponents.GUN_PAINTJOB,unknown);
                var fallback=draw(krag,ItemDisplayContext.GUI);
                result("unknown_paint_3d_fallback_"+(unknown.isEmpty()?"empty":"missing"),fallback.size()>2&&skin(fallback,"skins/krag"),fallback.size());
            }
            var shotgun=stack("1887");shotgun.set(ModDataComponents.GUN_PAINTJOB,"layer0");
            result("literal_layer0_skin",skin(draw(shotgun,ItemDisplayContext.GROUND),"item/1887"),"literal texture key");
            shotgun.set(ModDataComponents.GUN_PAINTJOB,"particle");
            var particleMesh=draw(shotgun,ItemDisplayContext.GROUND);
            result("literal_particle_skin",!particleMesh.isEmpty()&&particleMesh.stream().allMatch(q->q.getSprite().contents().name().equals(ResourceLocation.parse("minecraft:block/iron_block"))),"literal texture key");

            var actual=stack("g11k2");var current=stack("g11k2");
            current.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("scope",List.of(stack("attachment_red_dot"))));
            player.getInventory().items.set(selected,current);
            faces("retained_mesh_current_hand_attachment",actual,first,216);
            faces("third_person_actual_stack_attachment",actual,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,204);
            player.getInventory().items.set(selected,ItemStack.EMPTY);faces("retained_mesh_invalid_hand",actual,first,0);
            predictionContext(actual,first,selected);
            replacementPolicies(g11);
            cataloguePaints();
            transformedGeometry();
        }finally{
            player.getInventory().items.set(selected,main);player.getInventory().offhand.set(0,off);
            if(oldContext==null)net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.CONTEXT_FOR_FRAME.remove();
            else net.pttheta.loveandwar.gun.client.geometry.TurboRigBakedModel.CONTEXT_FOR_FRAME.set(oldContext);
        }
        return 1;
    }

    private static void predictionContext(ItemStack retained,ItemDisplayContext context,int selected){
        var mc=Minecraft.getInstance();var previous=mc.player.getInventory().items.get(selected);
        var snapshots=new java.util.LinkedHashMap<java.lang.reflect.Field,Object>();
        try{
            var type=net.pttheta.loveandwar.gun.client.anim.LocalGunAnim.class;
            for(var field:type.getDeclaredFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers())&&!java.lang.reflect.Modifier.isFinal(field.getModifiers())){
                field.setAccessible(true);snapshots.put(field,field.get(null));
            }
            var current=stack("mars");mc.player.getInventory().items.set(selected,current);
            var group=net.pttheta.loveandwar.gun.content.GunContent.gun("mars").orElseThrow().actionGroups().stream().filter(g->g.key().equals("primary_fire")).findFirst().orElseThrow();
            net.pttheta.loveandwar.gun.client.anim.LocalGunAnim.predict("mars",net.pttheta.loveandwar.gun.runtime.GunActionState.IDLE,group);
            var observed=type.getDeclaredField("lastGroupTotal");observed.setAccessible(true);observed.setInt(null,-77);
            draw(retained,context);
            result("different_item_re_equip_enters_current_prediction",observed.getInt(null)==0,observed.getInt(null));
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        finally{
            mc.player.getInventory().items.set(selected,previous);
            for(var entry:snapshots.entrySet())try{entry.getKey().set(null,entry.getValue());}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
        }
    }

    @SuppressWarnings("unchecked") private static void replacementPolicies(ItemStack gun){
        var mc=Minecraft.getInstance();var shaper=mc.getItemRenderer().getItemModelShaper();
        var attachment=stack("attachment_red_dot");
        var definitions=net.pttheta.loveandwar.gun.content.GunContent.guns();var original=definitions.get("g11k2");
        java.util.Map<net.minecraft.world.item.Item,net.minecraft.client.resources.model.BakedModel> cache=null;
        net.minecraft.client.resources.model.BakedModel saved=null;
        try{
            var field=shaper.getClass().getDeclaredField("models");field.setAccessible(true);
            cache=(java.util.Map<net.minecraft.world.item.Item,net.minecraft.client.resources.model.BakedModel>)field.get(shaper);saved=cache.get(attachment.getItem());
            gun.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("scope",List.of(attachment)));
            var json=net.pttheta.loveandwar.gun.content.GunDef.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,original).getOrThrow().getAsJsonObject();
            json.getAsJsonObject("scopeAttachments").addProperty("hideDefaultMesh",false);
            definitions.put("g11k2",net.pttheta.loveandwar.gun.content.GunDef.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow());
            faces("hide_false_keeps_default_and_child",gun,ItemDisplayContext.GROUND,258);
            cache.put(attachment.getItem(),mc.getModelManager().getMissingModel());
            result("missing_child_fixture_installed",shaper.getItemModel(attachment)==mc.getModelManager().getMissingModel(),shaper.getClass().getName());
            faces("missing_child_rig_hide_false",gun,ItemDisplayContext.GROUND,204);
            definitions.put("g11k2",original);faces("missing_child_rig_hide_true",gun,ItemDisplayContext.GROUND,162);
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        finally{definitions.put("g11k2",original);if(cache!=null&&saved!=null)cache.put(attachment.getItem(),saved);}
    }
    private static void cataloguePaints(){
        var mc=Minecraft.getInstance();
        for(var name:net.pttheta.loveandwar.gun.content.GunContent.guns().keySet()){
            var gun=stack(name);var raw=mc.getItemRenderer().getItemModelShaper().getItemModel(gun);
            if(!(raw instanceof net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel animated))continue;
            var rig=animated.baseRig();if(rig.paintjobs().isEmpty())continue;
            var resource=mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("createloveandwar","models/item/"+name+".json")).orElseThrow();
            try(var reader=resource.openAsReader()){
                var textures=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("textures");
                for(var paint:rig.paintjobs()){
                    gun.set(ModDataComponents.GUN_PAINTJOB,paint);var mesh=draw(gun,ItemDisplayContext.GROUND);var icon=draw(gun,ItemDisplayContext.GUI);
                    var expected=ResourceLocation.parse(textures.get(paint).getAsString());
                    result("catalogue_mesh_"+name+"_"+paint,!mesh.isEmpty()&&mesh.stream().allMatch(q->q.getSprite().contents().name().equals(expected)),mesh.size());
                    result("catalogue_icon_"+name+"_"+paint,!icon.isEmpty()&&icon.stream().allMatch(q->q.getSprite().contents().name().equals(rig.icon(paint).orElseThrow())),icon.size());
                }
            }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        }
    }
    private static volatile int consumed;
    private static void transformedGeometry(){
        var mc=Minecraft.getInstance();
        for(var name:net.pttheta.loveandwar.gun.content.GunContent.guns().keySet()){
            var model=mc.getItemRenderer().getItemModelShaper().getItemModel(stack(name));
            if(!(model instanceof net.pttheta.loveandwar.gun.client.anim.AnimatedTurboRigModel animated))continue;
            var rig=animated.baseRig();boolean equal=true;int faces=0;
            for(var part:rig.rawData().parts().values()){
                var original=rig.partQuads(part.name());if(original.isEmpty())continue;
                var sprite=original.getFirst().getSprite();
                for(var placement:List.of(new org.joml.Matrix4f().translate(.2F,.4F,-.3F).rotateY(.3F).rotateZ(-.7F),new org.joml.Matrix4f().rotateY((float)Math.PI).scale(.8F,1.2F,1.1F))){
                    var expected=new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
                    for(var element:part.elements())expected.addAll(net.pttheta.loveandwar.gun.client.geometry.TurboRigQuadBaker.bakeElement(element,ignored->sprite,v->rig.rootTransform().apply(placement.transformPosition(v))));
                    var actual=rig.meshPart(part.name(),"default",placement,rig);faces+=actual.size();equal&=actual.size()==expected.size();
                    for(int q=0;equal&&q<actual.size();q++){
                        // Vertex+UV pairs must survive placement even when a reflected/flat face reorders winding.
                        var a=actual.get(q).getVertices();var b=expected.get(q).getVertices();
                        for(int v=0;v<4;v++){
                            boolean found=false;
                            for(int w=0;w<4;w++){
                                boolean same=a[v*8+4]==b[w*8+4]&&a[v*8+5]==b[w*8+5];
                                for(int axis=0;axis<3;axis++)same&=Math.abs(Float.intBitsToFloat(a[v*8+axis])-Float.intBitsToFloat(b[w*8+axis]))<.00001;
                                found|=same;
                            }
                            equal&=found;
                        }
                    }
                }
            }
            result("transformed_geometry_"+name,equal,faces);
        }
    }
    private static int profile(){
        var mc=Minecraft.getInstance();if(mc.player==null)return 0;
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();
        for(String scene:List.of("bare","telescope","brass")){
            var gun=stack(scene.equals("brass")?"krag":"g11k2");
            if(scene.equals("telescope"))gun.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY.with("scope",List.of(stack("attachment_telescopic_sight"))));
            if(scene.equals("brass"))gun.set(ModDataComponents.GUN_PAINTJOB,"krag_brass");
            for(int i=0;i<1000;i++)consumed+=draw(gun,ItemDisplayContext.GROUND).size();
            for(int batch=0;batch<7;batch++){
                long bytes=bean.getThreadAllocatedBytes(thread),started=System.nanoTime();int faces=0;
                for(int i=0;i<500;i++)faces+=draw(gun,ItemDisplayContext.GROUND).size();
                long elapsed=System.nanoTime()-started;bytes=bean.getThreadAllocatedBytes(thread)-bytes;consumed+=faces;
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_ATTACHMENT_PROFILE scene={} batch={} renders=500 nanos={} bytes={} faces={}",scene,batch,elapsed,bytes,faces);
            }
        }
        return 1;
    }
}
