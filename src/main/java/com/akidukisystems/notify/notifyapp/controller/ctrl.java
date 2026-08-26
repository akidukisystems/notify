package com.akidukisystems.notify.notifyapp.controller;

import java.util.LinkedList;
import java.util.Queue;
import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

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

public class ctrl {

    @FXML
    private StackPane rootPane;

    private Weather weather;
    private Configure configure;
    private Animation animation;

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

    // GUI から親をセット
    public void setClass(Weather weather, Configure configure) {
        this.weather = weather;
        this.configure = configure;
        init();
    }

    private void init() {
        double screenWidth = 1920;
        double screenHeight = 1080;

        configure.loadWallpaperColors();

        animation = new Animation();

        themeManager = new ThemeManager();
        wallpaperController = new WallpaperController(rootPane, configure, themeManager, screenWidth, screenHeight);

        clockController = new ClockController(themeManager);
        clockController.start();

        weatherUIController = new WeatherUIController(themeManager, weather, this::scrollMessage, wallpaperController::isPastedDay);
        alertUIController = new AlertUIController(weather, this::scrollMessage);
        batteryUIController = new BatteryUIController(themeManager, configure.getMouseBatteryDeviceId());

        // スクロール
        tickerBox = new HBox();
        tickerBox.setAlignment(Pos.CENTER_LEFT);
        tickerBox.setStyle("-fx-background-color: rgba(0,0,0,0.5);"); // 背景半透明
        tickerBox.setPrefHeight(40);

        tickerLabel = new Label("文字スクロールテストABCDEabcdeＡＢＣＤＥａｂｃｄｅ012345０１２３４５あいうえお");
        tickerLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
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
            GUI.stage.hide();
            pauseBackgroundUpdates();
        });

        buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(batteryUIController.getNode(), changeWallpaperButton, changeNightModeButton, setFullScreenButton, configButton, hideToTrayButton);


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

    // トレイ格納中は時計・壁紙更新・天気更新・UI反転を止める(バッテリー取得は天気更新に連動)
    public void pauseBackgroundUpdates() {
        clockController.pause();
        wallpaperTimeline.pause();
        refreshWeatherTimeline.pause();
        reverseUITimeline.pause();
    }

    public void resumeBackgroundUpdates() {
        clockController.resume();
        wallpaperTimeline.play();
        refreshWeatherTimeline.play();
        reverseUITimeline.play();

        // 格納中に古くなっている可能性があるため、天気・警報・バッテリーは復元時に即時更新する
        // (updateWeatherUIはラベル/アイコンを直接差し替えるだけでアニメーションは挟まない)
        updateWeatherUI();
    }

    // UI 更新処理
    private void updateWeatherUI() {
        weather.fetch()
            .thenRun(() -> {
                Platform.runLater(() -> {
                    weatherUIController.update();
                    alertUIController.update();
                    batteryUIController.refresh();
                });
            });
    }

    // VBox作る用
    private VBox createVBox(int spacing, Node... children) {
        VBox box = new VBox(spacing, children);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // スクロール表示メッセージの追加
    private void scrollMessage(String message) {
        
        // messageQueue.add(message);

        // if (!isScrolling) {
        //     playNextMessage();
        // }
    }
    // メッセージ表示
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

    // ナイトモード
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

    // ナイトモードか知る
    private boolean isNightMode() {
        return !clockController.getSecBox().isVisible();
    }

    private void setFullScreen(boolean isFullscreen) {
        GUI.stage.setFullScreen(isFullscreen);
    }

    // UIを左右反転させる
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

    private SVGIcon createIcon(String path, double size, Color color) {
        SVGImage svg = SVGLoader.load(getClass().getResource(path));
        SVGImage scaled = svg.scaleTo(size);
        SVGIcon icon = new SVGIcon(scaled, size);
        icon.setColor(color);
        icon.setEffect(themeManager.getShadow());
        return icon;
    }
}