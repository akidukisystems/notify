package com.akidukisystems.notify.notifyapp.controller.wallpaper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import com.akidukisystems.notify.notifyapp.ColorHelper;
import com.akidukisystems.notify.notifyapp.Configure;
import com.akidukisystems.notify.notifyapp.Configure.WallpaperColor;
import com.akidukisystems.notify.notifyapp.controller.theme.ThemeManager;

import javafx.animation.FadeTransition;
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

    private final List<ImageView> bgViews = new ArrayList<>();
    private final List<ImageView> fgViews = new ArrayList<>();
    private final List<String> wallpaperFiles = new ArrayList<>();

    private int currentWallpaperIndex = 0;
    private boolean isSwitchingWallpaper = false;

    public WallpaperController(StackPane rootPane, Configure configure, ThemeManager themeManager,
                                double screenWidth, double screenHeight) {
        this.configure = configure;
        this.themeManager = themeManager;

        loadWallpapers(rootPane, screenWidth, screenHeight);
        applyInitialColors();
    }

    private void loadWallpapers(StackPane rootPane, double screenWidth, double screenHeight) {
        List<String> paths = new ArrayList<>();
        Path dir = Paths.get(configure.getWallpaperPath());

        try (Stream<Path> stream = Files.list(dir)) {
            stream
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".png");
                })
                .forEach(p -> {
                    wallpaperFiles.add(p.getFileName().toString());
                    paths.add(p.toUri().toString());
                });
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String path : paths) {
            Image image = new Image(path);
            ImageView bgView = new ImageView(image);
            bgViews.add(bgView);

            GaussianBlur blur = new GaussianBlur(configure.getBlur());
            ColorAdjust darken = new ColorAdjust();
            darken.setBrightness(-0.1);
            blur.setInput(darken);
            bgView.setEffect(blur);

            double imageRatio = image.getWidth() / image.getHeight();
            double screenRatio = screenWidth / screenHeight;
            double scaleFactor = 1.10;

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

            fgView.setOpacity(0);
            bgView.setOpacity(0);
            fgViews.add(fgView);
            rootPane.getChildren().addAll(bgView, fgView);
        }

        fgViews.get(currentWallpaperIndex).setOpacity(1.0);
        bgViews.get(currentWallpaperIndex).setOpacity(1.0);
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

        themeManager.initColors(textColor, wbColor);
    }

    public void switchWallpaper() {
        if (fgViews.isEmpty()) return;
        if (isSwitchingWallpaper) return;

        isSwitchingWallpaper = true;

        String tag = getTimeTag();
        if (tag.contains("midnight")) tag = "midnight";

        List<String> filtered = getWallpapersByTag(tag);
        filtered.removeIf(f -> !wallpaperFiles.contains(f));

        if (filtered.isEmpty()) {
            filtered = wallpaperFiles;
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