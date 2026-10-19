<p align="center">
  <img src="https://raw.githubusercontent.com/7gups/Octifine/main/branding/octifine-logo.png" alt="Octifine" width="120"/>
</p>

<h1 align="center"><b>Octifine</b></h1>

<p align="center">
  An Android launcher and settings app for <b>Octane</b>, the 2021 Roblox client.
</p>

<p align="center">
  <a href="https://github.com/7gups/Octifine/releases/latest">Latest release</a> |
  <a href="https://github.com/7gups/Octifine">Source</a> |
  <a href="https://octane.wtf">Octane</a>
</p>

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/7gups/Octifine/output/github-contribution-grid-snake-dark.svg">
    <img alt="Contribution snake" src="https://raw.githubusercontent.com/7gups/Octifine/output/github-contribution-grid-snake.svg" width="100%"/>
  </picture>
</p>

<div align="center">

[![Latest Release][shield-latest]][repo-latest]
[![Downloads][shield-downloads]][repo-releases]
[![Stars][shield-stars]][repo-stargazers]

</div>

<h5 align="center">
  Leave a star if you like the project! ⭐️
</h5>

---

> [!IMPORTANT]
> Octifine targets Octane 2.463 (build 417004). It runs on **Android 7.0 and above**. Flags and mods are applied through Shizuku, USB debugging or root.

## Install

1. Download the latest `Octifine-Android-direct-<version>.apk` from the [releases page][repo-latest].
2. Install it and open Octifine.
3. Install Octane from [octane.wtf](https://octane.wtf), then choose it in the target selector.

Use the `direct` build for sideloading. The `play` build is for the Play Store.

## Features

- **Library:** pinned games, games you've played, your current game under "Playing now", game passes, join by server or link, and I'm Feeling Lucky (picks from your ten most recently played games).
- **Flags:** about 3,000 flags confirmed to exist in the 2021 build, with search, categories, descriptions, and presets for performance, quality, network, interface and debugging.
- **Mods:** file overlays applied to Octane, including a crash test for finding flags that crash the client (debug builds only).
- **Home:** your library, New on the Catalog, your welcome card with limiteds, the contribution snake, and the latest release.
- **Launcher:** Material 3 navigation, a floating save button, and a launch button that appears after a save.

## Building

The Android project is in [`android/`](android). You need a JDK that works with the Android Gradle plugin, and the Android SDK. The Rust core uses the Android NDK.

```bash
cd android
./gradlew assembleDirectDebug
```

For a signed release, set these Gradle properties (the names match `android/app/build.gradle.kts`) in `~/.gradle/gradle.properties` or on the command line:

```properties
voidstrap.storeFile=/path/to/octifine-release.jks
voidstrap.storePassword=your_store_password
voidstrap.keyAlias=your_key_alias
voidstrap.keyPassword=your_key_password
```

Release APKs are built by the `android-release` workflow in `.github/workflows`. The workflow needs the keystore secrets in the repository settings.

The app version comes from [`Directory.Build.props`](Directory.Build.props).

## Notes

- Octifine is not affiliated with Roblox Corporation or the Octane project.
- Octane's API and launch behaviour may change. Some features depend on what the server returns at the time.

## Credits

Octifine is based on [Voidstrap](https://github.com/KloBraticc/Voidstrap) by KloBraticc, which is built on [Bloxstrap](https://github.com/bloxstraplabs/bloxstrap) by pizzaboxer. Third-party code, fonts and artwork keep their own licenses, which are listed in the app's Licenses screen.

[shield-latest]:    https://img.shields.io/github/v/release/7gups/Octifine?color=7a39fb
[shield-downloads]: https://img.shields.io/github/downloads/7gups/Octifine/total?color=8a2be2
[shield-stars]:     https://img.shields.io/github/stars/7gups/Octifine?color=ffd700
[repo-latest]:      https://github.com/7gups/Octifine/releases/latest
[repo-releases]:    https://github.com/7gups/Octifine/releases
[repo-stargazers]:  https://github.com/7gups/Octifine/stargazers
