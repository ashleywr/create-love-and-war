package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.*;
import net.pttheta.loveandwar.item.ModDataComponents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class ActiveGroupContextGameTests {
    private static ItemStack item(String key) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + key))); }
    private static GunActionState state(ItemStack stack) { return stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE); }
    private static FakePlayer player(GameTestHelper h, ItemStack gun) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ContextTest"));
        p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(2, 6, 2)).getCenter());
        p.setItemInHand(InteractionHand.MAIN_HAND, gun); return p;
    }
    private static ActionGroup group(String counter, String repeat, int delay) {
        return ActionGroup.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            {"key":"shared","repeatMode":"%s","actions":[{"actionType":"SwitchMode","duration":0.4,"delay":%s}],
             "modifiers":[{"stat":"mode","setValue":"%s"}]}
            """.formatted(repeat,delay,counter))).getOrThrow();
    }
    private static GunDef definition(ActionGroup base) {
        var json=GunDef.CODEC.encodeStart(JsonOps.INSTANCE,GunContent.gun("mars").orElseThrow()).getOrThrow().getAsJsonObject();
        json.add("modes",JsonParser.parseString("""
            [{"key":"base","values":["0","1","2","3"],"defaultValue":"0"},
             {"key":"attached","values":["0","1","2","3"],"defaultValue":"0"}]
            """));
        json.add("actionGroups",JsonParser.parseString("["+ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,base).getOrThrow()+"]"));
        json.add("inputHandlers",JsonParser.parseString("""
            [{"inputType":"SpecialKey1","nodes":[{"actionGroupToTrigger":"shared"}]},
             {"inputType":"SpecialKey2","nodes":[{"deferToAttachment":true,"attachmentType":"Sights","attachmentIndex":0}]}]
            """));
        return GunDef.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
    }
    private static void fixture(GunDef gun,ActionGroup attached,Runnable run) {
        var originalGun=GunContent.guns().put("mars",gun);
        var json=AttachmentDef.CODEC.encodeStart(JsonOps.INSTANCE,GunContent.attachment("attachment_red_dot").orElseThrow()).getOrThrow().getAsJsonObject();
        json.add("actionOverrides",JsonParser.parseString("["+ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,attached).getOrThrow()+"]"));
        json.add("handlerOverrides",JsonParser.parseString("[{\"inputType\":\"SpecialKey2\",\"nodes\":[{\"actionGroupToTrigger\":\"shared\"}]}]"));
        var originalAttachment=GunContent.attachments().put("attachment_red_dot",AttachmentDef.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
        try { run.run(); } finally { GunContent.guns().put("mars",originalGun); GunContent.attachments().put("attachment_red_dot",originalAttachment); }
    }
    private static void attach(ItemStack gun) { gun.set(ModDataComponents.GUN_ATTACHMENTS,new GunAttachments(List.of(),List.of(),List.of(),List.of(item("attachment_red_dot")),List.of())); }
    private static void input(FakePlayer p,ItemStack gun,String input) { GunEngine.handleInput(p,gun,InteractionHand.MAIN_HAND,input,true); }
    private static void clean(FakePlayer p,ItemStack gun) { GunFireScheduler.releaseAll(p);GunFireScheduler.cancel(p,gun);GunActionTimeline.cancelAll(p,gun); }

    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void inputAliasesReuseFirstActionsInBothOrders(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","SemiAuto",0);var def=definition(base);
        for(boolean attachmentFirst:new boolean[]{false,true}) {
            var gun=item("mars");attach(gun);var p=player(h,gun);
            try { fixture(def,attached,()->{
                input(p,gun,attachmentFirst?"SpecialKey2":"SpecialKey1");
                input(p,gun,attachmentFirst?"SpecialKey1":"SpecialKey2");
                String retained=attachmentFirst?"attached":"base",other=attachmentFirst?"base":"attached";
                h.assertTrue("2".equals(state(gun).modeValues().get(retained))&&!state(gun).modeValues().containsKey(other),
                        "Same Def.key must retrigger first context/actions; retained="+retained+" actual="+state(gun).modeValues());
            }); } finally { clean(p,gun); }
        }
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void directAliasDoesNotReplaceExistingFrameActions(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","SemiAuto",0);var def=definition(base);
        var gun=item("mars");var p=player(h,gun);
        try {
            GunEngine.triggerActions(p,gun,def,base,GunAttachments.EMPTY);
            GunEngine.triggerActions(p,gun,def,attached,GunAttachments.EMPTY);
            h.assertTrue("2".equals(state(gun).modeValues().get("base"))&&!state(gun).modeValues().containsKey("attached"),"Runtime alias retains original action list");
        } finally { clean(p,gun); }
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void retriggerAlsoRetainsFirstGunDefinition(GameTestHelper h) {
        var group=group("base","SemiAuto",0);var first=definition(group);
        var json=GunDef.CODEC.encodeStart(JsonOps.INSTANCE,first).getOrThrow().getAsJsonObject();
        json.add("modes",JsonParser.parseString("""
            [{"key":"base","values":["a","b"],"defaultValue":"a"}]
            """));
        var second=GunDef.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();var gun=item("mars");var p=player(h,gun);
        try {
            GunEngine.triggerActions(p,gun,first,group,GunAttachments.EMPTY);
            GunEngine.triggerActions(p,gun,second,group,GunAttachments.EMPTY);
            h.assertTrue("2".equals(state(gun).modeValues().get("base")),"Immediate action retains first context's gun definition and its mode cycle");
        } finally {clean(p,gun);}
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void aliasKeepsPendingActionsAndFirstDefinition(GameTestHelper h) {
        var base=group("base","SemiAuto",2);var attached=group("attached","SemiAuto",0);var def=definition(base);
        var gun=item("mars");var p=player(h,gun);
        GunEngine.triggerActions(p,gun,def,base,GunAttachments.EMPTY);
        GunEngine.triggerActions(p,gun,def,attached,GunAttachments.EMPTY);
        for(int tick=1;tick<=3;tick++) { int now=tick;h.runAtTickTime(tick,()->{
            GunEngine.tick(p,gun);
            if(now==3) { try {
                h.assertTrue("1".equals(state(gun).modeValues().get("base"))&&!state(gun).modeValues().containsKey("attached"),"Delayed original action is reset/requeued, never replaced by candidate action");
                h.succeed();
            } finally { clean(p,gun); } }
        }); }
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void toggleCandidateFinishesAnAlreadyStartedNonToggleAlias(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","Toggle",0);var def=definition(base);
        var gun=item("mars");attach(gun);var p=player(h,gun);
        fixture(def,attached,()->{
            input(p,gun,"SpecialKey1");
            h.assertTrue(GunEngine.predictStartedGroup(p,gun,"SpecialKey2",p.level().getGameTime()).isEmpty(),"Requested Toggle stopping an old nonToggle must not predict a new trigger");
            input(p,gun,"SpecialKey2");
        });
        try {
            h.assertTrue(!state(gun).activeToggles().contains("shared")&&!state(gun).modeValues().containsKey("attached"),"Candidate Toggle marks existing started instance finished without starting a replacement");
            h.assertTrue(GunActionTimeline.isFinishing(p,gun,"shared"),"Finished action remains until its final tick");
        } finally { clean(p,gun); }
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void nonToggleCandidateRetriggersOriginalToggleWithoutReplacingIt(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","Toggle",0);var def=definition(base);
        var gun=item("mars");attach(gun);var p=player(h,gun);
        try { fixture(def,attached,()->{
            input(p,gun,"SpecialKey2");input(p,gun,"SpecialKey1");
            h.assertTrue("2".equals(state(gun).modeValues().get("attached"))&&!state(gun).modeValues().containsKey("base"),"Candidate nonToggle reuses original Toggle actions");
            h.assertTrue(state(gun).activeToggles().contains("shared"),"Actual Toggle instance stays active");
        }); } finally { clean(p,gun); }
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void retainedProviderSurvivesRemovalAndActualItemPacketButIsNotSaved(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","SemiAuto",0);var def=definition(base);
        var gun=item("mars");attach(gun);var p=player(h,gun);
        try { fixture(def,attached,()->{
            input(p,gun,"SpecialKey2");
            var active=GunActiveGroups.of(gun);var entry=active.entries().getFirst();
            h.assertTrue(entry.key().equals("shared")&&entry.origin().attachmentType().equals("Sights")&&entry.origin().attachmentIndex()==0
                    &&entry.origin().definitionKey().equals("attachment_red_dot")&&entry.owner().equals(p.getUUID()),"Descriptor carries actual creating slot/provider/owner");
            h.assertTrue(entry.resolve().orElseThrow().equals(attached),"Shared reader resolves actual attachment definition, not colliding base");
            gun.set(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY);
            input(p,gun,"SpecialKey1");
            h.assertTrue(GunActiveGroups.of(gun)==active&&entry.resolve().orElseThrow().equals(attached),"Removing attachment changes current inputs, not first-created definition/provenance");
            var packet=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(packet,gun);
                h.assertTrue(packet.readableBytes()<1024,"Snapshot contains IDs, not full action definitions: bytes="+packet.readableBytes());
                var received=ItemStack.STREAM_CODEC.decode(packet);
                h.assertTrue(GunActiveGroups.of(received).equals(active),"Actual ItemStack packet retains active definition/provider/activation");
                h.assertTrue(GunActiveGroups.of(received).entries().getFirst().resolve().orElseThrow().equals(attached),"Packet client view resolves retained action list after slot emptied");
            } finally {packet.release();}
            var saved=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)gun.save(h.getLevel().registryAccess()));
            h.assertTrue(!saved.has(ModDataComponents.GUN_ACTIVE_GROUPS),"Runtime action stack is not persisted to item saves");
        }); } finally {clean(p,gun);}
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void copiedAndTransferredStacksCannotResumeProviderMetadata(GameTestHelper h) {
        var group=group("base","SemiAuto",0);var def=definition(group);var gun=item("mars");var owner=player(h,gun);
        try {
            GunEngine.triggerActions(owner,gun,def,group,GunAttachments.EMPTY);
            var original=GunActiveGroups.of(gun);var copy=gun.copy();
            GunEngine.tick(owner,copy);
            h.assertTrue(GunActiveGroups.of(copy).entries().isEmpty()&&GunActiveGroups.of(gun).equals(original),"Copy loses runtime metadata without changing original owner");
            var recipient=player(h,gun);GunEngine.tick(recipient,gun);
            h.assertTrue(GunActiveGroups.of(gun).entries().isEmpty(),"Transferred stack cannot adopt prior owner instance");
            clean(recipient,gun);
        } finally {clean(owner,gun);}
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void toggleFinishesOnFinalTickThenCreatesNewActivation(GameTestHelper h) {
        var group=group("base","Toggle",0);var def=definition(group);var gun=item("mars");var p=player(h,gun);
        fixture(def,group,()->{input(p,gun,"SpecialKey1");input(p,gun,"SpecialKey1");});
        var first=GunActiveGroups.of(gun);
        h.assertTrue(first.entries().size()==1,"Finishing context remains visible through final callbacks");
        h.runAtTickTime(1,()->{
            try {
                GunEngine.tick(p,gun);
                h.assertTrue(GunActiveGroups.of(gun).entries().isEmpty(),"Context removed at final tick");
                fixture(def,group,()->input(p,gun,"SpecialKey1"));
                h.assertTrue(!GunActiveGroups.of(gun).entries().getFirst().activation().equals(first.entries().getFirst().activation()),"New runtime instance gets a new activation identity");
                h.succeed();
            } finally {clean(p,gun);}
        });
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void sameAliasRetainsClockAndDoesNotRewriteSnapshotEachTick(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","SemiAuto",0);var def=definition(base);
        var gun=item("mars");attach(gun);var p=player(h,gun);
        fixture(def,attached,()->input(p,gun,"SpecialKey1"));var first=GunActiveGroups.of(gun);
        h.runAtTickTime(1,()->{
            try { fixture(def,attached,()->{
                GunEngine.tick(p,gun);input(p,gun,"SpecialKey2");
                h.assertTrue(state(gun).groupTicksElapsed()==1&&state(gun).groupTotalTicks()==17,"Retrigger retains progress and extends duration*triggerCount");
                h.assertTrue(GunActiveGroups.of(gun)==first,"No new snapshot or activation for an unchanged tick/retrigger");
                h.assertTrue("2".equals(state(gun).modeValues().get("base")),"Retrigger actually executes retained instance");
            });h.succeed(); } finally {clean(p,gun);}
        });
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void metadataKeepsActivationOrderAndNonToggleGroups(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var def=definition(base);var gun=item("mars");var p=player(h,gun);
        var json=ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,base).getOrThrow().getAsJsonObject();json.addProperty("key","later");
        var later=ActionGroup.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        try {
            GunEngine.triggerActions(p,gun,def,base,GunAttachments.EMPTY);GunEngine.triggerActions(p,gun,def,later,GunAttachments.EMPTY);
            var snapshot=GunActiveGroups.of(gun);
            h.assertTrue(snapshot.entries().stream().map(GunActiveGroups.Entry::key).toList().equals(List.of("shared","later")),"Metadata preserves insertion order and does not depend on activeToggles");
            h.assertTrue(state(gun).activeToggles().isEmpty(),"NonToggle fixture has no toggle entries");
            GunActionTimeline.cancel(p,gun,"shared");
            h.assertTrue(GunActiveGroups.of(gun).entries().stream().map(GunActiveGroups.Entry::key).toList().equals(List.of("later")),"Cancel removes only matching descriptor");
            GunActionTimeline.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            h.assertTrue(GunActiveGroups.of(gun).entries().isEmpty(),"Logout clears runtime metadata");
        } finally {clean(p,gun);}
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void validAttachmentMissingOverrideDoesNotBorrowBaseGroup(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var def=definition(base);var gun=item("mars");attach(gun);var p=player(h,gun);
        var json=ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,base).getOrThrow().getAsJsonObject();json.addProperty("key","unrelated");
        var unrelated=ActionGroup.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        try {fixture(def,unrelated,()->input(p,gun,"SpecialKey2"));
            h.assertTrue(state(gun).modeValues().isEmpty()&&GunActiveGroups.of(gun).entries().isEmpty(),"A valid attached definition returns its own missing-group result, not a base same-key fallback");
        } finally {clean(p,gun);}
        h.succeed();
    }

    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void previousHolderCleanupCannotEraseNewHolderActivation(GameTestHelper h) {
        var group=group("base","SemiAuto",0);var def=definition(group);var gun=item("mars");var first=player(h,gun);var second=player(h,gun);
        try {
            GunEngine.triggerActions(first,gun,def,group,GunAttachments.EMPTY);
            GunEngine.tick(second,gun);
            GunEngine.triggerActions(second,gun,def,group,GunAttachments.EMPTY);
            var snapshot=GunActiveGroups.of(gun);
            h.assertTrue(snapshot.entries().getFirst().owner().equals(second.getUUID()),"New holder owns its new instance");
            GunActionTimeline.cancelAll(first,gun);
            GunActionTimeline.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(first));
            h.assertTrue(GunActiveGroups.of(gun).equals(snapshot)&&GunActionTimeline.owns(second,gun,"shared"),"Previous holder's stale cancellation/logout cannot erase new holder metadata");
        } finally {clean(first,gun);clean(second,gun);}
        h.succeed();
    }

    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void startRestrictionsComeFromRetainedProviderInBothOrders(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var json=ActionGroup.CODEC.encodeStart(JsonOps.INSTANCE,base).getOrThrow().getAsJsonObject();
        json.addProperty("twoHanded",true);base=ActionGroup.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        var attached=group("attached","SemiAuto",0);var def=definition(base);
        for(boolean attachmentFirst:new boolean[]{false,true}) {
            var gun=item("mars");attach(gun);var p=player(h,gun);
            try {fixture(def,attached,()->{
                input(p,gun,attachmentFirst?"SpecialKey2":"SpecialKey1");
                p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.STICK));
                input(p,gun,attachmentFirst?"SpecialKey1":"SpecialKey2");
                String counter=attachmentFirst?"attached":"base";
                h.assertTrue((attachmentFirst?"2":"1").equals(state(gun).modeValues().get(counter)),"CanStart uses first actual group hand restrictions, not requested provider flags; attachmentFirst="+attachmentFirst);
            });} finally {clean(p,gun);}
        }
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void attachmentOriginKeepsActualNonzeroSlotIndex(GameTestHelper h) {
        var base=group("base","SemiAuto",0);var attached=group("attached","SemiAuto",0);var def=definition(base);
        var json=GunDef.CODEC.encodeStart(JsonOps.INSTANCE,def).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("inputHandlers").get(1).getAsJsonObject().getAsJsonArray("nodes").get(0).getAsJsonObject().addProperty("attachmentIndex",1);
        def=GunDef.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();var gun=item("mars");var p=player(h,gun);
        gun.set(ModDataComponents.GUN_ATTACHMENTS,new GunAttachments(List.of(),List.of(),List.of(),List.of(ItemStack.EMPTY,item("attachment_red_dot")),List.of()));
        try {fixture(def,attached,()->{
            input(p,gun,"SpecialKey2");var entry=GunActiveGroups.of(gun).entries().getFirst();
            h.assertTrue(entry.origin().attachmentIndex()==1&&entry.origin().attachmentType().equals("Sights"),"Resolved node carries actual index1, not a guessed first attachment");
        });} finally {clean(p,gun);}
        h.succeed();
    }
    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void firingSchedulerRetainsOriginalAliasActions(GameTestHelper h) {
        var gun=item("g11k2");var p=player(h,gun);var def=GunContent.gun("g11k2").orElseThrow();
        var first=ActionGroup.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
            {"key":"shared","actions":[{"actionType":"Shoot","duration":0.4}]}
            """)).getOrThrow();
        var second=ActionGroup.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
            {"key":"shared","actions":[{"actionType":"Shoot"},{"actionType":"Shoot"}]}
            """)).getOrThrow();
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS,new net.pttheta.loveandwar.item.custom.AmmoPouchContents(java.util.stream.IntStream.range(0,5).mapToObj(i->item("caseless_bullet")).toList()));
        try {
            GunFireScheduler.start(p,gun,def,first,GunAttachments.EMPTY);var snapshot=GunActiveGroups.of(gun);
            GunFireScheduler.start(p,gun,def,second,GunAttachments.EMPTY);
            h.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size()==3,"Aliased scheduler retrigger executes one original Shoot, not two requested actions");
            h.assertTrue(GunActiveGroups.of(gun)==snapshot,"Firing retrigger retains activation identity");
        } finally {clean(p,gun);}
        h.succeed();
    }

    @GameTest(template="projectile_arena",templateNamespace="createloveandwar")
    public static void soundOnlyInvocationDoesNotReplacePendingFullActions(GameTestHelper h) {
        var def=GunContent.gun("g11k2").orElseThrow();
        var group=ActionGroup.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
            {"key":"shared","actions":[{"actionType":"Shoot","delay":2,"duration":0.4}]}
            """)).getOrThrow();
        for(boolean soundsFirst:new boolean[]{false,true}) {
            var gun=item("g11k2");var p=player(h,gun);
            gun.set(ModDataComponents.GUN_LOADED_ROUNDS,new net.pttheta.loveandwar.item.custom.AmmoPouchContents(List.of(item("caseless_bullet"))));
            if(soundsFirst) {GunEngine.triggerSounds(p,gun,def,group);GunEngine.triggerActions(p,gun,def,group,GunAttachments.EMPTY);}
            else {GunEngine.triggerActions(p,gun,def,group,GunAttachments.EMPTY);GunEngine.triggerSounds(p,gun,def,group);}
            for(int tick=1;tick<=3;tick++) {int now=tick;h.runAtTickTime(tick,()->{
                try {
                    GunEngine.tick(p,gun);
                    h.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size()==(now<2?1:0),"Sound-only dispatch is an invocation filter, not a new instance identity; soundsFirst="+soundsFirst+" tick="+now);
                } finally {if(now==3)clean(p,gun);}
            });}
        }
        h.runAtTickTime(4,h::succeed);
    }

}
