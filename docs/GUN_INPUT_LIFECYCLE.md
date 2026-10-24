# Gun input lifecycle

The pinned Flans0.4.316 `ClientInputHooks` samples raw keys at client END. Attack and Use each process pressed, held, then released state. `ActionManager` visits the selected main-hand gun before the offhand gun. Interaction callbacks do not author presses. The port now follows this sampling/order for firing, and applies the same two-hand dispatch to special/reload keys.

An executed baseline on cc2a36c set a loaded Mars's raw Attack key, invoked the actual END handler, and inspected pending recoil before queued replies could run. It produced no impulse. The same command body now produces17.0. A down/up entirely between END polls is intentionally ignored, matching the source sampling rule. This is target runtime evidence plus source control-flow inspection; no original1.20 client execution is claimed.

## Press, hold and release

`GunInputDispatcher` routes sampled inputs through the owning prediction/transport sink. It captures both gun identities in main/offhand order. Held processing updates an existing firing instance without restarting it; a missing FullAuto Shoot instance can start from a hold, including after reload or selection changes. Semi and Burst do not gain extra initial triggers from holding. Player replacement clears references.

The source's held branch is subtle: an unstarted non-FullAuto group receives an input update without `OnStart`; cancellation removes that unstarted instance. Cancellation preserves an already started Semi/Burst while `Finished()` remains false. It does not cancel a live burst on every held poll.

An explicit held/control phase is carried in `CGunInput`. The server independently resolves and validates the input, and only permits a held start for a missing FullAuto group. Controlled inputs disable the older autonomous server restart after reload. The owner authors the new activation, so recoil and authoritative shots retain one presentation authority. Existing server-origin/unpredicted callers keep the old path. Mandatory gun protocol is **9.0**; matching integrated registration/codecs are tested, mixed-version and dedicated rejection are unverified.

**Retained adaptation:** requests/releases retain captured gun UUIDs across selection changes. This also releases previously addressed guns, preventing a moved automatic gun from remaining stuck. Screen interruption releases captured controls. The recovered extra Fire2 binding is combined with vanilla Use as one logical input, preventing one alias from releasing or toggling the other. Offhand gun handling preserves unrelated main-hand interactions.

Synthetic tap helpers now explicitly sample the press before sampling release. The identity-release fixture similarly polls its press before changing selection. The separate queued-request fixture still sends selectionB before the request naming gunA. Those fixture changes are deliberate; the old interaction-only synthetic tap is not evidence of physical between-poll behavior.

## Verification and remaining scope

The suite passes186 GameTests/96 JUnit tests. New scenarios cover main/offhand order and ammunition, offhand-only input, two-handed restrictions, missed samples, held Semi/Burst, FullAuto selection changes, both captured releases, actual reload/owner restart without fallback receipts, control codecs and owner replacement. Prior all49 initial firing, fractional cadence, delayed actions and final-shot tests remain green.

Scripted native checks use the actual raw END handler and registered channel. Raw input now predicts before replies, an unsampled interaction does not fire, and main/offhand magazines each consume3->2 on both client and server. G11 recoil, MAT-49 final phase and gun identity regression scenarios also pass. A separate final Use check toggles ADS on then off, with matching server/client states and an observed FOV change. These are controlled commands, not physical mouse tests, original-client comparison, dedicated observers or a full pack run.

## Active-hold profile

The initial implementation of the new held path repeatedly resolved an already active FullAuto context and drained every runtime queue twice per hold. An active, non-continuing FullAuto node already ends the source's possible-group evaluation after updating its instance. That case now returns directly; modal guards are checked before the shortcut, while attachment expansion, continuing nodes and reload use the full resolver. Queues still flush at END and before a newly authored held start.

The opt-in `law_test_input_profile` measures the actual owning `GunPrediction.held` path on a loaded MAT-49:5000 warmup calls, seven batches of10000, allocation through HotSpot's thread allocation counter. It checks that every measured call creates zero new starts. The comparison is against the first implementation of this new held path, not the older build without held processing.

| Median across seven batches | Initial held path | Optimized held path |
| --- | ---: | ---: |
| Allocated bytes/call | 4920 | 736 |
| Elapsed nanoseconds/call | 2845.93 | 1369.22 |

Allocation fell about85%. Both runs show continued JIT warmup; timing is indicative for this isolated case, not a stable throughput, FPS or TPS claim. The complete dispatcher, render workload, other action/provider combinations, multiplayer and representative pack profiling remain outside this measurement.

Full parity remains open: general multi-success/concurrent groups, non-Shoot held/local action effects, additional repeat modes, original ballistic/spread RNG, ability contexts, copied-ID epochs, definition reloads and dedicated latency/TPS behavior. The wider migration matrix still governs completion.
