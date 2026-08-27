package com.akidukisystems.notify.notifyapp.controller;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;

import com.akidukisystems.notify.notifyapp.Configure;
import com.akidukisystems.notify.notifyapp.GUI;
import com.akidukisystems.notify.notifyapp.Weather;
import com.akidukisystems.notify.notifyapp.controller.alert.AlertUIController;
import com.akidukisystems.notify.notifyapp.controller.battery.BatteryUIController;
import com.akidukisystems.notify.notifyapp.controller.clock.ClockController;
import com.akidukisystems.notify.notifyapp.controller.settings.SettingsController;
import com.akidukisystems.notify.notifyapp.controller.theme.ThemeManager;
import com.akidukisystems.notify.notifyapp.controller.wallpaper.WallpaperController;
import com.akidukisystems.notify.notifyapp.controller.weather.WeatherUIController;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * メイン画面(FXML: main.fxml)のコントローラー。時計・天気・警報・壁紙・バッテリーなど各機能コントローラーを
 * 組み立て、ボタン操作やタイマーによる定期更新、トレイ格納時の一時停止/再開などアプリ全体の配線を担う。
 */
public class ctrl {

    @FXML
    private StackPane rootPane;

    private Weather weather;
    private Configure configure;
    private Animation animation;
    private final SVGHelper svgHelper = new SVGHelper();

    private HBox buttonsBox;
    private Label tickerLabel;
    private HBox tickerBox;
    private VBox LeftTopBox;
    
    private Queue<String> messageQueue = new LinkedList<>();
    private boolean isScrolling = false;

    private ThemeManager themeManager;
    private ClockController clockController;
    private BatteryUIController batteryUIController;
    private WeatherUIController weatherUIController;
    private AlertUIController alertUIController;
    private WallpaperController wallpaperController;

    private Timeline wallpaperTimeline;
    private Timeline refreshWeatherTimeline;
    private Timeline reverseUITimeline;

    // バッテリー残量取得はPowerShell経由のためWindows専用。他OSでは表示・取得ともに行わない
    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase().contains("win");

    private BiConsumer<Integer, Integer> wallpaperProgressListener;
    private Runnable wallpaperReadyListener;

    /**
     * 壁紙読み込みの進捗コールバックを登録する。GUI(スプラッシュ画面)から{@link #setClass}より前に呼ぶ必要がある。
     *
     * @param onProgress 読み込み進捗(読込済み数, 総数)の通知先
     * @param onReady    壁紙が半数読み込めた時点で1回だけ呼ばれるコールバック
     */
    public void setWallpaperLoadListener(BiConsumer<Integer, Integer> onProgress, Runnable onReady) {
        this.wallpaperProgressListener = onProgress;
        this.wallpaperReadyListener = onReady;
    }

    /** {@link GUI}から{@link Weather}/{@link Configure}を受け取り、画面全体を初期化する。 */
    public void setClass(Weather weather, Configure configure) {
        this.weather = weather;
        this.configure = configure;
        init();
    }

