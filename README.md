# Shorts

Shorts opens YouTube Shorts (`https://www.youtube.com/shorts`) in an in-app WebView. An internet connection is required. Web-page camera, microphone, and geolocation requests are denied.

## User-initiated location email

Location is accessed only after the user opens **Email location**, reviews the disclosure, taps **Prepare email**, and grants Android location permission. The app requests a fresh location and opens the device's email app with:

- To: `ram9003777754@gmail.com`
- Subject: `Location`
- Message: device manufacturer/model and Android version, coordinates, accuracy, timestamp, and a map link.

The user can review or edit the draft and must tap **Send** in the email app. The app does not send email automatically, track location in the background, or run a location foreground service. Closing the draft cancels sending. No email account password or email credential is stored in the app. Denying location permission leaves Shorts usable.

The app requests internet and approximate/precise foreground location permissions only. It does not request SMS, contacts, camera, microphone, file/storage, notification, or background-location permissions.

## Build and install

Requires Android Studio or a compatible JDK, Android SDK Platform 34, and Android Build Tools. From Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install on a connected Android device with USB debugging enabled:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug APK is signed with a development key and is intended for testing, not Play Store distribution.
