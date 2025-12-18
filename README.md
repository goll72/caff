Caff
====

Caff is a minimal Android application that provides a quick settings tile
implementing Caffeine Mode functionality (similar to LineageOS).

---

SHA256 signing key fingerprint:
`5F:7C:F6:44:98:2B:E2:1C:28:B6:78:FB:64:8E:D0:61:77:48:2E:2D:92:98:D6:EE:16:E8:BF:D1:9C:27:24:89`
(GitHub releases)

## Usage

Tapping the quick settings tile will toggle caffeine and cycle through
different durations. If the tile is unavailable, you will have to disable
battery optimizations (see [notes](#notes)). Long-pressing the tile will
show a preferences menu, where custom durations can be added/removed.

## Building

If you have `nix`, you can get a development shell
with all dependencies set up by running:

```sh
$ nix develop
```

If you don't have `nix` you will have to install the
dependencies manually.

Then, to build the project, run

```sh
$ gradle build
```

The APK will be available in `app/build/outputs/apk/`.

## Screenshots

![Caffeine disabled](./metadata/en-US/images/phoneScreenshots/1.png)

![Caffeine enabled](./metadata/en-US/images/phoneScreenshots/2.png)

![Infinite duration](./metadata/en-US/images/phoneScreenshots/3.png)

![Preferences menu](./metadata/en-US/images/phoneScreenshots/4_preferences.png)

## Notes

For the app to work properly in Android 12 or later, you
will have to disable battery optimizations for the app
(go to `App info` $\rightarrow$ `App battery usage` and
set it to `Unrestricted`).

This app only supports Android 8 or later and it is intended
to be very minimal. If you want support for older versions of
Android or a more featureful option, check out [Caffeinate](https://github.com/abdalmoniem/Caffeinate).
Feel free to open an issue or send a PR for stuff like new
features or fixes for quirky OEMs though, as long as they're
not too disruptive.
