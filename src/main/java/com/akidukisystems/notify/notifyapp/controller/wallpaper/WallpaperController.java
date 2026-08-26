package com.akidukisystems.notify.notifyapp.controller.wallpaper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.akidukisystems.notify.notifyapp.ColorHelper;
import com.akidukisystems.notify.notifyapp.Configure;
import com.akidukisystems.notify.notifyapp.Configure.WallpaperColor;
import com.akidukisystems.notify.notifyapp.controller.theme.ThemeManager;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class WallpaperController {

    private final Configure configure;
    private final ThemeManager themeManager;
    private final ColorHelper colorHelper = new ColorHelper();
    private final StackPane rootPane;
    private final double screenWidth;
    private final double screenHeight;

    private final List<ImageView> bgViews = new ArrayList<>();
    private final List<ImageView> fgViews = new ArrayList<>();
    private final List<String> wallpaperFiles = new ArrayList<>();

    private int currentWallpaperIndex = -1;
    private boolean isSwitchingWallpaper = false;

    private int loadedCount = 0;
    private boolean halfLoadedNotified = false;

    // onProgress: (読込済み数, 総数) を都度通知。onHalfLoaded: 半数読み込めた時点で1回だけ呼ばれる
    private final BiConsumer<Integer, Integer> onProgress;
    private final Runnable onHalfLoaded;

    public WallpaperController(StackPane rootPane, Configure configure, ThemeManager themeManager,
                                double screenWidth, double screenHeight,
                                BiConsumer<Integer, Integer> onProgress, Runnable onHalfLoaded) {
        this.rootPane = rootPane;
        this.configure = configure;
        this.themeManager = themeManager;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.onProgress = onProgress;
        this.onHalfLoaded = onHalfLoaded;

        listWallpaperFiles();

        if (wallpaperFiles.isEmpty()) {
            if (onHalfLoaded != null) onHalfLoaded.run();
            return;
        }

        // bgViews/fgViewsは読み込み済みのものだけ埋まる。未読込みはnullのまま
        for (int i = 0; i < wallpaperFiles.size(); i++) {
            bgViews.add(null);
            fgViews.add(null);
        }

        int initialIndex = pickInitialIndex();
        currentWallpaperIndex = initialIndex;

        startBackgroundLoading(initialIndex);
    }

    private void listWallpaperFiles() {
        Path dir = Paths.get(configure.getWallpaperPath());

        try (Stream<Path> stream = Files.list(dir)) {
            stream
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".png");
                })
                .forEach(p -> wallpaperFiles.add(p.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 壁紙を1枚ずつバックグラウンドスレッドで読み込み、都度JavaFXスレッドへ反映する
    private void startBackgroundLoading(int initialIndex) {
        List<Integer> order = new ArrayList<>();
        order.add(initialIndex);
        for (int i = 0; i < wallpaperFiles.size(); i++) {
            if (i != initialIndex) order.add(i);
        }

        Thread loaderThread = new Thread(() -> {
            for (int index : order) {
                String fileName = wallpaperFiles.get(index);
                String uri = Paths.get(configure.getWallpaperPath(), fileName).toUri().toString();

                Image image = null;
                try {
                    image = new Image(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Image loaded = image;
                Platform.runLater(() -> onWallpaperLoaded(index, loaded, index == initialIndex));
            }
        }, "wallpaper-loader");

        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void onWallpaperLoaded(int index, Image image, boolean isInitial) {
        loadedCount++;

        if (image != null) {
            double imageRatio = image.getWidth() / image.getHeight();
            double screenRatio = screenWidth / screenHeight;
            double scaleFactor = 1.10;

            ImageView bgView = new ImageView(image);
            GaussianBlur blur = new GaussianBlur(configure.getBlur());
            ColorAdjust darken = new ColorAdjust();
            darken.setBrightness(-0.1);
            blur.setInput(darken);
            bgView.setEffect(blur);

            if (imageRatio > screenRatio) {
                bgView.setFitHeight(screenHeight * scaleFactor);
                bgView.setFitWidth(screenHeight * imageRatio * scaleFactor);
            } else {
                bgView.setFitWidth(screenWidth * scaleFactor);
                bgView.setFitHeight(screenWidth / imageRatio * scaleFactor);
            }

            ImageView fgView = new ImageView(image);
            fgView.setPreserveRatio(true);
            if (imageRatio > screenRatio) fgView.setFitWidth(screenWidth);
            else fgView.setFitHeight(screenHeight);

            bgView.setOpacity(isInitial ? 1.0 : 0.0);
            fgView.setOpacity(isInitial ? 1.0 : 0.0);

            bgViews.set(index, bgView);
            fgViews.set(index, fgView);

            // 常に一番奥(index 0)へ挿入し、後から読み込まれた壁紙が時計・天気UIを覆わないようにする
            rootPane.getChildren().addAll(0, java.util.List.of(bgView, fgView));

            if (isInitial) {
                applyInitialColors();
            }
        }

        updateLoadingProgress();
    }

    private void updateLoadingProgress() {
        int total = wallpaperFiles.size();

        if (onProgress != null) onProgress.accept(loadedCount, total);

        if (!halfLoadedNotified && loadedCount >= (total + 1) / 2) {
            halfLoadedNotified = true;
            if (onHalfLoaded != null) onHalfLoaded.run();
        }
    }

    private void applyInitialColors() {
        WritableImage snapshot = fgViews.get(currentWallpaperIndex).snapshot(new SnapshotParameters(), null);
        String currentWallpaperFileName = wallpaperFiles.get(currentWallpaperIndex);
        WallpaperColor userColors = configure.getColors(currentWallpaperFileName);

        Color textColor;
        Color wbColor;

        if (userColors != null && userColors.getMainColor() != null && userColors.getMonoColor() != null) {
            textColor = userColors.getMainColor();
            wbColor = userColors.getMonoColor();
        } else {
            Color[] colors = colorHelper.calculateColors(snapshot);
            textColor = colors[2];
            wbColor = colors[0];
        }

        // 非同期読み込みのため、Clock/Weather/Battery等のリスナーは既に登録済み。applyColorsで確実に通知する
        themeManager.applyColors(textColor, wbColor);
    }

    public void switchWallpaper() {
        if (currentWallpaperIndex == -1 || fgViews.get(currentWallpaperIndex) == null) return;
        if (isSwitchingWallpaper) return;

        List<String> loadedFiles = new ArrayList<>();
        for (int i = 0; i < wallpaperFiles.size(); i++) {
            if (bgViews.get(i) != null) loadedFiles.add(wallpaperFiles.get(i));
        }
        if (loadedFiles.size() <= 1) return;

        isSwitchingWallpaper = true;

        String tag = getSimplifiedTimeTag();

        List<String> filtered = getWallpapersByTag(tag);
        filtered.retainAll(loadedFiles);

        if (filtered.isEmpty()) {
            filtered = new ArrayList<>(loadedFiles);
        }

        if (filtered.size() > 1) {
            filtered.remove(wallpaperFiles.get(currentWallpaperIndex));
        }

        Random rand = new Random();
        String nextFile = filtered.get(rand.nextInt(filtered.size()));

        int nextIndex = wallpaperFiles.indexOf(nextFile);
        if (nextIndex == -1) {
            isSwitchingWallpaper = false;
            return;
        }

        int fadeAnimationTime = 2;

        ImageView currentfg = fgViews.get(currentWallpaperIndex);
        ImageView nextfg = fgViews.get(nextIndex);
        ImageView currentbg = bgViews.get(currentWallpaperIndex);
        ImageView nextbg = bgViews.get(nextIndex);

        FadeTransition fadeOutfg = new FadeTransition(Duration.seconds(fadeAnimationTime), currentfg);
        fadeOutfg.setFromValue(1);
        fadeOutfg.setToValue(0);

        FadeTransition fadeInfg = new FadeTransition(Duration.seconds(fadeAnimationTime), nextfg);
        fadeInfg.setFromValue(0);
        fadeInfg.setToValue(1);

        FadeTransition fadeOutbg = new FadeTransition(Duration.seconds(fadeAnimationTime), currentbg);
        fadeOutbg.setFromValue(1);
        fadeOutbg.setToValue(0);

        FadeTransition fadeInbg = new FadeTransition(Duration.seconds(fadeAnimationTime), nextbg);
        fadeInbg.setFromValue(0);
        fadeInbg.setToValue(1);

        fadeOutfg.setOnFinished(e -> {
            fgViews.get(currentWallpaperIndex).setOpacity(0.0);
            bgViews.get(currentWallpaperIndex).setOpacity(0.0);

            currentWallpaperIndex = nextIndex;

            fgViews.get(currentWallpaperIndex).setOpacity(1.0);
            bgViews.get(currentWallpaperIndex).setOpacity(1.0);

            WritableImage snapshot = fgViews.get(currentWallpaperIndex).snapshot(new SnapshotParameters(), null);

            String currentWallpaperFileName = wallpaperFiles.get(currentWallpaperIndex);
            WallpaperColor userColors = configure.getColors(currentWallpaperFileName);

            Color textColor;
            Color wbColor;

            if (userColors != null && userColors.getMainColor() != null && userColors.getMonoColor() != null) {
                textColor = userColors.getMainColor();
                wbColor = userColors.getMonoColor();
            } else {
                Color[] colors = colorHelper.calculateColors(snapshot);
                textColor = colors[2];
                wbColor = colors[0];
            }

            // ★ここが今までの二重更新を解消するポイント。
            // themeManagerに1回通知するだけで、Clock/Weather/Battery全てのリスナーに一括反映される
            themeManager.applyColors(textColor, wbColor);

            isSwitchingWallpaper = false;
        });

        fadeOutfg.play();
        fadeOutbg.play();
        fadeInbg.play();
        fadeInfg.play();
    }

    private int pickInitialIndex() {
        String tag = getSimplifiedTimeTag();

        List<String> candidates = getWallpapersByTag(tag);
        if (candidates.isEmpty()) {
            candidates = wallpaperFiles;
        }

        String chosen = candidates.get(new Random().nextInt(candidates.size()));
        return wallpaperFiles.indexOf(chosen);
    }

    private List<String> getWallpapersByTag(String tag) {
        List<String> result = new ArrayList<>();

        for (String file : wallpaperFiles) {
            WallpaperColor wc = configure.getColors(file);

            if (wc == null) {
                result.add(file);
                continue;
            }

            if (wc.getTags().contains(tag)) {
                result.add(file);
            }
        }

        return result;
    }

    private String getSimplifiedTimeTag() {
        String tag = getTimeTag();
        if (tag.contains("midnight")) tag = "midnight";
        return tag;
    }

    private String getTimeTag() {
        int hour = LocalDateTime.now().getHour();

        if (hour < 5) return "midnightAM";
        if (hour >= 5 && hour < 10) return "morning";
        if (hour >= 10 && hour < 17) return "day";
        if (hour >= 17 && hour < 20) return "evening";
        if (hour >= 20 && hour < 22) return "night";
        return "midnightPM";
    }

    public boolean isPastedDay() {
        String tag = getTimeTag();
        return "evening".equals(tag) || "night".equals(tag) || "midnightPM".equals(tag);
    }
}
