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
                if(!"g11k2".equals(GunLookup.contentKey(gun)))throw new IllegalStateException("Use G11 timing setup before the sight fixture");
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
        event.getDispatcher().register(Commands.literal("law_test_sight_client").executes(context->{
            var player=Minecraft.getInstance().player;if(player==null)return 0;var gun=player.getMainHandItem();
            var def=GunLookup.gunDef(gun).orElseThrow();
            var factor=GunAimZoom.addSights(SightFov.EMPTY,def,gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS,GunAttachments.EMPTY),GunActiveGroups.of(gun),player.getUUID()).mean();
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_SIGHT_CLIENT providers=[{}] activations=[{}] factor={} overlay={} toggles={}",providers(gun),activations(gun),factor,GunScopeOverlay.scopeOverlay(gun,player.getUUID()),gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE,GunActionState.IDLE).activeToggles());return 1;
        }));
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
