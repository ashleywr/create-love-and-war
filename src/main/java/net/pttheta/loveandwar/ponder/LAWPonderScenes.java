package net.pttheta.loveandwar.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.element.BeltItemElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.blocks.drawing.DrawingBehaviour;
import net.pttheta.loveandwar.blocks.drawing.DrawingPressBlockEntity;
import net.pttheta.loveandwar.blocks.stamping.StampingBehaviour;
import net.pttheta.loveandwar.blocks.stamping.StampingPressBlockEntity;
import net.pttheta.loveandwar.item.ModItems;

public class LAWPonderScenes {
   public static void drawing(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("drawing_press", "Processing Items with the Drawing Press");
      scene.configureBasePlate(0, 0, 5);
      scene.world().showSection(util.select().layer(0), Direction.UP);
      scene.idle(5);
      ElementLink<WorldSectionElement> depot = scene.world().showIndependentSection(util.select().position(2, 1, 1), Direction.DOWN);
      scene.world().moveSection(depot, util.vector().of(0.0, 0.0, 1.0), 0);
      scene.idle(10);
      Selection pressS = util.select().position(2, 3, 2);
      BlockPos pressPos = util.grid().at(2, 3, 2);
      BlockPos depotPos = util.grid().at(2, 1, 1);
      scene.world().setKineticSpeed(pressS, 0.0F);
      scene.world().showSection(pressS, Direction.DOWN);
      scene.idle(10);
      scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 5), Direction.NORTH);
      scene.idle(3);
      scene.world().showSection(util.select().position(2, 2, 3), Direction.SOUTH);
      scene.idle(3);
      scene.world().showSection(util.select().position(2, 3, 3), Direction.NORTH);
      scene.world().setKineticSpeed(pressS, -32.0F);
      scene.effects().indicateSuccess(pressPos);
      scene.idle(10);
      Vec3 pressSide = util.vector().blockSurface(pressPos, Direction.WEST);
      scene.overlay().showText(60).pointAt(pressSide).placeNearTarget().attachKeyFrame().text("The Drawing Press can process items provided beneath it");
      scene.idle(70);
      scene.overlay()
         .showText(60)
         .pointAt(pressSide.subtract(0.0, 2.0, 0.0))
         .placeNearTarget()
         .text("The Input items can be dropped or placed on a Depot under the Press");
      scene.idle(50);
      ItemStack brass = new ItemStack((ItemLike)AllItems.BRASS_SHEET.get());
      scene.world().createItemOnBeltLike(depotPos, Direction.NORTH, brass);
      Vec3 depotCenter = util.vector().centerOf(depotPos.south());
      scene.overlay().showControls(depotCenter, Pointing.UP, 60).rightClick().withItem(brass);
      scene.idle(10);
      Class<DrawingPressBlockEntity> type = DrawingPressBlockEntity.class;
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getDrawingBehaviour().start(DrawingBehaviour.Mode.BELT));
      scene.idle(30);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getDrawingBehaviour().makePressingParticleEffect(depotCenter.add(0.0, 0.5, 0.0), brass));
      scene.world().removeItemsFromBelt(depotPos);
      ItemStack sheet = new ItemStack((ItemLike)ModItems.BRASS_CUP.get(), 1);
      scene.world().createItemOnBeltLike(depotPos, Direction.UP, sheet);
      scene.idle(10);
      scene.overlay().showControls(depotCenter, Pointing.UP, 60).rightClick().withItem(sheet);
      scene.idle(60);
      scene.world().hideIndependentSection(depot, Direction.NORTH);
      scene.idle(5);
      scene.world().showSection(util.select().fromTo(0, 1, 3, 0, 2, 3), Direction.DOWN);
      scene.idle(10);
      scene.world().showSection(util.select().fromTo(4, 1, 2, 0, 2, 2), Direction.SOUTH);
      scene.idle(20);
      BlockPos beltPos = util.grid().at(0, 1, 2);
      scene.overlay()
         .showText(40)
         .pointAt(util.vector().blockSurface(beltPos, Direction.WEST))
         .placeNearTarget()
         .attachKeyFrame()
         .text("When items are provided on a belt...");
      scene.idle(30);
      ElementLink<BeltItemElement> ingot = scene.world().createItemOnBelt(beltPos, Direction.SOUTH, brass);
      scene.idle(15);
      ElementLink<BeltItemElement> ingot2 = scene.world().createItemOnBelt(beltPos, Direction.SOUTH, brass);
      scene.idle(15);
      scene.world().stallBeltItem(ingot, true);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getDrawingBehaviour().start(DrawingBehaviour.Mode.BELT));
      scene.overlay().showText(50).pointAt(pressSide).placeNearTarget().attachKeyFrame().text("The Press will hold and process them automatically");
      scene.idle(30);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getDrawingBehaviour().makePressingParticleEffect(depotCenter.add(0.0, 0.5, 0.0), brass));
      scene.world().removeItemsFromBelt(pressPos.below(2));
      ingot = scene.world().createItemOnBelt(pressPos.below(2), Direction.UP, sheet);
      scene.world().stallBeltItem(ingot, true);
      scene.idle(15);
      scene.world().stallBeltItem(ingot, false);
      scene.idle(15);
      scene.world().stallBeltItem(ingot2, true);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getDrawingBehaviour().start(DrawingBehaviour.Mode.BELT));
      scene.idle(30);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getDrawingBehaviour().makePressingParticleEffect(depotCenter.add(0.0, 0.5, 0.0), brass));
      scene.world().removeItemsFromBelt(pressPos.below(2));
      ingot2 = scene.world().createItemOnBelt(pressPos.below(2), Direction.UP, sheet);
      scene.world().stallBeltItem(ingot2, true);
      scene.idle(15);
      scene.world().stallBeltItem(ingot2, false);
   }

   public static void stamping(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("stamping_press", "Processing Items with the Stamping Press");
      scene.configureBasePlate(0, 0, 5);
      scene.world().showSection(util.select().layer(0), Direction.UP);
      scene.idle(5);
      ElementLink<WorldSectionElement> depot = scene.world().showIndependentSection(util.select().position(2, 1, 1), Direction.DOWN);
      scene.world().moveSection(depot, util.vector().of(0.0, 0.0, 1.0), 0);
      scene.idle(10);
      Selection pressS = util.select().position(2, 3, 2);
      BlockPos pressPos = util.grid().at(2, 3, 2);
      BlockPos depotPos = util.grid().at(2, 1, 1);
      scene.world().setKineticSpeed(pressS, 0.0F);
      scene.world().showSection(pressS, Direction.DOWN);
      scene.idle(10);
      scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 5), Direction.NORTH);
      scene.idle(3);
      scene.world().showSection(util.select().position(2, 2, 3), Direction.SOUTH);
      scene.idle(3);
      scene.world().showSection(util.select().position(2, 3, 3), Direction.NORTH);
      scene.world().setKineticSpeed(pressS, -32.0F);
      scene.effects().indicateSuccess(pressPos);
      scene.idle(10);
      Class<StampingPressBlockEntity> type = StampingPressBlockEntity.class;
      ItemStack template = new ItemStack((ItemLike)ModItems.TEMPLATE_UPPER_RECEIVER.get());
      Vec3 pressSide = util.vector().blockSurface(pressPos, Direction.WEST);
      scene.overlay().showControls(pressSide, Pointing.UP, 60).rightClick().withItem(template);
      scene.overlay().showText(60).pointAt(pressSide).placeNearTarget().attachKeyFrame().text("Apply template with right click. Shift + Right click to remove");
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().addTemplate(template));
      scene.idle(70);
      scene.overlay()
         .showText(60)
         .pointAt(pressSide)
         .placeNearTarget()
         .attachKeyFrame()
         .text("With a template applied, the Stamping press will process items beneath it");
      scene.idle(70);
      scene.overlay()
         .showText(60)
         .pointAt(pressSide.subtract(0.0, 2.0, 0.0))
         .placeNearTarget()
         .text("The Input items can be dropped or placed on a Depot under the Press");
      scene.idle(50);
      ItemStack brass = new ItemStack(AllItems.BRASS_SHEET.asItem());
      scene.world().createItemOnBeltLike(depotPos, Direction.NORTH, brass);
      Vec3 depotCenter = util.vector().centerOf(depotPos.south());
      scene.overlay().showControls(depotCenter, Pointing.UP, 60).rightClick().withItem(brass);
      scene.idle(10);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().start(StampingBehaviour.Mode.BELT));
      scene.idle(30);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().makePressingParticleEffect(depotCenter.add(0.0, 0.5, 0.0), brass));
      scene.world().removeItemsFromBelt(depotPos);
      ItemStack sheet = new ItemStack((ItemLike)ModItems.UPPER_RECEIVER_BRASS.get(), 1);
      scene.world().createItemOnBeltLike(depotPos, Direction.UP, sheet);
      scene.idle(10);
      scene.overlay().showControls(depotCenter, Pointing.UP, 60).rightClick().withItem(sheet);
      scene.idle(60);
      scene.world().hideIndependentSection(depot, Direction.NORTH);
      scene.idle(5);
      scene.world().showSection(util.select().fromTo(0, 1, 3, 0, 2, 3), Direction.DOWN);
      scene.idle(10);
      scene.world().showSection(util.select().fromTo(4, 1, 2, 0, 2, 2), Direction.SOUTH);
      scene.idle(20);
      BlockPos beltPos = util.grid().at(0, 1, 2);
      scene.overlay()
         .showText(40)
         .pointAt(util.vector().blockSurface(beltPos, Direction.WEST))
         .placeNearTarget()
         .attachKeyFrame()
         .text("When items are provided on a belt...");
      scene.idle(30);
      ElementLink<BeltItemElement> ingot = scene.world().createItemOnBelt(beltPos, Direction.SOUTH, brass);
      scene.idle(15);
      ElementLink<BeltItemElement> ingot2 = scene.world().createItemOnBelt(beltPos, Direction.SOUTH, brass);
      scene.idle(15);
      scene.world().stallBeltItem(ingot, true);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().start(StampingBehaviour.Mode.BELT));
      scene.overlay().showText(50).pointAt(pressSide).placeNearTarget().attachKeyFrame().text("The Press will hold and process them automatically");
      scene.idle(30);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().makePressingParticleEffect(depotCenter.add(0.0, 0.5, 0.0), brass));
      scene.world().removeItemsFromBelt(pressPos.below(2));
      ingot = scene.world().createItemOnBelt(pressPos.below(2), Direction.UP, sheet);
      scene.world().stallBeltItem(ingot, true);
      scene.idle(15);
      scene.world().stallBeltItem(ingot, false);
      scene.idle(15);
      scene.world().stallBeltItem(ingot2, true);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().start(StampingBehaviour.Mode.BELT));
      scene.idle(30);
      scene.world().modifyBlockEntity(pressPos, type, pte -> pte.getStampingBehaviour().makePressingParticleEffect(depotCenter.add(0.0, 0.5, 0.0), brass));
      scene.world().removeItemsFromBelt(pressPos.below(2));
      ingot2 = scene.world().createItemOnBelt(pressPos.below(2), Direction.UP, sheet);
      scene.world().stallBeltItem(ingot2, true);
      scene.idle(15);
      scene.world().stallBeltItem(ingot2, false);
   }

   public static void distillation(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("distillation_column", "Setting up Fractional Distillation");
      scene.configureBasePlate(1, 0, 5);
      scene.showBasePlate();
      Selection distillationTank = util.select().fromTo(3, 2, 2, 4, 4, 3);
      Selection windowedTank = util.select().fromTo(3, 2, 4, 4, 4, 5);
      Selection tank = util.select().fromTo(3, 2, 0, 4, 4, 1);
      Selection blazeBurners = util.select().fromTo(3, 1, 2, 4, 1, 3);
      Selection pipe = util.select().fromTo(1, 1, 2, 2, 2, 2);
      Selection pipe1 = util.select().fromTo(0, 0, 2, 0, 2, 2);
      Selection cog1 = util.select().position(0, 0, 4);
      Selection cog2 = util.select().position(1, 1, 3);
      Selection cog3 = util.select().position(0, 1, 3);
      Selection pump = util.select().position(1, 2, 2);
      scene.idle(15);
      ElementLink<WorldSectionElement> tankElement = scene.world().showIndependentSection(tank, Direction.DOWN);
      scene.world().moveSection(tankElement, util.vector().of(0.0, 0.0, 2.0), 0);
      scene.world().showSection(blazeBurners, Direction.NORTH);
      scene.idle(20);
      scene.overlay()
         .showText(50)
         .attachKeyFrame()
         .text("Use a Thermostat on fluid tanks to convert them to a Distillation Column")
         .pointAt(util.vector().topOf(2, 2, 2))
         .placeNearTarget();
      scene.idle(60);
      scene.overlay().showControls(util.vector().topOf(3, 3, 2), Pointing.LEFT, 60).rightClick().withItem(new ItemStack((ItemLike)ModItems.THERMOSTAT.get()));
      scene.idle(15);
      ElementLink<WorldSectionElement> distillationTankElement = scene.world().showIndependentSectionImmediately(distillationTank);
      scene.world().moveSection(tankElement, util.vector().of(0.0, 10000.0, 2.0), 0);
      scene.world().hideIndependentSection(tankElement, Direction.DOWN);
      scene.idle(30);
      scene.world().showSection(pipe, Direction.DOWN);
      scene.world().showSection(pipe1, Direction.DOWN);
      scene.world().showSection(cog3, Direction.DOWN);
      scene.world().showSection(cog2, Direction.DOWN);
      scene.world().showSection(cog1, Direction.DOWN);
      scene.idle(30);
      scene.overlay().showText(50).attachKeyFrame().text("Pump in your input fluid").pointAt(util.vector().topOf(0, 2, 2)).placeNearTarget();
      scene.idle(30);
      scene.world().modifyKineticSpeed(cog1, f -> 16.0F);
      scene.world().modifyKineticSpeed(cog2, f -> -32.0F);
      scene.world().modifyKineticSpeed(cog3, f -> -32.0F);
      scene.world().modifyKineticSpeed(pump, f -> 64.0F);
      scene.idle(30);
      scene.overlay().showText(50).attachKeyFrame().text("Apply heat").pointAt(util.vector().centerOf(3, 1, 2)).placeNearTarget();
      scene.idle(60);
      scene.world().modifyBlocks(util.select().fromTo(3, 1, 2, 4, 1, 3), b -> (BlockState)b.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED), false);
      scene.idle(15);
      scene.overlay().showText(50).attachKeyFrame().text("Product is created in fractions").pointAt(util.vector().centerOf(3, 4, 2)).placeNearTarget();
      scene.idle(70);
      scene.overlay().showText(50).attachKeyFrame().text("Each column layer produces one product").pointAt(util.vector().centerOf(3, 4, 2)).placeNearTarget();
      scene.idle(35);
      scene.idle(15);
      scene.idle(60);
   }
}
