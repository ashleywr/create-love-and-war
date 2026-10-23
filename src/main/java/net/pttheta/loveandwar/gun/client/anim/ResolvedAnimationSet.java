package net.pttheta.loveandwar.gun.client.anim;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ResolvedAnimationSet {
   private final Map<String, Flanimation.Sequence> sequencesByName;
   private final Map<String, Map<String, PartPose>> effectivePoseByKeyframe;

   private ResolvedAnimationSet(Map<String, Flanimation.Sequence> sequencesByName, Map<String, Map<String, PartPose>> effectivePoseByKeyframe) {
      this.sequencesByName = sequencesByName;
      this.effectivePoseByKeyframe = effectivePoseByKeyframe;
   }

   static ResolvedAnimationSet resolve(Flanimation set) {
      Map<String, Flanimation.Keyframe> byName = new LinkedHashMap<>();

      for (Flanimation.Keyframe kf : set.keyframes()) {
         byName.put(kf.name(), kf);
      }

      Map<String, Map<String, PartPose>> resolved = new LinkedHashMap<>();

      for (String name : byName.keySet()) {
         resolvePose(name, byName, resolved, new HashSet<>());
      }

      Map<String, Flanimation.Sequence> sequences = new LinkedHashMap<>();

      for (Flanimation.Sequence seq : set.sequences()) {
         List<Flanimation.Frame> sorted = seq.frames().stream().sorted(Comparator.comparingInt(Flanimation.Frame::tick)).toList();
         sequences.put(seq.name(), new Flanimation.Sequence(seq.name(), seq.ticks(), sorted));
      }

      return new ResolvedAnimationSet(Map.copyOf(sequences), Map.copyOf(resolved));
   }

   private static Map<String, PartPose> resolvePose(
      String name, Map<String, Flanimation.Keyframe> byName, Map<String, Map<String, PartPose>> cache, Set<String> visiting
   ) {
      Map<String, PartPose> cached = cache.get(name);
      if (cached != null) {
         return cached;
      }

      Flanimation.Keyframe kf = byName.get(name);
      if (kf != null && visiting.add(name)) {
         Map<String, PartPose> merged = new LinkedHashMap<>();

         for (String parent : kf.parents()) {
            merged.putAll(resolvePose(parent, byName, cache, visiting));
         }

         for (Flanimation.RawPose pose : kf.poses()) {
            merged.put(pose.applyTo(), PartPose.from(pose));
         }

         visiting.remove(name);
         Map<String, PartPose> immutable = Map.copyOf(merged);
         cache.put(name, immutable);
         return immutable;
      } else {
         return Map.of();
      }
   }

   public Map<String, PartPose> pose(String keyframeName) {
      return this.effectivePoseByKeyframe.getOrDefault(keyframeName, Map.of());
   }

   public Optional<Flanimation.Sequence> sequence(String name) {
      return Optional.ofNullable(this.sequencesByName.get(name));
   }

   public Map<String, PartPose> poseAtTime(Flanimation.Sequence seq, float t) {
      return this.poseAtTime(seq, t, false);
   }

   public Map<String, PartPose> poseAtTime(Flanimation.Sequence seq, float t, boolean smoothKick) {
      List<Flanimation.Frame> frames = seq.frames();
      if (frames.isEmpty()) {
         return Map.of();
      }

      if (!(t < frames.get(0).tick()) && frames.size() != 1) {
         for (int i = 0; i < frames.size() - 1; i++) {
            Flanimation.Frame a = frames.get(i);
            Flanimation.Frame b = frames.get(i + 1);
            if (t <= b.tick()) {
               float span = b.tick() - a.tick();
               float u = span <= 0.0F ? 1.0F : Math.min(1.0F, Math.max(0.0F, (t - a.tick()) / span));
               float shaped = Easing.shape(a.exit(), b.entry(), u, smoothKick && i == 0 && a.tick() == 0);
               return blend(this.pose(a.frame()), this.pose(b.frame()), shaped);
            }
         }

         return this.pose(frames.get(frames.size() - 1).frame());
      } else {
         return this.pose(frames.get(0).frame());
      }
   }

   private static Map<String, PartPose> blend(Map<String, PartPose> a, Map<String, PartPose> b, float t) {
      if (t <= 0.0F) {
         return a;
      }

      if (t >= 1.0F) {
         return b;
      }

      Set<String> parts = new LinkedHashSet<>(a.keySet());
      parts.addAll(b.keySet());
      Map<String, PartPose> out = new LinkedHashMap<>();

      for (String part : parts) {
         out.put(part, PartPose.lerp(a.getOrDefault(part, PartPose.IDENTITY), b.getOrDefault(part, PartPose.IDENTITY), t));
      }

      return out;
   }
}
