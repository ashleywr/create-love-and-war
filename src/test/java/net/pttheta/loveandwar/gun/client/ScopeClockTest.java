package net.pttheta.loveandwar.gun.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScopeClockTest {
    @Test void zeroDelayHidesOnThirdClientTick(){
        var clock=new ScopeClock(new float[]{0},0);assertFalse(clock.hidden());
        clock.tick();assertFalse(clock.hidden());clock.tick();assertFalse(clock.hidden());clock.tick();assertTrue(clock.hidden());
    }
    @Test void nonzeroDelayUsesTruncatedTicksAndSkipsDueTick(){
        for(float delay:new float[]{2.9F,.9F,-.9F,-4}){
            var clock=new ScopeClock(new float[]{delay},1);
            int hiddenAt=Math.max(1,(int)delay)+3;
            for(int tick=1;tick<hiddenAt;tick++){clock.tick();assertFalse(clock.hidden(),"delay="+delay+" tick="+tick);}
            clock.tick();assertTrue(clock.hidden(),"delay="+delay);
        }
    }
    @Test void retriggerOverwritesPendingWithoutResettingMagnitude(){
        var clock=new ScopeClock(new float[]{2},1);
        clock.tick();clock.tick();clock.tick();clock.tick(); // magnitude .0975
        assertFalse(clock.hidden());clock.observe(2);
        clock.tick();assertFalse(clock.hidden());clock.tick();assertFalse(clock.hidden());clock.tick();assertTrue(clock.hidden());
        clock.observe(3);clock.tick();assertTrue(clock.hidden(),"Retrigger pauses ticks, never resets a matured instance");
    }
    @Test void repeatedTriggerCanKeepPendingButUnchangedSnapshotsCannot(){
        var clock=new ScopeClock(new float[]{2},1);
        for(int i=0;i<8;i++){clock.observe(i+2);clock.tick();assertFalse(clock.hidden());}
        for(int i=0;i<3;i++){clock.observe(9);clock.tick();assertFalse(clock.hidden());}
        clock.tick();assertTrue(clock.hidden());
    }
    @Test void independentActionsAllowAnyMatureScopeToHide(){
        var clock=new ScopeClock(new float[]{20,0},1);
        clock.tick();clock.tick();assertFalse(clock.hidden());clock.tick();assertTrue(clock.hidden());
    }
    @Test void newActivationStartsFreshAndNoScopeNeverHides(){
        var old=new ScopeClock(new float[]{0},0);for(int i=0;i<20;i++)old.tick();assertTrue(old.hidden());
        var fresh=new ScopeClock(new float[]{0},0);assertFalse(fresh.hidden());
        var empty=new ScopeClock(new float[0],0);for(int i=0;i<20;i++)empty.tick();assertFalse(empty.hidden());
    }
}
