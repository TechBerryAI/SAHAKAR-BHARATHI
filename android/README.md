# Android

Jetpack Compose client. Gradle root is this folder. The application module is `:app`.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Debug talks to `http://10.0.2.2:8000/`. Release reads `api.base.url` from gitignored `local.properties` here or one level up. Production value: `https://sahakar-bharathi-api.onrender.com/`.
