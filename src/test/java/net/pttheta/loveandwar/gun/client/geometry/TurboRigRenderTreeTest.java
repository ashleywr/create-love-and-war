package net.pttheta.loveandwar.gun.client.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurboRigRenderTreeTest {
    private static TurboRigModelData.AttachPoint point(String name,String parent,float x,float y,float z){
        return new TurboRigModelData.AttachPoint(name,parent,new TurboRigModelData.Vec3(x,y,z),TurboRigModelData.Vec3.ZERO);
    }
    private static TurboRigRenderTree tree(TurboRigModelData.AttachPoint... points){
        return new TurboRigRenderTree(new TurboRigModelData(Map.of(),List.of(points),List.of(),Map.of()));
    }
    @Test void rootIgnoresItsOwnOffsetButMountAndAnimationComposeInOrder(){
        var tree=tree(point("body","",999,999,999),point("sights","body",0,16,0),point("tip","sights",16,0,0));
        var seen=new ArrayList<String>();
        tree.visit(name->switch(name){case "body"->new Matrix4f().translate(1,0,0);case "sights"->new Matrix4f().rotateZ((float)-Math.PI/2);default->null;},(name,matrix)->{
            seen.add(name);var p=matrix.transformPosition(new Vector3f(name.equals("sights")?1:0,0,0));
            if(!name.equals("body")){assertEquals(1,p.x,.00001);assertEquals(0,p.y,.00001);assertEquals(0,p.z,.00001);}return true;
        });
        assertEquals(List.of("body","sights","tip"),seen);
    }
    @Test void gatePrunesWholeSubtreeAfterVisitingMountWithoutMesh(){
        var tree=tree(point("sights","body",0,0,0),point("child","sights",0,0,0),point("orphan","absent",0,0,0));
        var seen=new ArrayList<String>();tree.visit(n->null,(n,m)->{seen.add(n);return !n.equals("sights");});
        assertEquals(List.of("body","sights"),seen);
        seen.clear();tree.visit(n->null,(n,m)->{seen.add(n);return true;});assertEquals(List.of("body","sights","child"),seen);
    }
    @Test void duplicateNamesUseLastPointAndCyclesCannotRecurseIntoRoot(){
        var tree=tree(point("sights","absent",8,0,0),point("sights","body",16,0,0),point("body","sights",0,0,0));
        var seen=new ArrayList<String>();tree.visit(n->null,(n,m)->{seen.add(n);if(n.equals("sights"))assertEquals(1,m.m30(),.00001);return true;});
        assertEquals(List.of("body","sights"),seen);
    }
    @Test void plainAndNumberedZeroAreDistinctMountsAndGenericPrefixIsNotDispatched(){
        assertEquals(new TurboRigRenderTree.Mount("scope",0),TurboRigRenderTree.mount("sights"));
        assertEquals(TurboRigRenderTree.mount("sights"),TurboRigRenderTree.mount("sights_0_extra"));
        assertEquals(new TurboRigRenderTree.Mount("scope",1),TurboRigRenderTree.mount("sights_1"));
        assertEquals(new TurboRigRenderTree.Mount("generic",0),TurboRigRenderTree.mount("sightsHousing"));
        assertNull(TurboRigRenderTree.mount("generic_0"));assertNull(TurboRigRenderTree.mount("Sights"));
        assertNull(TurboRigRenderTree.mount("sights_bad"));assertNull(TurboRigRenderTree.mount("sights_"));assertNull(TurboRigRenderTree.mount("sights_-1"));
        var seen=new ArrayList<Float>();tree(point("sights","body",0,0,0),point("sights_0","body",16,0,0)).visit(n->null,(n,m)->{if(TurboRigRenderTree.mount(n)!=null)seen.add(m.m30());return true;});
        assertEquals(List.of(0F,1F),seen);
    }
}
