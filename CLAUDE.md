# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Chronoscape (陽だまり) — a JavaFX fullscreen/windowed desktop app that overlays a clock, OpenWeather-based weather forecast, JMA (気象庁) warnings, and an auto-rotating wallpaper slideshow with theme-color extraction. Personal project, single developer (akidukisystems).

**Name origin**: "Chronoscape" is a coined blend of *chrono-* (time/clock) and *-scape* (landscape/wallpaper), reflecting the app's core idea of overlaying a clock on a rotating wallpaper backdrop. "陽だまり" (hidamari, "a sunny/warm spot") is the Japanese subtitle, evoking the always-on, ambient presence of the app sitting quietly on the desktop.

## Build / run

The system `JAVA_HOME` on this machine points at a JRE 1.8, but the project requires JDK 25 — override it explicitly for every Maven invocation:

```bash
JAVA_HOME="/c/Program Files/Java/jdk-25.0.2" ./mvnw.cmd clean compile
JAVA_HOME="/c/Program Files/Java/jdk-25.0.2" ./mvnw.cmd clean package -DskipTests
```

Run the packaged jar:

```bash
java -jar target/notify-0.0.1-SNAPSHOT.jar
```

During development, running `NotifyApplication.main` directly from the IDE also works.

There are no automated tests in this repo (no `src/test` directory) — verification is compile success plus manual/visual testing of the running app.

## Runtime config

On first launch the app bootstraps `%APPDATA%\NotifyApp\settings.json` (or `~/.notifyapp/settings.json` off Windows) from a bundled classpath template, then reads/writes that external file thereafter (`Configure.java`). The OpenWeather API key lives in a sibling `apikey.json`, kept out of the repo. `SettingsController` is the in-app editor for both. When testing config-dependent behavior (wallpaper path, API key, JMA area code), check/edit the real file at that path rather than the bundled template — the template is only a first-run seed.

## Architecture

**Startup chain**: `NotifyApplication.main` → `GUI.launchApp` → JavaFX `GUI.start(Stage)`. `GUI.start` builds `Core` (loads `Configure`, constructs `Weather`, calls back into `GUI.setClass`), loads `main.fxml` → `ctrl` controller, then shows a borderless `SplashScreen` (separate `Stage`) while wallpapers load asynchronously in the background. Once roughly half the wallpapers are decoded, the splash closes and the real `Stage` (assigned to the static `GUI.stage` field) is shown along with `TrayManager` (system tray icon/menu).

Ordering matters here: `GUI.stage` must be assigned *before* `controller.setClass(...)` runs, because `setClass` can synchronously trigger the "no wallpapers found" callback which calls `GUI.stage.show()`.

**Controller composition**: `ctrl` (bound to `main.fxml`) is the hub. It owns the `Timeline`s driving periodic updates (wallpaper rotation, weather refresh, UI left/right reversal for burn-in prevention) and delegates feature-specific UI to per-domain controllers, each constructed with what it needs and none of `ctrl`'s other concerns:
- `controller.clock.ClockController` — the animated digital clock
- `controller.weather.WeatherUIController` — current conditions + 3-day forecast, per-day tap-to-expand overlay (uses `SVGHelper.createIcon` for weather icons)
- `controller.alert.AlertUIController` — JMA warning/advisory badges in a `GridPane`, tap-to-expand detail popup
- `controller.battery.BatteryUIController` — Bluetooth mouse battery via `BatteryManager` (PowerShell-based, Windows-only — gated by an `IS_WINDOWS` check in `ctrl`; hidden entirely on other OSes)
- `controller.wallpaper.WallpaperController` — background-thread wallpaper decode/rotation/theme-color extraction (see below)
- `controller.settings.SettingsController` — standalone `Stage` for editing all `Configure` fields, launched from a gear icon

**Theme propagation**: `controller.theme.ThemeManager` is a small observer hub — feature controllers register a `ThemeListener` in their constructor, and `applyColors(textColor, wbColor)` (called once wallpaper colors are extracted/loaded) pushes the new color pair to every listener. Because wallpaper loading is asynchronous, `ThemeManager` must default `fixedTextColor`/`wbColor` to non-null (`WHITE`/`BLACK`) — listener constructors can run before any real color exists, and a null `textFill` renders as invisible text with no error. `Configure.getFontScale()` is threaded through `ThemeManager.setFontScale()` and applied in every `-fx-font-size` calculation across controllers.

**Wallpaper loading**: `WallpaperController` lists image files, picks an initial index by time-of-day tag, then loads all wallpapers one at a time on a daemon `Thread`, marshaling each result back via `Platform.runLater`. The *first* image to successfully decode (not necessarily the originally-picked index) becomes the visible/current one — this avoids a permanent-lockup bug where a failed initial decode would leave `switchWallpaper()`'s guard clause blocking forever. New wallpaper `ImageView`s are always inserted at index 0 of the root `StackPane`'s children (`addAll(0, ...)`) so they stay behind clock/weather/alert UI regardless of load order. `onProgress`/`onHalfLoaded` callbacks (set via `ctrl.setWallpaperLoadListener`, which must be called before `ctrl.setClass()`) drive the splash screen and the splash→main-window handoff.

**Tray behavior**: `TrayManager` bridges `java.awt.SystemTray`/`TrayIcon` into JavaFX (`Platform.setImplicitExit(false)` keeps the process alive when the window is hidden). `hideStage()` is the single canonical "minimize to tray" path, used by the window close request, the iconified listener, and the in-app tray-minimize button — don't duplicate hide logic elsewhere. Hiding/showing the stage pauses/resumes `ctrl`'s timelines (`pauseBackgroundUpdates`/`resumeBackgroundUpdates`) rather than leaving them running while the window isn't visible.

**Icon rendering**: SVG icons load via `fxsvgimage` (`SVGLoader`/`SVGImage`), wrapped by `SVGHelper.createIcon(resourcePath, size, color, shadow)` — the shared path used by `ctrl` and `WeatherUIController` for fill+stroke icon coloring. Two library-specific gotchas to know before touching SVG assets in `src/main/resources/icons/svg/`:
- `fxsvgimage` does not understand the CSS `currentColor` keyword (logs "Color currentColor is illegal" per occurrence) — source SVGs must use a literal hex color even though it gets overridden programmatically afterward.
- SVGs authored in Material-Symbols style with `viewBox="0 -960 960 960"` render wrong and must have the viewBox rewritten to `viewBox="0 0 960 960"` (path data itself is left untouched).
- Stroke-only line-art icons (`fill="none"`) are not handled by `SVGHelper.createIcon` (it sets both fill and stroke) — see `SplashScreen.java`'s custom stroke-only recolor method for that case.

## Commit conventions

Commit messages are in Japanese. Trailer: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>` when Claude authored/co-authored the change.