    /** 各機能コントローラーの構築、ボタン配線、定期更新タイマーの起動など画面全体の初期化を行う。 */
    private void init() {
        double screenWidth = 1920;
        double screenHeight = 1080;

        configure.loadWallpaperColors();

        animation = new Animation();

        themeManager = new ThemeManager();
        themeManager.setFontScale(configure.getFontScale());
        wallpaperController = new WallpaperController(rootPane, configure, themeManager, screenWidth, screenHeight,
            wallpaperProgressListener, wallpaperReadyListener);

        clockController = new ClockController(themeManager);
        clockController.start();

        weatherUIController = new WeatherUIController(themeManager, weather, this::scrollMessage, wallpaperController::isPastedDay);
        alertUIController = new AlertUIController(weather, this::scrollMessage, configure.getFontScale());
        batteryUIController = new BatteryUIController(themeManager, configure.getMouseBatteryDeviceId());

        // スクロール
        tickerBox = new HBox();
        tickerBox.setAlignment(Pos.CENTER_LEFT);
        tickerBox.setStyle("-fx-background-color: rgba(0,0,0,0.5);"); // 背景半透明
        tickerBox.setPrefHeight(40);

        tickerLabel = new Label("文字スクロールテストABCDEabcdeＡＢＣＤＥａｂｃｄｅ012345０１２３４５あいうえお");
        tickerLabel.setStyle("-fx-font-size: " + (24 * configure.getFontScale()) + "px; -fx-text-fill: white;");
        tickerBox.getChildren().add(tickerLabel);
        tickerBox.setVisible(false);

        // ボックス
        LeftTopBox = createVBox(30, clockController.getDateLabel(), clockController.getTimeBox(),
            weatherUIController.getLine1Box(), weatherUIController.getLine2Box(),
            weatherUIController.getForecastsBox(), alertUIController.getNode());

        // 壁紙更新ボタン
        Button changeWallpaperButton = new Button();
        changeWallpaperButton.setGraphic(createIcon("/icons/svg/refresh.svg", 20, Color.WHITE));
        changeWallpaperButton.setPrefWidth(20);
        changeWallpaperButton.setPrefHeight(20);
        changeWallpaperButton.setOnAction(e -> {
            wallpaperController.switchWallpaper();
        });

        Button changeNightModeButton = new Button();
        changeNightModeButton.setGraphic(createIcon("/icons/svg/routine.svg", 20, Color.WHITE));
        changeNightModeButton.setPrefWidth(20);
        changeNightModeButton.setPrefHeight(20);
        changeNightModeButton.setOnAction(e -> {
            setNightMode(!isNightMode());
        });

        Button setFullScreenButton = new Button();
        setFullScreenButton.setGraphic(createIcon("/icons/svg/fs.svg", 20, Color.WHITE));
        setFullScreenButton.setPrefWidth(20);
        setFullScreenButton.setPrefHeight(20);
        setFullScreenButton.setOnAction(e -> {
            setFullScreen(!GUI.stage.isFullScreen());
        });

        // GUI.stageはcontroller初期化完了後に設定されるため、次のパルスまで待って購読する
        Platform.runLater(() -> {
            GUI.stage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) -> {
                String iconPath = isFullScreen ? "/icons/svg/fullscreen_exit.svg" : "/icons/svg/fs.svg";
                setFullScreenButton.setGraphic(createIcon(iconPath, 20, Color.WHITE));
            });
        });

        Button configButton = new Button();
        configButton.setGraphic(createIcon("/icons/svg/settings.svg", 20, Color.WHITE));
        configButton.setPrefWidth(20);
        configButton.setPrefHeight(20);
        configButton.setOnAction(e -> {
            new SettingsController(configure).show();
        });

        Button hideToTrayButton = new Button();
        hideToTrayButton.setGraphic(createIcon("/icons/svg/minimize.svg", 20, Color.WHITE));
        hideToTrayButton.setPrefWidth(20);
        hideToTrayButton.setPrefHeight(20);
        hideToTrayButton.setOnAction(e -> {
            GUI.trayManager.hideStage();
        });

        buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        if (IS_WINDOWS) {
            buttonsBox.getChildren().add(batteryUIController.getNode());
        }
        buttonsBox.getChildren().addAll(changeWallpaperButton, changeNightModeButton, setFullScreenButton, configButton, hideToTrayButton);


        // 左上にくっつける
        AnchorPane clockPane = new AnchorPane(LeftTopBox, tickerBox, buttonsBox);

        AnchorPane.setTopAnchor(LeftTopBox, 20.0);
        AnchorPane.setRightAnchor(LeftTopBox, null);
        AnchorPane.setLeftAnchor(LeftTopBox, 20.0);

        AnchorPane.setBottomAnchor(tickerBox, 0.0);
        AnchorPane.setLeftAnchor(tickerBox, 0.0);
        AnchorPane.setRightAnchor(tickerBox, 0.0);

        AnchorPane.setRightAnchor(buttonsBox, 0.0);
        AnchorPane.setBottomAnchor(buttonsBox, 0.0);

        rootPane.getChildren().add(clockPane);

        // 壁紙切替
        wallpaperTimeline = new Timeline(
            new KeyFrame(Duration.seconds(configure.getWallpaperChangeSecond()), e -> wallpaperController.switchWallpaper())
        );
        wallpaperTimeline.setCycleCount(Timeline.INDEFINITE);
        wallpaperTimeline.play();

        System.out.println(configure.getRefreshWeatherSecond());

        // お天気更新
        refreshWeatherTimeline = new Timeline(
            new KeyFrame(Duration.seconds(configure.getRefreshWeatherSecond()), e -> updateWeatherUI())
        );
        refreshWeatherTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshWeatherTimeline.play();

        // UI左右反転
        reverseUITimeline = new Timeline(
            new KeyFrame(Duration.seconds(configure.getReverseUISecond()), e -> reverseUI())
        );
        reverseUITimeline.setCycleCount(Timeline.INDEFINITE);
        reverseUITimeline.play();

        // 初回更新
        updateWeatherUI();
    }

    /** トレイ格納中は時計・壁紙更新・天気更新・UI反転を止める(バッテリー取得は天気更新に連動)。 */
    public void pauseBackgroundUpdates() {
        clockController.pause();
        wallpaperTimeline.pause();
        refreshWeatherTimeline.pause();
        reverseUITimeline.pause();
    }

    /** 停止していた各種更新を再開し、更新周期を超えて古くなっていれば天気・警報・バッテリーを即時更新する。 */
    public void resumeBackgroundUpdates() {
        clockController.resume();
        wallpaperTimeline.play();
        refreshWeatherTimeline.play();
        reverseUITimeline.play();

        // 格納中に更新周期を超えて古くなっている場合のみ、天気・警報・バッテリーを即時更新する
        // (updateWeatherUIはラベル/アイコンを直接差し替えるだけでアニメーションは挟まない)
        long intervalMillis = configure.getRefreshWeatherSecond() * 1000L;
        if (System.currentTimeMillis() - lastWeatherFetchMillis >= intervalMillis) {
            updateWeatherUI();
        }
    }

    private long lastWeatherFetchMillis = 0;

    /** 天気を非同期取得し、完了後に天気・警報・バッテリー(Windowsのみ)の各UIを更新する。 */
    private void updateWeatherUI() {
        lastWeatherFetchMillis = System.currentTimeMillis();

        weather.fetch()
            .thenRun(() -> {
                Platform.runLater(() -> {
                    weatherUIController.update();
                    alertUIController.update();
                    if (IS_WINDOWS) {
                        batteryUIController.refresh();
                    }
                });
            });
    }

    /** 中央揃えのVBoxを生成する。 */
    private VBox createVBox(int spacing, Node... children) {
        VBox box = new VBox(spacing, children);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /**
     * スクロールメッセージをキューに追加する(現在は無効化されており、実質何もしない)。
     * 各コントローラーからのメッセージ通知の受け口として渡されている。
     */
    private void scrollMessage(String message) {

        // messageQueue.add(message);

        // if (!isScrolling) {
        //     playNextMessage();
        // }
    }

    /** キューにたまったメッセージを順番に画面下部で右から左へスクロール表示する。 */
    private void playNextMessage() {
        if (messageQueue.isEmpty()) {
            isScrolling = false;
            tickerBox.setVisible(false);
            tickerLabel.setTranslateX(0);
            return;
        }

        isScrolling = true;
        String message = messageQueue.poll();

        Platform.runLater(() -> {
            // まだ表示しない
            tickerLabel.setVisible(false);

            tickerLabel.setText(message);

            // CSS・レイアウトを反映
            tickerLabel.applyCss();
            tickerLabel.layout();

            double labelWidth = tickerLabel.getLayoutBounds().getWidth();

            double startX = rootPane.getWidth() + 50.0;
            double endX = -labelWidth - 500.0;

            // メッセージ長に応じてスクロール時間を調整
            double baseDuration = 30.0;
            double duration = baseDuration * (labelWidth / 1000.0);
            duration = Math.max(20.0, duration);

            // 表示前に開始位置へ
            tickerLabel.setTranslateX(startX);

            // 夜間モードでなければ表示
            if (!isNightMode()) {
                tickerBox.setVisible(true);
                tickerLabel.setVisible(true);
            }

            TranslateTransition scroll =
                    new TranslateTransition(
                            Duration.seconds(duration),
                            tickerLabel);

            scroll.setFromX(startX);
            scroll.setToX(endX);
            scroll.setInterpolator(Interpolator.LINEAR);

            scroll.setOnFinished(e -> {
                // メッセージだけ隠す
                tickerLabel.setVisible(false);

                // 位置をリセット
                tickerLabel.setTranslateX(0);

                // 次のメッセージ
                playNextMessage();
            });

            scroll.play();
        });
    }

    /** 画面全体を暗くし、秒表示をフェードアウトさせる(解除時は逆にフェードイン)。 */
    private void setNightMode(boolean isNight) {
        if(isNight) {
            ColorAdjust darken = new ColorAdjust();
            darken.setBrightness(-0.75);
            rootPane.setEffect(darken);

            if(isScrolling)
                tickerBox.setVisible(false);

            animation.fadeOut(clockController.getSecBox(), false);
        }
        else
        {
            rootPane.setEffect(null);

            if(isScrolling)
                tickerBox.setVisible(true);

            animation.fadeIn(clockController.getSecBox());
        }
    }

    /** 現在ナイトモード(秒表示が非表示)かどうかを返す。 */
    private boolean isNightMode() {
        return !clockController.getSecBox().isVisible();
    }

    /** ウィンドウのフルスクリーン状態を切り替える。 */
    private void setFullScreen(boolean isFullscreen) {
        GUI.stage.setFullScreen(isFullscreen);
    }

    /** 焼き付き防止のため、時計・天気パネルの表示位置を左右にフェード切替する。 */
    private void reverseUI() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), LeftTopBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            if(AnchorPane.getLeftAnchor(LeftTopBox) == null)
            {
                AnchorPane.setRightAnchor(LeftTopBox, null);
                AnchorPane.setLeftAnchor(LeftTopBox, 20.0);
            }
            else
            {
                AnchorPane.setRightAnchor(LeftTopBox, 20.0);
                AnchorPane.setLeftAnchor(LeftTopBox, null);
            }

            FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), LeftTopBox);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    /** 指定した色を適用したSVGアイコンを生成する(主にボタングラフィック用)。 */
    private SVGIcon createIcon(String path, double size, Color color) {
        return svgHelper.createIcon(path, size, color, themeManager.getShadow());
    }
}