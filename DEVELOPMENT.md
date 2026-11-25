# Requiem development plan

Repository is the user-selected BareMinimumStudios/requiem checkout. Display name is Requiem. Internal mod and asset namespace is requiem_cinematics to avoid Ladysnake's existing requiem ID.

1. Establish Cloche Kotlin targets from DifficultyEx conventions. Compile both loaders using Java 21 and the checked-in Gradle wrapper.
2. Implement shared mortality rules, UUID-over-name overrides, cause filters and transaction snapshots. Test all modes, cause filters, duplicate decisions and last-life behavior.
3. Record true deaths in overworld SavedData. Build loader-native packets authenticated by the connection. Verify reconnect, server restart, stale and replayed decisions, vanilla respawn bypass, and failed delivery recovery.
4. Apply spectator, limbo and ban on final confirmation. Test permissions, operator release, login enforcement, integrated server behavior and preservation of vanilla drops.
5. Build The Between, a confirmation view, deferred exit, optional first-join scene and hyperspace-to-first-person handoff. Keep text, scale, colors, themes and sounds configurable. Check reduced motion, muted audio, keyboard, narration, shaders and resize.
6. Smoke-test dedicated server and client on Fabric and NeoForge. Check Incapacitated revive and bleed-out, plus grave, shader and respawn mods independently. Claim only tested compatibility.

Build with gradlew.bat build. Launch the Cloche client and server runs separately on each loader. Run test against the shared lifecycle rules before multiplayer and restart cases.

## Development status

Requiem ships as a mod on both the client and the server. Install the matching
Fzzy Config and Kotlin runtime on each side, plus Fabric API on Fabric.
Use UUIDs or case-insensitive names as override keys and values such as
LIMITED:3, HARDCORE, UNLIMITED or DISABLED. A UUID override takes priority.
Changing the configured starting allocation leaves existing records intact.
On integrated servers a ban policy places the player in spectator instead.
Use /requiem release <online player> <lives> to release a player and set their
limited-mode allocation. Dedicated-server bans also require a vanilla pardon.

Incapacitated 2.0.1 is the first integration target. The shared tail injection
observes completed player deaths. Validate that a down-and-revive leaves the
record untouched and that bleed-out, give-up, and overkill each count once.
Run the same inventory and XP checks with a grave mod. Run the screen with
and without a shader mod. The current presentation uses screen-space stars
and configurable sounds, with plain and reduced-motion modes.

## Verified locally

- Java 21 offline build for both loader JARs and the shared mortality tests.
- Packaged metadata and remapped Fabric mixins.
- Dedicated NeoForge development server reached the ready state. Fabric loaded
  the mod and config and stopped at the Minecraft EULA prompt.
- Client rendering, full death/reconnect/respawn flows and third-party mod
  combinations still require in-game checks. The initial warp is a
  screen-space effect, and the handoff has not been visually verified.
