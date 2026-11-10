# PowerReboot

PowerReboot is a fork of [RebootNya](https://github.com/daisukiKaffuChino/RebootNya) by daisukiKaffuChino that runs on **Android 8.0 (API 26) and newer**. The original app requires Android 9.

It is a simple reboot utility that gives quick access to reboot options that normally need elevated privileges (reboot, power off, recovery, bootloader, safe mode, soft reboot, restart System UI, lock screen). It works with **Root** or **[Shizuku](https://shizuku.rikka.app/)**.

## Download

Get the latest APK from the **[Releases page](https://github.com/JMGLT/PowerReboot/releases/latest)** and install it (you may need to allow installs from your browser or file manager). Requires Android 8.0+ and either Root or Shizuku.

## Changes from upstream

- `minSdk` lowered from 28 to 26.
- `PackageInfo.longVersionCode` (API 28) replaced with `PackageInfoCompat.getLongVersionCode`.
- App renamed to PowerReboot, with its own `applicationId` (`io.github.jmglt.powerreboot`), so it installs next to the original RebootNya instead of replacing it. The Java/Kotlin package is unchanged to keep the diff small.
- The Shizuku user-service component now uses the real package name instead of a hardcoded one.
- Work mode is now **Automatic** by default: Shizuku if it is running and authorized, otherwise Root, otherwise a new **Local ADB** mode (upstream always defaulted to Shizuku, which fails with "Shizuku permission denied" on devices without it). You can still pick a mode in Settings.
- On Android TV boxes, **Power off** in Local ADB mode turns HDMI-CEC off and puts the box in standby (many boxes turn `reboot -p` into a reboot, and CEC wakes a sleeping box right away).
- **Power off on Android TV boxes:** these devices usually reboot on `reboot -p`, and HDMI-CEC can wake them right back up, so on TV devices "Power off" turns CEC off and puts the box in standby (wake it with its own remote).
- New **Local ADB** mode: the app talks to the device's own `adbd` on `127.0.0.1:5555`, so it works without root and without Shizuku. Requirements: network/wireless debugging enabled on the device (port 5555, e.g. `adb tcpip 5555` once), and accepting the "Allow USB debugging?" prompt the first time (tick "Always allow"). This is why the app now declares the `INTERNET` permission (loopback only).

Everything else is upstream code. This fork has not been tested on every Android 8.x device; please open an issue if something fails.

## Intent-based control

Same as upstream: send one of these actions to class `github.daisukikaffuchino.rebootnya.MainActivity` (using this app's package name).

```xml
<action android:name="github.daisukikaffuchino.rebootnya.action.LAUNCH" />
<action android:name="github.daisukikaffuchino.rebootnya.action.CLOSE" />
<action android:name="github.daisukikaffuchino.rebootnya.action.TOGGLE" />
```

## Building

Requires JDK 21 and the Android SDK (compileSdk 37). `./gradlew assembleDebug`, or run the **Build APK** GitHub Actions workflow and download the artifact.

## License

Apache-2.0, same as upstream (see [LICENSE](LICENSE)). Original work © daisukiKaffuChino and contributors. Third-party libraries: AboutLibraries, Android Jetpack, Material Components, libsu, RikkaX, Shizuku-API.
