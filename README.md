Caff
====

Caff is a minimal Android application that provides a quick settings tile
implementing Caffeine Mode functionality inspired by LineageOS so that it
can be used on other Android operating systems.

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

## Notes

Since this is mostly for personal use, both `flake.nix`
and `app/build.gradle.kts` only support the Android API
level 36 (Android 16). If you want to support older
releases, you will have to edit these two files.

For the app to work properly in Android 12 or later, you
will have to disable battery optimizations for the app
(go to `App info` $\rightarrow$ `App battery usage` and
set it to `Unrestricted`).
