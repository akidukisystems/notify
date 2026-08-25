package com.akidukisystems.notify.notifyapp.controller;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class WeatherForecastUI {
    public Label dayLabel;
    public Label label;
    public ImageView iconView;
    public VBox box;

    public WeatherForecastUI(String dayText, Color textColor, DropShadow ds) {
        dayLabel = new Label(dayText);
        label = new Label("晴れ");
        iconView = new ImageView();

        dayLabel.setStyle("-fx-font-size: 22px;");
        label.setStyle("-fx-font-size: 24px;");

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