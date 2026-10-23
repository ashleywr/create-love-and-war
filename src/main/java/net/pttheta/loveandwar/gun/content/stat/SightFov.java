package net.pttheta.loveandwar.gun.content.stat;

import java.util.List;
import net.pttheta.loveandwar.gun.content.ActionGroup;

/** Original ClientRenderHooks averages each active ADS/Scope action, then divides the player modifier. */
public record SightFov(float sum, int count) {
    public static final SightFov EMPTY = new SightFov(0, 0);

    public SightFov add(ActionGroup group, List<ModifierSource> sources) {
        int sights = 0;
        for (var action : group.actions()) {
            if ("AimDownSights".equals(action.actionType()) || "Scope".equals(action.actionType())) sights++;
        }
        if (sights == 0) return this;
        // AimDownSightAction.GetFloat uses zero-base accumulation, not the raw action fovFactor field.
        float factor = StatResolver.resolve("fov_factor", 0, sources);
        float total = sum;
        for (int i = 0; i < sights; i++) total += factor;
        return new SightFov(total, count + sights);
    }

    public float mean() { return count == 0 ? 1 : sum / count; }

    public float apply(float incoming, float strength) {
        float factor = (1 - strength) + mean() * strength;
        // Deviation: refuse invalid projection values instead of propagating the original NaN/zero divisor.
        if (!Float.isFinite(factor) || factor <= 0) return incoming;
        float adjusted = incoming / factor;
        return Float.isFinite(adjusted) ? adjusted : incoming;
    }
}
