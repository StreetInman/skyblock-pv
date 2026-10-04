# Skyblock Profile Viewer (/pv) — Plan

## 1. The API key question (decide this first)

Hypixel's API needs a key, and a published mod **cannot ship your key** inside the jar (it would leak and break Hypixel's API policy).

- **Development keys** (from developer.hypixel.net) expire every few days and are only for your own testing.
- **Production / application keys** are issued per project after you apply on the developer dashboard and describe what the app does.

**Recommended architecture:** a tiny backend proxy holds the production key; the mod talks only to the proxy.

```
Minecraft client (mod) ──HTTPS──▶ your proxy (Cloudflare Worker) ──▶ api.hypixel.net
                                     │ holds the key, caches ~5 min, rate-limits per IP
```

- Cloudflare Workers + KV is free at hobby scale and ~100 lines of code.
- Caching matters: one popular player getting /pv'd by 50 people should cost 1 Hypixel request, not 50.
- Phase 1 (while building): the mod reads *your* dev key from a local config file and calls Hypixel directly. Phase 2 (before publishing): apply for a production key and switch the mod's base URL to the proxy.

## 1b. Staying within Hypixel's rules (no ban risk)

Hypixel does not approve or whitelist any mod; its Allowed Modifications page lists *kinds* of mods and says all use is at your own risk. This mod stays clearly on the safe side by design:
- It only reads the official public API and draws a client-side screen. It never sends packets to the Hypixel server, never automates anything, never reads hidden in-game info.
- No in-game advantage: it shows the same data public sites like SkyCrypt show.
- Uses its own registered API key (dev key while building, production key for release), never a shared or scraped one.
- Rule for the codebase: no mixins that touch networking/packets, no chat auto-replies, no auto-opening of menus.

## 2. Platform

- **Fabric, Minecraft 1.21.x** (whatever version Skyblocker/SkyHanni currently target, pinned at scaffold time), Java 21, Gradle + Fabric Loom.
- Libraries: Fabric API, Gson (bundled), optionally YACL or Cloth Config for settings, Mod Menu integration.
- Client-side only. It reads public API data and draws a screen, so it's within Hypixel's allowed-mod rules (no packet tampering, no automation).

## 3. Data flow

1. `/pv <name>` (client command via Fabric's ClientCommandRegistrationCallback). No name = yourself.
2. Name → UUID via Mojang (`api.mojang.com/users/profiles/minecraft/<name>`), cached.
3. `GET /v2/skyblock/profiles?uuid=…` → all profiles; pick the one with `selected: true`.
4. Optional extras: `/v2/skyblock/museum`, `/v2/skyblock/garden`, `/v2/player` (rank, for the header).
5. Static data, no key needed: `/v2/resources/skyblock/items`, `/skills`, `/collections` (cached on disk for a day).
6. Decode inventories: each is base64 → gzip → NBT (e.g. `members[uuid].inventory.inv_contents.data`, ender chest, wardrobe, talisman bag, backpacks, pets).
7. Compute derived stats (skill levels from XP tables, catacombs level, slayer levels, networth later).
8. Build an immutable `ProfileView` model, hand it to the GUI on the render thread.

All HTTP is async (`java.net.http.HttpClient` + `CompletableFuture`) so the game never freezes; the screen shows a loading state.

**Gotchas to plan around:**
- **Legacy item NBT.** Hypixel stores items in 1.8.9 format (numeric ids + `Damage`, `tag.display.Name` with § codes, `SkullOwner` textures). You need a legacy-id → modern-item mapping table and a converter to modern `ItemStack` data components. This is the hardest single piece; it's worth studying how Skyblocker/Firmament handle it.
- **API toggles.** Players can disable inventory/collection API access; the GUI must show "API disabled" instead of empty slots.
- **Rate limits.** Respect the `RateLimit-*` response headers; back off on 429.

## 4. GUI layout

One custom `Screen`, chest-style background, tabs along the top:

| Tab | Shows |
|---|---|
| Overview | Skin render (player model), rank, SB level, purse/bank, fairy souls, profile switcher dropdown |
| Skills | Each skill with level + progress bar, skill average |
| Dungeons | Catacombs level, class levels, floor completions / best times |
| Slayers | Per-boss level and kills |
| Inventory | Armor, equipment (necklace/cloak/belt/gloves), hotbar, inventory grid |
| Wardrobe | All wardrobe slots, equipped slot highlighted |
| Loadouts | Saved loadouts, if the API exposes them (unverified) |
| Storage | Ender chest pages, backpacks |
| Trophy Fishing | Per-fish tier grid (bronze/silver/gold/diamond), totals, trophy fisher rank |
| Accessories | Talisman bag, magical power |
| Pets | Pet grid with level, active pet highlighted |
| Collections / Museum | Later phases |

Items render with vanilla tooltip on hover (lore comes straight from the NBT). Keyboard: number keys switch tabs, Esc closes.

## 5. Code structure

```
src/main/java/.../pv/
  PvMod.java                 entrypoint, registers /pv
  api/ HypixelClient, MojangClient, Cache, RateLimiter
  data/ ProfileParser, NbtDecoder, LegacyItemConverter, SkillTables
  model/ ProfileView, SkillData, DungeonData, InventoryData ...
  gui/ PvScreen, tabs/*Tab, widgets/ItemGrid, ProgressBar, PlayerModel
  config/ PvConfig
proxy/                       Cloudflare Worker (TypeScript)
```

## 6. Testing

- **Unit tests (JUnit)** on the pure parts: save a few real API responses as JSON fixtures (your own profile, an API-disabled one, an ironman/stranded one) and test parsing, NBT decoding, skill-level math, legacy item conversion. No Minecraft launch needed.
- **Dev client:** `./gradlew runClient` launches Minecraft with the mod; join Hypixel and `/pv` yourself and friends.
- **Offline fixture mode:** a config flag that loads fixture JSON instead of calling the API, so you can iterate on the GUI without burning requests.
- **CI:** GitHub Actions runs `./gradlew build test` on every push.

## 7. Documentation & publishing

- **GitHub:** public repo, MIT or LGPL license, README with screenshots/GIF, install steps, privacy note (what the proxy sees/logs), "not affiliated with Hypixel" disclaimer, CHANGELOG.
- **Releases:** tag `v0.1.0` → GitHub Action builds the jar and publishes to GitHub Releases and **Modrinth** (via the Minotaur Gradle plugin or `mc-publish` action, using a Modrinth token stored as a repo secret). CurseForge optional, same action.
- **Modrinth page:** description, gallery, client-side only, Fabric + version tags, link to source.

## 8. Milestones

1. Scaffold Fabric mod, `/pv` command, dev-key config, fetch + log profile JSON.
2. Parsing + model + unit tests on fixtures.
3. GUI shell with Overview + Skills tabs.
4. NBT decode + legacy item conversion → Inventory/Storage/Accessories tabs.
5. Dungeons, Slayers, Pets tabs; profile switcher.
6. Proxy + production key application.
7. README, screenshots, CI release pipeline, v0.1.0 on Modrinth.
