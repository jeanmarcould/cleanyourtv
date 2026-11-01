# CHiQ AI PONT (Changhong DVB64, Android 14): local customizations

Branch `chiq-ai-pont-custom`, on top of upstream. Not pushed anywhere.

## What was changed in the launcher
- Frosted glass built from a pre-blurred copy of the wallpaper (`Frost`, `DrawerFrost`, `LeftFrost`, `DockDecoration`).
- Clock and weather panels, one dock tile each (`LeftFrost`, `WeatherPanel`, `Weather`, `WeatherIcon`: outline, filled, colorful sets).
- Wallpaper crossfade, daily/weekly/monthly updates from Wikimedia Commons with credits (`WallpaperPlayer`, `WallpaperUpdater`).
- Ambient mode after idle: only the panels, the wallpaper and the photo credit (`MainActivity`).
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
