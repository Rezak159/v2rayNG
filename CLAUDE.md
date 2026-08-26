# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A fork of [2dust/v2rayNG](https://github.com/2dust/v2rayNG) rebranded as **A4VPN** (`applicationId` `com.a4vpn.app.alpha`, Gradle root project `a4vpn`). The Kotlin package is still `com.v2ray.ang` — do not rename it. Upstream's XML/Fragment UI has been replaced by a Compose UI (`ui/A4*.kt` plus per-feature `ui/<feature>/` packages); most other layers are close to upstream, so upstream files are still a useful reference when merging.

Comments and commit messages in fork-specific code are frequently in Russian; match the surrounding language when editing those files.

## Build & test

Everything runs from the `V2rayNG/` directory (Gradle 9.5, AGP 9.3, Kotlin 2.4, JDK 17 target / JDK 21 toolchain in CI, compileSdk/targetSdk 37, minSdk 24).

```bash
cd V2rayNG && ./gradlew assembleDebug
```

```bash
cd V2rayNG && ./gradlew test
```

Single test class or method (flavor is required in the task name; `fdroid` and `playstore` exist):

```bash
cd V2rayNG && ./gradlew :app:testPlaystoreDebugUnitTest --tests "com.v2ray.ang.UtilsTest"
```

Restrict ABI splits when iterating locally (default builds four ABIs plus a universal APK):

```bash
cd V2rayNG && ./gradlew assembleDebug -PABI_FILTERS=arm64-v8a
```

No lint/format/typecheck task is wired up. Tests are JUnit 4 + Mockito, unit tests only, in `app/src/test/java/`.

### Endpoint config (a build fails without it)

External links the app sends people to (Telegram bot, site) are not in the repo. Copy
`V2rayNG/a4.properties.example` to `V2rayNG/a4.properties` (gitignored) and fill in
`a4.telegramBotUrl` / `a4.siteUrl`; CI passes them as the `A4_TELEGRAM_BOT_URL` /
`A4_SITE_URL` env vars from repo secrets instead. Gradle reads them in the `a4Endpoint`
block of `app/build.gradle.kts`, emits `BuildConfig.TELEGRAM_BOT_URL` /
`BuildConfig.SITE_URL`, and fails configuration with an actionable message if either is
missing. Kotlin reads them through `AppConfig.TELEGRAM_BOT_URL` / `AppConfig.SITE_URL`,
never `BuildConfig` directly.

### Native prerequisites (a build fails without them)

`app/libs/` is **not** in the repo. Two artifacts must be dropped there first — see `.github/workflows/build.yml` for the canonical sequence:

- `libv2ray.aar` — the Xray core, downloaded from the [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) release matching the tag of the checked-out `AndroidLibXrayLite/` submodule.
- `libs/<abi>/libhev-socks5-tunnel.so` and `libhevsockstun.so` — built by `./compile-hevtun.sh` from the repo root with `NDK_HOME` set, then copied into `V2rayNG/app/libs/`.

Both submodules (`AndroidLibXrayLite`, `hev-socks5-tunnel`) must be checked out recursively. CI also injects `ndkVersion` into `app/build.gradle.kts`; locally set it yourself if ndk-build is needed.

## Architecture

### Two processes

The UI runs in the main process; every core-touching service runs in `:RunSoLibV2RayDaemon` (declared in `AndroidManifest.xml`), and WorkManager jobs in `:bg`. This split is the single most important constraint:

- **Never load native/core classes from the UI process.** `LauncherManager.startContextService` deliberately skips an `isRunning` check for this reason — the check happens inside `CoreServiceManager` in the daemon process.
- Cross-process communication is `Serializable` payloads over broadcasts, funneled through `helper/MessageHelper.kt` (`sendMsg2Service` / `sendMsg2UI` / `sendMsg2TestService`) with `MSG_*` codes from `AppConfig`.
- Shared state crosses the boundary via MMKV opened in `MMKV.MULTI_PROCESS_MODE`.

