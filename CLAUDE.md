# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A fork of [2dust/v2rayNG](https://github.com/2dust/v2rayNG) rebranded as **A4VPN** (`applicationId` `com.a4vpn.app.alpha`, Gradle root project `a4vpn`). The Kotlin package is still `com.v2ray.ang` — do not rename it. Upstream is Compose too (it migrated during the 2.3.x line); the fork keeps upstream's screens and adds its own design layer on top (`ui/A4*.kt`), editing upstream files in place where it has to. Upstream files stay a useful reference when merging.

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

### Server-side lists

`handler/BypassAppListManager.kt` owns the per-app bypass selection. It fetches the
server-maintained list from `AppConfig.BYPASS_APP_LIST_URL` through `DirectNetworkHttp` (triggered
from `SubscriptionTelemetry` after a subscription refresh) and keeps three sets apart in MMKV:
server packages, packages the person added, and server packages the person removed. The effective
`PREF_PER_APP_PROXY_SET` is `(server + manual) - removed`, so server updates never undo a manual
choice. Write the per-app selection only through `replaceUserSelection`, never straight into
`PREF_PER_APP_PROXY_SET` while bypass mode is on. There is no built-in fallback: until the first
successful fetch the server set is empty. `docs/bypass-apps.txt` is a proposed list for that URL;
nothing in the app reads it, so publishing it is a server-side step.

A server list changes routing without anyone opening a settings screen, so it stays paired with a
one-time explanation: the first list that moves installed apps out of the tunnel (with per-app
proxy on) is written to `AppConfig.PREF_RECOMMENDED_BYPASS_ANNOUNCE`; `ui/A4BypassNotice.kt` shows
it on the main screen after the diagnostics question and clears the key. Don't add another
auto-applied routing default without a matching notice.

### Native prerequisites (a build fails without them)

`app/libs/` is **not** in the repo. Two artifacts must be dropped there first — see `.github/workflows/build.yml` for the canonical sequence:

- `libv2ray.aar` — the Xray core, downloaded from the [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) release matching the tag of the checked-out `AndroidLibXrayLite/` submodule.
- `libs/<abi>/libhev-socks5-tunnel.so` and `libhevsockstun.so` — built by `./compile-hevtun.sh` from the repo root with `NDK_HOME` set, then copied into `V2rayNG/app/libs/`.

Both submodules (`AndroidLibXrayLite`, `hev-socks5-tunnel`) must be checked out recursively. CI also injects `ndkVersion` into `app/build.gradle.kts`; locally set it yourself if ndk-build is needed.

## Upstream merges

The fork tracks [2dust/v2rayNG](https://github.com/2dust/v2rayNG) by merging release tags.
**Current base: upstream 2.3.3** (merge commit `be5116f8`, 2026-08-13). Update this line with
every merge — otherwise the next one starts with archaeology.

The `upstream` remote is not configured in a fresh clone:

```bash
git remote add upstream https://github.com/2dust/v2rayNG && git fetch upstream --tags
```

Rules that keep merges cheap:

- **One tag at a time**, each on its own `merge/upstream-<tag>` branch with a build in between.
  Never jump straight to the newest tag, and never let releases pile up — conflict cost grows
  much faster than the number of skipped releases. Measured reference: 2.3.3 → 2.3.5 (34 upstream
  commits, 96 files, +2537/-1305) produced 9 conflicted files and 14 conflict hunks; the fork's own
  divergence at that point was 116 files, overlapping upstream in 56 of them.
- Conflicts concentrate where fork logic was written *inside* upstream files: `ui/main/*`
  (`MainActivity`, `MainViewModel`, `MainRepository`, `MainServiceEvent`), `ui/compose/Components.kt`,
  `handler/AngConfigManager.kt`, `AngApplication.kt`, `app/build.gradle.kts`. When adding fork
  behaviour, prefer a new `A4*.kt` file plus a one-line hook in the upstream file.
- Upstream theme work (Monet dynamic color, bottom-bar and secondary-color changes) merges cleanly
  but visually overrides the A4 palette on every screen using `AppTheme` instead of `A4Theme` — only
  the A4 screens use the latter. Re-check the theme after any merge touching `ui/compose/Theme.kt`.
- Upstream changes to `MmkvManager`, profile replacement/cleanup and service start/restart paths land
  on top of fork-specific behaviour (free subscription profile, `LauncherManager`); read those diffs
  instead of trusting a clean auto-merge.
- If the `AndroidLibXrayLite` submodule tag moved, re-download the matching `libv2ray.aar`. A stale
  AAR still builds and silently runs the old core.
- Before merging into `master`: `./gradlew test assembleDebug`, then walk `docs/fork-checklist.md`.

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

### Logging and diagnostics

- `util/LogUtil.kt` is the only logging entry point (level read from `AppConfig.PREF_LOGLEVEL` and
  cached). Do not call `android.util.Log` directly in new code.
- `util/DirectNetworkHttp.kt` sends requests over Wi-Fi/mobile explicitly, never through this app's
  own VPN. **Every A4 backend call — telemetry, crash reports, update manifest, bypass list — must go
  through it**, or the request loops back into our own tunnel while the VPN is up.
- `handler/SubscriptionTelemetry.kt` defines the anonymous-event shape: an `installationId` (UUID in
  MMKV under `AppConfig.PREF_INSTALLATION_ID`), app version/versionCode, Android API, device model,
  ABI. Reuse that id and payload shape for any new event type rather than inventing another.
- Anything reporting crashes or diagnostics must install itself in **every process** (`main`,
  `:RunSoLibV2RayDaemon`, `:bg`) — the interesting crashes happen in the daemon, and
  `AngApplication.onCreate` runs in all three. A Java-level handler cannot see native crashes in
  `libv2ray`/`libhev-socks5-tunnel`; those need an NDK-level handler.
- Crash/error reporting is implemented: `handler/DiagnosticsReporter.kt` (opt-in gate, per-process
  init from `AngApplication.onCreate`, Sentry SDK pointed at our self-hosted GlitchTip),
  `handler/DiagnosticsScrubber.kt` (redaction, covered by `DiagnosticsScrubberTest` — keep the test
  in step with the regexes) and `util/DirectSentryTransport.kt` (envelopes over `DirectNetworkHttp`).
  `LogUtil` turns every `warn`/`error` into a breadcrumb. The DSN is an *optional* endpoint
  (`a4.glitchtipDsn` / `A4_GLITCHTIP_DSN`): blank is legal and simply disables reporting, so builds
  never fail over it. Deployment, server layout and the agent-access recipe are kept out
  of this public repo, in a local gitignored `docs/diagnostics.md`.
- Privacy is a hard constraint here, not a preference: core logs contain server addresses, SNI/host
  values, UUIDs and subscription URLs. Never upload raw logcat, scrub or hash those fields before
  sending, and keep reporting behind an explicit opt-in. `ui/logcat/LogcatActivity.kt` is a
  user-initiated share, not a source for automatic upload.

### Flavors and locales

`fdroid` and `playstore` flavors differ only in `applicationIdSuffix`, the `DISTRIBUTION` BuildConfig field, versionCode arithmetic in `applicationVariants.all`, and a shortcuts resource. `app/src/dev/` and `app/src/pre_release/` are leftover source sets with no matching flavor. Locale list is pinned via `localeFilters` in `app/build.gradle.kts` — adding a translation requires editing that list.

## Note on docs/AGENTS.md

`docs/AGENTS.md` covers the same ground but has drifted: it claims no product flavors, ViewBinding, and a flat `ui/` package. Prefer this file, and update both when the structure changes.
