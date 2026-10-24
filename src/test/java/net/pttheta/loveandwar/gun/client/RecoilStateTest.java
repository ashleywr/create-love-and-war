package net.pttheta.loveandwar.gun.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecoilStateTest {
    @Test void firstShotUsesHalfAuthoredPitchAndEndTickRecurrence(){
        var state=new RecoilState();state.shot(0,15,0,99);
        assertEquals(0,state.pitch(1));state.tick();assertEquals(2.8125F,state.pitch(1));assertEquals(0,state.pitch(0));
        assertEquals(1.40625F,state.pitch(.5F));state.tick();assertEquals(3.515625F,state.pitch(1));assertEquals(2.8125F,state.pitch(0));
    }
    @Test void allCallbacksAccumulateBeforeTickAndYawKeepsGaussianSign(){
        var state=new RecoilState();state.shot(4,8,0,-.5);state.shot(2,8,1,3);
        state.tick();assertEquals(1.5F,state.yaw(1));assertEquals((4+8*RecoilState.pitchFactor(1))*.375F,state.pitch(1));
    }
    @Test void triggerIndexUsesOriginalExponentialAndDoesNotResetOnRetrigger(){
        assertEquals(.5F,RecoilState.pitchFactor(0));assertEquals(.125F+.375F*(float)Math.exp(-.125F),RecoilState.pitchFactor(1));
        assertTrue(RecoilState.pitchFactor(2)<RecoilState.pitchFactor(1));assertEquals(.125F,RecoilState.pitchFactor(10000));
    }
    @Test void negativeResolvedStatsKeepZeroMaximumAndZeroHorizontalDoesNotInventJitter(){
        var state=new RecoilState();state.shot(-20,-30,0,123);state.tick();assertEquals(0,state.yaw(1));assertEquals(0,state.pitch(1));
        state.shot(0,8,0,-3);state.tick();assertEquals(0,state.yaw(1));assertEquals(1.5F,state.pitch(1));
    }
    @Test void partialTickIsClampedAndStackDecaysWithoutAnotherShot(){
        var state=new RecoilState();state.add(4,8);state.tick();assertEquals(0,state.yaw(-5));assertEquals(1.5F,state.yaw(5));
        for(int i=0;i<200;i++)state.tick();assertEquals(0,state.yaw(1),1.0E-10);assertEquals(0,state.pitch(1),1.0E-10);
    }
    @Test void oppositeYawImpulsesCancelButPitchStacks(){
        var state=new RecoilState();state.add(5,4);state.add(-5,6);state.tick();assertEquals(0,state.yaw(1));assertEquals(3.75F,state.pitch(1));
    }
}
