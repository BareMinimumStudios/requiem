# Requiem Cinematics — Modrinth draft

**Summary** · A starlit death screen with fading memories, cinematic resurrection, and configurable lives.

**Project setup** · Mod · Game Mechanics, Adventure · Client required, server required · Custom license (The Bare Minimum License v1.1)

Use `requiem-cinematics` if available. Use studio-created artwork for the icon and banner and studio-captured gameplay for the gallery.

---

# Requiem

**Minecraft 1.21.1 · Fabric + NeoForge · Java 21**

A few stars come into focus. *Where… am I?*

Requiem replaces Minecraft’s death screen with **The Between**, a quiet place to decide whether to return. Your last moments flicker past in reverse as the stars stretch into lightspeed. The camera eases into first person, and the sounds of the world gradually return.

## How it works

- **Return or decide later.** Unlimited lives are the default, and the life counter is hidden unless you enable it. Pending decisions survive reconnects and server restarts.
- **Set the stakes for your world.** Choose Unlimited, Limited, Hardcore, or Disabled. Set exceptions by player UUID or name and pick which PvP, mob, and environmental deaths count.
- **Choose what final death means.** After confirmation, players can enter Spectator, wait in Limbo for an operator to release them, or be banned from a dedicated server.
- **Make the scene your own.** Adjust dialogue, colors, particles, timing, and sound. Enable a first-join cinematic or use reduced motion, standard buttons, or muted cinematics. The optional fading memories stay in client memory and are never written to disk by Requiem.

## Installation

Install the matching Requiem file and dependencies on the server and each client. Singleplayer uses the same client installation.

- **Fabric** · Fabric API, Fabric Language Kotlin, and Fzzy Config.
- **NeoForge** · Kotlin for Forge and Fzzy Config.

**Incapacitated 2.0.1** is optional. A successful revive does not spend a life. Give-up and overkill were tested on dedicated servers with both loaders.

## Configuration

Settings are in `config/requiem_cinematics/settings.toml`. The server controls life rules and final outcomes. Cinematic preferences are client-side. Operators can release a player from Limbo with `/requiem release <online player> <lives>`.

## Links and support

- [Documentation](https://github.com/BareMinimumStudios/requiem#readme) · [Source code](https://github.com/BareMinimumStudios/requiem) · [Report an issue](https://github.com/BareMinimumStudios/requiem/issues)
- [Bare Minimum Studios](https://github.com/BareMinimumStudios) · [Community Discord](https://discord.gg/pcRw79hwey) · [Ko-fi](https://ko-fi.com/pokesmells)
- [The Bare Minimum License v1.1](https://github.com/BareMinimumStudios/requiem/blob/HEAD/LICENSE)

When reporting a bug, include your Requiem version, loader, and the relevant client or server log.
