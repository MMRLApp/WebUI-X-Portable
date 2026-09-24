# WebUI X


![GitHub Release](https://img.shields.io/github/v/release/MMRLApp/WebUI-X-Portable?label=Latest%20Release)
![Google Play Release](https://img.shields.io/endpoint?url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcom.dergoogler.mmrl.wx%26l%3DInstalls%26m%3D%24shortinstalls&label=Google%20Play&color=red)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/MMRLApp/WebUI-X-Portable/total?label=GitHub%20Downloads)

## Key Features

### Native MX WebUI Engine
* **High-Performance Web Views**: Native WebUI rendering engine delivering seamless modern UI integration.
* **Status Bar Contrast Support**: Dynamic tinting for status bar icons based on web page background luminance.

### Extensibility & Runtime Interfaces
* **Lua Plugin Support**: Load custom Lua scripts located at the module webroot (`index.lua`) directly within the WebUI.
* **DEX Plugin Support**: Dynamically load and register Java/Kotlin DEX plugins specified in `webroot/config.json`.
* **KernelSU JavaScript Bridge**: Extended JS APIs permitting safe shell execution (`exec`, `spawn`), Toast display, fullscreen toggles, and module metadata queries.
* **POSIX FileSystem Bridge**: Asynchronous and synchronous root-backed file I/O capabilities directly exposed to JS interfaces via `SuFile`.
* **System Package API**: Exposes device app listing, package metadata, and dynamic icon rendering via custom `ksu://` URI handling.

### Permissions & Security Model
* **Granular WebUI Permissions**: Micro-permissions for gating sensitive features (shell execution, filesystem access, package queries).
* **Content Security Policy (CSP)**: Built-in CSP manager to merge module security policies, with optional complete opt-out (`*` or blank).

### Developer Tools & Debugging
* **In-App DevTools**: Built-in inspector features including Console, Network, View, and DOM tree tabs powered by `Jsoup`.
* **DevTools Snippets**: Quick developer actions (border elements, page reloads, editable page mode).
* **Rich Crash Handler**: Structured reporting with markdown help documentation for quick issue triage.

### File Explorer & Code Editor
* **Built-in File Explorer**: Root and non-root file browsing.
* **TextMate Code Editor**: In-app code editor featuring syntax highlighting with dynamic themes powered by TextMate language scopes.

### Dashboard & Module Management
* **Home Dashboard**: Quick overview of device information, system storage, working mode status (Root vs Non-Root), and module analytics.
* **Module Shortcuts**: Create pinned home screen shortcuts directly to individual WebUI modules.
* **ZIP Importer**: Directly import and extract module ZIP archives within the application.

## Preview

<p>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="32%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="32%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="32%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="32%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="32%" />
</p>

## Translate

Get involved with WebUI X: Portable by translating it into your language!

[![Translation status](https://hosted.weblate.org/widget/mmrlapp/wxp-main/multi-auto.svg)](https://hosted.weblate.org/engage/mmrlapp/)
