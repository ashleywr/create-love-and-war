# Ordered input candidates and independent actions

The reference is Flans0.4.316 `GunContext.EvaluateInputHandler`, `EvaluateAttachmentInputHandler` and `ActionManager.IteratePossibleActionGroups`, used by the pinned official L&W1.20.1 release. These findings come from the pinned source; an original client has not been executed for this unit.

The source first builds an ordered candidate list using the mode and reload state at input entry. Actions execute afterward. A mode switch in the first action therefore cannot remove an already selected candidate or add a newly matching one during the same input. Each candidate then receives its own start checks. A successful candidate follows its continuation flag; a rejected candidate permits fallback. Independent groups do not share a blanket gun-busy restriction.

Attachment expansion is one level. Its child flag replaces the parent's flag. A valid stopping child truncates expansion before start checks, even if that child later refuses to start. Nested deferrals warn and are ignored. Empty group keys and groups without actions are invalid and cannot execute, consume input or truncate child expansion. Root reload flags are checked before attachment expansion; the child reload flag is not checked again.

**Quirk kept:** the bundled laser-scope gadget child stops evaluation even though its root deferral continues. Restoring continuation does not make every installed gadget activate together. The synthetic tests deliberately use continuing children where continuation is expected. The source's mode-compound presence rule is also retained: after the first switch creates the compound, another absent mode reads empty and cycles to its first value, rather than advancing its declared default.

## Implemented and verified

`GunEngine` now collects candidates before executing unclaimed inputs, attempts independent non-Shoot actions in order, retains same-key instances, and preserves an actual reload controller while an allowed independent action executes. The first-context predictor and claimed-firing path use the same candidate construction, including validity and attachment expansion rules.

Fourteen added GameTests cover ordered Toggle/Semi groups, mode snapshot inclusion/exclusion, successful stopping and rejected fallback, same-key reuse, parent/child continuation, ignored nested deferral, invalid root/child groups, independent strict lifetime boundaries and preservation of an actual reload stage. Three corrected source-derived tests fail on unchanged d258e1d production in a separate export. An earlier fixture incorrectly expected the second absent mode to advance to1; after correcting it to0, that unchanged export still fails all three cases. Two additional tests fail on the initial continuation implementation before the validity fix.

Validity enforcement also exposed eleven older mode/restriction tests whose supposed valid groups had no actions. Their fixtures now explicitly contain an inert ADS action; their mode, gate, cancellation and fallback assertions remain. The initial failures and pre-correction fixtures are preserved outside the repository. The corrected full suite passes200 GameTests/96 JUnit tests.

An opt-in integrated-client fixture temporarily substitutes two ADS Toggle groups, with factors2 and4, into Mars. Use starts both in order; the server and client observe both descriptors, mean factor3 and the expected player FOV modifier. A second Use removes both and restores the factor1 path. Six client assertions pass. MAT-49 owner/server final-shot, prediction-feedback and G11 before-reply/recoil regressions also pass. Cleanup restores the original definition, inventory, mode and view; the helper is excluded from release JARs. This shared-JVM fixture does not prove dedicated definition synchronization, physical input, an original-client comparison or observer/latency behavior.

## Remaining work

This is an intermediate repair of independent non-Shoot input continuation. Claimed firing and local prediction still select one firing context. The server scheduler still holds one firing cycle per physical gun; unclaimed input stops after its first accepted Shoot group so a sibling cannot silently replace that live cycle. Full multi-Shoot continuation requires aggregate claims, independent group lifecycles, shared source cadence and matching client activation/tick order. It remains required for full parity.

Non-Shoot owner-local effects, held/release behavior and repeat modes are not completed by this repair. Sound, Animation, AttachEffect, Laser and SpawnParticle need their original local callbacks and rendering paths. Original ballistic/RNG/ability contexts, dedicated/observer/latency tests and the wider migration matrix remain open. No performance improvement is claimed for candidate construction; representative profiling remains required. No publication or deployment occurred.
