# NeoForge port audit and repair status

Updated 2026-10-23. This is a maintained, unofficial reconstruction. It builds, but it is **not yet a faithful replacement for the original gun engine**. The first source audit identifies substantial omissions; the tested fixes below do not certify every subsystem.

## Pinned references and recovery history

- Gameplay reference: official Create: Love & War `0.4.2-1.20.1-Create-6.0.6`, Modrinth version `1FjYbLl5`.
- Original engine dependency inspected: Flan's Mod Reloaded `0.4.316`, compatible with the official release's declared dependency range. Historical players may have used different dependency versions.
- Recovered port baseline: `1.0.9-1.21.1`, input SHA-256 `3cb2032a80019661c388c5204a6ec1c5899d8e743b168f47560fb465d1c6d4bb`.
- Commit `6767017`, tag `recovered-1.0.9`, preserves all 194 recovered Java units before fixes. All 194 were compared against the untouched decompiler output, ignoring line-ending representation only: zero missing or changed files.
- Commit `0b96e69` contains compilation reconstruction separately from behavioral repairs: generic types, interface default-method calls, ambiguous nested types and the obsolete VecHelper import.
- Java 21; Minecraft 1.21.1; NeoForge 21.1.253; Create 6.0.10-280. See [reference-manifest.json](reference-manifest.json) for download URLs and hashes.

The official project metadata declares MIT. Its binary retains an `All Rights Reserved` metadata string, also copied into the recovered port; the unofficial port's project listing declares CC-BY-NC-4.0. These are recorded as provenance, not silently replaced with a blanket new license. Original credits remain Proto and Alcoholism. Nothing has been published from this reconstruction.

## Confirmed fixes in this checkout

| Repair | Original or baseline defect | Evidence |
| --- | --- | --- |
| Bench shape and occlusion | Flans used `box(1,0,1,15,12,15)`; replacement inherited a full cube and hid neighboring terrain | Baseline GameTests failed; six bench shapes and 24 orientations now pass; isolated client screenshot inspected |
| Bench inventory drops | Replacement omitted original `onRemove` drop behavior | Baseline dropped zero stored items; test now recovers all three inventories and prevents drops on facing changes |
| Legacy craft progress synchronization | Menu discarded received countdown data | Menu retains received values; countdown completion is now limited to saved crafts from the recovered port |
| Gun crafting selection and timing | Rewrite chose the first matching recipe, ignored ingredient positions, and added a countdown absent from original gun crafting | Ordered matching, explicit server-validated selection, immediate result preview and take restored; all 45 bench-allowed recipes craft in GameTests; selection/take observed in a client |
| Crafted-part persistence | Rewrite assembled a bare gun | Ordered canonical part IDs saved and synchronized as an immutable component; item NBT and packet round trips verified; runtime trait effects remain pending |
| Bench opening packet | Client required an existing world block entity and disconnected if the menu arrived first | Menu now constructs its own synchronized inventory from server-provided bench identity; baseline client disconnect reproduced, corrected client opens successfully; packet-construction regression test |
| Missing fluid models | Seven fluid blockstates and seven inventory models absent in both official and port binaries | Added vanilla-compatible fluid block models and existing bucket inventory parents; client model loading reports no missing mod models/textures |
| Missing ammunition/egg texture references | Buckshot skin and unused egg texture absent upstream | Buckshot uses the existing matching grenade skin; egg uses vanilla spawn-egg parent; direct reference audit and client load verified |
| Tungsten armor slot values | Port read old helmet/chest/legs/boots array in reverse slot convention | Baseline failed; registered material now gives 5/7/5/4 in the original order |
| Oil distillation heat/duration | Codec expected camelCase, bundled target recipe used snake_case | Baseline decoded zero duration; loaded recipe now requires HEATED and 100 ticks |
| Distillation controller capability after NBT read | Constructor cached a satellite's private tank before its controller was restored | Baseline reload test failed; lazy/current-controller resolution and invalidation now route fill/drain to the controller |
| Independent stat percentages | Original 50% factor became a 150% factor | Failing-to-passing unit tests, with legacy multiplier decoding and the existing damage rebalance adapted together |
| Content self-check expectation | Smoke check encoded the incorrect percentage interpretation | All bundled content parses; corrected authored spread/damage assertions pass |
| Existing JEI category namespace | Create's string builder overload used `create:` IDs | Builder now receives explicit `createloveandwar:` resource locations; compile verified; JEI visual run remains pending |

