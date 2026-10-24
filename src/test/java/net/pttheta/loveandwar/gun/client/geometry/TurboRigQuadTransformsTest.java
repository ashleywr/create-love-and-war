package net.pttheta.loveandwar.gun.client.geometry;

import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurboRigQuadTransformsTest {
    private static BakedQuad quad(){
        int[] vertices=new int[32];float[][] points={{0,1,0},{0,0,0},{1,0,0},{1,1,0}};
        for(int i=0;i<4;i++){
            for(int axis=0;axis<3;axis++)vertices[i*8+axis]=Float.floatToRawIntBits(points[i][axis]);
            vertices[i*8+3]=0xFF123456;vertices[i*8+4]=Float.floatToRawIntBits(i*.125F);vertices[i*8+5]=Float.floatToRawIntBits(i*.25F);
            vertices[i*8+6]=0xF000F0;vertices[i*8+7]=127<<16;
        }
        return new BakedQuad(vertices,3,Direction.SOUTH,null,true,false);
    }
    @Test void translationAndRotationPreserveUvLightColourAndImmutableInput(){
        var original=quad();var saved=original.getVertices().clone();var source=List.of(original);
        var result=TurboRigQuadTransforms.place(source,new Matrix4f().translate(2,3,4).rotateY((float)Math.PI/2));var moved=result.getFirst();
        assertArrayEquals(saved,original.getVertices());assertNotSame(original,moved);assertEquals(Direction.EAST,moved.getDirection());
        assertEquals(2,Float.intBitsToFloat(moved.getVertices()[0]),.00001);assertEquals(4,Float.intBitsToFloat(moved.getVertices()[1]),.00001);assertEquals(4,Float.intBitsToFloat(moved.getVertices()[2]),.00001);
        assertEquals(3,moved.getTintIndex());assertTrue(moved.isShade());assertFalse(moved.hasAmbientOcclusion());
        for(int i=0;i<4;i++){
            for(int at=3;at<=6;at++)assertEquals(saved[i*8+at],moved.getVertices()[i*8+at]);
            assertEquals(127,moved.getVertices()[i*8+7]&255);
        }
    }
    @Test void reflectionReversesWindingAndKeepsUvAttachedToItsVertex(){
        var source=quad();var result=TurboRigQuadTransforms.place(List.of(source),new Matrix4f().scaling(-2,3,1)).getFirst();
        assertEquals(Direction.SOUTH,result.getDirection());
        for(int i=0;i<4;i++){
            int from=(i==0?0:4-i)*8;
            assertEquals(-2*Float.intBitsToFloat(source.getVertices()[from]),Float.intBitsToFloat(result.getVertices()[i*8]),.00001);
            assertEquals(source.getVertices()[from+4],result.getVertices()[i*8+4]);
        }
    }
    @Test void identityAndEmptyMeshesReuseExistingImmutableLists(){
        var source=List.of(quad());assertSame(source,TurboRigQuadTransforms.place(source,new Matrix4f()));
        var empty=List.<BakedQuad>of();assertSame(empty,TurboRigQuadTransforms.place(empty,new Matrix4f().translate(1,2,3)));
    }
}
