package net.pttheta.loveandwar.gun.runtime;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.pttheta.loveandwar.gun.content.GunContent;

/** Ordered part definition IDs, matching FlanItem.SetCraftingInputs rather than storing mutable stacks. */
public record CraftedParts(List<ResourceLocation> parts) {
    public static final CraftedParts EMPTY = new CraftedParts(List.of());
    public static final Codec<CraftedParts> CODEC = ResourceLocation.CODEC.listOf().xmap(CraftedParts::new, CraftedParts::parts);
    public static final StreamCodec<ByteBuf, CraftedParts> STREAM_CODEC = ResourceLocation.STREAM_CODEC
            .apply(ByteBufCodecs.list()).map(CraftedParts::new, CraftedParts::parts);

    public CraftedParts {
        parts = List.copyOf(parts);
    }

    public static CraftedParts from(RecipeInput input) {
        var parts = new ArrayList<ResourceLocation>();
        // Original quirk: every PartItem in the input container contributes, including unused slots.
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).isEmpty()) continue;
            var id = BuiltInRegistries.ITEM.getKey(input.getItem(i).getItem());
            if (id.getNamespace().equals("createloveandwar") && GunContent.part(id.getPath()).isPresent()) parts.add(id);
        }
        return new CraftedParts(parts);
    }
}
