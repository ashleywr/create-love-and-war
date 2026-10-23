package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.InlineAbility;
import net.pttheta.loveandwar.gun.content.PartDef;
import net.pttheta.loveandwar.gun.content.TraitDef;
import net.pttheta.loveandwar.item.ModDataComponents;

/** FlanItem.GetTraits: combine providers by definition identity and cap each accumulated level. */
public final class GunTraits {
    private GunTraits() {}

    public record Trait(ResourceLocation id, TraitDef definition, int level) {}
    public record Ability(ResourceLocation id, InlineAbility definition, int level) {}

    public static Map<ResourceLocation, Trait> resolve(ItemStack stack) {
        var traits = new LinkedHashMap<ResourceLocation, Trait>();
        for (var part : stack.getOrDefault(ModDataComponents.CRAFTED_PARTS, CraftedParts.EMPTY).parts()) {
            if (part.getNamespace().equals("createloveandwar")) {
                GunContent.part(part.getPath()).ifPresent(def -> addProviders(traits, def.traits()));
            }
        }
        var attachments = stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
        for (String category : List.of("barrel", "grip", "stock", "scope", "generic")) {
            for (ItemStack attachment : attachments.forCategory(category)) {
                GunLookup.attachmentDef(attachment).ifPresent(def -> addProviders(traits, def.abilities()));
            }
        }
        return Collections.unmodifiableMap(traits);
    }

    private static void addProviders(Map<ResourceLocation, Trait> traits, List<PartDef.TraitRef> providers) {
        for (var provider : providers) {
            var id = ResourceLocation.tryParse(provider.trait());
            if (id == null || !id.getNamespace().equals("createloveandwar")) continue;
            // Definitions.Get accepts both the bare definition key and its folder-prefixed alias.
            if (id.getPath().startsWith("traits/")) id = id.withPath(id.getPath().substring(7));
            var definition = GunContent.trait(id.getPath());
            if (definition.isEmpty()) continue;
            var previous = traits.get(id);
            int level = Math.min(definition.get().maxLevel(), (previous == null ? 0 : previous.level()) + provider.level());
            traits.put(id, new Trait(id, definition.get(), level));
        }
    }

    public static List<Ability> abilities(ItemStack gun) {
        var abilities = new ArrayList<Ability>();
        GunLookup.gunDef(gun).ifPresent(def -> {
            var id = BuiltInRegistries.ITEM.getKey(gun.getItem());
            for (int i = 0; i < def.staticAbilities().size(); i++) {
                abilities.add(new Ability(id.withPath("gun/" + id.getPath() + "/" + i), def.staticAbilities().get(i), 1));
            }
        });
        for (var trait : resolve(gun).values()) {
            for (int i = 0; i < trait.definition().abilities().size(); i++) {
                abilities.add(new Ability(trait.id().withPath("trait/" + trait.id().getPath() + "/" + i),
                        trait.definition().abilities().get(i), trait.level()));
            }
        }
        return List.copyOf(abilities);
    }
}
