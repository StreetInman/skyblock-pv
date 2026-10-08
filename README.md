# SkyBlock Profile Viewer

A client-side Fabric mod for Hypixel SkyBlock. Type `/pv <player>` to open a window showing
that player's stats and gear, without leaving the game. Builds are available for Minecraft
**26.1.2, 26.2 and 26.3**.

> **Status: early development (v0.1.0).** It works with your own Hypixel developer key. A
> public release with no key setup needed is planned (see [Roadmap](#roadmap)).

## Features

| Tab | Shows |
|---|---|
| Stats | SkyBlock level, **networth** (hover for a breakdown), magical power, selected power, tuning, active pet, purse, bank, fairy souls, skill average, Catacombs, class average, trophy fish rank, all slayer levels |
| Skills | Every skill with level and progress to the next level |
| Dungeons | Catacombs and class levels, secrets, runs, best M7 S+, and an **M7 calculator** (below) |
| Inv | Armor, equipment (necklace, cloak, belt, gloves), inventory and hotbar |
| Storage | Every ender chest page and backpack, one per page. Flip through with **<** and **>** |
| Accs | Accessory bag |
| Trophy | All 18 trophy fish with bronze/silver/gold/diamond counts and your Trophy Hunter rank |

Search for another player from the box at the top of the window. Items show with their real icons, including custom player-head textures. Hover any item to see its full name and lore. If a player has more than one profile, use
**Profile ▸** to switch between them.

### M7 calculator

The Dungeons tab shows how many S+ Master Mode 7 runs the player needs to reach:

- **Catacombs 50**, hidden once reached.
- **Class average 50**: the total runs, plus how many to play as each class. The plan always
  plays whichever class is furthest from 50. A class marked *passive only* will reach 50 just from the
  team XP it gets while you play the others.

It uses the same model as [adjectils.com](https://adjectils.com/dungeon.html): every run is
S+, and the classes you don't play get a share (25% by default) of their class XP. Boosts are read
from the profile automatically:

- essence-shop class perks (Toxophilite, Unbridled Rage, Heart of Gold, Cold Efficiency, Diamond in the Rough)
- the highest Hecatomb level on any visible item
- Scarf's Studies, Thesis or Grimoire, and the Catacombs Expert Ring in the accessory bag
- the **Catacombs Graduate** (Scarf shard, +2% class XP per level) and **Catacombs Explorer**
  (Bonzo shard, +1% Catacombs XP per level) attributes from the profile's hunting shards
- Derpy as the current mayor

Hover the calculator to see exactly which boosts were found. Boosts the API can't see go in
`config/skyblock-pv.json`: `dungeonGlobalBoostPercent`, `dungeonExtraClassBoostPercent`, and
`dungeonTeamShare`. If the shard levels it shows are wrong, set `dungeonGraduateLevel` and
`dungeonExplorerLevel` (0–10; `-1` means read from the profile).

### Settings (`/skyblockpv`)

`/skyblockpv` opens the mod's settings. Everything there is cosmetic and only changes what you see:

- **Player size**: shrink or enlarge your own player model (optionally everyone's). Hitboxes and
  movement are unchanged; only the drawn model is scaled.
- **First-person items**: item size, X/Y/Z position, X/Y/Z rotation, swing animation speed, and
  skipping the dip when you switch items. On 26.1.2 and 26.2 swing speed can be slower or faster;
  on 26.3, where swing timing is tied into combat, only the drawn animation is sped up, so it can only
  be made faster.

### Networth

Networth is an estimate, in the same style as networth mods and sites. Each item counts at its
lowest BIN or bazaar price, plus enchantments, recombobulator, potato books, gemstones, master
stars, scrolls and similar upgrades. Pets, sacks, essence, purse and bank are included. Dungeon
stars bought with essence aren't counted. Bazaar prices come from the Hypixel API and lowest
BIN prices from [Coflnet](https://sky.coflnet.com) (`lowestBinUrl` in the config), refreshed every
10 minutes.

## Is it allowed on Hypixel?

Hypixel doesn't approve or whitelist mods. Its
[Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)
policy bans mods that give an advantage, automate actions, or change how the client
talks to the server. This mod does none of those:

- It reads the **official public Hypixel API** over HTTPS, the same data sites like SkyCrypt use.
- It only draws its own screen and changes how things look on your screen. It never sends
  packets to the server, never clicks or types for you, and never reveals information the game
  hides. Player size and item animations don't touch hitboxes, attack timing or anything the
  server sees.

As with any mod, Hypixel says all mods are used at your own risk.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) and the
   [Fabric API](https://modrinth.com/mod/fabric-api) mod for your Minecraft version.
2. Download the jar for your Minecraft version from
   [Releases](https://github.com/StreetInman/skyblock-pv/releases) (for example
   `skyblock-pv-0.2.0+26.1.2.jar`) and put it in your `mods` folder.
3. Start the game once. The mod creates `config/skyblock-pv.json`.
4. *(Development builds only)* Get a key from the [Hypixel Developer Dashboard](https://developer.hypixel.net/)
   and paste it into `apiKey` in that file. Development keys expire after a few days, so you'll need to replace yours from time to time.

## Commands

| Command | What it does |
|---|---|
| `/pv` | Opens your own profile |
| `/pv <player>` | Opens another player's profile |
| `/spv`, `/sbpv` | Same as `/pv`, for when another mod has taken `/pv` |
| `/skyblockpv` | Opens the settings (player size, item animations) |
| `/pvdump <player>` | Saves the raw API response to `config/skyblock-pv/dumps/` (for debugging) |

## Privacy

Looking up a player sends their name to Mojang (to
get their UUID) and the UUID to the Hypixel API. In development builds your API key is
stored only in your local config file.

## Building from source

Requires JDK 25.

```sh
./gradlew build                 # mod jar for the default version → fabric/build/libs/
./gradlew build -Pmc=26.1.2     # or for another version in versions/
./gradlew :fabric:runClient     # launch Minecraft with the mod
./gradlew -PcoreOnly :core:test # data-layer tests only, no Minecraft download
```

The project has two modules:

- **`core/`** is plain Java with no Minecraft code. It holds the API clients, the NBT decoder,
  the profile parser (equipment, storage, trophy fish…) and skill math, and it's unit-tested.
- **`fabric/`** is the mod itself: the `/pv` command, the config file and the GUI.

Version-specific code is kept to one small `Compat` class per version under `fabric/src/compat/`.
See [docs/plan.md](docs/plan.md) for the full design and [docs/publishing.md](docs/publishing.md)
for releases, Modrinth and API keys.

## Roadmap

- [x] `/pv` command, API client, profile parsing, tabs listed above
- [x] Real item icons (via [legacy-item-dfu](https://github.com/AzureAaron/legacy-item-dfu))
- [x] Dungeons tab with M7 calculator, slayers
- [x] Storage tab (ender chest, backpacks)
- [x] Networth, player search, builds for 26.1.2 / 26.2 / 26.3
- [ ] Pets tab
- [ ] Loadouts tab (the API exposes them under `member.loadout`)
- [ ] Player skin render on the overview
- [ ] API proxy + production key so players don't need their own key
- [ ] First release on Modrinth

## Credits

- [legacy-item-dfu](https://github.com/AzureAaron/legacy-item-dfu) by AzureAaron (Apache-2.0) converts Hypixel's item data.
- The dungeon XP model follows [adjectils](https://adjectils.com/dungeon.html), as ported in the
  MIT-licensed [rtca-bot-hypixel](https://github.com/BLACKUM/rtca-bot-hypixel).
- Lowest BIN prices for networth come from [Coflnet](https://sky.coflnet.com).

## License

MIT. Not affiliated with or endorsed by Hypixel or Mojang.
