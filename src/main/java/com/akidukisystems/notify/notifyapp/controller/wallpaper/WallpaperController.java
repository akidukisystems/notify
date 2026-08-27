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
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 壁紙フォルダの画像をバックグラウンドスレッドで読み込み、フェード切替・時間帯タグに応じたランダム選択・
 * 壁紙から抽出したテーマカラーの反映までを担うコントローラー。
 */
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

    /**
     * 壁紙ファイル一覧を取得し、初回表示分をバックグラウンドで読み込み始める。
     *
     * @param rootPane     壁紙のImageViewを追加する親ノード
     * @param configure    壁紙フォルダのパス等を保持する設定
     * @param themeManager テーマカラー反映先
     * @param screenWidth  画面幅(壁紙のフィットサイズ計算に使用)
     * @param screenHeight 画面高さ(同上)
     * @param onProgress   読み込み進捗(読込済み数, 総数)の通知先
     * @param onHalfLoaded 壁紙が半数読み込めた時点で1回だけ呼ばれるコールバック
     */
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
            showNoWallpaperMessage();
            if (onHalfLoaded != null) onHalfLoaded.run();
            return;
        }

        // bgViews/fgViewsは読み込み済みのものだけ埋まる。未読込みはnullのまま
        for (int i = 0; i < wallpaperFiles.size(); i++) {
            bgViews.add(null);
            fgViews.add(null);
        }

        int initialIndex = pickInitialIndex();

        startBackgroundLoading(initialIndex);
    }

    /** 壁紙フォルダが見つからない/空だった場合、設定画面へ誘導するメッセージを表示する。 */
    private void showNoWallpaperMessage() {
        Label message = new Label(
            "壁紙が見つかりませんでした。\n右下の設定アイコンから壁紙フォルダを指定してください。"
        );
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setStyle(
            "-fx-text-fill: white; -fx-font-size: 22px; " +
            "-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 10; -fx-padding: 24;"
        );

        StackPane.setAlignment(message, Pos.CENTER);
        rootPane.getChildren().add(message);
    }

    /** 壁紙フォルダ内の.jpg/.pngファイル名を{@link #wallpaperFiles}に列挙する。 */
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

    /**
     * 壁紙を1枚ずつバックグラウンドスレッドで読み込み、都度{@link Platform#runLater}経由でJavaFXスレッドへ反映する。
     * {@code initialIndex}を最初に読み込むよう順序を組み替える。
     */
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
                Platform.runLater(() -> onWallpaperLoaded(index, loaded));
            }
        }, "wallpaper-loader");

        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private boolean initialShown = false;

    /**
     * 1枚分の壁紙読み込み完了(またはデコード失敗)をFXスレッド上で処理する。
     * まだ画面に何も表示していなければ、この壁紙を初期表示として採用する。
     */
    private void onWallpaperLoaded(int index, Image image) {
        loadedCount++;

        if (image != null) {
            // 最初に選んだ壁紙の読み込みに失敗していた場合は、最初に読み込めた壁紙を代わりに使う
            boolean showAsInitial = !initialShown;

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

            bgView.setOpacity(showAsInitial ? 1.0 : 0.0);
            fgView.setOpacity(showAsInitial ? 1.0 : 0.0);

            bgViews.set(index, bgView);
            fgViews.set(index, fgView);

            // 常に一番奥(index 0)へ挿入し、後から読み込まれた壁紙が時計・天気UIを覆わないようにする
            rootPane.getChildren().addAll(0, java.util.List.of(bgView, fgView));

            if (showAsInitial) {
                initialShown = true;
                currentWallpaperIndex = index;
                applyInitialColors();
            }
        }

        updateLoadingProgress();
    }

    /** 読み込み進捗を{@link #onProgress}へ通知し、半数に達した時点で1回だけ{@link #onHalfLoaded}を呼ぶ。 */
    private void updateLoadingProgress() {
        int total = wallpaperFiles.size();

        if (onProgress != null) onProgress.accept(loadedCount, total);

        if (!halfLoadedNotified && loadedCount >= (total + 1) / 2) {
            halfLoadedNotified = true;
            if (onHalfLoaded != null) onHalfLoaded.run();
        }
    }

    /** 現在の壁紙のスナップショット(またはメタデータ指定のテーマカラー)から、初期テーマカラーを{@link ThemeManager}へ反映する。 */
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

    /**
     * 現在時刻のタグに合う、読み込み済みの壁紙の中からランダムに1枚選んでフェード切替する。
     * 切替完了後、新しい壁紙のスナップショットからテーマカラーを再計算して反映する。
     */
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

    /** 現在時刻のタグに合う壁紙の中から、最初に読み込み・表示する1枚をランダムに選ぶ(まだ読み込みは行わない)。 */
    private int pickInitialIndex() {
        String tag = getSimplifiedTimeTag();

        List<String> candidates = getWallpapersByTag(tag);
        if (candidates.isEmpty()) {
            candidates = wallpaperFiles;
        }

        String chosen = candidates.get(new Random().nextInt(candidates.size()));
        return wallpaperFiles.indexOf(chosen);
    }

    /** 指定タグを持つ壁紙(タグ未設定の壁紙も含む)のファイル名一覧を返す。 */
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

    /** {@link #getTimeTag()}の結果を、深夜帯(AM/PM)は"midnight"にまとめて返す。 */
    private String getSimplifiedTimeTag() {
        String tag = getTimeTag();
        if (tag.contains("midnight")) tag = "midnight";
        return tag;
    }

    /** 現在時刻から時間帯タグ(morning/day/evening/night/midnightAM/midnightPM)を求める。 */
    private String getTimeTag() {
        int hour = LocalDateTime.now().getHour();

        if (hour < 5) return "midnightAM";
        if (hour >= 5 && hour < 10) return "morning";
        if (hour >= 10 && hour < 17) return "day";
        if (hour >= 17 && hour < 20) return "evening";
        if (hour >= 20 && hour < 22) return "night";
        return "midnightPM";
    }

    /** 現在時刻が「今日の予報がもう終わった時間帯(夕方以降)」かどうかを返す。 */
    public boolean isPastedDay() {
        String tag = getTimeTag();
        return "evening".equals(tag) || "night".equals(tag) || "midnightPM".equals(tag);
    }
}
