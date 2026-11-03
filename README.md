# YT Car 2.0 – Passenger UI + Background Manager

## Passenger Mode
- Adds an in-app Passenger Mode UI with a distinct passenger-oriented theme and large controls.
- Passenger Mode applies inside YT Car on the device/display where the app is actually running.
- This is NOT an Android Auto video projection implementation and does not guarantee display on the MG RX5 factory head unit.
- No speed-lock bypass, vehicle-state spoofing, or hidden-video workaround is included.

## Background Manager
- Includes Media3 foreground media service from v1.4.
- The "Âm thanh nền" control plays user-selected local/authorized audio files while the app is backgrounded.
- It does not extract YouTube audio or override YouTube background-playback policy.
- YouTube Music button opens the official YouTube Music app.

## YouTube
- YouTube mobile website is embedded in WebView while YT Car is foregrounded.
- Fullscreen HTML5 video is supported where the YouTube site/WebView permits it.
- WebView is paused when YT Car goes to background.

## Build and test
1. Upload the `YtCar` folder to GitHub.
2. Run **Actions → Build APK**.
3. Download artifact `YtCar-apk`, then install `app-release.apk`.
4. Test YouTube while YT Car is foregrounded.
5. Test background audio by choosing a local audio file with **Âm thanh nền**.

## Important MG RX5 / Android Auto limitation
The phone app cannot force its UI or video onto the factory display through ordinary Android Auto. Passenger Mode will appear on the factory screen only if MG provides an officially supported passenger-display mode/API or another supported projection path. If the factory unit only supports standard Android Auto, YT Car video will not appear there. Video restrictions while driving are not bypassed.

This is a source package. It has been ZIP-integrity checked but has not been compiled or device-tested in this environment.


## v2.1 – Tắt quyền truy cập vị trí/GPS
- Ứng dụng không khai báo quyền `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` hoặc `ACCESS_BACKGROUND_LOCATION`.
- WebView tắt `geolocationEnabled`.
- Mọi yêu cầu định vị từ trang web trong WebView đều bị từ chối.
- Các tính năng video, Passenger Mode và âm thanh nền không cần GPS.


## v2.2 – Build fix
- Removed the unsupported `WebSettings.geolocationEnabled` Kotlin property that caused `Unresolved reference`.
- GPS remains disabled by design: the app manifest has no fine/coarse/background location permissions, and WebView geolocation prompts are explicitly denied in `WebChromeClient.onGeolocationPermissionsShowPrompt`.
