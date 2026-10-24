package net.pttheta.loveandwar.gun.client;

import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdsPoseStateTest {
    private static final ItemDisplayContext RIGHT=ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, LEFT=ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    private static AdsPoseState.Selection selection(String name) { return new AdsPoseState.Selection(name,"",-1); }
    private static void activate(AdsPoseState state,float dt,String path,String eye) {
        state.beginFrame(); state.activate(RIGHT,path,selection(eye)); state.endFrame(dt);
    }
    @Test void originalInOutRatesAndLongFrameHaveNoExtraTimeCap() {
        var state=new AdsPoseState();
        activate(state,.05F,"optic","eye_line");assertEquals(.4F,state.blend(RIGHT),.00001);
        activate(state,.05F,"optic","eye_line");assertEquals(.64F,state.blend(RIGHT),.00001);
        state.beginFrame();state.endFrame(.05F);assertEquals(.32F,state.blend(RIGHT),.00001);
        activate(state,.125F,"optic","eye_line");assertEquals(1,state.blend(RIGHT));
        state.beginFrame();state.endFrame(.1F);assertNull(state.eyeLine(RIGHT,s->new Matrix4f()));
    }
    @Test void sameHandPathReusesCapturedSelectionAndBlendUntilRemoved() {
        var state=new AdsPoseState();
        activate(state,.05F,"optic","old");
        activate(state,.05F,"optic","new");
        assertEquals(.64F,state.blend(RIGHT),.00001);
        state.eyeLine(RIGHT,s->{assertEquals("old",s.eyeLine());return new Matrix4f();});
        state.beginFrame();state.endFrame(.05F);
        activate(state,.05F,"optic","new_activation");assertEquals(.592F,state.blend(RIGHT),.00001);
        state.eyeLine(RIGHT,s->{assertEquals("old",s.eyeLine());return new Matrix4f();});
        state.beginFrame();state.endFrame(.1F);
        activate(state,.05F,"optic","recaptured");
        state.eyeLine(RIGHT,s->{assertEquals("recaptured",s.eyeLine());return new Matrix4f();});
    }
    @Test void duplicateActionsInOnePathDoNotDoubleWeightThePose() {
        var state=new AdsPoseState();state.beginFrame();
        state.activate(RIGHT,"optic",selection("first"));state.activate(RIGHT,"optic",selection("second"));state.endFrame(.05F);
        assertEquals(.4F,state.blend(RIGHT),.00001);
        int[] lookups={0};state.eyeLine(RIGHT,s->{lookups[0]++;assertEquals("first",s.eyeLine());return new Matrix4f();});
        assertEquals(1,lookups[0]);
    }
    @Test void attachedPathAndBasePathRemainSeparateAndResolveCurrentRigEvenWhileExiting() {
        var state=new AdsPoseState();state.beginFrame();
        state.activate(RIGHT,"optic",selection("base"));
        state.activate(RIGHT,"Sights/0/optic",new AdsPoseState.Selection("attached","Sights",0));state.endFrame(.05F);
        assertEquals(.8F,state.blend(RIGHT),.00001);
        assertEquals(2,state.eyeLine(RIGHT,s->new Matrix4f().translate(s.attachmentIndex()<0?0:4,0,0)).m30(),.00001);
        state.beginFrame();state.endFrame(.05F);
        assertEquals(6,state.eyeLine(RIGHT,s->new Matrix4f().translate(s.attachmentIndex()<0?4:8,0,0)).m30(),.00001);
        assertEquals(.4F,state.blend(RIGHT),.00001);
    }
    @Test void otherHandPresenceHalvesOwnPoseUntilItsExitIsRemoved() {
        var state=new AdsPoseState();state.beginFrame();
        state.activate(RIGHT,"optic",selection("right"));state.endFrame(.125F);
        state.beginFrame();state.activate(RIGHT,"optic",selection("right"));state.activate(LEFT,"optic",selection("left"));state.endFrame(.001F);
        assertEquals(.5F,state.poseBlend(RIGHT));assertTrue(state.hasOtherHand(RIGHT));
        state.beginFrame();state.activate(RIGHT,"optic",selection("right"));state.endFrame(.001F);
        assertFalse(state.hasOtherHand(RIGHT));assertEquals(1,state.poseBlend(RIGHT));
    }
    @Test void normalizedPositionMeanAndOuterBlendAreIndependent() {
        var eyes=List.of(new Matrix4f(),new Matrix4f().translate(8,0,0));
        assertEquals(6,AdsPoseState.weightedEye(eyes,List.of(.25F,.75F)).m30(),.00001);
        assertEquals(6,AdsPoseState.weightedEye(eyes,List.of(.125F,.375F)).m30(),.00001);
    }
    @Test void orientationUsesWeightedOrderedShortestArcSlerp() {
        var identity=new Matrix4f();var quarter=new Matrix4f().rotateZ((float)Math.PI/2);
        var mean=AdsPoseState.weightedEye(List.of(identity,quarter),List.of(1F,3F));
        var direction=mean.transformDirection(new Vector3f(1,0,0));
        assertEquals(Math.cos(Math.toRadians(67.5)),direction.x,.00001);
        var x=new Matrix4f().rotateX((float)Math.PI/2);var y=new Matrix4f().rotateY((float)Math.PI/2);var z=quarter;
        var q=AdsPoseState.weightedEye(List.of(x,y,z),List.of(1F,1F,1F)).getNormalizedRotation(new Quaternionf());
        assertEquals(.29734409F,q.x,.00001);assertEquals(.29734409F,q.y,.00001);
        assertEquals(.27113896F,q.z,.00001);assertEquals(.86582715F,q.w,.00001);
        var reversed=AdsPoseState.weightedEye(List.of(z,y,x),List.of(1F,1F,1F)).getNormalizedRotation(new Quaternionf());
        assertEquals(.27113896F,reversed.x,.00001);assertEquals(.29734409F,reversed.z,.00001);
    }
    @Test void missingLookupRetainsItsWeightInsteadOfSelectingAnotherSight() {
        var state=new AdsPoseState();state.beginFrame();
        state.activate(RIGHT,"a",selection("missing"));state.activate(RIGHT,"b",selection("present"));state.endFrame(.125F);
        assertEquals(4,state.eyeLine(RIGHT,s->s.eyeLine().equals("missing")?null:new Matrix4f().translate(8,0,0)).m30(),.00001);
    }
    @Test void invalidZeroWeightMeanIsFiniteIdentity() {
        var mean=AdsPoseState.weightedEye(List.of(new Matrix4f(),new Matrix4f().translate(8,0,0)),List.of(0F,0F));
        assertTrue(mean.equals(new Matrix4f(),.00001F));
    }
    @Test void exitThresholdIsStrictlyBelowOnePercent() {
        var state=new AdsPoseState();activate(state,.0025F,"optic","eye_line");
        state.beginFrame();state.endFrame(.05F);
        assertEquals(.01F,state.blend(RIGHT));assertNotNull(state.eyeLine(RIGHT,s->new Matrix4f()));
        state.beginFrame();state.endFrame(.05F);assertNull(state.eyeLine(RIGHT,s->new Matrix4f()));
    }
    @Test void logicalHandsFollowPreferredPhysicalArm() {
        assertEquals(RIGHT,AdsBlendState.displayHand(InteractionHand.MAIN_HAND,HumanoidArm.RIGHT));
        assertEquals(LEFT,AdsBlendState.displayHand(InteractionHand.OFF_HAND,HumanoidArm.RIGHT));
        assertEquals(LEFT,AdsBlendState.displayHand(InteractionHand.MAIN_HAND,HumanoidArm.LEFT));
        assertEquals(RIGHT,AdsBlendState.displayHand(InteractionHand.OFF_HAND,HumanoidArm.LEFT));
    }
}
