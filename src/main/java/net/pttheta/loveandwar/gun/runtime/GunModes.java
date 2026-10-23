package net.pttheta.loveandwar.gun.runtime;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.InputNode;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Pinned mode defaults, stored-compound semantics and SwitchModeAction selection. */
public final class GunModes {
    private GunModes() {}

    public static String value(GunDef def, GunActionState state, String key) {
        if (state.modesInitialized()) return state.modeValues().getOrDefault(key, "");
        return def.modes().stream().filter(mode -> mode.key().equals(key)).findFirst().map(GunDef.ModeEntry::defaultValue).orElse("");
    }

    static boolean modalCheck(GunDef def, GunActionState state, String check) {
        if (check.isEmpty()) return true;
        String[] fields = check.split(":");
        // Malformed trailing separators are refused instead of reproducing the original array-index crash.
        if (check.contains(":") && fields.length < 2) return false;
        return fields.length > 0 && value(def, state, fields[0]).equals(check.contains(":") ? fields[1] : "on");
    }

    static void switchMode(ServerPlayer player, ItemStack gun, GunDef def, List<ModifierSource> sources) {
        String key = StatResolver.resolveOverride("mode", "mode", sources);
        var entry = def.modes().stream().filter(mode -> mode.key().equals(key)).findFirst();
        if (entry.isEmpty()) { GunEngine.diag("Cannot switch undefined gun mode '{}'", key); return; }
        var state = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        String next = StatResolver.resolveOverride("set_value", "", sources);
        if (next.isEmpty()) {
            var values = entry.get().values();
            if (values.isEmpty()) { GunEngine.diag("Cannot cycle empty gun mode '{}'", key); return; }
            String current = value(def, state, key);
            int index = -1;
            for (int i = 0; i < values.size(); i++) if (values.get(i).equals(current)) index = i;
            next = values.get((index + 1) % values.size());
        }
        select(player, gun, def, key, next);
    }

    private static void select(ServerPlayer player, ItemStack gun, GunDef def, String key, String next) {
        var attachments = gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
        for (var handler : def.inputHandlers()) {
            for (var node : handler.nodes()) {
                // Quirk kept: source uses startsWith, not an exact mode-key comparison.
                if (!node.modalCheck().startsWith(key)) continue;
                var fields = node.modalCheck().split(":");
                if (node.modalCheck().contains(":") && fields.length < 2) continue;
                if (next.equals(node.modalCheck().contains(":") ? fields[1] : "on")) continue;
                String groupKey = cancellationKey(def, attachments, node, handler.inputType());
                if (!groupKey.isEmpty()) {
                    if (!GunActionTimeline.owns(player, gun, groupKey)) {
                        var current = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                        if (current.activeToggles().contains(groupKey)) gun.set(ModDataComponents.GUN_ACTION_STATE, current.withToggle(groupKey, false));
                    }
                    GunActionTimeline.finishAfterTick(player, gun, groupKey);
                    GunFireScheduler.finishModeGroup(player, gun, groupKey);
                }
            }
        }
        // Re-read after finishing a firing cycle so selection cannot overwrite its cleaned runtime state.
        gun.set(ModDataComponents.GUN_ACTION_STATE, gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE).withMode(key, next));
        // SwitchMode ability dispatch joins the remaining general triggered-ability port.
    }

    private static String cancellationKey(GunDef def, GunAttachments attachments, InputNode node, String input) {
        String rootKey = def.actionGroups().stream().filter(group -> group.key().equals(node.actionGroupToTrigger())).findFirst().map(group -> group.key()).orElse("");
        if (!node.deferToAttachment()) return rootKey;
        var stacks = attachments.forCategory(GunAttachments.category(node.attachmentType()));
        if (node.attachmentIndex() < 0 || node.attachmentIndex() >= stacks.size()) return rootKey;
        var attachment = GunLookup.attachmentDef(stacks.get(node.attachmentIndex()));
        if (attachment.isEmpty()) return rootKey;
        var handler = attachment.get().handlerOverrides().stream().filter(value -> value.inputType().equals(input)).findFirst();
        if (handler.isEmpty() || handler.get().nodes().isEmpty()) return rootKey;
        // Original overwrites the path for every child, even an unselected child: only the last is marked finished.
        String lastKey = handler.get().nodes().getLast().actionGroupToTrigger();
        // Active ActionStack lookup compares resolved Def.key, aliasing base/attachment slots with equal plain keys.
        return attachment.get().actionOverrides().stream().filter(group -> group.key().equals(lastKey)).findFirst().map(group -> group.key()).orElse("");
    }
}
