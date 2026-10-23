# Create: Love and War — maintained NeoForge fork

Minecraft 1.21.1, NeoForge 21.1.253, Java 21 and Create 6.0.10.

This project reconstructs the unavailable source of the installed 1.0.9 NeoForge port and maintains targeted bug fixes. Original package names and registry IDs are retained; behavior is compared against the official release while identified regressions are repaired.

The recovered port has substantial fidelity gaps in its replacement gun engine. See [the audit and migration status](docs/MIGRATION_STATUS.md) for verified repairs, remaining defects and evidence limits. This checkout is an experimental maintenance base.

## Build

Install Java 21, then run:

```powershell
.\gradlew.bat build
```

The mod JAR and source JAR are written to `build/libs/`. Dependencies are resolved from the NeoForge, Create, Registrate and JEI Maven repositories; no local mod JARs are required to compile.

Development tasks: `runClient`, `runServer` and `runGameTestServer`. Development worlds and logs are stored outside the repository in the workspace's `private/` directory.

Run `./gradlew build runGameTestServer` to execute the unit and NeoForge regression tests. Test sources are separate from the distributed mod JAR.

## Credits and provenance

Original Create: Love and War by Proto and Alcoholism, integrating industry with firearm production. This fork starts from the unofficial Minecraft 1.21.1 NeoForge port, version 1.0.9. Its source was recovered with Vineflower because that port did not provide a source checkout. Reconstructed Java may differ in formatting and local variable names from the original source.

The unchanged input binary and raw recovery are retained separately from this repository. Source input SHA-256: `3cb2032a80019661c388c5204a6ec1c5899d8e743b168f47560fb465d1c6d4bb`.
