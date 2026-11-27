# Requiem Cinematics — Modrinth draft

**Summary** · A starlit death screen with fading memories, cinematic resurrection, and configurable lives.

**Project setup** · Mod · Game Mechanics, Adventure · Client required, server required · Custom license (The Bare Minimum License v1.0)

Use `requiem-cinematics` if available. Use studio-created artwork for the icon and banner and studio-captured gameplay for the gallery.

---

![Blue stars shimmer across The Between in Minecraft](https://cdn.modrinth.com/data/cached_images/4d92cb3717ef995587f3cd60cf7f9eb1329eee08.png)

**Minecraft 1.21.1 · Fabric + NeoForge · Java 21**

## The Between

A few stars come into focus. A question appears. *Where… am I?*

Requiem replaces the usual death screen with a quiet space called The Between. Click through a short dialogue, then choose whether to return to the world or decide later. When you return, faint glimpses of your last moments play newest first and recede as the stars stretch into lightspeed. The camera settles into first person as the soundtrack gives way to the sounds around you.

The glimpses come from your own play session. They’re low-resolution on purpose, held in memory, and never written to disk by Requiem.

## Decide what death costs

- **Unlimited** is the default. Return as often as you like, with the life counter hidden.
- **Limited** gives players a configurable number of lives.
- **Hardcore** makes the first qualifying death final.
- **Disabled** leaves the player on Minecraft's usual death flow.

Servers can decide which PvP, mob, and environmental deaths count, credit a recent attacker, and set exceptions by UUID or player name. Pending decisions survive disconnects and server restarts. Servers can also allow players to choose final death voluntarily. Final death asks for confirmation and can lead to Spectator, a dreamlike Limbo with an operator-controlled wake-up, or a dedicated-server ban.

## Make it fit your world

Change the dialogue, colors, particle density, timing, and sounds. Enable a first-join cinematic, turn off memories, or choose reduced motion, standard buttons, or muted cinematics. The death scene leaves the death reason out.

## Installation

Use the matching Requiem build on the client and server.

| Loader | Required dependencies |
| --- | --- |
| Fabric | Fabric API, Fabric Language Kotlin, Fzzy Config |
| NeoForge | Kotlin for Forge, Fzzy Config |

Incapacitated 2.0.1 is optional. Requiem waits for an actual death before opening The Between. A successful revive leaves the player's lives alone. Give-up and overkill were checked on dedicated Fabric and NeoForge servers.

[Source and setup](https://github.com/BareMinimumStudios/requiem) · [Issues](https://github.com/BareMinimumStudios/requiem/issues) · [Community Discord](https://discord.gg/pcRw79hwey) · [Ko-fi](https://ko-fi.com/pokesmells)
