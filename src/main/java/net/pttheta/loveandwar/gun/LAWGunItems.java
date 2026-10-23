package net.pttheta.loveandwar.gun;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.pttheta.loveandwar.gun.runtime.GunItem;

public final class LAWGunItems {
   private static final Map<String, DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>>> WORKBENCH_TYPES = new ConcurrentHashMap<>();
   private static final Map<String, DeferredBlock<Block>> WORKBENCH_BLOCKS = new ConcurrentHashMap<>();

   private LAWGunItems() {
   }

   public static DeferredItem<Item> Gun(Items items, String ns, String name) {
      return items.register(name, () -> new GunItem(props(ContentPack.maxStackSize("guns", name, 1)), name));
   }

   public static DeferredItem<Item> Bullet(Items items, String ns, String name) {
      return items.registerSimpleItem(name, props(ContentPack.maxStackSize("bullets", name, 64)));
   }

   public static DeferredItem<Item> Part(Items items, String ns, String name) {
      return items.registerSimpleItem(name, props(ContentPack.maxStackSize("parts", name, 64)));
   }

   public static DeferredItem<Item> Attachment(Items items, String ns, String name) {
      return items.registerSimpleItem(name, props(ContentPack.maxStackSize("attachments", name, 1)));
   }

   public static DeferredItem<Item> Magazine(Items items, String ns, String name) {
      return items.registerSimpleItem(name, props(ContentPack.maxStackSize("magazines", name, 1)));
   }

   private static Properties props(int stackSize) {
      return new Properties().stacksTo(stackSize);
   }

   public static DeferredBlock<Block> Workbench_Block(Blocks blocks, String ns, String name) {
      DeferredBlock<Block> block = blocks.register(
         name,
         () -> new WorkbenchBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
               .mapColor(MapColor.METAL)
               .strength(3.5F)
               .sound(SoundType.METAL)
               .requiresCorrectToolForDrops(),
            name
         )
      );
      WORKBENCH_BLOCKS.put(name, block);
      return block;
   }

   public static DeferredItem<Item> Workbench_Item(Items items, String ns, String name, DeferredBlock<Block> block) {
      return items.register(name, () -> new BlockItem((Block)block.get(), new Properties()));
   }

   public static DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> Workbench_TileEntityType(
      DeferredRegister<BlockEntityType<?>> types, String ns, String name
   ) {
      DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> type = types.register(name, () -> {
         Block block = (Block)WORKBENCH_BLOCKS.get(name).get();
         return new BlockEntityType((pos, state) -> new WorkbenchBlockEntity(workbenchBlockEntityType(name), pos, state), Set.of(block), null);
      });
      WORKBENCH_TYPES.put(name, type);
      return type;
   }

   @Nullable
   static BlockEntityType<WorkbenchBlockEntity> workbenchBlockEntityType(String name) {
      DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> holder = WORKBENCH_TYPES.get(name);
      return holder != null && holder.isBound() ? (BlockEntityType)holder.get() : null;
   }
}
