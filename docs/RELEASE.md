# Requiem release handoff

- The suggested public listing title is **Requiem Cinematics**. [CurseForge](https://www.curseforge.com/minecraft/mc-mods/requiem) and [Modrinth](https://modrinth.com/mod/requiem) already have a Requiem project from Ladysnake. Confirm the suggested slug in each dashboard before adding links to the README.
- The staged installer artifacts use stable release versions. Confirm the version before uploading. If the version changes, rebuild and smoke-test the exact files that will be uploaded.
- Create separate Fabric and NeoForge version entries for Minecraft 1.21.1. Mark Fzzy Config required for both, Fabric API and Fabric Language Kotlin required for Fabric, and Kotlin for Forge required for NeoForge. Treat Incapacitated as optional.
- Use the original studio-owned return track for redistribution. Add the track's attribution when submitting on CurseForge.
- **Modrinth review** · The [content rules](https://modrinth.com/legal/rules/#6-usage-of-generative-ai) require the platform disclosure when description or publishing relies on generative AI, prohibit generated or derived page images, and bar public publication when the project contents are primarily or entirely generated. Review project eligibility before submission. Use studio-created branding for Modrinth and keep the supplied pixel artwork on GitHub and CurseForge.
- **CurseForge review** · Follow the [submission guide](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project). Upload title artwork as branding. Use genuine gameplay for the gallery.

## Build and publish

Run `pwsh -File scripts/build-release.ps1 -Offline` to build, test, and stage both local JARs with checksums under `build/release/<version>/`. Omit `-Offline` when dependencies need to download. Use `-Version 0.1.1` to stage a specific version without changing `gradle.properties`.

The GitHub **build** workflow runs on main and pull requests. The manual **publish** workflow reads the stable version from gradle.properties on the selected ref and requires a matching dated entry in CHANGELOG.md. Use **stage** to build and smoke-test both JARs. Select **all** to publish both loaders to Modrinth and CurseForge and create one GitHub release. Use the same commit for staging and publishing.

CurseForge uses project `1834464` and the `CURSEFORGE_TOKEN` Actions secret. Modrinth uses project `O8SDSjiJ` and the `MODRINTH_TOKEN` Actions secret. Confirm the Modrinth eligibility and disclosure review above before selecting it. The GitHub target uses the workflow token to create a release with both loaders. Each external loader target runs separately, so a retry can be limited to the failed upload.

Both platforms display versions as X.Y.Z+1.21.1-fabric or X.Y.Z+1.21.1-neoforge. The staged JAR filenames use that same format. Both workflows extract the matching dated entry from CHANGELOG.md into RELEASE-NOTES.md. Keep future entries under Unreleased until you cut a release. Each publishing target uses the staged notes for its selected version.
