package net.pttheta.loveandwar.gun.client.geometry;

import net.pttheta.loveandwar.gun.client.AdsPoseState.Selection;
import org.joml.Matrix4f;

/** Static attach-point lookup used by the original uncached GunContextItem (empty animation stack). */
final class AdsEyeLine {
    private AdsEyeLine() {}

    static Matrix4f resolve(TurboRigModelData gun, Selection selection, TurboRigModelData attachment) {
        if (gun == null) return new Matrix4f();
        if (selection.attachmentIndex() < 0) return point(gun, selection.eyeLine());
        String mount = selection.attachmentType().toLowerCase(java.util.Locale.ROOT);
        String plain = mount;
        // BakedTurboRig.GetAttachmentPoint: index zero prefers the unnumbered mount when present.
        if (selection.attachmentIndex() != 0 || gun.attachPoints().stream().noneMatch(point -> point.name().equals(plain)))
            mount += "_" + selection.attachmentIndex();
        return point(gun, mount).mul(point(attachment, selection.eyeLine()));
    }

    private static Matrix4f point(TurboRigModelData rig, String name) {
        if (rig == null) return new Matrix4f();
        var matrix = TurboRigAttachTransforms.attachPointMatrix(rig, name);
        // Each failed lookup contributes identity, retaining any mount or child already accumulated.
        return matrix == null ? new Matrix4f() : matrix;
    }
}
