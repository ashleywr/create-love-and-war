# Attachment draw composition profile

Measured in the native NeoForge 1.21.1 development client on Java 21. This compares the first repaired renderer, which rebakes installed child elements per draw, with the cached-local-quad implementation. It does not compare the original 1.20.1 renderer or the incomplete recovered renderer.

The opt-in `law_test_attachment_profile` command runs on the render thread in a disposable integrated world. Each scene warms up with 1,000 calls, then records seven batches of 500 real ItemRenderer model/override, GROUND transform and quad queries. Elapsed CPU wall time uses System.nanoTime; allocation uses the current thread's ThreadMXBean counter. The table gives medians per query. Each attached query returns 198 faces; bare G11 returns 204 and brass Krag 336. These are CPU composition queries without GPU draw submission.

| Scene | Element rebake time | Cached geometry time | Element rebake allocation | Cached geometry allocation |
| --- | ---: | ---: | ---: | ---: |
| Bare G11 | 4.762 μs | 4.328 μs | 6,360 B | 5,720 B |
| G11 + telescope | 19.841 μs | 6.909 μs | 54,768 B | 12,992 B |
| Brass Krag | 7.049 μs | 4.788 μs | 10,024 B | 8,904 B |

The telescope case allocates about 76% less and takes about 65% less composition time in this run. JIT/GC/scheduling can affect timing and escape analysis; these are focused measurements from one host and run pair, not FPS, GPU, dedicated-server or modpack performance claims. The earlier cached implementation also showed small bare/brass allocation regressions; avoiding empty-section matrix copies and unnecessary idle muzzle resolution removed them in the final run.

Local geometry is baked once per available rig/skin/part, alongside existing static placed geometry. It costs additional bounded model-reload memory in exchange for eliminating element/UV/material reconstruction during transformed draws; retained heap and reload duration have not been profiled. No cache uses stack UUIDs, activations or ticks. Matrix placement clones packed vertices, retaining UV/light/colour/tint/shade/AO/emissive data and the existing authored-axis normal convention. Native checks compare vertex/UV pairs with direct element baking under two transforms for all 49 gun rigs, plus an exact installed telescope placement check. This does not establish complete original normals, material or render-backend equivalence.

Private evidence retains both profile logs, the pre-optimization source snapshot, source audits, test reports and native screenshots. The release artifacts exclude the opt-in command and all other development helpers.
