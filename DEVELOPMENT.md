# Requiem development plan

Repository is the user-selected BareMinimumStudios/requiem checkout. Display name is Requiem. Internal mod and asset namespace is requiem_cinematics to avoid Ladysnake's existing requiem ID.

1. Establish Cloche Kotlin targets from DifficultyEx conventions. Compile both loaders using Java 21 and the checked-in Gradle wrapper.
2. Implement shared mortality rules, UUID-over-name overrides, cause filters and transaction snapshots. Test all modes, cause filters, duplicate decisions and last-life behavior.
3. Record true deaths in overworld SavedData. Build loader-native packets authenticated by the connection. Verify reconnect, server restart, stale and replayed decisions, vanilla respawn bypass, and failed delivery recovery.
4. Apply spectator, limbo and ban on final confirmation. Test permissions, operator release, login enforcement, integrated server behavior and preservation of vanilla drops.
5. Build The Between, a confirmation view, deferred exit, optional first-join scene and hyperspace-to-first-person handoff. Keep text, scale, colors, themes and sounds configurable. Check reduced motion, muted audio, keyboard, narration, shaders and resize.
6. Smoke-test dedicated server and client on Fabric and NeoForge. Check Incapacitated revive and bleed-out, plus grave, shader and respawn mods independently. Claim only tested compatibility.

Build with `gradlew.bat build` using Java 21. The Cloche tasks
`runFabric1211Client`, `runFabric1211Server`, `runNeoforge1211Client`, and
`runNeoforge1211Server` each have their own directory under `build/run`.

## Development status

Requiem ships as a mod on both the client and the server. Install the matching
Fzzy Config and Kotlin runtime on each side, plus Fabric API on Fabric.
Use UUIDs or case-insensitive names as override keys and values such as
LIMITED:3, HARDCORE, UNLIMITED or DISABLED. A UUID override takes priority.
Changing the configured starting allocation leaves existing records intact.
On integrated servers a ban policy places the player in spectator instead.
Use /requiem release <online player> <lives> to release a player and set their
limited-mode allocation. The dedicated-server ban policy writes to Minecraft's
vanilla ban list.

The client settings control the title, subtitle, choice labels, palette,
particle count, warp duration, reveal duration, camera settling and sounds.
The life count is hidden by default and can be enabled with
`showLivesRemaining`.
The three star layers are generated on the client without frame capture.
The stars twinkle in place before accelerating toward the camera in perspective.
Stars that pass the camera reappear in the distance, and the flight keeps
slowing to a still point before the world reveal. World ambience begins
returning under the still stars, and the camera eases into first person. `twinkleHoldTicks` controls the pause on the
star field after selecting Return to Existence. `warpTicks` controls the flight
and reveal after the destination is ready. `revealTicks` controls the fade into
first person and defaults to 52 ticks (2.6 seconds), within the 580-tick
sequence. For the shorter timing and lower star count from the prior local
settings, use warpTicks = 48, evealTicks = 18 and starCount = 240.
Toggle `colorfulStars` for the
occasional colored flares.
The themed buttons ease in on hover and mark keyboard focus with bright
corners. Final-death confirmation uses the same controls and starts with
Go Back focused. After final death is confirmed, the stars fade into
Spectator view over 42 ticks by default. Adjust this with `spectatorRevealTicks`.
Plain interface uses Minecraft's standard buttons.
Reduced motion uses instant button highlights, holds
particles still and uses a short fade with no camera motion. A world can
enable the first-join sequence independently of its death rules.

When `showLastMoments` is enabled, the return cinematic shows up to twelve
recent first-person world frames before the reveal. Their feathered edges
blend into the star field. The images play newest-first, crossfade faster and recede as the
flight progresses. Each is at most 128 pixels on its longest edge. Frames stay in client memory. Gameplay
interface elements such as the HUD are excluded.

The decision screen opens with two quiet beats and a short line of
dialogue. Click or press Enter or Space to advance each beat. Customize the line with `disorientedText`, and the prompt and
choices with the existing text settings. Disable the delay with
`playDecisionDialogue`. Reduced motion and plain interface show the
choices immediately.