Latest verification: `gradlew build runGameTestServer` succeeded with **20 GameTests and 4 JUnit tests**, including saved-countdown migration. GameTests run through normal NeoForge loading in disposable worlds, not a mocked Minecraft bootstrap. Release JAR inspected to exclude GameTest classes, templates and AutoMine. Development client selected and took a Chassepot, with five part IDs and zero inputs confirmed by the server; pagination and final slot layout inspected. This is not a full Magitema compatibility test.

## Adaptation matrix and remaining work

| Subsystem | Assessment | Next verification or repair |
| --- | --- | --- |
| Registry IDs/content catalog | Original ModItems/ModBlocks IDs retained; six benches, 49 gun registrations, 25 bullets, 87 part registrations, 11 attachments | Check actual runtime registry/catalog consistency and orphaned definitions |
| Bench rendering/storage | Core shape, terrain visibility, item preservation and menu-opening race repaired | Dedicated-server client reconnect, persisted inventory and attachment scenarios |
| Gun recipe selection | Implemented/tested: explicit choices, original ordered inputs and immediate take, all 45 allowed recipes reachable | Dedicated multiplayer/reload scenarios; original has one additional orphaned optical-scope recipe |
| Crafted materials/traits | Provenance implemented/tested; **P1: runtime trait abilities still missing** | Resolve capped trait levels and stat/trigger effects from parts/attachments; compare material variants after reconnect |
| Burst firing | **P1: incorrect**; held input restarts bursts and release cancels remaining shots | Restore finite server-scheduled bursts and press/release tests |
| Projectile engine | **P1: partial replacement**; acceleration, drag, gravity, guidance/fuses and bullet-specific models ignored or absent | Reconstruct authored projectile behavior; controlled original/target scenarios |
| Ammunition pouches | **P1/P2: registered but inert**; 278-line bundle-like item became a capacity/tag holder | Restore insert/extract/drop/persistence/tooltips and accepted-ammo/capacity rules |
| Magazine conversion | Missing selection and persistence; first matching magazine always chosen | Restore upgrade workflow and chosen magazine component |
| Action timing | Delays increase busy duration but actions run immediately | Schedule sounds, casings, shooting and particles at authored offsets |
| Aiming/scopes | Base ADS overrides attached scope, authored FOV has extra base 1, scope overlay ignores attachments | Resolve effective action group consistently; test optical sights and eye lines |
| Gun/attachment visuals | Paint component is not consumed; installed meshes not rendered; recoil lacks original camera feedback | Render skin variants and mounted attachments; compare original transforms/backends |
| Impacts/damage | Hitscan range/penetration, damage identity, headshot handling, hit feedback and decals differ | Separate intentional balance settings from lost engine behavior; controlled damage/penetration tests |
| Action restrictions | Underwater/two-hand/reload flags decoded but not consistently enforced | Server validation and client refusal/message scenarios |
| Reload/content infrastructure | Custom definitions are static classpath caches rather than server-reloadable/synchronized definitions | Implement resource reload listeners, invalidation and server/client definition synchronization |
| Machine recipes | Heat/time repaired; custom assembly serializers lost old array aliases | Codec compatibility tests and positive/negative machine scenarios |
| Stamping | Empty-template belt guard omitted; wrong nonempty-template fallback is inherited | Restore missing guard first; label stricter matching as an inherited bug fix |
| Machine visuals | Instanced press heads use generic Create model, fallback uses custom model; partly inherited | Correct partials and compare Flywheel/fallback render paths |
| Distillation | Controller reload routing repaired; inherited output acceptance/loss risks remain | Full multiblock reload, mismatched output fluids, real pipes and process tests |
| Built-in packs/config | Four original pack switches retained but discovery hooks absent | Restore no-oil/simple-brass/Vender pack discovery and schema validation |
| JEI/Ponder | Machine JEI categories present; namespace repaired; gun recipes/catalysts missing; Ponder scenes present | Restore gun category; inspect actual JEI/Ponder screens |
| Robotdog | Most source behavior retained; inherited healing, owner-validation and scanner pickup issues remain | Explicit regression scenarios before calling these safe |
| Fluids/worldgen/loot | IDs and inspected numeric settings retained; data paths migrated | New-world generation, reload, loot and fluid-overlay validation; separate inherited loot quirks |
| Datagen | Source adapted, not run; cooking helpers lost explicit namespace and may regenerate different recipe IDs | Add isolated `runData`, preserve names, compare generated catalog before using output |
| Tool/armor platform semantics | Defense repaired; harvest tags and leggings durability not fully verified | Compare original custom tier/durability behavior against 1.21.1 |
| Networking | Server derives held gun from player; no arbitrary client damage payload found in scoped review | Standalone server/client, invalid/spammed requests, offhand and lag tests |

