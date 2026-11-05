# CHiQ AI PONT (Changhong DVB64, Android 14): local customizations

Branch `chiq-ai-pont-custom`, on upstream 3.4 (`fc148d5a`; launcher versionName 3.4, versionCode 13). Not pushed anywhere.

## What was changed in the launcher
- Frosted glass built from a pre-blurred copy of the wallpaper (`Frost`, `DrawerFrost`, `LeftFrost`, `DockDecoration`).
- Clock and weather panels, one dock tile each (`LeftFrost`, `WeatherPanel`, `Weather`, `WeatherIcon`: outline, filled, colorful sets).
- Wallpaper crossfade, daily/weekly/monthly updates from Wikimedia Commons with credits (`WallpaperPlayer`, `WallpaperUpdater`).
- Ambient mode after idle, one minute by default (`ambient_minutes`, in Clock settings): only the panels, the wallpaper and the photo credit (`MainActivity`). The panels stay put; the per-minute random drift was removed.
- Gear menu: Network, Go to Google TV Home, Reorder apps, Customize home screen, Settings.
- Customize home screen: Clock, Weather, Wallpapers (settings + manager), Default home launcher.
- Home key helper: Home over a screensaver, default-launcher switch, power-on redirect of the Google TV home (`HomeKeyService`).
- Reorder mode cues (`ReorderCue`), Back-to-dock, Live TV banner fix (`ShiftedBanner`).

The launcher now has the INTERNET permission, used only by `WallpaperUpdater` (Wikimedia Commons) and `Weather` (Open-Meteo).

## Theme: `themes/mytv`
`config.json` carries the dock, rotation, glass and the remote-button mappings (`app_keys`):
Netflix `scan:136`, Prime Video `scan:235` (infrared) and `scan:430` (Bluetooth), Live TV `scan:377`. YouTube worked already.
Wallpaper photos are not committed; credits are in `wallpapers/CREDITS.md` and `captions.txt`.

## Debloat: `examples/chiq-ai-pont/dvb64-android14-*.txt`
Seven packages disabled with `tools/disable.sh`, restore with `tools/restore.sh` and the same file. The upstream
`debloat.txt` in that folder is for another model (Android 11) and was left untouched.

## Build and install (Mac, Wireless debugging)
    export JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
    cd launcher && ./gradlew assembleRelease
    export TV_ADB_BINARY=/opt/homebrew/bin/adb TV_HOST=<tv ip>:<port>
    # push + `pm install -r` the APK (tools/b64push.py, tools/tv.py), then REBIND the accessibility services:
    # clear `enabled_accessibility_services`, set `accessibility_enabled 0`, wait 1 s, restore both.
    # After a reinstall the services are listed as enabled but are not bound until this is done.
Do not use `am force-stop` on the launcher: it removes its accessibility services from the list.

## Known gaps
- Wireless debugging authorization is lost whenever the TV restarts: pair again with `adb pair`.

## Change log

### 2026-11-05
- Ambient mode: the clock and weather panels no longer drift while Home sits idle, and ambient starts after
  one minute instead of five (the Clock settings cycle now includes 1). Committed as `16bed024`.
- Merged upstream 3.4 (`fc148d5a`) into the branch as `5d319c42`. The single conflict was the `tile_menu`
  block in `config.xml`: resolved to the upstream comment with the value true, keeping the local dock apps,
  `fill_dock` false, `top_menu` true and the `com.cltv.hybrid` Live TV package. Taken from upstream:
  ClockRefresh, MemInfo, `inputs_cec_state_only`, the Add-to-dock fix, the Xiaomi picture shortcut and the
  Hebrew bar fix. Nothing local was dropped.
- Built 3.4 (versionCode 13) and installed it over the TV's 3.2 with `adb install -r` while YouTube was in
  front. The APK carries the same debug certificate as the installed one, so the upgrade needed no uninstall
  and no app on screen was stopped. The accessibility list was refreshed afterwards and all three services
  (tvQuickActions, HomeKeyService, InputKeyService) are bound again.
- The release APK is 621,761 B, above upstream's 589,824 B gate; the local build was already over it before
  the merge, and the gate only applies to upstream PRs.
- Removed the one-off `CODEX_RECAP_2026-11-04.md`; its lasting facts live in this file.

### Notes for future merges
- Upstream has no ambient mode and no `ambient_minutes`. `DisplaySettingsActivity`, `WeatherPanel`,
  `LeftFrost`, `WallpaperUpdater` and the ambient code in `MainActivity` are local-only: they never conflict
  with upstream, but no merge will maintain them for us.
