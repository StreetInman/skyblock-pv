# Publishing

## GitHub releases

Releases are built by `.github/workflows/release.yml`. Pushing a tag such as `v0.2.0` builds one jar
per Minecraft version in `versions/`, attaches them all to a GitHub release, and (once set up below)
uploads them to Modrinth.

```sh
git tag v0.2.0
git push origin v0.2.0
```

Bump `mod_version` in `gradle.properties` first so the jar names match the tag.

## Modrinth (one-time setup)

1. Sign in at [modrinth.com](https://modrinth.com) and choose **Create a project** → *Mod*.
   Suggested values: name *SkyBlock Profile Viewer*, URL `skyblock-pv`, license MIT,
   **client side: required, server side: unsupported**, loader Fabric, categories *Utility*.
   Link the GitHub repo as the source and issue tracker.
2. On the project page open **Settings** and copy the **Project ID**. Put it in
   `gradle.properties` as `modrinth_id=`.
3. In your Modrinth account go to **Settings → Personal access tokens**, create a token with the
   *Create versions* scope, and add it to the GitHub repo under **Settings → Secrets and variables
   → Actions** as `MODRINTH_TOKEN`.
4. Push a tag. The release workflow uploads each jar as its own Modrinth version tagged with its
   Minecraft version and the Fabric API dependency.

Modrinth reviews a project before it goes public, which usually takes a few days.

## API keys

Hypixel has two kinds of key on the [developer dashboard](https://developer.hypixel.net/):

- **Development keys** are for testing. They expire after a few days, so you'd have to keep making
  new ones. They must not be shared or shipped inside a mod.
- **Production keys** are for public apps. You apply on the dashboard, describing the project;
  once approved they don't expire, and they come with a rate limit sized for the app.

A published mod can't contain a key, because anyone could pull it out of the jar. The plan is a
small proxy (for example a Cloudflare Worker) that holds the production key, adds it to requests,
and caches responses so many players looking at the same profile cost one Hypixel request. Release
builds set `apiBaseUrl` to the proxy and leave `apiKey` empty. Nothing in the mod changes apart from
that default.
