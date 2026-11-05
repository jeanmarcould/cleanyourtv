# CHiQ AI PONT (Changhong DVB64, Android 14): local customizations

Branch `chiq-ai-pont-custom`, on upstream 3.4 (`fc148d5a`; launcher versionName 3.4, versionCode 13). Pushed to the `jeanmarcould/cleanyourtv` fork only; upstream is untouched.

## What was changed in the launcher
- Frosted glass built from a pre-blurred copy of the wallpaper (`Frost`, `DrawerFrost`, `LeftFrost`, `DockDecoration`).
- Clock and weather panels, one dock tile each (`LeftFrost`, `WeatherPanel`, `Weather`, `WeatherIcon`: outline, filled, colorful sets).
- Wallpaper crossfade, daily/weekly/monthly updates from Wikimedia Commons with credits (`WallpaperPlayer`, `WallpaperUpdater`).
- Ambient mode after idle, one minute by default (`ambient_minutes`, in its own Ambient mode screen): only the panels, the wallpaper and the photo credit (`MainActivity`). The panels stay put; the per-minute random drift was removed.
- Gear menu: Network, Go to Google TV Home, Reorder apps, Customize home screen, Settings.
- Customize home screen: Clock, Weather, Wallpapers (settings + manager), Ambient mode, Default home launcher.
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
- Home stays Google TV's at the system level, by choice: the launcher is the Home *key* via HomeKeyService, while
  `resolve-activity` and `mHomeProcess` stay on `com.google.android.apps.tv.launcherx`. Android caps an
  intent-filter priority above 0 for non-privileged apps, so launcherx (system app, priority 2) always outranks
  this one (checked on 2026-11-05); `ROLE_HOME` and `cmd package set-home-activity` (prints Success) are both
  ignored by this firmware. Taking real Home needs launcherx's HomeActivity disabled, and that is exactly what
  the gear menu's Google TV row opens, so the shortcut and real Home cannot both hold. The build was reverted
  rather than shipping a priority that Android caps to 0.

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
- Ambient mode left the Clock screen for its own top-level entry, row 4 of Customize home screen
  (`AmbientSettingsActivity`): an on/off switch, then a 1/2/5/10/15/30 min timeout dropdown, shown only while
  it is on. Off is `ambient_minutes` 0; the last timeout is remembered in `ambient_last_minutes` for the switch.
  The Clock screen is now time and date only.
- Wallpapers reworked (`WallpaperSettingsActivity`): refresh rate, Transition (fade / none), then Manage
  wallpapers. The fade is a fixed 1.6 s and `wallpaper_fade_ms` is ignored. The new `WallpaperManageActivity`
  has Refresh now, Automatic update frequency and View wallpapers.
- Strings `ambient_mode` and `wallpapers_view` added in default, French and Hebrew; the fade-duration strings
  are gone. Both new activities are non-exported.
- Committed as `3c9ba2cf`, built (622,561 B) and installed on the TV; the three accessibility services rebound.
- New `wallpaper_order: "cycle"` for rotations: every picture in the collection comes up once per round, in a
  random order, before any repeat, and the round is kept across Home restarts. `mytv` uses it; `shuffle` still
  means pure random and `sequential` still walks the filenames.
- Wallpaper captions now read "Location - Author": `WallpaperUpdater.location()` drops the capture date and the
  photographer's archive code from the Commons title, and `themes/mytv/wallpapers/captions.txt` was regenerated
  from its `CREDITS.md`. The wallpaper manager shows each picture's arrival date under its tile (the updater's
  `wm_<epoch>_NN.jpg` timestamp, else the file date), and in the preview.
- Built with these, installed over 3.4, and pushed `themes/mytv/config.json` alone, so the 60 downloaded photos
  stayed; Home was restarted with `tools/theme_restart.py` and its three accessibility services rebound. APK is
  623,337 B. The 5-minute interval chosen in Home settings > Wallpapers still applies; `cycle` changes only the
  order. The TV's 60 captions were rewritten in the same pass.
- Tried to make this launcher the system Home: a priority-3 HOME filter (Android caps third-party priority at 0,
  launcherx is a system app at 2), a fresh `ROLE_HOME` grant and `cmd package set-home-activity`. All left
  `resolve-activity` on launcherx. The manifest change was reverted, the reverted build (622,561 B) reinstalled
  and the three accessibility services rebound; keeping the gear menu's Google TV row was the deciding
  constraint, so Home stays launcherx. No code change; the reasoning is under Known gaps.

### Notes for future merges
- Upstream has no ambient mode and no `ambient_minutes`. `DisplaySettingsActivity`, `WeatherPanel`,
  `LeftFrost`, `WallpaperUpdater` and the ambient code in `MainActivity` are local-only: they never conflict
  with upstream, but no merge will maintain them for us.
