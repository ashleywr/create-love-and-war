package net.pttheta.loveandwar.gun.client.geometry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdsPoseTransformTest {
    private static Matrix4f pose(float scale,float blend) throws ReflectiveOperationException {
        var display=new ItemTransform(new Vector3f(),new Vector3f(0.1F,0.2F,0.3F),new Vector3f(scale));
        var stack=new PoseStack();
        var method=TurboRigBakedModel.class.getDeclaredMethod("applyAdsPoseBlended",ItemTransform.class,Matrix4f.class,float.class,float.class,PoseStack.class);
        method.setAccessible(true);method.invoke(null,display,new Matrix4f().translate(0,0.25F,-0.125F),blend,0F,stack);
        return stack.last().pose();
    }
    @Test void fullAdsUsesUnitScaleRatherThanHipDisplayScale() throws ReflectiveOperationException {
        var matrix=pose(0.6875F,1);
        assertEquals(1,matrix.getScale(new Vector3f()).x,0.00001);
        assertEquals(-0.25F,matrix.m31(),0.00001);
        assertEquals(-0.375F,matrix.m32(),0.00001);
    }
    @Test void halfAdsInterpolatesAllTransformComponents() throws ReflectiveOperationException {
        var matrix=pose(0.6875F,0.5F);
        assertEquals(0.84375F,matrix.getScale(new Vector3f()).x,0.00001);
        assertEquals(-0.025F,matrix.m31(),0.00001);
    }
    @Test void hipStillUsesAuthoredUniformScale() throws ReflectiveOperationException {
        assertEquals(0.6875F,pose(0.6875F,0).getScale(new Vector3f()).x,0.00001);
    }
    @Test void otherHandRollRotatesEyeCancellationBeforeOuterBlend() throws ReflectiveOperationException {
        var method=TurboRigBakedModel.class.getDeclaredMethod("applyAdsPoseBlended",ItemTransform.class,Matrix4f.class,float.class,float.class,PoseStack.class);
        method.setAccessible(true);
        for(float roll:new float[]{-30,30}) {
            var stack=new PoseStack();
            method.invoke(null,ItemTransform.NO_TRANSFORM,new Matrix4f().translate(1,0,0),0.5F,roll,stack);
            var matrix=stack.last().pose();
            assertEquals(-0.4330127F,matrix.m30(),0.00001);
            assertEquals(roll<0?0.25F:-0.25F,matrix.m31(),0.00001);
            assertEquals(-0.25F,matrix.m32(),0.00001);
            var direction=matrix.transformDirection(new Vector3f(1,0,0));
            assertEquals(Math.cos(Math.toRadians(15)),direction.x,0.00001);
            assertEquals(Math.sin(Math.toRadians(roll/2)),direction.y,0.00001);
        }
    }
}
