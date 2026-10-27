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

JAR entries use stable ordering and fixed timestamps. A clean checkout and a fresh Git source export built in another directory produced byte-identical mod and source JARs with the build cache disabled. This uses the repository's LF line-ending policy and the pinned local environment; portability across other JDKs or dependency changes has not been certified.

Development tasks: `runClient`, `runServer` and `runGameTestServer`. Development worlds and logs are stored outside the repository in the workspace's `private/` directory.

Run `./gradlew build runGameTestServer` to execute the unit and NeoForge regression tests. Test sources are separate from the distributed mod JAR.

Gunsmithing uses a selected recipe and ordered ingredient slots, as in the official release. Choose a gun on the right, add matching parts, and take the immediate result. Click an empty ingredient slot to fill it from your inventory; Shift + right-click refills it or cycles material variants in creative mode. Finished guns retain their crafting parts and display their combined material traits; hold Shift for crafting details. Some trait triggers and other gun behavior are still being restored; see the migration status before relying on this build for gameplay.

## Credits and provenance

Original Create: Love and War by Proto and Alcoholism, integrating industry with firearm production. This fork starts from the unofficial Minecraft 1.21.1 NeoForge port, version 1.0.9. Its source was recovered with Vineflower because that port did not provide a source checkout. Reconstructed Java may differ in formatting and local variable names from the original source.

The official release's gun engine is Flan's Mod Reloaded by jamioflan. Its pinned 0.4.316 binary (MIT in its own metadata) is the reference for the reconstructed firing, traits, sound and projectile behavior. See the reference manifest and migration status for exact inputs and adaptation decisions.

The unchanged input binary and raw recovery are retained separately from this repository. Source input SHA-256: `3cb2032a80019661c388c5204a6ec1c5899d8e743b168f47560fb465d1c6d4bb`.

<!-- ashley-release-information -->

## My builds

This is an unofficial maintenance fork. Original authors retain ownership and credit. See the [upstream project](https://www.curseforge.com/minecraft/mc-mods/create-love-war) for the original mod. Local changes do not imply upstream endorsement.

Licensing: **MIT**. Existing license files, copyright notices, attribution and any separate asset terms remain in force. This fork does not relicense upstream work.

Provided **AS IS**, without warranty of any kind. Maintenance, updates and support are best effort; compatibility with every modpack is not guaranteed.

### Downloads and installation

Download a runtime `.jar` from [GitHub Releases](https://github.com/ashleywr/create-love-and-war/releases). Use the Minecraft version and loader named in that release and install its required dependencies. Do not install `-sources`, `-dev`, `-slim`, `-common` or test JARs as the mod.

Close Minecraft, back up your world, and place the matching runtime JAR in the instance's `mods/` directory. Replace older versions of this same mod rather than loading duplicates. For content mods, install matching versions on the server and its clients unless the release explicitly says client-only or server-only.

### Building and reporting problems

Use Java 21 and the checked-in Gradle wrapper:

```powershell
.\gradlew.bat build runGameTestServer
```

Build outputs are under `build/libs/` or each loader subproject's `build/libs/`. Some projects require the local development dependencies described above; those dependencies are not included in the download.

For a problem report, include the exact release filename, Minecraft and loader versions, relevant mod versions, `logs/latest.log`, and a crash report if one was written. Remove private information before sharing logs. Remove the JAR to uninstall; worlds containing this mod's blocks, entities or world generation may require restoring a backup.
