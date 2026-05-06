package com.akidukisystems.notify.notifyapp.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Stream;

import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import com.akidukisystems.notify.notifyapp.ColorHelper;
import com.akidukisystems.notify.notifyapp.Configure;
import com.akidukisystems.notify.notifyapp.GUI;
import com.akidukisystems.notify.notifyapp.Weather;
import com.akidukisystems.notify.notifyapp.Configure.WallpaperColor;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class ctrl {

    @FXML
    private StackPane rootPane;

    private Label dateLabel;

    private Label[] hourMinLabels = new Label[5]; // "HH:mm"
    private Label[] secLabels = new Label[2];     // "ss"

    private Label weatherTempLabel;
    private Label weatherFeelingTempLabel;
    private Label weatherHumidityLabel;
    private Label weatherPressureLabel;
    private Label weatherVisibilityLabel;
    private Label weatherWindSpeedLabel;
    private Label currentWeatherDetailLabel;

    private Weather weather;
    private Configure configure;
    private Animation animation;

    private ImageView iconView;

    private Rotate rotate;

    private GridPane alertGridPane;

    private int currentWallpaperIndex = 0;

    private List<ImageView> bgViews; // 背景ぼかし用
    private List<ImageView> fgViews; // 壁紙用
    private List<String> wallpaperFiles;
    private boolean isSwitchingWallpaper = false;

    private ColorHelper colorHelper;
    private Color fixedTextColor;
    private Color wbColor;
    private DropShadow ds;

    private Label tickerLabel;
    private HBox tickerBox;
    private HBox secBox;
    private VBox LeftTopBox;
    
    private Queue<String> messageQueue = new LinkedList<>();
    private boolean isScrolling = false;

    private Polygon windArrow;
    private SVGIcon weatherTempIcon;
    private SVGIcon weatherFeelingTempIcon;
    private SVGIcon weatherHumidityIcon;

    private List<Label> Labels;
    private List<WeatherForecastUI> forecasts;
    private String[] dayString = {"今日", "明日", "明後日", "3日後", "4日後", "5日後"};

    // GUI から親をセット
    public void setClass(Weather weather, Configure configure) {
        this.weather = weather;
        this.configure = configure;
        init();
    }

    private void init() {
        double screenWidth = 1920;
        double screenHeight = 1080;

        colorHelper = new ColorHelper();
        animation = new Animation();

        configure.loadWallpaperColors();
        configure.loadSettings();

        startClock();

        bgViews = new ArrayList<>(); // 背景ぼかし用
        fgViews = new ArrayList<>(); // 壁紙用
        wallpaperFiles = new ArrayList<>();

        wallpaperFiles = new ArrayList<>();

        List<String> paths = new ArrayList<>();

        // 壁紙パスを取得
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
            double scaleFactor = 1.10; // 10%ほど拡大

            if(imageRatio > screenRatio) {
                bgView.setFitHeight(screenHeight * scaleFactor);
                bgView.setFitWidth(screenHeight * imageRatio * scaleFactor);
            } else {
                bgView.setFitWidth(screenWidth * scaleFactor);
                bgView.setFitHeight(screenWidth / imageRatio * scaleFactor);
            }

            ImageView fgView = new ImageView(image);
            fgView.setPreserveRatio(true);

            if(imageRatio > screenRatio) fgView.setFitWidth(screenWidth);
            else fgView.setFitHeight(screenHeight);

            fgView.setOpacity(0); // 最初は透明にしておく
            bgView.setOpacity(0); // 最初は透明にしておく
            fgViews.add(fgView);
            rootPane.getChildren().addAll(bgView, fgView); // StackPaneに積む
        }

        fgViews.get(currentWallpaperIndex).setOpacity(1.0);
        bgViews.get(currentWallpaperIndex).setOpacity(1.0);

        WritableImage snapshot = fgViews.get(currentWallpaperIndex).snapshot(new SnapshotParameters(), null);

        // 文字色計算
        // まずはファイル名を取得
        String currentWallpaperFileName = wallpaperFiles.get(currentWallpaperIndex);
        WallpaperColor userColors = configure.getColors(currentWallpaperFileName);
        Color[] colors;

        if (userColors != null && userColors.getMainColor() != null && userColors.getMonoColor() != null) {
            fixedTextColor = userColors.getMainColor();
            wbColor = userColors.getMonoColor();
        } else {
            // JSONに定義されていない場合は通常の計算で取得
            colors = colorHelper.calculateColors(snapshot);
            fixedTextColor = colors[2];
            wbColor = colors[0];
        }


        // 文字の影
        ds = new DropShadow();
        ds.setOffsetX(2);
        ds.setOffsetY(2);
        ds.setRadius(16);
        ds.setSpread(0.4);

        if(wbColor.equals(Color.BLACK)) {
            ds.setColor(Color.color(1,1,1,1));
        }
        else
        {
            ds.setColor(Color.color(0,0,0,0.75));
        }

        // 時計ラベル
        HBox timeBox = new HBox(15);
        timeBox.setAlignment(Pos.CENTER);

        HBox hourMinBox = new HBox(5);
        hourMinBox.setAlignment(Pos.CENTER);

        // 分割
        String initialTime = "12:34";
        for (int i = 0; i < initialTime.length(); i++) {
            Label l = createLabel("" + initialTime.charAt(i), 156);
            hourMinLabels[i] = l;
            hourMinBox.getChildren().add(l);
        }

        // 分割
        secBox = new HBox(0);
        secBox.setAlignment(Pos.CENTER);
        String initialSec = "56";
        for (int i = 0; i < initialSec.length(); i++) {
            Label l = createLabel("" + initialSec.charAt(i), 112);
            secLabels[i] = l;
            secBox.getChildren().add(l);
        }

        // 時刻ボックスに秒数もいれる
        timeBox.getChildren().addAll(hourMinBox, secBox);

        // 日付はそのまま
        dateLabel = createLabel("", 72);

        // 1行目　気温
        weatherTempLabel = createLabel("", 48);
        weatherTempIcon = createIcon("/icons/svg/temp.svg", 32, fixedTextColor);
        VBox tempBox = createVBox(10, weatherTempIcon, create2elementsLabel(weatherTempLabel, "℃", 32));

        // 体感気温
        weatherFeelingTempLabel = createLabel("", 48);
        weatherFeelingTempIcon = createIcon("/icons/svg/person.svg", 32, fixedTextColor);
        VBox feelingTempBox = createVBox(10, weatherFeelingTempIcon, create2elementsLabel(weatherFeelingTempLabel, "℃", 32));

        // 湿度
        weatherHumidityLabel = createLabel("", 48);
        weatherHumidityIcon = createIcon("/icons/svg/humid.svg", 32, fixedTextColor);
        VBox humidityBox = createVBox(10, weatherHumidityIcon, create2elementsLabel(weatherHumidityLabel, "%", 32));

        // 2行目　大気圧
        weatherPressureLabel = createLabel("", 32);
        HBox weatherPressureBox = create2elementsLabel(weatherPressureLabel, "hPa", 28);

        // 視界
        weatherVisibilityLabel = createLabel("", 32);
        HBox weatherVisibilityBox = create2elementsLabel(weatherVisibilityLabel, "km", 28);

        // 風向き
        weatherWindSpeedLabel = createLabel("", 32);
        HBox weatherWindSpeedBox = create2elementsLabel(weatherWindSpeedLabel, "m/s", 28);

        windArrow = new Polygon();
        windArrow.getPoints().addAll(
            0.0, -20.0,  // 矢印の先端
            -5.0, 10.0,  // 左下
            5.0, 10.0    // 右下
        );
        windArrow.setFill(wbColor);

        rotate = new Rotate(0, 0, 0);
        windArrow.getTransforms().add(rotate);
        windArrow.setEffect(ds);

        StackPane windPane = new StackPane(windArrow);
        windPane.setPrefSize(50, 50); // 適宜サイズ調整
        VBox windBox = createVBox(0, windPane, weatherWindSpeedBox);

        // 3行目 天気
        currentWeatherDetailLabel = createLabel("", 32);

        iconView = new ImageView();
        iconView.setFitWidth(112);
        iconView.setFitHeight(112);
        iconView.setEffect(ds);
        VBox currentWeatherBox = createVBox(10, iconView, currentWeatherDetailLabel);


        // 3行目 天気予報
        forecasts = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            forecasts.add(new WeatherForecastUI(dayString[i], fixedTextColor, ds));
        }

        // HBoxにまとめる
        HBox weatherForecastsBox = new HBox(30);
        weatherForecastsBox.setAlignment(Pos.CENTER);
        weatherForecastsBox.getChildren().add(currentWeatherBox);

        for (WeatherForecastUI f : forecasts) {
            weatherForecastsBox.getChildren().add(f.box);
        }

        // 4行目　気象警報
        alertGridPane = new GridPane();
        alertGridPane.setHgap(10);
        alertGridPane.setVgap(10);
        alertGridPane.setPadding(new Insets(10));

        // スクロール
        tickerBox = new HBox();
        tickerBox.setAlignment(Pos.CENTER_LEFT);
        tickerBox.setStyle("-fx-background-color: rgba(0,0,0,0.5);"); // 背景半透明
        tickerBox.setPrefHeight(40);

        tickerLabel = new Label("文字スクロールテストABCDEabcdeＡＢＣＤＥａｂｃｄｅ012345０１２３４５あいうえお");
        tickerLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        tickerBox.getChildren().add(tickerLabel);
        tickerBox.setVisible(false);



        HBox weatherBoxLine1 = new HBox(30);
        weatherBoxLine1.setAlignment(Pos.CENTER);
        weatherBoxLine1.getChildren().addAll(tempBox, feelingTempBox, humidityBox);

        HBox weatherBoxLine2 = new HBox(30);
        weatherBoxLine2.setAlignment(Pos.CENTER);
        weatherBoxLine2.getChildren().addAll(weatherPressureBox, weatherVisibilityBox, windBox);

        // ボックス
        LeftTopBox = createVBox(30, dateLabel, timeBox, weatherBoxLine1, weatherBoxLine2, weatherForecastsBox, alertGridPane);

        // 壁紙更新ボタン
        Button changeWallpaperButton = new Button();
        changeWallpaperButton.setGraphic(createIcon("/icons/svg/refresh.svg", 20, Color.WHITE));
        changeWallpaperButton.setPrefWidth(20);
        changeWallpaperButton.setPrefHeight(20);
        changeWallpaperButton.setOnAction(e -> {
            switchWallpaper();
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
            setFullScreen(true);
        });

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(changeWallpaperButton, changeNightModeButton, setFullScreenButton);


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

        Labels = collectLabels(LeftTopBox);

        // 壁紙切替
        Timeline wallpaperTimeline = new Timeline(
            new KeyFrame(Duration.seconds(configure.getWallpaperChangeSecond()), e -> switchWallpaper())
        );
        wallpaperTimeline.setCycleCount(Timeline.INDEFINITE);
        wallpaperTimeline.play();

        System.out.println(configure.getRefreshWeatherSecond());

        // お天気更新
        Timeline refreshWeather = new Timeline(
            new KeyFrame(Duration.seconds(configure.getRefreshWeatherSecond()), e -> updateWeatherUI())
        );
        refreshWeather.setCycleCount(Timeline.INDEFINITE);
        refreshWeather.play();

        // UI左右反転
        Timeline reverseUI = new Timeline(
            new KeyFrame(Duration.seconds(configure.getReverseUISecond()), e -> reverseUI())
        );
        reverseUI.setCycleCount(Timeline.INDEFINITE);
        reverseUI.play();

        // 初回更新
        updateWeatherUI();
    }

    private List<String> getWallpapersByTag(String tag) {
        List<String> result = new ArrayList<>();

        for (String file : wallpaperFiles) {
            WallpaperColor wc = configure.getColors(file);

            // JSONに存在しない → 無条件で追加（＝タグなし扱い）
            if (wc == null) {
                result.add(file);
                continue;
            }

            // タグ一致
            if (wc.getTags().contains(tag)) {
                result.add(file);
            }
        }

        return result;
    }

    private void switchWallpaper() {
        if(fgViews.isEmpty()) return;
        if(isSwitchingWallpaper) return;

        isSwitchingWallpaper = true;

        String tag = getTimeTag();
        if(tag.contains("midnight"))
            tag = "midnight";

        List<String> filtered = getWallpapersByTag(tag);

        // 実在ファイルだけ残す
        filtered.removeIf(f -> !wallpaperFiles.contains(f));

        // フォールバック
        if (filtered.isEmpty()) {
            filtered = wallpaperFiles;
        }

        // 同じ壁紙を避ける
        if (filtered.size() > 1) {
            filtered.remove(wallpaperFiles.get(currentWallpaperIndex));
        }

        Random rand = new Random();
        String nextFile = filtered.get(rand.nextInt(filtered.size()));

        int nextIndex = wallpaperFiles.indexOf(nextFile);
        if (nextIndex == -1) return;
        
        // アニメーションの長さ
        int fadeAnimationTime = 2;

        // フェード適用
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
            Color[] colors;

            if (userColors != null && userColors.getMainColor() != null && userColors.getMonoColor() != null) {
                fixedTextColor = userColors.getMainColor();
                wbColor = userColors.getMonoColor();
            } else {
                // JSONに定義されていない場合は通常の計算で取得
                colors = colorHelper.calculateColors(snapshot);
                fixedTextColor = colors[2];
                wbColor = colors[0];
            }

            fadeTextColorForAll(Labels, fixedTextColor);

            isSwitchingWallpaper = false;
        });

        fadeOutfg.play();
        fadeOutbg.play();
        fadeInbg.play();
        fadeInfg.play();
    }

    private void startClock() {
        scheduleNextTick();
    }

    private void scheduleNextTick() {
        LocalDateTime now = LocalDateTime.now();

        // 次の「秒切り替わり」までのミリ秒
        long delay = 1000 - now.getNano() / 1_000_000;

        Timeline t = new Timeline(new KeyFrame(Duration.millis(delay), e -> {
            updateClock();
            scheduleNextTick(); // 再帰で次を予約
        }));
        t.setCycleCount(1);
        t.play();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();

        String nextHourMin = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String nextSec = now.format(DateTimeFormatter.ofPattern("ss"));
        String nextDate = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (EEE)", Locale.ENGLISH));

        // 時刻ラベル更新
        for (int i = 0; i < hourMinLabels.length; i++) {
            if (!hourMinLabels[i].getText().equals("" + nextHourMin.charAt(i)))
                fadeLabel(hourMinLabels[i], "" + nextHourMin.charAt(i));
        }

        for (int i = 0; i < secLabels.length; i++) {
            if (!secLabels[i].getText().equals("" + nextSec.charAt(i)))
                fadeLabel(secLabels[i], "" + nextSec.charAt(i));
        }

        if (!dateLabel.getText().equals(nextDate))
            fadeLabel(dateLabel, nextDate);
    }

    // UI 更新処理
    private void updateWeatherUI() {
        weather.fetch()
            .thenRun(() -> {
                Platform.runLater(() -> {
                    updateCurrentWeather();
                    updateForecasts();
                    updateAlerts();
                });
            });
    }

    private void updateCurrentWeather() {
        weatherTempLabel.setText(String.format("%.1f", weather.getTemp() - 273.15));
        weatherFeelingTempLabel.setText(String.format("%.1f", weather.getFeelingTemp() - 273.15));
        weatherHumidityLabel.setText(String.format("%d", (int) weather.getHumidity()));
        weatherPressureLabel.setText(String.format("%d", weather.getPressure()));
        weatherVisibilityLabel.setText(String.format("%d", weather.getVisibility() /1000));
        weatherWindSpeedLabel.setText(String.format("%.1f", weather.getWindSpeed()));

        int newWindDeg = weather.getWindDeg();
        rotate.setAngle(newWindDeg);

        Image image = iconView.getImage();

        if (image == null || !image.getUrl().endsWith("/"+ weather.getWeatherIcon() + ".png")) {
            String url = "https://openweathermap.org/payload/api/media/file/"+ weather.getWeatherIcon() + ".png";
            iconView.setImage(new Image(url, true));
        }

        currentWeatherDetailLabel.setText(weather.getWeatherDetail());

        double uvi = weather.getUvi();
        if(uvi >= 3.0)
        {
            if(uvi < 6.0) {
                // 6.0未満(3.0～5.9)
                scrollMessage("紫外線指数は "+ String.format("%.1f", uvi) +"(中程度) です。");
            } else if(uvi < 8.0) {
                // 8.0未満(6.0～7.9)
                scrollMessage("紫外線指数は "+ String.format("%.1f", uvi) +"(強い) です。");
            } else if(uvi < 11.0) {
                // 11.0未満(8.0～10.9)
                scrollMessage("紫外線指数は "+ String.format("%.1f", uvi) +"(非常に強い) です。");
            } else {
                // 11.0以上
                scrollMessage("紫外線指数は "+ String.format("%.1f", uvi) +"(極端に強い) です。");
            }
        }
    }

    private void updateForecasts() {
        List<Weather.Daily> daily = weather.getDaily();

        int j = 0;
        if(isPastedDay())
            j = 1;

        for (int i = 0; i < forecasts.size(); i++) {
            if (i >= daily.size()) break;

            Weather.Daily d = daily.get(j);
            WeatherForecastUI f = forecasts.get(i);

            Image image = f.iconView.getImage();

            if (image == null || !image.getUrl().endsWith("/"+ d.weatherIcon + ".png")) {
                String url = "https://openweathermap.org/payload/api/media/file/" + d.weatherIcon + ".png";
                f.iconView.setImage(new Image(url, true));
            }

            f.label.setText(d.weatherDetail);
            f.dayLabel.setText(dayString[j]);

            scrollMessage(""+ dayString[j] +"の天気は"+ d.weatherDetail +"、日中の最高気温は　"
                + String.format("%.1f", d.temp_max - 273.15) +"℃　最低気温は　"
                + String.format("%.1f", d.temp_min - 273.15) +"℃　おおよその平均気温は　"
                + String.format("%.1f", d.temp_day - 273.15) +"℃　です。");

            j++;
        }
    }

    private void updateAlerts() {
        // 警報・注意報
        alertGridPane.getChildren().clear();

        int row = 0;
        int col = 0;
        for (Weather.Alert alert : weather.getAlerts()) {
            Label typeLabel = new Label(alert.event);

            // 背景
            Region bg = new Region();
            bg.setPrefSize(150, 50);
            bg.setOpacity(0.5);

            typeLabel.setStyle("-fx-font-size: 24px;");
            typeLabel.setAlignment(Pos.CENTER);

            // 度合いに応じて色を変更
            if (alert.event.contains("注意報")) {
                bg.setStyle("-fx-background-color: yellow;-fx-background-radius: 10;-fx-border-radius: 10;");
                typeLabel.setTextFill(Color.BLACK);
            } else if (alert.event.contains("警報")) {
                bg.setStyle("-fx-background-color: red;-fx-background-radius: 10;-fx-border-radius: 10;");
                typeLabel.setTextFill(Color.WHITE);
            }
            // 特別警報は「警報」が入ってるから、elseにすると正しく動かなくなる
            if (alert.event.contains("特別警報")) {
                bg.setStyle("-fx-background-color: purple;-fx-background-radius: 10;-fx-border-radius: 10;");
                typeLabel.setTextFill(Color.WHITE);
            }

            scrollMessage(""+ alert.event +"が発表されています。");

            StackPane alertPane = new StackPane(bg, typeLabel);
            alertPane.setPadding(new Insets(10));
            alertGridPane.add(alertPane, col, row);
            row++;
        }
    }

    // 日時更新のアニメーション部分
    private void fadeLabel(Label label, String newText) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(100), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(100), label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        fadeOut.setOnFinished(e -> {
            label.setText(newText);
            fadeIn.play();
        });

        fadeOut.play();
    }

    // 全部のラベルの色を変える
    private void fadeTextColorForAll(List<Label> allLabels, Color newColor) {
        for (Label l : allLabels) {
            fadeTextColor(l, newColor);  // 先ほどのアニメーションメソッド
        }
        updateShadowColor();
        windArrow.setFill(wbColor);
        weatherTempIcon.setColor(newColor);
        weatherFeelingTempIcon.setColor(newColor);
        weatherHumidityIcon.setColor(newColor);
    }

    // ラベルの色を変える部分
    private void fadeTextColor(Label label, Color newColor) {
        final Color oldColor = (Color) label.getTextFill();

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e -> label.setTextFill(oldColor)),
            new KeyFrame(Duration.millis(300), 
                new javafx.animation.KeyValue(label.textFillProperty(), newColor))
        );
        timeline.play();
    }

    // 文字の影を更新する
    private void updateShadowColor() {
        if(wbColor.equals(Color.BLACK)) {
            ds.setColor(Color.color(1,1,1,1));
        } else {
            ds.setColor(Color.color(0,0,0,0.75));
        }
    }

    // ラベルをもってくる
    private List<Label> collectLabels(Parent parent) {
        List<Label> labels = new ArrayList<>();
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label l) {
                labels.add(l);
            } else if (node instanceof Parent p) {
                labels.addAll(collectLabels(p)); // 再帰
            }
        }
        return labels;
    }

    // ラベル作る用
    private Label createLabel(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px;");
        l.setTextFill(fixedTextColor);
        l.setEffect(ds);
        return l;
    }

    // VBox作る用
    private VBox createVBox(int spacing, Node... children) {
        VBox box = new VBox(spacing, children);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // 単位と値のラベルをつくる
    private HBox create2elementsLabel(Label valueLabel, String unit, int unitSize) {
        Label unitLabel = createLabel(unit, unitSize);
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(valueLabel, unitLabel);
        return hBox;
    }

    // スクロール表示メッセージの追加
    private void scrollMessage(String message) {
        messageQueue.add(message); // キューに追加
        if (!isScrolling) {
            playNextMessage();
        }
    }

    // メッセージ表示
    private void playNextMessage() {
        if (messageQueue.isEmpty()) {
            isScrolling = false;
            tickerBox.setVisible(false);
            return;
        }

        isScrolling = true;
        String message = messageQueue.poll();

        Platform.runLater(() -> {
            tickerLabel.setText(message);

            if(!isNightMode())
                tickerBox.setVisible(true);

            double startX = rootPane.getWidth() +50.0;
            double endX = -tickerLabel.getWidth();

            // メッセージ長に応じてスクロール時間を調整（基本30秒を基準）
            double baseDuration = 30; // 秒
            double duration = baseDuration * (tickerLabel.getWidth() / 1000.0); // 500px基準で調整
            duration = Math.max(20, duration); // 最低5秒

            TranslateTransition scroll = new TranslateTransition(Duration.seconds(duration), tickerLabel);
            scroll.setFromX(startX);
            scroll.setToX(endX);
            scroll.setInterpolator(Interpolator.LINEAR);

            scroll.setOnFinished(e -> {
                tickerLabel.setTranslateX(0);
                playNextMessage(); // 次のメッセージ再生
            });

            scroll.play();
        });
    }

    // 時間帯タグを生成
    private String getTimeTag() {
        int hour = LocalDateTime.now().getHour();

        if (hour < 5) return "midnightAM";
        if (hour >= 5 && hour < 10) return "morning";
        if (hour >= 10 && hour < 17) return "day";
        if (hour >= 17 && hour < 20) return "evening";
        if (hour >= 20 && hour < 22) return "night";
        return "midnightPM";
    }

    // 1日の半分くらい過ぎたか
    private boolean isPastedDay() {
        String tag = getTimeTag();

        if("evening".equals(tag) || "night".equals(tag) || "midnightPM".equals(tag))
            return true;
        return false;
    }

    // ナイトモード
    private void setNightMode(boolean isNight) {
        if(isNight) {
            ColorAdjust darken = new ColorAdjust();
            darken.setBrightness(-0.75);
            rootPane.setEffect(darken);

            if(isScrolling)
                tickerBox.setVisible(false);

            
            animation.fadeOut(secBox, false);
        }
        else
        {
            rootPane.setEffect(null);

            if(isScrolling)
                tickerBox.setVisible(true);

            animation.fadeIn(secBox);
        }
    }

    // ナイトモードか知る
    private boolean isNightMode() {
        return !secBox.isVisible();
    }

    private void setFullScreen(boolean isFullscreen) {
        GUI.stage.setFullScreen(true);
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
        icon.setEffect(ds);
        return icon;
    }
}