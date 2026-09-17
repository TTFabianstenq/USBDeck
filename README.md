# USBDeck

USBDeck is an Android USB utility for OTG flash drives. It detects USB attach/detach, requests USB host permission, grants folder access through the Storage Access Framework, browses and edits files on the drive, and downloads user-selected open-source PS3 development resources over HTTPS.

## Features

- USB attach/detach and storage volume monitoring
- USB host permission flow (`UsbManager.requestPermission` + `BroadcastReceiver`)
- Shared USB state via `StateFlow` / MVVM
- File browser: list, search, sort, create folder, rename, delete, copy, multi-select
- Download manager: HTTPS, progress, cancel, retry, SHA-256 when a hash is supplied
- Dark / light / system themes

## Requirements

- JDK 17
- Android SDK platform 35
- Gradle 8.9

## Android compatibility

- minSdk 26 / targetSdk 35 / compileSdk 35
- applicationId `com.donutsmp.usbdeck` (debug suffix `.debug`)

## USB notes

Modern Android does not give apps a raw mounted path to every USB stick. USBDeck uses the USB Host API for hardware detection/permission and SAF + StorageVolume for files.

## Filesystem limitations

A normal Android app cannot format a USB drive and cannot unmount a volume. The Format screen explains this and does not fake success. Safely Eject cancels in-flight work and tells you it is safe to unplug.

Filesystem type (FAT32/exFAT/NTFS) is usually not exposed to third-party apps.

## Permissions

- INTERNET and ACCESS_NETWORK_STATE for HTTPS downloads
- USB host feature required=false
- No broad storage, root, accessibility, or device-admin permission

## Build

```bash
gradle :app:assembleDebug
```

GitHub Actions workflow `.github/workflows/build.yml` uploads artifact `USBDeck-debug.apk`.

Release signing uses env vars / GitHub Secrets. Never commit keystores.

## Homebrew safety

The Homebrew tab links official `ps3dev` GitHub source archives and documentation. It does not ship CFW, exploits, or game dumps. Downloads are never executed.

## License

MIT. See LICENSE.
