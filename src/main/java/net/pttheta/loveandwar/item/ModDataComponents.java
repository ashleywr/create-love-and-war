package net.pttheta.loveandwar.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.DataComponents;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.CraftedParts;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

public class ModDataComponents {
   public static final DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "createloveandwar");
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<CraftedParts>> CRAFTED_PARTS = COMPONENTS.registerComponentType(
      "crafted_parts", builder -> builder.persistent(CraftedParts.CODEC).networkSynchronized(CraftedParts.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmmoPouchContents>> AMMO_POUCH_CONTENTS = COMPONENTS.registerComponentType(
      "ammo_pouch_contents", builder -> builder.persistent(AmmoPouchContents.CODEC).networkSynchronized(AmmoPouchContents.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmmoPouchContents>> GUN_LOADED_ROUNDS = COMPONENTS.registerComponentType(
      "gun_loaded_rounds", builder -> builder.persistent(AmmoPouchContents.CODEC).networkSynchronized(AmmoPouchContents.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunActionState>> GUN_ACTION_STATE = COMPONENTS.registerComponentType(
      "gun_action_state", builder -> builder.persistent(GunActionState.CODEC).networkSynchronized(GunActionState.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunAttachments>> GUN_ATTACHMENTS = COMPONENTS.registerComponentType(
      "gun_attachments", builder -> builder.persistent(GunAttachments.CODEC).networkSynchronized(GunAttachments.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunActiveGroups>> GUN_ACTIVE_GROUPS = COMPONENTS.registerComponentType(
      "gun_active_groups", builder -> builder.networkSynchronized(GunActiveGroups.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GUN_PAINTJOB = COMPONENTS.registerComponentType(
      "gun_paintjob", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
   );

   public static void register(IEventBus eventBus) {
      COMPONENTS.register(eventBus);
   }
}
