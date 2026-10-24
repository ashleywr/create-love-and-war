# Independent server firing groups

The pinned Flans0.4.316 `ActionStack` retains each plain-key group independently and shares one shot cooldown across the gun. Its `TickActions` visits groups in reverse activation order, completing the pending callbacks and repeat attempt of each group before visiting the next. `ActionGroupInstance.DoInitialTrigger`, `CheckRetrigger` and `Finished` define the callback counts and lifetime boundaries. These are inspected source facts; this unit has not executed the original1.20 client.

`GunFireScheduler` now retains separate cycles per plain group, with one shared `GunCadence` and input-control state per physical stack. Unclaimed input can start continuing Shoot groups without replacing the first cycle. A zero-count start still creates its timeline, descriptor and retained context. Same-key requests retain the first instance/provider and cannot reset cooldown. Finishing one cycle preserves siblings and updates the single-group fast-path reference. Invalid remote starts do not insert empty cycles.

`GunActionTimeline` advances groups in reverse activation order and invokes each server-origin firing cycle immediately after that group's pending callbacks. The subsequent scheduler pass is idempotent for an already advanced cycle. This prevents processing all delayed shots before all repeats. Single-group ticking avoids the snapshot used to protect concurrent traversal from callback-driven cancellation. No measured performance improvement is claimed.

## Source behavior preserved

- With two continuing FullAuto groups at0.1s delay, the first starts with one callback and the second owns an instance with zero callbacks. At the first eligible later tick, the later activation receives shared cooldown first. Independent groups do not each get a fresh firing budget.
- Initial delays<=0 each invoke one callback without spending shared cooldown. Nonpositive repeat delay instead becomes0.05s.
- Burst counts limit retrigger attempts, including attempts that produce zero callbacks. A positive-delay burst that starts with zero callbacks can finish on its first tick because `Progress > Duration * TriggerCount` then compares against0. It does not wait indefinitely for a cooldown opportunity.
- Start candidates snapshot modes/reload gates, while successive CanStart checks observe current ammunition. Delayed starts can both pass before either consumes a round. Their pending callbacks then compete in reverse activation order, including an empty callback after the last round has been consumed.
- A later group's repeat can consume the last round before an earlier group's pending callback. Processing all pending callbacks first changes this result.
- Unclaimed release preserves each finishing group's final pending tick; sibling cancellation and retained contexts remain independent. Existing server-origin fallback timing is retained; the owner-authored path supplies player final-shot behavior.

## Evidence and remaining work

Three new ownership/cadence tests fail on unchanged6cb2b3a production. After the cycle-map repair, two additional packet-observer tests expose reversed delayed-shot ownership and incorrect pending/repeat interleaving. Those failures are preserved outside the repository. A separate expanded fixture initially expected a zero-trigger burst to survive; the source lifetime condition contradicts that expectation, so the fixture now asserts its first-tick retirement. That exploratory failure is not counted as a production defect.

Ten new GameTests cover independent ownership, positive/zero delay, shared cadence/order, final pending release, sibling finishing, burst attempts/lifetime, same-key retention, updated CanStart ammunition and last-round callback/receipt ownership. The complete suite passes210 GameTests/96 JUnit tests. Existing integrated-client ADS, MAT-49 authored/final-shot/feedback and G11 prediction/recoil scenarios are regression checks for the refactor; they do not exercise concurrent owner-authored groups.

**Full firing concurrency is incomplete.** `CGunInput`, owner prediction and claimed input still carry/select one firing context. The server explicitly retains the prior one-authored-cycle contract until an aggregate claim schema and shared owner-phase validation are implemented. The new multi-cycle path is verified for unclaimed/server-origin Fire1 groups. The common held/release flag, generic other-input lifecycles, all local action effects, mixed-clock/provider behavior, observer/latency/TPS testing and the wider migration/performance matrix remain open. Protocol remains9.0. No publishing or deployment occurred.
