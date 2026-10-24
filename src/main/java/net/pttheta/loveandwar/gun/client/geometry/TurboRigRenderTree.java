package net.pttheta.loveandwar.gun.client.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Body-rooted traversal used by the original direct mesh renderer, including AP-only nodes. */
public final class TurboRigRenderTree {
    public record Mount(String category,int index) {}
    private record Node(String name,Matrix4f offset,List<Node> children) {}
    private final Node root;

    public TurboRigRenderTree(TurboRigModelData data) {
        var points=new LinkedHashMap<String,TurboRigModelData.AttachPoint>();
        for(var point:data.attachPoints())points.put(point.name(),point); // Last duplicate wins in the source loader.
        Map<String,List<TurboRigModelData.AttachPoint>> children=new HashMap<>();
        for(var point:points.values())children.computeIfAbsent(point.attachTo(),ignored->new ArrayList<>()).add(point);
        root=compile("body",new Matrix4f(),children,new HashSet<>());
    }
    private static Node compile(String name,Matrix4f offset,Map<String,List<TurboRigModelData.AttachPoint>> children,Set<String> path) {
        path.add(name);var result=new ArrayList<Node>();
        for(var point:children.getOrDefault(name,List.of())) {
            // Malformed cycles are pruned rather than reproducing the original infinite recursion.
            if(path.contains(point.name()))continue;
            var p=point.offset();var e=point.euler();
            var local=new Matrix4f().translate(p.x()/16,p.y()/16,p.z()/16).rotate(new Quaternionf()
                .rotateY((float)Math.toRadians(-e.y())).rotateX((float)Math.toRadians(-e.x())).rotateZ((float)Math.toRadians(-e.z())));
            result.add(compile(point.name(),local,children,path));
        }
        path.remove(name);return new Node(name,offset,List.copyOf(result));
    }
    /** Returning false still permits the visitor's attachment draw, then prunes the base subtree. */
    public void visit(Function<String,Matrix4f> poses,BiPredicate<String,Matrix4f> visitor) {
        visit(root,new Matrix4f(),poses,visitor);
    }
    private static void visit(Node node,Matrix4f parent,Function<String,Matrix4f> poses,BiPredicate<String,Matrix4f> visitor) {
        var placement=new Matrix4f(parent).mul(node.offset());var pose=poses.apply(node.name());
        if(pose!=null)placement.mul(pose);
        if(visitor.test(node.name(),placement))for(var child:node.children())visit(child,placement,poses,visitor);
    }
    public static Mount mount(String name) {
        if(!(name.startsWith("grip")||name.startsWith("barrel")||name.startsWith("stock")||name.startsWith("sights")))return null;
        var segments=name.split("_");int index=0;
        if(name.contains("_")) {
            // Source parses unchecked; malformed names are a non-crashing content boundary here.
            if(segments.length<2)return null;
            try{index=Integer.parseInt(segments[1]);}catch(NumberFormatException ignored){return null;}
            if(index<0)return null;
        }
        String category=switch(segments[0].toLowerCase(Locale.ROOT)) {
            case "grip"->"grip";case "barrel"->"barrel";case "stock"->"stock";case "sights"->"scope";default->"generic";
        };
        return new Mount(category,index);
    }
}
