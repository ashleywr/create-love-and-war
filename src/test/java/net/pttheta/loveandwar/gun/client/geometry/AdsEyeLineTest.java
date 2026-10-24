package net.pttheta.loveandwar.gun.client.geometry;

import java.util.List;
import java.util.Map;
import net.pttheta.loveandwar.gun.client.AdsPoseState.Selection;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdsEyeLineTest {
    private static TurboRigModelData.AttachPoint point(String name,String parent,float x,float y,float z,float yaw) {
        return new TurboRigModelData.AttachPoint(name,parent,new TurboRigModelData.Vec3(x,y,z),new TurboRigModelData.Vec3(0,yaw,0));
    }
    private static TurboRigModelData rig(TurboRigModelData.AttachPoint... points) {
        return new TurboRigModelData(Map.of(),List.of(points),List.of(),Map.of());
    }
    private static void position(Matrix4f matrix,float x,float y,float z) {
        assertEquals(x,matrix.m30(),.00001);assertEquals(y,matrix.m31(),.00001);assertEquals(z,matrix.m32(),.00001);
    }
    @Test void mountAndChildUseRecursivePixelTransformsWithoutUnrelatedSiblingRotation() {
        var gun=rig(point("body","",0,2,0,0),point("sights","body",0,4,2.5F,0));
        var child=rig(point("eye_line","body",0,2.5F,-2,0),point("sights","body",0,0,0,180));
        position(AdsEyeLine.resolve(gun,new Selection("eye_line","Sights",0),child),0,8.5F/16,.5F/16);
    }
    @Test void zeroIndexPrefersPlainMountAndHigherIndexUsesNumberedMount() {
        var gun=rig(point("sights","",0,0,0,0),point("sights_0","",0,7.25F,-2,0),point("sights_1","",11,8.5F,0,0));
        var child=rig(point("eye_line","",0,2.5F,-2,0));
        position(AdsEyeLine.resolve(gun,new Selection("eye_line","Sights",0),child),0,2.5F/16,-2F/16);
        position(AdsEyeLine.resolve(gun,new Selection("eye_line","Sights",1),child),11F/16,11F/16,-2F/16);
        position(AdsEyeLine.resolve(rig(point("sights_0","",0,7.25F,-2,0)),new Selection("eye_line","Sights",0),child),0,9.75F/16,-4F/16);
    }
    @Test void missingMountKeepsChildAndMissingChildOrAttachmentKeepsMount() {
        var mount=rig(point("sights","",16,32,48,0));var child=rig(point("eye_line","",64,0,0,0));
        var selection=new Selection("eye_line","Sights",0);
        position(AdsEyeLine.resolve(rig(),selection,child),4,0,0);
        position(AdsEyeLine.resolve(mount,selection,rig()),1,2,3);
        position(AdsEyeLine.resolve(mount,selection,null),1,2,3);
        position(AdsEyeLine.resolve(null,selection,child),0,0,0);
    }
    @Test void missingBarePointDoesNotGuessOtherEyeAndMissingParentPreservesLocalPoint() {
        var gun=rig(point("eye_line_1","absent",16,32,48,0));
        position(AdsEyeLine.resolve(gun,new Selection("eye_line","",-1),null),0,0,0);
        position(AdsEyeLine.resolve(gun,new Selection("eye_line_1","",-1),null),1,2,3);
    }
    @Test void mountRotationTransformsChildPositionAndOrientation() {
        var gun=rig(point("sights","",16,0,0,90));var child=rig(point("eye_line","",16,0,0,90));
        var result=AdsEyeLine.resolve(gun,new Selection("eye_line","Sights",0),child);
        position(result,1,0,1);
        assertEquals(-1,result.m00(),.00001);assertEquals(-1,result.m22(),.00001);
    }
}
