# Quickstart: Offline ASR Translation

## Prerequisites

- Android device/emulator capable of running the app.
- A v1 offline model package with valid manifest for Japanese transcript and Japanese -> Vietnamese/English translation.
- Model download URL configured for the prototype, or a local package available through Android file picker.

## Prototype Status

- Current implementation includes the offline model manifest contract, model readiness repository, settings state/UI, player runtime mode selection, and a prototype offline engine.
- The prototype engine emits a fixed Japanese transcript and Vietnamese/English translation after a valid model manifest is installed in app-specific storage.
- Native sherpa-onnx or whisper.cpp runtime binding is intentionally behind `OfflineSubtitleEngine` and is not bundled yet.

## Build

```powershell
pwsh -NoProfile -Command "$env:JAVA_HOME='C:\Users\ducthanh276\.serena\language_servers\static\EclipseJDTLS\vscode-java\extension\jre\21.0.7-win32-x86_64'; $env:Path=('{0}\bin;{1}' -f $env:JAVA_HOME, $env:Path); .\gradlew.bat assembleDebug"
```

## Scenario 1: Import model manually

1. Open Settings > Subtitle/Translation.
2. Open Offline subtitle section.
3. Use Refresh model after placing a valid `manifest.json` package in the app model directory during prototype testing.
4. Verify model state becomes Ready.

## Scenario 2: Download model

1. Open Settings > Subtitle/Translation.
2. Verify Wi-Fi-only default is visible in the Offline subtitle section.
3. Download task implementation is pending native/model distribution wiring.

## Scenario 3: Run offline subtitle

1. Disable network after model is Ready.
2. Play a video with Japanese speech.
3. Enable offline subtitle.
4. Verify transcript appears within 5 seconds after speech.
5. Select Vietnamese then English target and verify translated subtitle appears after finalized transcript.

## Scenario 4: Seek/reset

1. Enable offline subtitle while video is playing.
2. Seek to another position.
3. Verify previous subtitle text is cleared and new transcript starts from the new position.

## Verification Commands

```powershell
pwsh -NoProfile -Command "$env:JAVA_HOME='C:\Users\ducthanh276\.serena\language_servers\static\EclipseJDTLS\vscode-java\extension\jre\21.0.7-win32-x86_64'; $env:Path=('{0}\bin;{1}' -f $env:JAVA_HOME, $env:Path); .\gradlew.bat testDebugUnitTest"
```
