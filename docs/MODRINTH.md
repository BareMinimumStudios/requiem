# Requiem Cinematics — Modrinth draft

**Summary** · A starlit death screen with fading memories, cinematic resurrection, and configurable lives.

**Project setup** · Mod · Game Mechanics, Adventure · Client required, server required · Custom license (The Bare Minimum License v1.1)

Use `requiem-cinematics` if available. Use studio-created artwork for the icon and banner and studio-captured gameplay for the gallery.

---

![Requiem eye-and-shards banner](https://cdn.modrinth.com/data/cached_images/eac4d7f1762c6da093f5221055bdf419387a0c89.png)

[![Modrinth](https://img.shields.io/badge/Modrinth-133329?logo=modrinth&logoColor=8CFFCF&style=for-the-badge)](https://modrinth.com/mod/requiem-cinematics) [![CurseForge](https://img.shields.io/badge/CurseForge-39232B?logo=curseforge&logoColor=FFA880&style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/requiem-cinematics) [![GitHub source](https://img.shields.io/badge/Source-242B49?logo=github&logoColor=EAF0FF&style=for-the-badge)](https://github.com/BareMinimumStudios/requiem) [![Report an issue](https://img.shields.io/badge/Issues-242B49?style=for-the-badge)](https://github.com/BareMinimumStudios/requiem/issues) [![Forks](https://img.shields.io/badge/Forks-242B49?style=for-the-badge)](https://github.com/BareMinimumStudios/requiem/forks) [![Stars](https://img.shields.io/badge/Stars-242B49?style=for-the-badge)](https://github.com/BareMinimumStudios/requiem/stargazers)

**Minecraft 1.21.1 · Fabric + NeoForge · Java 21**

*Where… am I?*

Requiem replaces Minecraft’s death screen with **The Between**, a quiet place to decide whether to return. Choose to wake up and your last moments flicker past in reverse. The stars stretch into lightspeed, the camera eases back into first person, and the sounds of the world return.

![Resurrection in Minecraft, from streaming stars to first person in the rain](https://raw.githubusercontent.com/BareMinimumStudios/requiem/main/docs/assets/gameplay/requiem-resurrection.gif)

## How it works

- **Return or decide later.** Lives are unlimited by default, and the counter is hidden. A pending choice survives reconnects and server restarts.
- **Set the stakes.** Choose Unlimited, Limited, Hardcore, or Disabled. Configure which PvP, mob, and environmental deaths count, with exceptions by player UUID or name.
- **Choose a final outcome.** Confirm final death to enter Spectator, wait in Limbo for an operator to release you, or trigger a dedicated-server ban, depending on the server’s settings.
- **Make The Between your own.** Adjust the dialogue, starfield, timing, and audio. Fading memories are optional and held in memory. Reduced motion, standard buttons, and muted cinematics are available.

## In game

**The Between**

![The Between’s blue starfield and resurrection choices](https://raw.githubusercontent.com/BareMinimumStudios/requiem/main/docs/assets/gameplay/requiem-the-between.jpg)

**Lightspeed**

![Starlight streaks past a fading memory during resurrection](https://raw.githubusercontent.com/BareMinimumStudios/requiem/main/docs/assets/gameplay/requiem-lightspeed.jpg)

**Back in the world**

![Minecraft first-person view in the rain after resurrection](https://raw.githubusercontent.com/BareMinimumStudios/requiem/main/docs/assets/gameplay/requiem-first-person.jpg)

## Installation

Install the matching Requiem build on each client and on the server for multiplayer.

| Loader | Required dependencies |
| --- | --- |
| Fabric | Fabric API, Fabric Language Kotlin, Fzzy Config |
| NeoForge | Kotlin for Forge, Fzzy Config |

**Incapacitated 2.0.1** is optional. A successful revive does not spend a life. Give-up and overkill were tested on dedicated servers with both loaders.

## Links and support

[Configuration and documentation](https://github.com/BareMinimumStudios/requiem/blob/main/README.md) · [Report an issue](https://github.com/BareMinimumStudios/requiem/issues/new) · [Discord](https://discord.gg/pcRw79hwey) · [Ko-fi](https://ko-fi.com/pokesmells)
