package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunContent;

@GameTestHolder("createloveandwar") @PrefixGameTestTemplate(false)
public final class ScopeTriggerGameTests {
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void delayedSightRetriggerPublishesClockEdgeWithoutReplacingActivation(GameTestHelper h){
        var gun=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:mars")));
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ScopeClock"));p.setPos(h.absolutePos(new BlockPos(2,6,2)).getCenter());p.setItemInHand(InteractionHand.MAIN_HAND,gun);
        var group=ActionGroup.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
            {"key":"delayed_scope_fixture","repeatMode":"FullAuto","actions":[{"actionType":"Scope","delay":2.9,"duration":10}]}
            """)).getOrThrow();
        try{
            var def=GunContent.gun("mars").orElseThrow();
            GunActionTimeline.trigger(p,gun,def,group,GunAttachments.EMPTY,false);
            var first=GunActiveGroups.of(gun);
            GunActionTimeline.synchronize(p,gun);h.assertTrue(GunActiveGroups.of(gun)==first,"Unchanged synchronization must not rewrite sight metadata");
            GunActionTimeline.trigger(p,gun,def,group,GunAttachments.EMPTY,false);
            var second=GunActiveGroups.of(gun);
            h.assertTrue(second!=first&&!second.equals(first),"Delayed Scope retrigger must publish an edge so client can overwrite its pending clock");
            h.assertTrue(second.entries().getFirst().activation().equals(first.entries().getFirst().activation()),"Retrigger retains original action instance/visibility magnitude");
            h.assertTrue(first.entries().getFirst().scopeTrigger()==1&&second.entries().getFirst().scopeTrigger()==2,"Delayed-scope edge counts advance independently of activation identity");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try{
                ItemStack.STREAM_CODEC.encode(buffer,gun);var received=ItemStack.STREAM_CODEC.decode(buffer);
                h.assertTrue(GunActiveGroups.of(received).equals(second)&&GunActiveGroups.of(received).entries().getFirst().scopeTrigger()==2,"Actual ItemStack packet retains the nonzero delayed-scope edge");
            }finally{buffer.release();}
            GunActionTimeline.synchronize(p,gun);h.assertTrue(GunActiveGroups.of(gun)==second,"No extra snapshot without a new delayed sight trigger");
            h.succeed();
        }finally{GunActionTimeline.cancelAll(p,gun);}
    }
}
