package com.akidukisystems.notify.notifyapp.controller.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.akidukisystems.notify.notifyapp.Weather;
import com.akidukisystems.notify.notifyapp.controller.SVGHelper;
import com.akidukisystems.notify.notifyapp.controller.SVGIcon;
import com.akidukisystems.notify.notifyapp.controller.WeatherForecastUI;
import com.akidukisystems.notify.notifyapp.controller.theme.ThemeManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * 現在の天気(気温・湿度・気圧など)と3日分の予報を表示するコントローラー。
 * 天気アイコンをクリックすると、その日の最高・最低・平均気温をオーバーレイ表示する。
 */
public class WeatherUIController implements ThemeManager.ThemeListener {

    private final ThemeManager themeManager;
    private final Weather weather;
    private final Consumer<String> messageConsumer;
    private final Supplier<Boolean> isPastedDaySupplier;
    private final List<Label> allLabels = new ArrayList<>();
    private final SVGHelper svgHelper = new SVGHelper();

    private final String[] dayString = {"今日", "明日", "明後日", "3日後", "4日後", "5日後"};

    private Label weatherTempLabel;
    private Label weatherFeelingTempLabel;
    private Label weatherHumidityLabel;
    private Label weatherPressureLabel;
    private Label weatherVisibilityLabel;
    private Label weatherWindSpeedLabel;
    private Label currentWeatherDetailLabel;

    private ImageView iconView;
    private Rotate rotate;
    private Polygon windArrow;

    private SVGIcon weatherTempIcon;
    private SVGIcon weatherFeelingTempIcon;
    private SVGIcon weatherHumidityIcon;

    private List<WeatherForecastUI> forecasts;

    private Popup forecastDetailPopup;
    private ImageView forecastDetailIconSource;

    private final HBox line1Box; // 気温・体感温度・湿度
    private final HBox line2Box; // 気圧・視界・風向
    private final HBox forecastsBox; // 現在の天気＋予報

    /**
     * @param themeManager         テーマカラー変更を購読するための{@link ThemeManager}
     * @param weather              表示元となる{@link Weather}
     * @param messageConsumer      紫外線指数・予報の説明などを流すスクロールメッセージの送り先
     * @param isPastedDaySupplier  「今日の予報がもう終わった時間帯か」を返す判定関数(予報の開始日をずらすために使用)
     */
    public WeatherUIController(ThemeManager themeManager, Weather weather,
                                Consumer<String> messageConsumer,
                                Supplier<Boolean> isPastedDaySupplier) {
        this.themeManager = themeManager;
        this.weather = weather;
        this.messageConsumer = messageConsumer;
        this.isPastedDaySupplier = isPastedDaySupplier;

        themeManager.addListener(this);

        // 1行目 気温
        weatherTempLabel = createLabel("", 48);
        weatherTempIcon = createIcon("/icons/svg/temp.svg", 32);
        VBox tempBox = createVBox(10, weatherTempIcon, create2elementsLabel(weatherTempLabel, "℃", 32));

        // 体感気温
        weatherFeelingTempLabel = createLabel("", 48);
        weatherFeelingTempIcon = createIcon("/icons/svg/person.svg", 32);
        VBox feelingTempBox = createVBox(10, weatherFeelingTempIcon, create2elementsLabel(weatherFeelingTempLabel, "℃", 32));

        // 湿度
        weatherHumidityLabel = createLabel("", 48);
        weatherHumidityIcon = createIcon("/icons/svg/humid.svg", 32);
        VBox humidityBox = createVBox(10, weatherHumidityIcon, create2elementsLabel(weatherHumidityLabel, "%", 32));

        line1Box = new HBox(30, tempBox, feelingTempBox, humidityBox);
        line1Box.setAlignment(Pos.CENTER);

        // 2行目 大気圧
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
            0.0, -20.0,
            -5.0, 10.0,
            5.0, 10.0
        );
        windArrow.setFill(themeManager.getWbColor());
        rotate = new Rotate(0, 0, 0);
        windArrow.getTransforms().add(rotate);
        windArrow.setEffect(themeManager.getShadow());

        StackPane windPane = new StackPane(windArrow);
        windPane.setPrefSize(50, 50);
        VBox windBox = createVBox(0, windPane, weatherWindSpeedBox);

        line2Box = new HBox(30, weatherPressureBox, weatherVisibilityBox, windBox);
        line2Box.setAlignment(Pos.CENTER);

