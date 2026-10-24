package net.pttheta.loveandwar.gun.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Original FirstPersonManager pose lifecycle, independent of the rendered gun and its animations. */
public final class AdsPoseState {
    public record Selection(String eyeLine, String attachmentType, int attachmentIndex) {}
    private static final class Instance {
        final ItemDisplayContext hand;
        final Selection selection;
        float blend;
        boolean active;
        Instance(ItemDisplayContext hand, Selection selection) { this.hand = hand; this.selection = selection; }
    }

    // Quirk kept: original keys/iteration are a HashMap of physical hand + group path, without gun/activation identity.
    private final HashMap<String, Instance> instances = new HashMap<>();

    public void beginFrame() { instances.values().forEach(instance -> instance.active = false); }

    public void activate(ItemDisplayContext hand, String groupPath, Selection selection) {
        instances.computeIfAbsent(hand.toString() + "/" + groupPath, ignored -> new Instance(hand, selection)).active = true;
    }

    public void endFrame(float seconds) {
        instances.values().removeIf(instance -> {
            float t = Math.clamp(seconds * (instance.active ? 8 : 10), 0, 1);
            instance.blend += t * ((instance.active ? 1 : 0) - instance.blend);
            return !instance.active && instance.blend < 0.01F;
        });
    }

    public float blend(ItemDisplayContext hand) {
        float sum = 0;
        for (var instance : instances.values()) if (instance.hand == hand) sum += instance.blend;
        return Math.clamp(sum, 0, 1);
    }

    public boolean hasOtherHand(ItemDisplayContext hand) {
        return instances.values().stream().anyMatch(instance -> instance.hand != hand);
    }

    public float poseBlend(ItemDisplayContext hand) { return blend(hand) * (hasOtherHand(hand) ? 0.5F : 1); }

    /** Each retained selection is resolved against the currently rendered gun, including during blend-out. */
    public Matrix4f eyeLine(ItemDisplayContext hand, Function<Selection, Matrix4f> lookup) {
        var eyes = new ArrayList<Matrix4f>();
        var weights = new ArrayList<Float>();
        for (var instance : instances.values()) if (instance.hand == hand) {
            var eye = lookup.apply(instance.selection);
            eyes.add(eye == null ? new Matrix4f() : eye);
            weights.add(instance.blend);
        }
        return eyes.isEmpty() ? null : weightedEye(eyes, weights);
    }

    static Matrix4f weightedEye(java.util.List<Matrix4f> eyes, java.util.List<Float> weights) {
        if (eyes.size() == 1) return new Matrix4f(eyes.getFirst());
        double x = 0, y = 0, z = 0;
        float sum = 0;
        var rotations = new Quaternionf[eyes.size()];
        var factors = new float[eyes.size()];
        for (int i = 0; i < eyes.size(); i++) {
            var position = eyes.get(i).getTranslation(new Vector3f());
            float weight = weights.get(i);
            x += position.x * (double)weight; y += position.y * (double)weight; z += position.z * (double)weight;
            sum += weight;
            rotations[i] = eyes.get(i).getNormalizedRotation(new Quaternionf());
            factors[i] = weight;
        }
        // Safety deviation: the original divides by zero for multiple just-created zero-weight instances.
        if (!(sum > 0) || !Float.isFinite(sum)) return new Matrix4f();
        return new Matrix4f().translation((float)(x / sum), (float)(y / sum), (float)(z / sum))
                .rotate(Quaternionf.slerp(rotations, factors, new Quaternionf()));
    }
}
