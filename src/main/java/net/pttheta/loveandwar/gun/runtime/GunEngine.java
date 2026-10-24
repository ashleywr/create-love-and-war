package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.ActionDef;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.InputHandler;
import net.pttheta.loveandwar.gun.content.InputNode;
import net.pttheta.loveandwar.gun.content.MagazineDef;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GunEngine {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-runtime");

   static void diag(String message, Object... args) {
      if (Config.GUN_DIAGNOSTICS != null && (Boolean)Config.GUN_DIAGNOSTICS.get()) {
         LOGGER.info(message, args);
      } else {
         LOGGER.debug(message, args);
      }
   }

   private GunEngine() {
   }

   public static void handleInput(ServerPlayer player, ItemStack gunStack, InteractionHand hand, String inputType, boolean pressed) {
      if (!pressed) {
         if ("Fire1".equals(inputType) || GunPresentation.authored().present() && GunPresentation.claim().present()) {
            // The input router resolves the originally pressed gun, including a move to an unselected slot.
            GunFireScheduler.release(player, gunStack);
            if (gunStack.getItem() instanceof GunItem) {
               GunActionState state = gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
               gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.released());
            }
         }
      } else {
         if (!(gunStack.getItem() instanceof GunItem) || player.isSpectator() || !player.isAlive()) return;
         cleanOrphanedToggles(player, gunStack);
         Optional<GunDef> defOpt = GunLookup.gunDef(gunStack);
         if (defOpt.isEmpty()) {
            if (gunStack.getItem() instanceof GunItem) {
               diag("'{}' has no matching GunDef (registry path must equal a guns/*.json filename) - {} ignored", GunLookup.contentKey(gunStack), inputType);
            }
         } else {
            GunDef def = defOpt.get();
            Optional<InputHandler> handler = def.inputHandlers().stream().filter(h -> h.inputType().equals(inputType)).findFirst();
            if (handler.isEmpty()) {
               diag("Gun '{}' has no inputHandlers entry for inputType '{}'", GunLookup.contentKey(gunStack), inputType);
            } else {
               if ("Fire1".equals(inputType) && !GunFireScheduler.press(player, gunStack) && !GunPresentation.heldInput()) return;
               GunActionState state = (GunActionState)gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
               GunAttachments attachments = (GunAttachments)gunStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
               AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
               Optional<GunEngine.Resolved> resolved = resolveNode(player, gunStack, def, attachments, state, rounds, handler.get().nodes(), inputType, Optional.empty(), GunActiveGroups.Origin.base(gunStack), true);
               if (resolved.isPresent() && heldTriggerFallthrough(player, gunStack, def, resolved.get(), inputType, state)) {
                  diag(
                     "Gun '{}': '{}' needs a fresh Fire1 press, not the held one that fired the last round",
                     GunLookup.contentKey(gunStack),
                     resolved.get().groupKey()
                  );
               } else {
                  if (resolved.isPresent()) {
                     tryTrigger(player, gunStack, def, resolved.get(), attachments, state);
                  } else if (LOGGER.isDebugEnabled()) {
                     diag(
                        "Gun '{}': no node under inputType '{}' resolved to a satisfied action group (out of ammo / busy / no attachment installed / failed modalCheck are the usual reasons)",
                        GunLookup.contentKey(gunStack),
                        inputType
                     );
                  }
               }
            }
         }
      }
   }

   public record PredictedContext(GunDef gun,ActionGroup group,GunActiveGroups.Origin origin) {}

   public static Optional<ActionGroup> predictStartedGroup(Player shooter, ItemStack gunStack, String inputType, long gameTime) {
      return predictInputContext(shooter,gunStack,inputType,gameTime).map(PredictedContext::group);
   }

   public static Optional<PredictedContext> predictInputContext(Player shooter, ItemStack gunStack, String inputType, long gameTime) {
      Optional<GunDef> defOpt = GunLookup.gunDef(gunStack);
      if (defOpt.isEmpty()) {
         return Optional.empty();
      }

      GunDef def = defOpt.get();
      Optional<InputHandler> handler = def.inputHandlers().stream().filter(h -> h.inputType().equals(inputType)).findFirst();
      if (handler.isEmpty()) {
         return Optional.empty();
      }

      GunActionState state = (GunActionState)gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
      GunAttachments attachments = (GunAttachments)gunStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
      AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      Optional<GunEngine.Resolved> resolved = resolveNode(shooter, gunStack, def, attachments, state, rounds, handler.get().nodes(), inputType, Optional.empty(), GunActiveGroups.Origin.base(gunStack), true);
      if (!resolved.isEmpty() && !heldTriggerFallthrough(shooter, gunStack, def, resolved.get(), inputType, state)) {
         if (resolveActionGroup(def, resolved.get()).map(group -> GunFireMode.of(group.repeatMode()) == GunFireMode.TOGGLE).orElse(false)
               && hasRetainedGroup(shooter, gunStack, resolved.get().groupKey())) return Optional.empty();
         Optional<ActionGroup> groupOpt = effectiveActionGroup(shooter, gunStack, def, resolved.get());
         if (groupOpt.isEmpty()) {
            return Optional.empty();
         }

         ActionGroup group = groupOpt.get();
         GunFireMode mode = GunFireMode.of(group.repeatMode());
         if (mode == GunFireMode.TOGGLE) {
            return Optional.empty();
         }

         boolean firesRound = group.actions().stream().anyMatch(a -> "Shoot".equals(a.actionType()));
         if (firesRound && mode == GunFireMode.SEMI_AUTO && state.waitingForRelease()) {
            return Optional.empty();
         }

         if (firesRound && gameTime < state.cooldownUntilGameTime()) {
            return Optional.empty();
         }

         var origin=GunActiveGroups.of(gunStack).find(group.key()).map(GunActiveGroups.Entry::origin).orElse(resolved.get().origin());
         return Optional.of(new PredictedContext(def,group,origin));
      } else {
         return Optional.empty();
      }
   }

   public static void tick(ServerPlayer player, ItemStack gunStack) {
      if (!player.isAlive() || player.isSpectator()) {
         GunFireScheduler.releaseAll(player);
         GunFireScheduler.cancel(player, gunStack);
         GunActionTimeline.cancelAll(player, gunStack);
         var state = gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
         for (String key : state.activeToggles()) state = state.withToggle(key, false);
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0).withWaitingForRelease(false));
         return;
      }
      GunActionTimeline.tick(player, gunStack);
      cleanOrphanedToggles(player, gunStack);
      try {
         var previous = gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
         if (!previous.activeGroup().isEmpty() && !GunActionTimeline.owns(player, gunStack, previous.activeGroup())
               && !GunFireScheduler.owns(player, gunStack, previous.activeGroup())) {
            // Original ActionStack is runtime-only: saved/copied/transferred or dropped groups cannot resume.
            gunStack.set(ModDataComponents.GUN_ACTION_STATE, previous.idled().withBurstRemaining(0).withWaitingForRelease(false));
         }
         if (GunFireScheduler.tick(player, gunStack)) return;
         var state = gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
         if (!state.activeGroup().isEmpty() && GunActionTimeline.advanceState(player, gunStack, state.activeGroup())) {
            var def = GunLookup.gunDef(gunStack);
            if (def.isPresent()) {
               var ticked = state.tickedGroup();
               if (ticked.groupTicksElapsed() < ticked.groupTotalTicks() && !GunActionTimeline.isFinishing(player, gunStack, state.activeGroup())) {
                  gunStack.set(ModDataComponents.GUN_ACTION_STATE, ticked);
               } else if (isReloadGroup(def.get(), ticked.activeGroup())) {
                  GunReload.advance(player, gunStack, def.get(), ticked);
               } else {
                  gunStack.set(ModDataComponents.GUN_ACTION_STATE, ticked.idled());
               }
            }
         }
      } finally { GunActionTimeline.finishTick(player, gunStack); }
   }

   private static void cleanOrphanedToggles(ServerPlayer player, ItemStack stack) {
      GunActionTimeline.synchronize(player, stack);
      var previous = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
      var current = previous;
      for (String key : previous.activeToggles()) if (!GunActionTimeline.owns(player, stack, key)) current = current.withToggle(key, false);
      if (current != previous) stack.set(ModDataComponents.GUN_ACTION_STATE, current);
   }

   static void tryHeldFire(ServerPlayer player, ItemStack gunStack) {
      var definition = GunLookup.gunDef(gunStack);
      var rounds = gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      if (definition.isEmpty() || rounds.isEmpty()) return;
      var def = definition.get();
      var handler = def.inputHandlers().stream().filter(h -> h.inputType().equals("Fire1")).findFirst();
      if (handler.isEmpty()) return;
      var state = gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
      var attachments = gunStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
      var resolved = resolveNode(player, gunStack, def, attachments, state, rounds, handler.get().nodes(), "Fire1", Optional.empty(), GunActiveGroups.Origin.base(gunStack), true);
      if (resolved.isPresent() && effectiveActionGroup(player, gunStack, def, resolved.get()).map(g -> GunFireMode.of(g.repeatMode()) == GunFireMode.FULL_AUTO).orElse(false)) {
         tryTrigger(player, gunStack, def, resolved.get(), attachments, state);
      }
   }

   private static Optional<GunEngine.Resolved> resolveNode(
      Player shooter,
      ItemStack gunStack,
      GunDef def,
      GunAttachments attachments,
      GunActionState state,
      AmmoPouchContents rounds,
      List<InputNode> nodes,
      String inputType,
      Optional<AttachmentDef> context,
      GunActiveGroups.Origin origin,
      boolean rootHandler
   ) {
      for (InputNode node : nodes) {
         // Pinned GunContext applies this gate before expanding an attachment. Nested attachment nodes omit it.
         if (rootHandler && !node.canTriggerWhileReloading() && isReloadGroup(def, state.activeGroup())) continue;
         if (node.modalCheck().isEmpty() || modalCheckPasses(def, state, node.modalCheck())) {
            if (node.deferToAttachment()) {
               String category = GunAttachments.category(node.attachmentType());
               List<ItemStack> slotStacks = attachments.forCategory(category);
               if (node.attachmentIndex() < slotStacks.size()) {
                  Optional<AttachmentDef> attDef = GunLookup.attachmentDef(slotStacks.get(node.attachmentIndex()));
                  Optional<InputHandler> overrideHandler = attDef.flatMap(
                     ad -> ad.handlerOverrides().stream().filter(h -> h.inputType().equals(inputType)).findFirst()
                  );
                  if (overrideHandler.isPresent()) {
                     Optional<GunEngine.Resolved> r = resolveNode(shooter, gunStack, def, attachments, state, rounds, overrideHandler.get().nodes(), inputType, attDef, new GunActiveGroups.Origin(node.attachmentType(), node.attachmentIndex(), GunLookup.contentKey(slotStacks.get(node.attachmentIndex()))), false);
                     if (r.isPresent()) {
                        return r;
                     }
                  }
               }

            } else {
               String key = node.actionGroupToTrigger();
               if (!key.isEmpty()) {
                  GunEngine.Resolved candidate = new GunEngine.Resolved(key, context, origin);
                  if (isSatisfied(shooter, gunStack, def, candidate, rounds, state)) {
                     return Optional.of(candidate);
                  }
                  // Original attachment expansion stops at a valid child's flag before CanStart runs.
                  if (!rootHandler && !node.andContinueEvaluating() && resolveActionGroup(def, candidate).isPresent()) return Optional.empty();
               }
            }
         }
      }

      return Optional.empty();
   }

   private static boolean isSatisfied(Player shooter, ItemStack gunStack, GunDef def, GunEngine.Resolved candidate, AmmoPouchContents rounds, GunActionState state) {
      Optional<ActionGroup> groupOpt = effectiveActionGroup(shooter, gunStack, def, candidate);
      if (groupOpt.isEmpty()) {
         LOGGER.warn(
            "inputHandlers node references undefined action group '{}' (not present in actionGroups[] or the resolved attachment's actionOverrides[])",
            candidate.groupKey()
         );
         return false;
      } else {
         ActionGroup group = groupOpt.get();
         if (!GunActionRestrictions.canStart(shooter, group)) return false;
         if (isReloadStart(def, group.key())) {
            var magazine = def.magazines().isEmpty() ? Optional.<MagazineDef>empty() : GunLookup.resolveMagazineForSlot(def.magazines().getFirst());
            if (magazine.isEmpty() || !GunReload.canLoad(shooter, gunStack, magazine.get())) return false;
         }
         boolean needsAmmo = group.actions().stream().anyMatch(a -> "Shoot".equals(a.actionType()));
         if (needsAmmo && rounds.isEmpty()) {
            diag("Action group '{}' needs ammo but GUN_LOADED_ROUNDS is empty", group.key());
            return false;
         } else {
            GunFireMode fireMode = GunFireMode.of(group.repeatMode());
            boolean reentrantFire = needsAmmo && state.activeGroup().equals(group.key());
            boolean reloadOverFire = isReloadStart(def, group.key()) && !isReloadGroup(def, state.activeGroup());
            if (fireMode != GunFireMode.TOGGLE && !reentrantFire && !reloadOverFire && !hasRetainedGroup(shooter, gunStack, group.key()) && state.busy()) {
               diag("Action group '{}' blocked: gun is busy (mid-action or mid-reload)", group.key());
               return false;
            } else {
               return true;
            }
         }
      }
   }

   private static boolean heldTriggerFallthrough(Player shooter, ItemStack gunStack, GunDef def, GunEngine.Resolved resolved, String inputType, GunActionState state) {
      return "Fire1".equals(inputType) && state.waitingForRelease()
         ? effectiveActionGroup(shooter, gunStack, def, resolved).map(g -> g.actions().stream().noneMatch(a -> "Shoot".equals(a.actionType()))).orElse(false)
         : false;
   }

   private static boolean isReloadStart(GunDef def, String key) {
      return def.reloads().stream().anyMatch(rd -> rd.startActionKey().equals(key));
   }

   private static boolean isReloadGroup(GunDef def, String key) {
      return !key.isEmpty()
         && def.reloads()
            .stream()
            .anyMatch(
               rd -> key.equals(rd.startActionKey()) || key.equals(rd.ejectActionKey()) || key.equals(rd.loadOneActionKey()) || key.equals(rd.endActionKey())
            );
   }

   private static Optional<ActionGroup> resolveActionGroup(GunDef def, GunEngine.Resolved resolved) {
      if (resolved.context().isPresent()) {
         return resolved.context().get().actionOverrides().stream().filter(g -> g.key().equals(resolved.groupKey())).findFirst();
      }

      return def.actionGroups().stream().filter(g -> g.key().equals(resolved.groupKey())).findFirst();
   }

   private static Optional<ActionGroup> retainedGroup(Player shooter, ItemStack stack, String key) {
      if (shooter instanceof ServerPlayer server) return GunActionTimeline.context(server, stack, key).map(GunActionTimeline.Context::group);
      return GunActiveGroups.of(stack).find(key).filter(entry -> entry.owner().equals(shooter.getUUID())).flatMap(GunActiveGroups.Entry::resolve);
   }

   private static boolean hasRetainedGroup(Player shooter, ItemStack stack, String key) {
      return retainedGroup(shooter, stack, key).isPresent();
   }

   private static Optional<ActionGroup> effectiveActionGroup(Player shooter, ItemStack stack, GunDef def, Resolved resolved) {
      // An undefined candidate still cannot create/target a group merely because another provider is active.
      return resolveActionGroup(def, resolved).flatMap(candidate -> retainedGroup(shooter, stack, candidate.key()).or(() -> Optional.of(candidate)));
   }

   private static boolean modalCheckPasses(GunDef def, GunActionState state, String modalCheck) {
      return GunModes.modalCheck(def, state, modalCheck);
   }

   private static void tryTrigger(
      ServerPlayer player,
      ItemStack gunStack,
      GunDef def,
      GunEngine.Resolved resolved,
      GunAttachments attachments,
      GunActionState state
   ) {
      Optional<ActionGroup> groupOpt = resolveActionGroup(def, resolved);
      if (groupOpt.isEmpty()) {
         LOGGER.warn("tryTrigger called with unresolvable action group '{}'", resolved.groupKey());
         return;
      }
      ActionGroup candidate = groupOpt.get();
      var retained = GunActionTimeline.context(player, gunStack, candidate.key());
      if(GunPresentation.heldInput()&&(GunFireMode.of(candidate.repeatMode())!=GunFireMode.FULL_AUTO||retained.isPresent()))return;
      if(!GunPresentation.permits(candidate.key(),retained.map(GunActionTimeline.Context::origin).orElse(resolved.origin())))return;
      if(!GunActionTimeline.permitsPresentation(player,gunStack,candidate.key()))return;
      // Quirk kept: Toggle is checked on the requested context, while CanStart/actions use the retained instance.
      if (GunFireMode.of(candidate.repeatMode()) == GunFireMode.TOGGLE && retained.isPresent()) {
         var current = gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, current.withToggle(candidate.key(), false));
         GunActionTimeline.finishAfterTick(player, gunStack, candidate.key());
         GunFireScheduler.finishModeGroup(player, gunStack, candidate.key());
         return;
      }
      ActionGroup group = retained.map(GunActionTimeline.Context::group).orElse(candidate);
      GunDef actualGun = retained.map(GunActionTimeline.Context::gun).orElse(def);
      var origin = retained.map(GunActionTimeline.Context::origin).orElse(resolved.origin());
      if(!GunPresentation.permits(group.key(),origin))return;
      GunFireMode mode = GunFireMode.of(group.repeatMode());
      if (mode == GunFireMode.TOGGLE) {
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.withToggle(group.key(), true));
         triggerActions(player, gunStack, actualGun, group, attachments, origin);
         return;
      }
      boolean firesRound = group.actions().stream().anyMatch(action -> "Shoot".equals(action.actionType()));
      if (firesRound && (mode == GunFireMode.SEMI_AUTO || mode == GunFireMode.FULL_AUTO || mode == GunFireMode.BURST_FIRE)) {
         GunFireScheduler.start(player, gunStack, actualGun, group, attachments, origin);
         return;
      }
      long now = player.level().getGameTime();
      if (firesRound && now < state.cooldownUntilGameTime()) return;
      Optional<GunDef.ReloadDef> reloadDef = actualGun.reloads().stream().filter(rd -> rd.startActionKey().equals(group.key())).findFirst();
      if (reloadDef.isPresent()) {
         GunFireScheduler.cancel(player, gunStack);
         GunReload.start(player, gunStack, actualGun, reloadDef.get(), state.withBurstRemaining(0));
         return;
      }
      int progress = retained.map(GunActionTimeline.Context::progress).orElse(0);
      long triggers = retained.map(GunActionTimeline.Context::triggers).orElse(0L) + 1;
      int total = (int)Math.min(Integer.MAX_VALUE, (long)groupDurationTicks(group) * triggers + 1);
      GunActionState next = state.withFiringFrame(group.key(), progress, total, state.cooldownUntilGameTime(), state.burstRemaining(), state.waitingForRelease());
      if (firesRound) next = next.withCooldown(now + Math.round(group.repeatDelay() * 20.0F));
      gunStack.set(ModDataComponents.GUN_ACTION_STATE, next);
      triggerActions(player, gunStack, actualGun, group, attachments, origin);
   }

   static int groupDurationTicks(ActionGroup group) {
      return group.durationTicks();
   }

   static void triggerActions(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group, GunAttachments attachments) {
      triggerActions(player, gunStack, def, group, attachments, GunActiveGroups.Origin.base(gunStack));
   }

   static void triggerActions(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group, GunAttachments attachments, GunActiveGroups.Origin origin) {
      GunActionTimeline.trigger(player, gunStack, def, group, attachments, false, origin);
   }

   static void executeAction(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group, ActionDef action, List<ModifierSource> sources, int progress) {
      executeAction(player,gunStack,def,group,action,sources,progress,0);
   }

   static void executeAction(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group, ActionDef action, List<ModifierSource> sources, int progress,long triggerIndex) {
      switch (action.actionType()) {
         case "Shoot":
            doShoot(player, gunStack, sources, group.key(), progress,triggerIndex);
            break;
         case "EjectCasing":
            doEjectCasing(player, def);
            break;
         case "PlaySound":
            GunSounds.send(player, group, action, sources);
            break;
         case "SwitchMode":
            GunModes.switchMode(player, gunStack, def, sources);
            break;
         case "SpawnParticle":
            doSpawnParticle(player);
      }
   }

   static void triggerSounds(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group) {
      GunActionTimeline.trigger(player, gunStack, def, group, gunStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY), true);
   }

   private static void doShoot(ServerPlayer player, ItemStack gunStack, List<ModifierSource> sources, String group, int progress,long triggerIndex) {
      AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      boolean producedShot=false;
      if (!rounds.isEmpty()) {
         ItemStack round = rounds.items().get(0);
         List<ItemStack> remaining = new ArrayList<>(rounds.items().subList(1, rounds.items().size()));
         GunAmmo.set(gunStack, new AmmoPouchContents(remaining));
         diag("SHOT fired, {} rounds left", remaining.size());
         var bullet=GunLookup.bulletDef(round);
         if(bullet.isPresent())producedShot=GunBallistics.fire(player,
                 BuiltInRegistries.ITEM.getKey(round.getItem()),bullet.get(),sources,group,GunTargeting.target(player,gunStack),progress);
      }
      var context=net.pttheta.loveandwar.gun.content.stat.ModifierContext.ofGroups(List.of(group));
      float vertical=producedShot?Math.max(0,net.pttheta.loveandwar.gun.content.stat.StatResolver.resolve("vertical_recoil",0,sources,context)):0;
      float horizontal=producedShot?Math.max(0,net.pttheta.loveandwar.gun.content.stat.StatResolver.resolve("horizontal_recoil",0,sources,context)):0;
      // Original local Shoot callback adds once after its shot collection, including a zero impulse for an empty collection.
      GunPresentation.shot(player,gunStack,horizontal,vertical,triggerIndex,!rounds.isEmpty());
   }

   private static void doSpawnParticle(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 var3 = player.getEyePosition().add(player.getLookAngle().scale(0.9)).subtract(0.0, 0.15, 0.0);
         level.sendParticles(ParticleTypes.FLASH, var3.x, var3.y, var3.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.SMOKE, var3.x, var3.y, var3.z, 4, 0.02, 0.02, 0.02, 0.01);
      }
   }

   private static void doEjectCasing(ServerPlayer player, GunDef def) {
      findMagazineDef(player, def)
         .ifPresent(
            mag -> {
               if (mag.ejectCasings() && !mag.spawnBulletCasing().isEmpty()) {
                  ResourceLocation itemId = parseRL(mag.spawnBulletCasing());
                  BuiltInRegistries.ITEM
                     .getOptional(itemId)
                     .ifPresent(
                        casingItem -> {
                           ItemStack casing = new ItemStack(casingItem);
                           Vec3 look = player.getLookAngle();
                           Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
                           Vec3 at = player.getEyePosition().add(look.multiply(1.0, 0.0, 1.0).normalize().scale(0.5)).add(right.scale(0.6)).add(0.0, -0.9, 0.0);
                           ItemEntity entity = new ItemEntity(player.level(), at.x, at.y, at.z, casing);
                           entity.setDeltaMovement(
                              right.x * 0.15 + (player.getRandom().nextFloat() - 0.5) * 0.05,
                              0.15 + player.getRandom().nextFloat() * 0.05,
                              right.z * 0.15 + (player.getRandom().nextFloat() - 0.5) * 0.05
                           );
                           player.level().addFreshEntity(entity);
                        }
                     );
               }
            }
         );
   }

   static Optional<MagazineDef> findMagazineDef(ServerPlayer player, GunDef def) {
      return def.magazines().isEmpty() ? Optional.empty() : GunLookup.resolveMagazineForSlot(def.magazines().get(0));
   }

   private static ResourceLocation parseRL(String s) {
      return s.indexOf(58) >= 0 ? ResourceLocation.parse(s) : ResourceLocation.withDefaultNamespace(s);
   }

   public static void tryInstallOrRemoveAttachment(ServerPlayer player, ItemStack gunStack, InteractionHand hand) {
      Optional<GunDef> defOpt = GunLookup.gunDef(gunStack);
      if (!defOpt.isEmpty()) {
         GunDef def = defOpt.get();
         InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         ItemStack otherStack = player.getItemInHand(other);
         GunAttachments attachments = (GunAttachments)gunStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
         if (!otherStack.isEmpty()) {
            Optional<AttachmentDef> attDefOpt = GunLookup.attachmentDef(otherStack);
            if (attDefOpt.isEmpty()) {
               return;
            }

            String category = GunAttachments.category(attDefOpt.get().attachmentType());
            GunDef.AttachmentSlot slot = GunLookup.slotFor(def.attachmentSlots(), category);
            List<ItemStack> current = attachments.forCategory(category);
            if (current.size() >= slot.numAttachmentSlots() || !GunLookup.matchesAttachmentSlot(slot, otherStack)) {
               return;
            }

            List<ItemStack> next = new ArrayList<>(current);
            next.add(otherStack.copyWithCount(1));
            gunStack.set(ModDataComponents.GUN_ATTACHMENTS, attachments.with(category, next));
            otherStack.shrink(1);
         } else {
            for (String category : new String[]{"generic", "scope", "stock", "grip", "barrel"}) {
               List<ItemStack> current = attachments.forCategory(category);
               if (!current.isEmpty()) {
                  List<ItemStack> next = new ArrayList<>(current);
                  ItemStack removed = next.remove(next.size() - 1);
                  gunStack.set(ModDataComponents.GUN_ATTACHMENTS, attachments.with(category, next));
                  player.getInventory().placeItemBackInInventory(removed);
                  return;
               }
            }
         }
      }
   }

   private record Resolved(String groupKey, Optional<AttachmentDef> context, GunActiveGroups.Origin origin) {
   }
}
