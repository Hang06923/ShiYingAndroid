# Contributing

Thanks for helping improve ShiYing Android.

## Before submitting

1. Do not upload downloaded videos, cookies, account credentials, signing files or personal paths.
2. Keep changes within the app's local-first architecture.
3. Update `NOTICE` when adding a dependency or third-party code.
4. Run:

```powershell
npm test
.\gradlew.bat testDebugUnitTest --rerun-tasks
.\gradlew.bat assembleDebug
```

5. Describe any real-device testing you performed and the Android version/device model.

Platform extractors change frequently. A fix should include a deterministic fixture or regression test where possible.

All contributions are accepted under GPL-3.0.
