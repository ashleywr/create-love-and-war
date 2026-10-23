# Projectile setup profile

The opt-in `ProjectileSetupProfile` fixture runs inside the normal NeoForge GameTest server. It constructs and launches a balanced sequence of all ten bundled projectile definitions with a G11 firing context. It does not insert entities, tick a battle, render frames or measure observer/network throughput.

Use a dedicated output directory for each run. From the repository, create an init script in the outer workspace, for example `../private/projectile-profile.gradle`:

```groovy
allprojects {
    afterEvaluate {
        neoForge.runs.gameTestServer {
            systemProperty 'law.projectileProfileDirectory', file('../private/profile-projectiles').absolutePath
        }
    }
}
```

Run `./gradlew --init-script ../private/projectile-profile.gradle runGameTestServer`. Inspect the XML result as well as `setup.json` and `setup.jfr` in the selected directory. The fixture is absent from normal test counts unless the property is supplied, and excluded from release JARs.

The workload warms up with 10,000 projectiles, then records five batches of 10,000. Each batch reports elapsed time, server-thread CPU time and JVM thread allocation bytes. JFR uses the JDK's profile configuration. Metadata packet sizes are measured through the actual `ClientboundSetEntityDataPacket` stream codec before the timed batches. These are uncompressed metadata bytes; outer framing, spawn/movement packets and observer throughput are excluded. A deterministic launch/fuse checksum helps compare runs; regression tests remain the behavioral oracle.

Compare fresh JVM runs using the same JDK, dependency versions and settings. Report all samples and the median rather than treating JIT/GC noise as a stable FPS or TPS result. JFR allocation sampling is statistical; the thread allocation counters are direct measurements. The component benchmark cannot certify end-to-end gameplay performance, disk-save cost, a dedicated observer's bandwidth or client frame rate.

## Recorded context optimization

Baseline production and fixture: `e446c7b`. The optimization retains the resolved objects on the server, sends only client physics/response/visual fields, and builds the complete cached save context when first needed. It eliminates encoding then decoding the full context during every launch. The saved schema retains authoritative impacts, modifiers and calculation inputs; emitted save tags are independent copies.

Same-environment runs, with all ten definitions and the same launch/fuse checksum:

| Component measurement | Before | After |
| --- | ---: | ---: |
| Median server-thread allocation, bytes/projectile | 61,662 | 11,219 |
| Median server-thread CPU, microseconds/projectile | 12.5 | 4.7 |
| Encoded metadata packet range, bytes | 997–1,550 | 191–593 |

Allocation fell about 82% in this setup workload. CPU samples varied with JIT/GC and timer resolution: all five batch values were 43.75/14.0625/12.5/12.5/10.9375 microseconds per projectile before and 18.75/4.6875/3.125/4.6875/4.6875 after. Full JSON samples, JFR recordings, hashes and the comparison are retained in the wrapper's private profiling directory. JFR sampled the old server-side decoding path; that path is bypassed during optimized initialization.

This shifts full save encoding to the first save and caches it thereafter. First-save cost, retained heap, actual observer load, trail/frame cost and battle TPS/FPS need separate measurements. The packet codec regression verifies observer motion/responses/particles/identity and exclusion of server-only fields; saved-impact/fuse/count tests verify that the authoritative data remains intact. A real client also displayed the four showcase meshes and particle emissions using the compact payload.
