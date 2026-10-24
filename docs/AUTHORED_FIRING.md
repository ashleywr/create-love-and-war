# Owner-authored firing phases

The pinned reference remains official Love & War 0.4.2 for Minecraft 1.20.1 with Flans 0.4.316. This unit replaces autonomous server repeats for locally predicted firing groups with validated owner-authored triggers. The server still executes ammunition consumption, ballistics, damage, observer effects and acknowledgement messages. Server-origin and unpredicted groups retain the existing world-clock path.

## Source behavior and desynchronization repair

Flans `ActionGroupInstance.OnTickClient` executes due callbacks and checks repeats before `ActionStack.TickActions` removes a finished instance. Released bursts continue; full-auto can execute a final eligible START callback after the END release. `ShootAction.CanRetrigger` disables autonomous server repeats for valid player owners.

The source sends dirty triggers from start and held/release updates. Its finish/removal path has no final dirty flush. Consequently a callback after release can affect local presentation/ammunition without reaching authoritative gameplay. This is a source control-flow finding; it has not been reproduced in a running original 1.20.1 client.

**Deviation: repair upstream desynchronization.** Explicit final flushing sends every owner-authored trigger before retirement. The paired MAT-49 oracle uses END0 press, START1, END1 release, START2. The previous port applied one server shot while the local runtime applied two. Both sides now apply two, preserving the original local callback phases. This does not claim that the original reliably transmitted its final shot.

## Transport and validation

- Initial trigger batches are atomic with the input request, including legitimate zero-trigger starts and multiple sub-tick callbacks. Subsequent messages carry a gun UUID, activation UUID, phase and trigger ordinal. They contain no reported hits or damage.
- The authenticated server player must still own the addressed inventory gun. The server resolves the input and retained first group/provider independently. An existing activation cannot switch between world-clock and owner-authored execution.
- The server validates nondecreasing phases, contiguous ordinals, fractional cadence, burst check windows, release/final phase and strict group lifetime. Duplicate triggers/steps and already retired messages cannot replay gameplay. Invalid active requests retire the group before further callbacks.
- A separate per-owner/gun rate budget survives activation changes. It has 30 ticks of catch-up credit, refills at 20 ticks per real second, and retains debt for long firing intervals. Reported phases may lead elapsed real time by at most 30 ticks. These are explicit security/latency adaptations, not the original wire protocol or a claim of arbitrary-latency parity.
- Due-action STEP messages advance the retained callback clock before repeats. Empty intervals can be skipped; pending callbacks retain their due phases, overwrite semantics and current-count indices. This callback clock does not implement the remaining general action `OnTick` behavior.
- **Clock adaptation:** owner-authored firing callbacks advance from validated owner phases; the original server used its own world ticks for pending actions. This keeps batched triggers, due callbacks and final retirement ordered together. Server-origin groups keep world ticks. Behavior under real transport delay/TPS divergence still requires verification.
- START work is queued for END transport. It is flushed before new END input edges, and final callbacks are sent even without another input edge. Owner change clears queued messages. A server denial/cancellation stops future local callbacks and queued requests without undoing recoil already displayed.

This milestone introduced gun protocol **8.0**. The later [input lifecycle](GUN_INPUT_LIFECYCLE.md) uses current protocol **9.0**. Matching development client/server registration has been exercised. Mixed-version rejection and dedicated multiplayer remain unverified.

## Evidence and limits

The failing paired MAT-49 baseline and a separate failing retirement baseline are preserved outside the repository. Server regressions cover the paired release, batched G11 burst, no synthesized world-tick repeats, duplicate delivery, authenticated ownership, activation mismatch, ordinal gaps, excessive phase lead, burst limits, fresh activation after cancellation, delayed overwrite/current-count callbacks, sub-tick multiple Shoot actions, the actual G11 animation lifetime and malformed metadata. Pure tests exercise cadence/lifetime validation, bounded catch-up and fractional/long-interval rate debt.

The expanded suite passes **178 GameTests and 96 JUnit tests**. All49 bundled guns exercise the new initial input/handler path with compatible ammunition, comparing local and authoritative round counts and callback indices. This uses the current compatibility matcher and does not establish the remaining magazine-selection/filter workflows.

A real integrated client exercises the interaction handler, actual registered payload channel and normal tick subscribers. Its one-poll MAT-49 sequence consumes 13 to 11 on both sides. A separate zero-length logical G11 tap consumes 12 to 9 with the unchanged recoil curve and total pitch 6.864933. Prediction still occurs before replies; duplicate owned feedback adds no impulse or camera RNG draws. These are scripted logical inputs, not physical mouse tests or an original-client comparison.

There are no per-idle-tick firing messages. Due callbacks share one STEP per activation/phase; final retirement is explicit. This is a bounded-work property, not a measured end-to-end performance result. Dedicated observers, delayed transport/TPS behavior, all held/both-hand/concurrent input semantics, local non-Shoot actions, ballistic spread/RNG order, ability contexts and copied-ID/ammunition epochs remain open. The wider migration matrix and representative performance gates remain required before completing the port.