        // 3行目 現在の天気
        currentWeatherDetailLabel = createLabel("", 32);
        iconView = new ImageView();
        iconView.setFitWidth(112);
        iconView.setFitHeight(112);
        iconView.setEffect(themeManager.getShadow());
        iconView.setCursor(Cursor.HAND);
        VBox currentWeatherBox = createVBox(10, iconView, currentWeatherDetailLabel);

        // 予報3日分
        forecasts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            forecasts.add(new WeatherForecastUI(dayString[i], themeManager.getTextColor(), themeManager.getShadow(), themeManager.getFontScale()));
        }

        forecastsBox = new HBox(30);
        forecastsBox.setAlignment(Pos.CENTER);
        forecastsBox.getChildren().add(currentWeatherBox);
        for (WeatherForecastUI f : forecasts) {
            forecastsBox.getChildren().add(f.box);
        }
    }

    // --- ctrl.java側でレイアウトに組み込むためのアクセサ ---
    /** 気温・体感温度・湿度の行を返す。 */
    public HBox getLine1Box() { return line1Box; }
    /** 気圧・視界・風向の行を返す。 */
    public HBox getLine2Box() { return line2Box; }
    /** 現在の天気+予報3日分の行を返す。 */
    public HBox getForecastsBox() { return forecastsBox; }

    /** 現在の天気と予報表示を最新の{@link Weather}の内容で更新する。 */
    public void update() {
        List<Weather.Daily> daily = weather.getDaily();
        updateCurrentWeather(daily);
        updateForecasts(daily);
    }

    /** 現在の気温・湿度・気圧などのラベルとアイコンを更新し、紫外線指数が高ければ注意喚起メッセージを流す。 */
    private void updateCurrentWeather(List<Weather.Daily> daily) {
        weatherTempLabel.setText(String.format("%.1f", weather.getTemp() - 273.15));
        weatherFeelingTempLabel.setText(String.format("%.1f", weather.getFeelingTemp() - 273.15));
        weatherHumidityLabel.setText(String.format("%d", (int) weather.getHumidity()));
        weatherPressureLabel.setText(String.format("%d", weather.getPressure()));
        weatherVisibilityLabel.setText(String.format("%d", weather.getVisibility() / 1000));
        weatherWindSpeedLabel.setText(String.format("%.1f", weather.getWindSpeed()));

        rotate.setAngle(weather.getWindDeg());

        Image image = iconView.getImage();
        if (image == null || !image.getUrl().endsWith("/" + weather.getWeatherIcon() + ".png")) {
            String url = "https://openweathermap.org/payload/api/media/file/" + weather.getWeatherIcon() + ".png";
            iconView.setImage(new Image(url, true));
        }

        currentWeatherDetailLabel.setText(weather.getWeatherDetail());

        if (!daily.isEmpty()) {
            Weather.Daily today = daily.get(0);
            iconView.setOnMouseClicked(e -> toggleForecastDetail(iconView, today));
        }

        double uvi = weather.getUvi();
        if (uvi >= 3.0) {
            if (uvi < 6.0) {
                messageConsumer.accept("紫外線指数は " + String.format("%.1f", uvi) + "(中程度) です。");
            } else if (uvi < 8.0) {
                messageConsumer.accept("紫外線指数は " + String.format("%.1f", uvi) + "(強い) です。");
            } else if (uvi < 11.0) {
                messageConsumer.accept("紫外線指数は " + String.format("%.1f", uvi) + "(非常に強い) です。");
            } else {
                messageConsumer.accept("紫外線指数は " + String.format("%.1f", uvi) + "(極端に強い) です。");
            }
        }
    }

    /** 3日分の予報アイコン・天候詳細・日付ラベルを更新し、各日の予報をスクロールメッセージで案内する。 */
    private void updateForecasts(List<Weather.Daily> daily) {
        int j = 0;
        if (isPastedDaySupplier.get()) j = 1;

        for (int i = 0; i < forecasts.size(); i++) {
            if (j >= daily.size()) break;

            Weather.Daily d = daily.get(j);
            WeatherForecastUI f = forecasts.get(i);

            Image image = f.iconView.getImage();
            if (image == null || !image.getUrl().endsWith("/" + d.weatherIcon + ".png")) {
                String url = "https://openweathermap.org/payload/api/media/file/" + d.weatherIcon + ".png";
                f.iconView.setImage(new Image(url, true));
            }

            f.label.setText(d.weatherDetail);
            f.dayLabel.setText(dayString[j]);
            f.iconView.setOnMouseClicked(e -> toggleForecastDetail(f.iconView, d));

            messageConsumer.accept("" + dayString[j] + "の天気は" + d.weatherDetail + "、日中の最高気温は　"
                + String.format("%.1f", d.temp_max - 273.15) + "℃　最低気温は　"
                + String.format("%.1f", d.temp_min - 273.15) + "℃　おおよその平均気温は　"
                + String.format("%.1f", d.temp_day - 273.15) + "℃　です。");

            j++;
        }
    }

    /** 天気アイコンクリックで最高・最低・平均気温をオーバーレイ表示する(もう一度押すと閉じる)。 */
    private void toggleForecastDetail(ImageView icon, Weather.Daily d) {
        if (forecastDetailPopup != null && forecastDetailPopup.isShowing()) {
            boolean sameIcon = forecastDetailIconSource == icon;
            forecastDetailPopup.hide();
            forecastDetailPopup = null;
            forecastDetailIconSource = null;
            if (sameIcon) return;
        }

        VBox content = new VBox(6,
            createDetailLine("最高(昼)", d.temp_max),
            createDetailLine("最低(朝)", d.temp_min),
            createDetailLine("平均", d.temp_day));
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: rgba(20,20,20,0.85); -fx-background-radius: 10;");

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(content);

        Point2D anchor = icon.localToScreen(0, icon.getBoundsInLocal().getHeight());
        popup.show(icon, anchor.getX(), anchor.getY() + 8);

        forecastDetailPopup = popup;
        forecastDetailIconSource = icon;
    }

    /** "ラベル  ○○℃"(ケルビン→摂氏変換込み)のオーバーレイ用テキスト行を生成する。 */
    private Label createDetailLine(String name, double kelvin) {
        Label l = new Label(name + "  " + String.format("%.1f℃", kelvin - 273.15));
        l.setTextFill(Color.WHITE);
        l.setStyle("-fx-font-size: " + (18 * themeManager.getFontScale()) + "px;");
        return l;
    }

    /** テーマカラーが変わった際、全ラベル・アイコン・風向き矢印の色をフェードで追従させる。 */
    @Override
    public void onThemeChanged(Color textColor, Color wbColor, DropShadow shadow) {
        for (Label l : allLabels) {
            fadeTextColor(l, textColor);
        }

        for (WeatherForecastUI f : forecasts) {
            fadeTextColor(f.dayLabel, textColor);
            fadeTextColor(f.label, textColor);
        }

        windArrow.setFill(wbColor);
        weatherTempIcon.setColor(textColor);
        weatherFeelingTempIcon.setColor(textColor);
        weatherHumidityIcon.setColor(textColor);
    }

    /** ラベルの文字色を300msかけて新しい色へアニメーションさせる。 */
    private void fadeTextColor(Label label, Color newColor) {
        final Color oldColor = (Color) label.getTextFill();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e -> label.setTextFill(oldColor)),
            new KeyFrame(Duration.millis(300), new KeyValue(label.textFillProperty(), newColor))
        );
        timeline.play();
    }

    /** 現在のテーマカラー・フォントサイズ倍率を反映したラベルを生成し、テーマ変更時の追従対象として記録する。 */
    private Label createLabel(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + (size * themeManager.getFontScale()) + "px;");
        l.setTextFill(themeManager.getTextColor());
        l.setEffect(themeManager.getShadow());
        allLabels.add(l); // ← 追加
        return l;
    }

    /** 中央揃えのVBoxを生成する。 */
    private VBox createVBox(int spacing, javafx.scene.Node... children) {
        VBox box = new VBox(spacing, children);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** 数値ラベルと単位ラベルを横に並べたボックスを生成する。 */
    private HBox create2elementsLabel(Label valueLabel, String unit, int unitSize) {
        Label unitLabel = createLabel(unit, unitSize);
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(valueLabel, unitLabel);
        return hBox;
    }

    /** 現在のテーマカラーを反映したSVGアイコンを生成する。 */
    private SVGIcon createIcon(String path, double size) {
        return svgHelper.createIcon(path, size, themeManager.getTextColor(), themeManager.getShadow());
    }
}