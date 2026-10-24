package net.pttheta.loveandwar.gun.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;

@EventBusSubscriber(modid="createloveandwar",value=Dist.CLIENT)
public final class ScopeVisibility {
    public enum GunRender { VISIBLE, SCOPED, INVALID_CONTEXT }
    private static final HashMap<UUID,ScopeClock> CLOCKS=new HashMap<>();
    private static Object level;
    private ScopeVisibility(){}
    private static void synchronizeLevel(){var current=Minecraft.getInstance().level;if(level!=current){CLOCKS.clear();level=current;}}

    private static ScopeClock observe(GunActiveGroups.Entry entry) {
        var clock=CLOCKS.get(entry.activation());
        if(clock==null){
            var group=entry.resolve();if(group.isEmpty())return null;
            var actions=group.get().actions().stream().filter(action->"Scope".equals(action.actionType())).toList();
            float[] delays=new float[actions.size()];for(int i=0;i<delays.length;i++)delays[i]=actions.get(i).delay();
            clock=new ScopeClock(delays,entry.scopeTrigger());CLOCKS.put(entry.activation(),clock);
        }else clock.observe(entry.scopeTrigger());
        return clock;
    }
    @SubscribeEvent static void tick(ClientTickEvent.Pre event){
        var mc=Minecraft.getInstance();
        synchronizeLevel();
        if(mc.player==null||mc.level==null){CLOCKS.clear();return;}
        var seen=new HashSet<UUID>();
        // Original ClientActionManager advances valid inventory gun stacks, including unequipped slots.
        for(int slot=0;slot<mc.player.getInventory().getContainerSize();slot++){
            var stack=mc.player.getInventory().getItem(slot);if(!(stack.getItem() instanceof GunItem))continue;
            for(var entry:GunActiveGroups.of(stack).entries())if(entry.owner().equals(mc.player.getUUID())&&seen.add(entry.activation())){
                var clock=observe(entry);if(clock!=null)clock.tick();
            }
        }
        CLOCKS.keySet().retainAll(seen);
    }
    public static boolean hidden(ItemStack stack,UUID owner){
        synchronizeLevel();
        for(var entry:GunActiveGroups.of(stack).entries())if(entry.owner().equals(owner)){
            var clock=observe(entry);if(clock!=null&&clock.hidden())return true;
        }
        return false;
    }
    public static GunRender renderState(ItemDisplayContext context){
        // Ordinary original BEWLR supplies LocalPlayer only for first-person contexts, never GUI/third person.
        if(!context.firstPerson())return GunRender.VISIBLE;
        var mc=Minecraft.getInstance();if(mc.player==null)return GunRender.VISIBLE;
        var hand=context==AdsBlendState.displayHand(InteractionHand.MAIN_HAND,mc.options.mainHand().get())?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        var current=mc.player.getItemInHand(hand);
        // Visibility uses the current logical-hand context; alignment separately uses the retained render stack.
        if(!(current.getItem() instanceof GunItem)||GunLookup.gunDef(current).isEmpty())return GunRender.INVALID_CONTEXT;
        return hidden(current,mc.player.getUUID())?GunRender.SCOPED:GunRender.VISIBLE;
    }
}
