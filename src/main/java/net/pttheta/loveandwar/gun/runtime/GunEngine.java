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
         if ("Fire1".equals(inputType)) {
            // Release belongs to the player's trigger, including a gun moved out of the selected slot.
            GunFireScheduler.releaseAll(player);
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
               if ("Fire1".equals(inputType) && !GunFireScheduler.press(player, gunStack)) return;
               GunActionState state = (GunActionState)gunStack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
               GunAttachments attachments = (GunAttachments)gunStack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
               AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
               Optional<GunEngine.Resolved> resolved = resolveNode(player, gunStack, def, attachments, state, rounds, handler.get().nodes(), inputType, Optional.empty(), true);
               if (resolved.isPresent() && heldTriggerFallthrough(def, resolved.get(), inputType, state)) {
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

   public static Optional<ActionGroup> predictStartedGroup(Player shooter, ItemStack gunStack, String inputType, long gameTime) {
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
      Optional<GunEngine.Resolved> resolved = resolveNode(shooter, gunStack, def, attachments, state, rounds, handler.get().nodes(), inputType, Optional.empty(), true);
      if (!resolved.isEmpty() && !heldTriggerFallthrough(def, resolved.get(), inputType, state)) {
         Optional<ActionGroup> groupOpt = resolveActionGroup(def, resolved.get());
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

         return Optional.of(group);
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
      var resolved = resolveNode(player, gunStack, def, attachments, state, rounds, handler.get().nodes(), "Fire1", Optional.empty(), true);
      if (resolved.isPresent() && resolveActionGroup(def, resolved.get()).map(g -> GunFireMode.of(g.repeatMode()) == GunFireMode.FULL_AUTO).orElse(false)) {
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
                     Optional<GunEngine.Resolved> r = resolveNode(shooter, gunStack, def, attachments, state, rounds, overrideHandler.get().nodes(), inputType, attDef, false);
                     if (r.isPresent()) {
                        return r;
                     }
                  }
               }

            } else {
               String key = node.actionGroupToTrigger();
               if (!key.isEmpty()) {
                  GunEngine.Resolved candidate = new GunEngine.Resolved(key, context);
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
      Optional<ActionGroup> groupOpt = resolveActionGroup(def, candidate);
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
            if (fireMode != GunFireMode.TOGGLE && !reentrantFire && !reloadOverFire && state.busy()) {
               diag("Action group '{}' blocked: gun is busy (mid-action or mid-reload)", group.key());
               return false;
            } else {
               return true;
            }
         }
      }
   }

   private static boolean heldTriggerFallthrough(GunDef def, GunEngine.Resolved resolved, String inputType, GunActionState state) {
      return "Fire1".equals(inputType) && state.waitingForRelease()
         ? resolveActionGroup(def, resolved).map(g -> g.actions().stream().noneMatch(a -> "Shoot".equals(a.actionType()))).orElse(false)
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
         Optional<ActionGroup> override = resolved.context().get().actionOverrides().stream().filter(g -> g.key().equals(resolved.groupKey())).findFirst();
         if (override.isPresent()) {
            return override;
         }
      }

      return def.actionGroups().stream().filter(g -> g.key().equals(resolved.groupKey())).findFirst();
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
      ActionGroup group = groupOpt.get();
      GunFireMode mode = GunFireMode.of(group.repeatMode());
      if (mode == GunFireMode.TOGGLE) {
         boolean turningOn = !state.activeToggles().contains(group.key());
         gunStack.set(ModDataComponents.GUN_ACTION_STATE, state.withToggle(group.key(), turningOn));
         if (turningOn) triggerActions(player, gunStack, def, group, attachments);
         else GunActionTimeline.finishAfterTick(player, gunStack, group.key());
         return;
      }
      boolean firesRound = group.actions().stream().anyMatch(a -> "Shoot".equals(a.actionType()));
      if (firesRound && (mode == GunFireMode.SEMI_AUTO || mode == GunFireMode.FULL_AUTO || mode == GunFireMode.BURST_FIRE)) {
         GunFireScheduler.start(player, gunStack, def, group, attachments);
         return;
      }
      long now = player.level().getGameTime();
      if (firesRound && now < state.cooldownUntilGameTime()) return;
      Optional<GunDef.ReloadDef> reloadDef = def.reloads().stream().filter(rd -> rd.startActionKey().equals(group.key())).findFirst();
      if (reloadDef.isPresent()) {
         GunFireScheduler.cancel(player, gunStack);
         GunReload.start(player, gunStack, def, reloadDef.get(), state.withBurstRemaining(0));
         return;
      }
      GunActionState next = state.withGroup(group.key(), groupDurationTicks(group) + 1, List.of());
      if (firesRound) next = next.withCooldown(now + Math.round(group.repeatDelay() * 20.0F));
      gunStack.set(ModDataComponents.GUN_ACTION_STATE, next);
      triggerActions(player, gunStack, def, group, attachments);
   }

   static int groupDurationTicks(ActionGroup group) {
      return group.durationTicks();
   }

   static void triggerActions(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group, GunAttachments attachments) {
      GunActionTimeline.trigger(player, gunStack, def, group, attachments, false);
   }

   static void executeAction(ServerPlayer player, ItemStack gunStack, GunDef def, ActionGroup group, ActionDef action, List<ModifierSource> sources, int progress) {
      switch (action.actionType()) {
         case "Shoot":
            doShoot(player, gunStack, sources, group.key(), progress);
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

   private static void doShoot(ServerPlayer player, ItemStack gunStack, List<ModifierSource> sources, String group, int progress) {
      AmmoPouchContents rounds = (AmmoPouchContents)gunStack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
      if (!rounds.isEmpty()) {
         ItemStack round = rounds.items().get(0);
         List<ItemStack> remaining = new ArrayList<>(rounds.items().subList(1, rounds.items().size()));
         gunStack.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(remaining));
         diag("SHOT fired, {} rounds left", remaining.size());
         GunLookup.bulletDef(round).ifPresent(bullet -> GunBallistics.fire(player,
                 BuiltInRegistries.ITEM.getKey(round.getItem()), bullet, sources, group, GunTargeting.target(player, gunStack),
                 progress));
      }
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

   private record Resolved(String groupKey, Optional<AttachmentDef> context) {
   }
}
