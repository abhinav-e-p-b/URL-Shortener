# Android App Build & Environment Setup Log

This document serves as a detailed record of the troubleshooting, configuration changes, and environment optimizations performed to get the Android URL Shortener app ready for building an APK.

## 1. Initial Environment & Compatibility Audit
Before attempting to build the APK, a full audit of the local development environment was conducted.

* **Target Requirements:** The app required `compileSdk = 36` and `buildToolsVersion = "36.0.0"`.
* **SDK Conflict:** Two separate Android SDK installations were found on the machine:
    1. `C:\Users\abhin\AppData\Local\Android\Sdk` (Active, referenced by Gradle) – Had API 37 but was **missing API 36**.
    2. `C:\Android` (Inactive) – Had API 36 but was missing Build Tools 36.0.0 (it had 35.0.0).

## 2. Configuration Fixes for SDK and Build Tools
To resolve the compatibility issues without requiring massive new SDK downloads, the following files were modified:

* **Modified `android/local.properties`:**
  * Changed `sdk.dir` from the `AppData` path to `C:\Android` to utilize the SDK that already had the required API 36 platform.
* **Modified `android/app/build.gradle.kts`:**
  * Removed the hard-pinned `buildToolsVersion = "36.0.0"` line. This allowed the Android Gradle Plugin (AGP 8.5.2) to automatically select the highly compatible `35.0.0` build tools already present in the `C:\Android` SDK.

## 3. Network Troubleshooting & Maven Mirrors
During the initial build attempts, Gradle consistently failed with **network timeout errors** (e.g., `Read timed out`) while trying to download dependencies (like `apksig-8.5.2.jar` and `hilt-android-gradle-plugin`) from Google's default Maven repository (`dl.google.com`). This is a common regional network issue.

To fix this, a comprehensive mirror strategy was implemented:
* **Global Gradle Timeouts:** Increased HTTP/HTTPS timeouts to 120s in `C:\Users\abhin\.gradle\gradle.properties` to improve resilience.
* **Cache Flushing:** Cleared corrupted/partial downloads and negative cache entries ("metadata-*" directories) from the Gradle cache (`~/.gradle/caches/modules-2`).
* **Tested Asian Mirrors:** Conducted live tests against Alibaba, Tencent, and Huawei Cloud Maven mirrors to find repositories that successfully resolved all required plugins (including KSP, AGP, and Hilt) with HTTP 200 responses.

## 4. Updates to `android/settings.gradle.kts`
To permanently bypass the `dl.google.com` timeouts, the repository configurations in `settings.gradle.kts` were entirely rewritten to use the fastest and most reliable mirrors (Tencent and Huawei) as primary sources.

**Changes made to `settings.gradle.kts`:**
* Updated the `pluginManagement { repositories { ... } }` block to prioritize Tencent, Huawei, and Alibaba Gradle-Plugin mirrors.
* Updated the `dependencyResolutionManagement { repositories { ... } }` block similarly.
* This ensures all AGP, Kotlin Symbol Processing (KSP), and Hilt dependencies are fetched reliably without stalling the build.

## 5. Storage Cleanup Audit
An analysis was performed on the disk space consumed by Android development tools. A detailed cleanup guide (`sdk-cleanup-guide.md`) was generated in the Antigravity system, identifying roughly **4.3 GB** of safely deletable files, including:
* The entire inactive SDK at `C:\Users\abhin\AppData\Local\Android\Sdk` (~1.5 GB).
* The unused NDK at `C:\Android\ndk` (~2.1 GB).
* CMake and legacy build tools.

## 6. Build Crash Fixes (`gradle.properties`)
During the final build stages, two specific crashes occurred which required updating the project-level `gradle.properties`:

* **AndroidX Requirement Error:** The build failed at `checkDebugAarMetadata` because modern UI libraries (like Jetpack Compose and Hilt) strictly require AndroidX. 
  * *Fix:* Added `android.useAndroidX=true` and `android.enableJetifier=true`.
* **Out of Memory (OOM) Crash:** The Gradle Daemon crashed due to "JVM garbage collector thrashing" because the default max heap was restricted to just 512 MB. 
  * *Fix:* Increased the Gradle JVM heap size to 4GB by adding `org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g`.

## 7. Current Status
The environment is fully configured, dependency mirrors are verified and working, and the Gradle setup is completely optimized with proper memory allocation and AndroidX support. 

To generate the APK, run the following command from the `android` directory:
```powershell
.\gradlew.bat assembleDebug
```
The resulting APK will be located at `android\app\build\outputs\apk\debug\app-debug.apk`.
