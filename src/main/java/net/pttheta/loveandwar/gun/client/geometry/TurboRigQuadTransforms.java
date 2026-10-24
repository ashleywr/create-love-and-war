package net.pttheta.loveandwar.gun.client.geometry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Places immutable baked local geometry without rebuilding element vertices, UVs or materials. */
public final class TurboRigQuadTransforms {
    private TurboRigQuadTransforms() {}
    public static List<BakedQuad> place(List<BakedQuad> source,Matrix4f matrix) {
        if(source.isEmpty()||matrix.equals(new Matrix4f(),1.0E-7F))return source;
        var result=new ArrayList<BakedQuad>(source.size());
        var position=new Vector3f();var normal=new Vector3f();
        boolean reflected=matrix.determinant3x3()<0;
        for(var quad:source) {
            int[] input=quad.getVertices(),vertices=new int[input.length];int stride=input.length/4;
            for(int vertex=0;vertex<4;vertex++) {
                int from=(reflected&&vertex>0?4-vertex:vertex)*stride,to=vertex*stride;
                System.arraycopy(input,from,vertices,to,stride);
                matrix.transformPosition(position.set(Float.intBitsToFloat(input[from]),Float.intBitsToFloat(input[from+1]),Float.intBitsToFloat(input[from+2])));
                vertices[to]=Float.floatToRawIntBits(position.x);vertices[to+1]=Float.floatToRawIntBits(position.y);vertices[to+2]=Float.floatToRawIntBits(position.z);
                int packed=input[from+7];
                if((packed&0xffffff)!=0) {
                    // Preserve the existing baker's transformed authored-axis normal convention.
                    matrix.transformDirection(normal.set((byte)packed/127F,(byte)(packed>>8)/127F,(byte)(packed>>16)/127F));
                    if(normal.lengthSquared()>1.0E-12F)normal.normalize();else normal.zero();
                    vertices[to+7]=packed&0xff000000|((byte)Math.round(normal.x*127)&255)|((byte)Math.round(normal.y*127)&255)<<8|((byte)Math.round(normal.z*127)&255)<<16;
                }
            }
            var a=point(vertices,stride);var b=point(vertices,2*stride);var c=point(vertices,0);
            normal.set(b).sub(a).cross(c.sub(a));
            Direction direction=quad.getDirection();
            if(normal.isFinite()&&normal.lengthSquared()>1.0E-12F)direction=Direction.getNearest(normal.x,normal.y,normal.z);
            result.add(new BakedQuad(vertices,quad.getTintIndex(),direction,quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion()));
        }
        return result;
    }
    private static Vector3f point(int[] data,int at){return new Vector3f(Float.intBitsToFloat(data[at]),Float.intBitsToFloat(data[at+1]),Float.intBitsToFloat(data[at+2]));}
}
