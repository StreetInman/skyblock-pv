# SkyBlock Profile Viewer

A client-side Fabric mod for Hypixel SkyBlock. Type `/pv <player>` to open a window showing
that player's stats and gear, without leaving the game.

> **Status: early development (v0.1.0).** It works with your own Hypixel developer key. A
> public release with no key setup needed is planned (see [Roadmap](#roadmap)).

## Features

| Tab | Shows |
|---|---|
| Overview | SkyBlock level, purse, bank, fairy souls, skill average, trophy fish rank |
| Skills | Every skill with level and progress to the next level |
| Inventory | Armor, equipment (necklace, cloak, belt, gloves), inventory and hotbar |
| Wardrobe | Every wardrobe slot, with the worn set marked |
| Accessories | Accessory bag |
| Trophy Fish | All 18 trophy fish with bronze/silver/gold/diamond counts and your Trophy Hunter rank |

Hover any item to see its full name and lore. If a player has more than one profile, use
**Profile ▸** to switch between them.

## Is it allowed on Hypixel?

Hypixel doesn't approve or whitelist mods. Its
[Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)
policy bans mods that give an advantage, automate actions, or change how the client
talks to the server. This mod does none of those:

- It reads the **official public Hypixel API** over HTTPS, the same data sites like SkyCrypt use.
- It only draws its own screen. It never sends packets to the server, never clicks or types
  for you, and never reveals information the game hides.

As with any mod, Hypixel says all mods are used at your own risk.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **26.2** and the
   [Fabric API](https://modrinth.com/mod/fabric-api) mod.
2. Put `skyblock-pv-<version>.jar` in your `mods` folder.
3. Start the game once. The mod creates `config/skyblock-pv.json`.
4. *(Development builds only)* Get a key from the [Hypixel Developer Dashboard](https://developer.hypixel.net/)
   and paste it into `apiKey` in that file. Development keys expire after a few days, so you'll need to replace yours from time to time.

## Commands

| Command | What it does |
|---|---|
| `/pv` | Opens your own profile |
| `/pv <player>` | Opens another player's profile |
| `/pvdump <player>` | Saves the raw API response to `config/skyblock-pv/dumps/` (for debugging) |

## Privacy

Looking up a player sends their name to Mojang (to
get their UUID) and the UUID to the Hypixel API. In development builds your API key is
stored only in your local config file.

## Building from source

Requires JDK 25.

```sh
./gradlew build                 # mod jar → fabric/build/libs/
./gradlew :fabric:runClient     # launch Minecraft with the mod
./gradlew -PcoreOnly :core:test # data-layer tests only, no Minecraft download
```

The project has two modules:

- **`core/`** is plain Java with no Minecraft code. It holds the API clients, the NBT decoder,
  the profile parser (wardrobe, equipment, trophy fish…) and skill math, and it's unit-tested.
- **`fabric/`** is the mod itself: the `/pv` command, the config file and the GUI.

See [docs/plan.md](docs/plan.md) for the full design.

## Roadmap

- [x] `/pv` command, API client, profile parsing, tabs listed above
- [ ] Real item icons (convert Hypixel's 1.8 item data to modern items)
- [ ] Storage tab (ender chest, backpacks), pets, dungeons, slayers
- [ ] Loadouts (pending: confirm the API exposes them, using `/pvdump`)
- [ ] Player skin render on the overview
- [ ] API proxy + production key so players don't need their own key
- [ ] First release on Modrinth

## License

MIT. Not affiliated with or endorsed by Hypixel or Mojang.