## Client audio

Use `buttonHoverSound`, `buttonClickSound`, and `warpSound` for sound-event IDs.
Each cue has a separate volume from 0 to 1. An empty ID silences that cue.
The bundled return track streams once at its recorded pitch, starting with the flight. Configured
replacement sounds can loop and sweep from `warpStartPitch` toward
`warpPeakPitch`. The warp audio fades into world sounds during the reveal.
`warpTailTicks` controls the trail after the reveal. It defaults to 28 ticks.
Set it to 0 to end the warp with the reveal. `returnSound` is the short
accent at the start of the reveal. Set `muteCinematics` to silence all
Requiem cues.

With `crossfadeWorldAudio` enabled, world sounds dip as the warp starts and
gradually return to their normal levels as first person comes into view.
`warpWorldVolume` sets their quietest level, from 0.01 to 1, and defaults to
0.12. Minecraft's volume sliders still set the base levels. Music and
interface sounds keep their usual volume. Muting or disabling cinematics
also disables the crossfade.

## Incapacitated development runs

Incapacitated 2.0.1 requires NeoForge 21.1.200 or later on Minecraft 1.21.1.
The Requiem development build uses NeoForge 21.1.267. Pass
`-PtestIncapacitated` to add the matching 2.0.1 artifact to either loader's
development run. On Fabric, extract Incapacitated's two bundled
`cardinal-components-*.jar` files from `META-INF/jars` and pass
`-PincapacitatedNestedMods=<directory>` so Cloche can remap them with the mod.
Published Requiem JARs require neither Incapacitated nor Cardinal Components.

## Verified locally

- Built both JARs and ran the shared mortality tests with Java 21.
- In integrated Minecraft 1.21.1 on Fabric and NeoForge with Incapacitated
  2.0.1, downed and revived a player without spending a life. The subsequent
  bleed-out spent one life from an allocation of three.
- Chose Later on both loaders, reopened the world, confirmed the same
  transaction and balance, then resurrected in first person in the original
  Adventure mode. Captured The Between and the warp from the game framebuffer.
- On both dedicated 1.21.1 loaders, two clients connected over loopback with
  Incapacitated 2.0.1. A downed player sent Incapacitated's give-up packet and
  spent one life (four to three). Choose Later disconnected that client.
  Reconnecting to the same server restored the pending transaction, and
  resurrection returned the player to Adventure mode with three lives.
- A 120-damage hit attributed to the other player bypassed incapacitation,
  counted as one PvP death (three to two), and allowed a second resurrection.
  The other client remained connected through both deaths and received no
  Requiem screen or life deduction.
- On both dedicated loaders, a fresh server process opened the same world
  after a deferred death. The player rejoined with the original death UUID
  and two remaining lives, then resurrected in Adventure mode.
- The final-death confirmation was exercised for all three policies on both
  loaders. Spectators could move. Limbo rejected movement and camera changes,
  corrected the client position, and ended when an operator ran
  `/requiem release`. Ban wrote a vanilla ban entry and denied reconnect.
- Each dedicated loader ran the same mortality matrix with two clients.
  Unlimited allowed a return without a finite balance. Limited spent one
  life per qualifying death and reached final death at zero. Hardcore reached
  final death on the first qualifying death. Disabled used vanilla respawn
  without creating a pending decision.
- A mixed-case username override enabled limited lives while the global mode
  was disabled. An uppercase UUID override took precedence over the matching
  username. PvP, zombie, and fall damage each produced the corresponding
  attribution, with one life spent when enabled and zero when excluded.
- Other optional mod combinations remain separate test cases.
- Hidden Fabric and NeoForge clients completed death, defer, reconnect, and
  return with the layered animation. The camera settled during the reveal and
  gameplay resumed in first person.

- The integrated release JARs passed hidden production-client gameplay on
  Fabric and NeoForge 1.21.1. Verified click-through dialogue and button audio,
  newest-first memory playback, synchronized warp audio, return to first person,
  world ambience recovery, and release of the captured frames.
