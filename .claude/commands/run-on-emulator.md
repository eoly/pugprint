---
description: Install on the running emulator, launch, and screenshot
allowed-tools: Bash
---
1. `./gradlew installDebug`
2. `adb shell am start -n com.example.pugprint/.MainActivity`
3. `adb exec-out screencap -p > /tmp/pugprint.png`
Report whether the activity launched and describe the screenshot.
