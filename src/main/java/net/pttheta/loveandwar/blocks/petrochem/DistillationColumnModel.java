package net.pttheta.loveandwar.blocks.petrochem;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;

public class DistillationColumnModel extends CTModel {
   protected static final ModelProperty<DistillationColumnModel.CullData> CULL_PROPERTY = new ModelProperty();

   public DistillationColumnModel(BakedModel originalModel) {
      super(originalModel, new DistillationColumnCTBehavior());
   }

   protected Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
      super.gatherModelData(builder, world, pos, state, blockEntityData);
      DistillationColumnModel.CullData cullData = new DistillationColumnModel.CullData();

      for (Direction d : Iterate.horizontalDirections) {
         cullData.setCulled(d, ConnectivityHandler.isConnected(world, pos, pos.relative(d)));
      }

      return builder.with(CULL_PROPERTY, cullData);
   }

   public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
      if (side != null) {
         return Collections.emptyList();
      }

      List<BakedQuad> quads = new ArrayList<>();

      for (Direction d : Iterate.directions) {
         if (!extraData.has(CULL_PROPERTY) || !((DistillationColumnModel.CullData)extraData.get(CULL_PROPERTY)).isCulled(d)) {
            quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
         }
      }

      quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
      return quads;
   }

   private static class CullData {
      boolean[] culledFaces = new boolean[4];

      public CullData() {
         Arrays.fill(this.culledFaces, false);
      }

      void setCulled(Direction face, boolean cull) {
         if (!face.getAxis().isVertical()) {
            this.culledFaces[face.get2DDataValue()] = cull;
         }
      }

      boolean isCulled(Direction face) {
         return face.getAxis().isVertical() ? false : this.culledFaces[face.get2DDataValue()];
      }
   }
}
