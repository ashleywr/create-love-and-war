package net.pttheta.loveandwar.blocks;

import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.pttheta.loveandwar.LoveAndWarMod;
import net.pttheta.loveandwar.blocks.drawing.DrawingPressBlock;
import net.pttheta.loveandwar.blocks.petrochem.DistillationColumnBlock;
import net.pttheta.loveandwar.blocks.petrochem.DistillationColumnModel;
import net.pttheta.loveandwar.blocks.stamping.StampingPressBlock;

public class LAWBlocks {
   public static final BlockEntry<DrawingPressBlock> DRAWING_PRESS = ((BlockBuilder)((BlockBuilder)((BlockBuilder)LoveAndWarMod.LAWREGISTRATE
               .block("drawing_press", DrawingPressBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .transform(TagGen.axeOrPickaxe()))
         .item(AssemblyOperatorBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<StampingPressBlock> STAMPING_PRESS = ((BlockBuilder)((BlockBuilder)((BlockBuilder)LoveAndWarMod.LAWREGISTRATE
               .block("stamping_press", StampingPressBlock::new)
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
               .transform(TagGen.axeOrPickaxe()))
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .transform(TagGen.axeOrPickaxe()))
         .item(AssemblyOperatorBlockItem::new)
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<DistillationColumnBlock> DISTILLATION_COLUMN = ((BlockBuilder)((BlockBuilder)LoveAndWarMod.LAWREGISTRATE
            .block("distillation_column", DistillationColumnBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(Properties::noOcclusion)
            .properties(p -> p.isRedstoneConductor((p1, p2, p3) -> true))
            .transform(TagGen.axeOrPickaxe()))
         .onRegister(CreateRegistrate.blockModel(() -> DistillationColumnModel::new)))
      .register();

   public static void register() {
   }
}
