# Quilly Crawler

[![Build Master](https://img.shields.io/github/workflow/status/quillraven/quilly-crawler/Build/master?event=push&label=Build%20master)](https://github.com/Quillraven/Quilly-Crawler/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.5.0-red.svg)](http://kotlinlang.org/)
[![LibGDX](https://img.shields.io/badge/LibGDX-1.10.0-green.svg)](https://github.com/libgdx/libgdx)
[![LibKTX](https://img.shields.io/badge/LibKTX-1.10.0--b1-blue.svg)](https://github.com/libktx/ktx)

**Quilly Crawler** will become a small dungeon crawler game with a round based fighting system
using [Kotlin](https://kotlinlang.org/) and [LibGDX](https://github.com/libgdx/libgdx).
It uses other open source libraries like [LibKTX](https://github.com/libktx/ktx),
[Ashley](https://github.com/libgdx/ashley), [Gdx-AI](https://github.com/libgdx/gdx-ai),
[Box2D](https://box2d.org/) or [Gdx-Controllers](https://github.com/libgdx/gdx-controllers).

Until it is finished, I will stream every weekend on [twitch](https://www.twitch.tv/quillraven).
Also, a VLog will be uploaded every sunday on my [YouTube](https://www.youtube.com/Quillraven) channel.
If you are interested, please feel free to subscribe :)

### Controls

Quilly Crawler supports keyboard and XBox Controller input.

**Keyboard**:
- Arrow keys for navigation / movement
- Space for any selection
- Escape for cancel a selection / menu
- I to open the inventory

**XBox**
- Left joystick for movement
- Analog pad for navigation
- A for any selection
- B for cancel a selection / menu
- Y to open the inventory

### About the game

![image](https://user-images.githubusercontent.com/93260/130333721-7305bdb3-dcec-4513-ae94-4cbb58c0348b.png)

Quilly Crawler is a dungeon crawler game where you play an old man and go deeper and deeper into an endless cave.
Every level contains enemies that you can fight, chests that you can loot, a shop to buy and sell items and a reaper
who can set you back in the dungeon if it gets too hard.

![image](https://user-images.githubusercontent.com/93260/130333775-e3c2c387-004d-404e-b06d-fcb4b0fa67bc.png)

The combat is a round based system where every entity is executing a single order per turn. The order is defined
by the agility of an entity. As a player you can either attack, use an ability or use an item. There are offensive
abilities like a Firebolt but also defensive abilities like a Heal or Protection ability. In addition some abilities
will add a buff like the Protes ability which reduces physical damage by 50% for three turns.

![image](https://user-images.githubusercontent.com/93260/130333803-87f3139b-d600-4664-95a0-625b42be88d2.png)

I have only added a few levels, enemies and items to the game. Feel free to fork this project and add your own content.
If you have any questions, don't hesitate to ask and use the [Discussions](https://github.com/Quillraven/Quilly-Crawler/discussions/14)
for it.

### Credits
- Twitch chat for supporting with some questions during development
- czyzby for his quick support and adjustments in [LibKTX](https://github.com/libktx/ktx)
- 0x72: [16x16 Dungeon Tileset II](https://0x72.itch.io/dungeontileset-ii)
- greatdocbrown: [Gamepad Icons](https://greatdocbrown.itch.io/gamepad-ui)
- Apostrophic Labs: [Immortal Font](https://www.1001freefonts.com/immortal.font)
- Mounir Tohami: [Pixel Art GUI](https://mounirtohami.itch.io/pixel-art-gui-elements?download)
- Alex's Assets: [16x16 RPG Icon Pack](https://alexs-assets.itch.io/16x16-rpg-item-pack)
- Tim Beek: [Royalty Free Music Pack](https://timbeek.itch.io/royalty-free-music-pack)
- OmegaPixelArt: [Gameboy Sfx Pack #1](https://omegaosg.itch.io/gameboy-sfx-pack)
- SubspaceAudio: [512 Sound Effects (8-Bit Style)](https://opengameart.org/content/512-sound-effects-8-bit-style)
- Little Robot Sound Factory: [Fantasy Sound Effects Library](https://opengameart.org/content/fantasy-sound-effects-library)
- Soluna Software: [Effects](https://opengameart.org/content/explosion-effects-and-more)
- ppeldo: [2D Pixel Art Game Spellmagic FX](https://ppeldo.itch.io/2d-pixel-art-game-spellmagic-fx)
