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

The workload warms up with 10,000 projectiles, then records five batches of 10,000. Each batch reports elapsed time, server-thread CPU time and JVM thread allocation bytes. JFR uses the JDK's profile configuration. Metadata packet sizes are measured through the actual `ClientboundSetEntityDataPacket` stream codec before the timed batches. A deterministic launch/fuse checksum helps compare runs; regression tests remain the behavioral oracle.

Compare fresh JVM runs using the same JDK, dependency versions and settings. Report all samples and the median rather than treating JIT/GC noise as a stable FPS or TPS result. JFR allocation sampling is statistical; the thread allocation counters are direct measurements. The component benchmark cannot certify end-to-end gameplay performance, disk-save cost, a dedicated observer's bandwidth or client frame rate.