The original 107 Java source units were inventoried across subsystem reviews, with mapped original sources retained privately. The target gun replacement and selected Flans engine paths were inspected separately. This is not a line-by-line certification of the entire Flans engine, every model/animation, or every runtime interaction.

All custom data filenames in the inspected gun families survive: 49 guns, 25 bullets, 11 attachments, 40 magazines, 14 traits, 78 parts, 13 materials. Successful decoding does not mean those fields are executed. Resource comparison, after target directory-name migration, found 1,883 byte-identical entries, 465 JSON-equivalent entries, 498 changed entries and 32 unmatched paths. Unmatched paths include build caches, metadata, renamed Forge/NeoForge/common-tag locations and an orphaned loot table; they are **not** 32 confirmed missing gameplay features.

## Gun crafting reference decisions

Reference paths: Flans `GunFabricationRecipe`, `GunCraftingOutputSlot`, `WorkbenchMenuGunCrafting`, `AbstractWorkbench` and `FlanItem` from the pinned dependency. Three baseline GameTests failed for missing selection, shapeless matching and bare results before repair.

- Original gun crafting is immediate. `craft_time` is serialized (default 20) but does not drive a gun countdown; the original countdown path belongs to part crafting. Existing saved NeoForge countdowns are finished once into real output storage; new crafts use a computed preview.
- **Quirk kept:** input order matters, extra unused slots are ignored for consumption, but every recognized part stack in the input inventory contributes one canonical part ID. Stack count and arbitrary input NBT are not copied into provenance. The output slot yields one gun per take even if a custom recipe declares a larger result count.
- **Quirk kept:** the official `gun_fabrication/stg57_scope` recipe produces `optical_scope`, which no authored bench allows. The remaining 45 recipes are allowed by at least one of the six benches. The recipe and filters are unchanged; the regression test identifies this exact orphan rather than treating it as an inaccessible gun.
- The six authored benches enable modification but do not enable painting. Removed the replacement's free paint-cycle button/request. Magazine upgrade parity remains separate work.
- UI adaptation: stable recipe-ID order with paged choices; click an empty ingredient slot to auto-fill, or Shift + right-click to refill/cycle. Survival transfer validates capacity before returning an existing stack, preventing the original partial-insertion loss risk. Creative cycles matching material variants.
- Previews are transient menu data and never saved/dropped as inventory. Normal pickup, hotbar swap, shift-click/full-inventory refusal, stale/distant requests, breaking a previewing bench, item/recipe serialization and unused-part contribution have regression coverage. The catalog is built once per open menu; assembled previews are cached until inputs or recipe identity change. These are structural reductions in repeated work, not a measured end-to-end performance result.

## Verification boundaries and release criteria

- Current evidence is strongest for the small repaired paths with failing baseline tests.
- Machine operation, gun parity, natural worldgen, multiplayer and full-pack interactions remain partially or wholly unverified.
- No new JAR was installed into Prism or the remote server. No saves or existing pack snapshots were replaced.
- Keep the experimental artifact out of a public release until the P1 queue and representative original/target scenarios are resolved. Publication remains a later, separately requested action.
