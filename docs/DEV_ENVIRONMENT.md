# macOS Dev Environment (Apple Silicon; Intel notes inline)

## 1. Brewfile — `brew bundle`
```
brew "openjdk@17"
brew "scrcpy"              # mirror/control a physical device
brew "python@3.12"        # for the bleak protocol spike
brew "jadx"               # APK decompiler (Phase 0)
cask "android-studio"     # Quail (2025.3.x) stable or newer
cask "android-platform-tools"   # adb, fastboot
cask "nrf-connect"        # nRF Connect for Desktop — BLE scanning
cask "wireshark-app"      # HCI snoop analysis (Phase 0); its installer prompts for sudo
```

## 2. Environment (~/.zshrc)
```
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```
`java_home` only sees the Homebrew JDK after it is symlinked into the system JVM dir (one-time):
```
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

## 3. SDK + emulator (arm64 on Apple Silicon)
```
sdkmanager "platform-tools" "platforms;android-37" "build-tools;36.0.0"   # compileSdk 37; AGP 9.4 needs build-tools 36.0.0
sdkmanager "system-images;android-36;google_apis;arm64-v8a"   # Intel: ...;x86_64
avdmanager create avd -n pug36 -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_7
emulator -avd pug36                      # add -no-window for headless CI-like runs
```

Gradle reads the SDK path from `local.properties` (gitignored, Android Studio writes it):
```
sdk.dir=/Users/<you>/Library/Android/sdk
```

## 4. Physical device (required for real printing — emulator has NO Bluetooth)
```
# USB: enable Developer Options + USB debugging, then:
adb devices
# Wireless: adb pair <ip:port>   then   adb connect <ip:port>
adb install -r app/build/outputs/apk/debug/app-debug.apk
scrcpy                                   # mirror the device on your Mac
adb logcat --pid=$(adb shell pidof -s com.example.pugprint)
adb exec-out screencap -p > shot.png     # capture a screenshot
```

## 5. Troubleshooting
- `adb` dies instantly with exit 137 (SIGKILL) → Gatekeeper quarantine on the cask binaries:
  `xattr -dr com.apple.quarantine /opt/homebrew/Caskroom/android-platform-tools/*/platform-tools`
- "adb: no devices" → check USB cable/data mode; `adb kill-server && adb start-server`.
- Emulator slow → ensure arm64 image on Apple Silicon (never x86 under emulation).
- BLE scan finds nothing on device → grant "Nearby devices"; ensure Location Services ON for API ≤30.
- Gradle/JDK mismatch → `./gradlew -version` should show Java 17; set JAVA_HOME as above.
