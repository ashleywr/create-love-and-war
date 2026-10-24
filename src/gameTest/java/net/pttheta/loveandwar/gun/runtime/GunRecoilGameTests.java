package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import net.pttheta.loveandwar.gun.network.SGunRecoil;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunRecoilGameTests {
    private static ItemStack stack(String name){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:"+name)));}
    private static ActionGroup group(){return ActionGroup.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
        {"key":"camera_recoil","actions":[{"actionType":"Shoot"}],"modifiers":[
          {"stat":"vertical_recoil","accumulators":[{"operation":"BaseAdd","value":15}]},
          {"stat":"horizontal_recoil","accumulators":[{"operation":"BaseAdd","value":9}]}]}
        """)).getOrThrow();}
    private static List<SGunRecoil> messages(GunAudioGameTests.Listener listener){return listener.packets.stream()
        .filter(p->p instanceof ClientboundCustomPayloadPacket c&&c.payload() instanceof SGunRecoil)
        .map(p->(SGunRecoil)((ClientboundCustomPayloadPacket)p).payload()).toList();}
    private static ItemStack loaded(GunAudioGameTests.Listener listener,int rounds){
        var gun=stack("mars");gun.set(ModDataComponents.GUN_LOADED_ROUNDS,new AmmoPouchContents(java.util.stream.IntStream.range(0,rounds).mapToObj(i->stack("pistol_bullet")).toList()));
        listener.player.setItemInHand(InteractionHand.MAIN_HAND,gun);listener.player.setXRot(-90);return gun;
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void firingPreservesExistingShooterVelocity(GameTestHelper helper) throws ReflectiveOperationException {
        try(var listener=new GunAudioGameTests.Listener(helper)){
            var gun=stack("mars");gun.set(ModDataComponents.GUN_LOADED_ROUNDS,new AmmoPouchContents(List.of(stack("pistol_bullet"))));
            listener.player.setItemInHand(InteractionHand.MAIN_HAND,gun);listener.player.setXRot(-25);listener.player.setYRot(90);
            var velocity=new Vec3(.25,-.125,.5);listener.player.setDeltaMovement(velocity);
            GunEngine.triggerActions(listener.player,gun,GunContent.gun("mars").orElseThrow(),group(),GunAttachments.EMPTY);
            helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS,AmmoPouchContents.EMPTY).isEmpty(),"Fixture must actually fire its round");
            helper.assertTrue(listener.player.getDeltaMovement().equals(velocity),"Original camera recoil must not change player velocity: "+listener.player.getDeltaMovement());
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void recoilCallbackIsOwnerOnlyAndRetainsResolvedStats(GameTestHelper helper) throws ReflectiveOperationException {
        try(var owner=new GunAudioGameTests.Listener(helper);var observer=new GunAudioGameTests.Listener(helper,5)){
            var gun=loaded(owner,1);var rotation=owner.player.getRotationVector();
            GunEngine.triggerActions(owner.player,gun,GunContent.gun("mars").orElseThrow(),group(),GunAttachments.EMPTY);
            helper.assertTrue(messages(owner).equals(List.of(new SGunRecoil(9,15,0))),"One owner callback carries zero-base resolved recoil and initial index0");
            helper.assertTrue(messages(observer).isEmpty(),"Observers must not receive another player's camera recoil");
            helper.assertTrue(owner.player.getRotationVector().equals(rotation),"Camera recoil must not rotate authoritative shooter aim");
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void callbackIndicesReuseRetainedInstanceAndSeparateActions(GameTestHelper helper) throws ReflectiveOperationException {
        try(var owner=new GunAudioGameTests.Listener(helper)){
            var gun=loaded(owner,6);var def=GunContent.gun("mars").orElseThrow();
            var json=ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,group()).getOrThrow().getAsJsonObject();
            json.getAsJsonArray("actions").add(json.getAsJsonArray("actions").get(0).deepCopy());
            var twice=ActionGroup.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            for(int i=0;i<3;i++)GunEngine.triggerActions(owner.player,gun,def,twice,GunAttachments.EMPTY);
            var packets=messages(owner);
            helper.assertTrue(packets.size()==6,"Two Shoot action entries each emit once per group callback");
            for(int i=0;i<6;i++)helper.assertTrue(packets.get(i).triggerIndex()==i/2,"Action entries share current group index, retained retriggers increment it");
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void emptyShotCollectionHasZeroRecoilAndStillRunsCallback(GameTestHelper helper) throws ReflectiveOperationException {
        try(var owner=new GunAudioGameTests.Listener(helper)){
            var gun=loaded(owner,0);GunEngine.triggerActions(owner.player,gun,GunContent.gun("mars").orElseThrow(),group(),GunAttachments.EMPTY);
            helper.assertTrue(messages(owner).equals(List.of(new SGunRecoil(0,0,0))),"Original empty result still calls AddRecoil(0,0), consuming one client Gaussian");
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void recoilUsesGunAttachmentModifiersAndNoInventedHorizontalBase(GameTestHelper helper) throws ReflectiveOperationException {
        try(var owner=new GunAudioGameTests.Listener(helper)){
            var gun=loaded(owner,1);var def=GunContent.gun("g11k2").orElseThrow();
            var firing=def.actionGroups().stream().filter(g->g.key().equals("primary_fire")).findFirst().orElseThrow();
            var attachments=GunAttachments.EMPTY.with("stock",List.of(stack("attachment_impact_stock")));
            GunEngine.triggerActions(owner.player,gun,def,firing,attachments);
            helper.assertTrue(messages(owner).equals(List.of(new SGunRecoil(0,2.5F,0))),"G11 vertical5 * independent50%, horizontal stays zero");
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void multipleRaysProduceOneCameraImpulse(GameTestHelper helper) throws ReflectiveOperationException {
        try(var owner=new GunAudioGameTests.Listener(helper)){
            var gun=loaded(owner,1);var json=ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,group()).getOrThrow().getAsJsonObject();
            json.getAsJsonArray("modifiers").add(JsonParser.parseString("{\"stat\":\"shot_count\",\"accumulators\":[{\"operation\":\"BaseAdd\",\"value\":7}]}"));
            var many=ActionGroup.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            GunEngine.triggerActions(owner.player,gun,GunContent.gun("mars").orElseThrow(),many,GunAttachments.EMPTY);
            long rays=owner.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket c&&c.payload() instanceof net.pttheta.loveandwar.gun.network.SShotTrail).count();
            helper.assertTrue(rays==8,"Fixture must actually produce eight rays, found "+rays);
            helper.assertTrue(messages(owner).equals(List.of(new SGunRecoil(9,15,0))),"Eight shot results still produce one maximum-based camera impulse");
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void zeroRayBulletConsumesAmmoButHasZeroImpulse(GameTestHelper helper) throws ReflectiveOperationException {
        try(var owner=new GunAudioGameTests.Listener(helper)){
            var gun=loaded(owner,1);var bullets=GunContent.bullets();var original=bullets.get("pistol_bullet");
            try{
                var json=net.pttheta.loveandwar.gun.content.BulletDef.CODEC.encodeStart(JsonOps.INSTANCE,original).getOrThrow().getAsJsonObject();
                json.add("hitscans",new com.google.gson.JsonArray());json.add("projectiles",new com.google.gson.JsonArray());
                bullets.put("pistol_bullet",net.pttheta.loveandwar.gun.content.BulletDef.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
                GunEngine.triggerActions(owner.player,gun,GunContent.gun("mars").orElseThrow(),group(),GunAttachments.EMPTY);
                helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS,AmmoPouchContents.EMPTY).isEmpty(),"Original consumes its bullet before producing shot results");
                helper.assertTrue(messages(owner).equals(List.of(new SGunRecoil(0,0,0))),"A valid bullet with no rays keeps the zero maximum and callback");
            }finally{bullets.put("pistol_bullet",original);}
        }
        helper.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void delayedRecoilUsesCurrentTriggerCountAndOverwritesPendingAction(GameTestHelper helper) throws ReflectiveOperationException {
        var owner=new GunAudioGameTests.Listener(helper);var gun=loaded(owner,1);var def=GunContent.gun("mars").orElseThrow();
        var json=ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,group()).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("actions").get(0).getAsJsonObject().addProperty("delay",2.9);
        json.getAsJsonArray("actions").get(0).getAsJsonObject().addProperty("duration",1);
        var delayed=ActionGroup.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        GunEngine.triggerActions(owner.player,gun,def,delayed,GunAttachments.EMPTY);
        GunEngine.triggerActions(owner.player,gun,def,delayed,GunAttachments.EMPTY);
        var early=new boolean[]{messages(owner).isEmpty()};
        helper.runAtTickTime(1,()->{GunEngine.tick(owner.player,gun);early[0]&=messages(owner).isEmpty();});
        helper.runAtTickTime(2,()->{
            try{
                GunEngine.tick(owner.player,gun);
                helper.assertTrue(early[0]&&messages(owner).equals(List.of(new SGunRecoil(9,15,2))),"Queued action runs once, receives current count2 rather than queued index0 or1");
                helper.succeed();
            }finally{owner.close();}
        });
    }
    @GameTest(template="empty",templateNamespace="createloveandwar")
    public static void recoilPayloadRoundTripsAndHasCompactInitialFrame(GameTestHelper helper){
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());
        try{
            for(long index:new long[]{0,128,3000000000L}){
                buffer.clear();var value=new SGunRecoil(1.25F,200,index);SGunRecoil.STREAM_CODEC.encode(buffer,value);
                if(index==0)helper.assertTrue(buffer.readableBytes()==9,"Initial owner impulse uses8 float bytes plus1 index byte");
                helper.assertTrue(SGunRecoil.STREAM_CODEC.decode(buffer).equals(value)&&buffer.readableBytes()==0,"All recoil fields round-trip without trailing bytes");
            }
        }finally{buffer.release();}
        helper.succeed();
    }
}