### Layers

- `AppConfig.kt` — every constant in the app: MMKV preference keys, `MSG_*` broadcast codes, ports, A4-specific endpoints (`APP_LINK_HOST`, `APP_UPDATE_MANIFEST_URL`, `FREE_SUB_URL`, `FREE_SUBSCRIPTION_ENABLED`). New preference keys go here, not inline.
- `handler/MmkvManager.kt` — the entire data layer. Storage is MMKV only, never SharedPreferences; separate MMKV IDs for profiles, raw configs, subscriptions, assets, and settings. All persistence goes through this object.
- `handler/` — the rest of the business logic (`SettingsManager` defaults and derived config, `AngConfigManager` profile import/export, `SubscriptionUpdater`, `GeoUpdater`, `WebDavManager` backups, `AppUpdateManager`/`UpdateCheckerManager` for the fork's in-app updater).
- `core/` — turning a stored profile into a running core. `CoreConfigContextBuilder` → `CoreConfigManager` (builds the Xray JSON, with `CoreOutboundBuilder` for outbounds) → `CoreServiceManager` (singleton owning core start/stop, traffic stats, reload) → `CoreNativeManager` (JNI bridge to the `libv2ray` AAR). `LauncherManager` is the entry point the UI calls.
- `service/` — the Android service shells. `CoreVpnService` (VpnService + `TProxyService` running hev-socks5-tunnel), `CoreProxyOnlyService` (local SOCKS/HTTP, no VPN), `CoreRootService` (root mode, backed by `root/`), plus `CoreTestService`/`RealPingWorkerService` (latency), `SubscriptionUpdateService`, `QSTileService`, `NetworkMonitor`. Each run mode implements `contracts/ServiceControl` and hands itself to `CoreServiceManager.serviceControl`.
- `fmt/` — one file per protocol (VMess, VLESS, Trojan, Shadowsocks, SOCKS, Hysteria2, WireGuard, custom), each parsing/serializing share links into `ProfileItem`. Adding a protocol means a `fmt/` file, an `EConfigType` entry, a `ui/server/Server*Activity`, and outbound support in `CoreOutboundBuilder`.
- `dto/`, `enums/`, `util/`, `extension/`, `helper/`, `receiver/` — as named; `dto/entities/` holds the persisted models (`ProfileItem`, `SubscriptionItem`, `RulesetItem`, …).

### UI

Compose only, Material 3, no ViewBinding/DataBinding/Fragments. Activities extend `ui/base/BaseComponentActivity` / `HelperBaseComponentActivity` and call a Composable screen from `setContent`.

- The main screen follows an MVI shape: `ui/main/MainContract.kt` (`MainUiState` + a sealed `MainAction`), `MainViewModel`, `MainRepository` implementing `MainDataSource`. Other features use a plainer ViewModel + `ui/base/BaseViewModel` (which owns `isLoading` and a `ViewModelEvent` channel for toasts/navigation).
- `ui/A4Theme.kt`, `A4MainScreen.kt`, `A4SettingsScreen.kt`, `A4PowerButton.kt`, `A4Glass.kt`, `A4Subscription.kt` are the fork's bespoke design layer; `ui/compose/` holds the reusable primitives (dialogs, form fields, settings rows, snackbars, `ThemeManager`).

### Flavors and locales

`fdroid` and `playstore` flavors differ only in `applicationIdSuffix`, the `DISTRIBUTION` BuildConfig field, versionCode arithmetic in `applicationVariants.all`, and a shortcuts resource. `app/src/dev/` and `app/src/pre_release/` are leftover source sets with no matching flavor. Locale list is pinned via `localeFilters` in `app/build.gradle.kts` — adding a translation requires editing that list.

## Note on docs/AGENTS.md

`docs/AGENTS.md` covers the same ground but has drifted: it claims no product flavors, ViewBinding, and a flat `ui/` package. Prefer this file, and update both when the structure changes.
