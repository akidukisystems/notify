package com.akidukisystems.notify.notifyapp.controller;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 予報1日分(日付ラベル・天気アイコン・詳細ラベル)をまとめたUI部品。
 */
public class WeatherForecastUI {
    public Label dayLabel;
    public Label label;
    public ImageView iconView;
    public VBox box;

    /**
     * @param dayText   表示する日付ラベル(例: "明日")
     * @param textColor 文字色
     * @param ds        適用するドロップシャドウ
     * @param fontScale フォントサイズ倍率
     */
    public WeatherForecastUI(String dayText, Color textColor, DropShadow ds, double fontScale) {
        dayLabel = new Label(dayText);
        label = new Label("晴れ");
        iconView = new ImageView();

        dayLabel.setStyle("-fx-font-size: " + (22 * fontScale) + "px;");
        label.setStyle("-fx-font-size: " + (24 * fontScale) + "px;");

        dayLabel.setTextFill(textColor);
        label.setTextFill(textColor);

        dayLabel.setEffect(ds);
        label.setEffect(ds);

        iconView.setFitWidth(64);
        iconView.setFitHeight(64);
        iconView.setEffect(ds);
        iconView.setCursor(Cursor.HAND);

        box = new VBox(10, dayLabel, iconView, label);
        box.setAlignment(Pos.CENTER);
    }
}