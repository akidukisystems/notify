package com.akidukisystems.notify.notifyapp.controller.alert;

import java.util.function.Consumer;

import com.akidukisystems.notify.notifyapp.JmaWarning;
import com.akidukisystems.notify.notifyapp.JmaWarningCode;
import com.akidukisystems.notify.notifyapp.Weather;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class AlertUIController {

    private final Weather weather;
    private final Consumer<String> messageConsumer;

    private final GridPane alertGridPane;

    private static final int MAX_ROWS = 3;

    public AlertUIController(Weather weather, Consumer<String> messageConsumer) {
        this.weather = weather;
        this.messageConsumer = messageConsumer;

        alertGridPane = new GridPane();
        alertGridPane.setHgap(10);
        alertGridPane.setVgap(10);
        alertGridPane.setPadding(new Insets(10));
    }

    public GridPane getNode() {
        return alertGridPane;
    }

    public void update() {
        alertGridPane.getChildren().clear();

        int row = 0;
        int col = 0;

        // 最大警戒レベル相当
        String currentMaxSignName = weather.getJma().getMaxSignificancyName();

        if (currentMaxSignName != null) {
            StackPane alertPane = createAlertPane(currentMaxSignName);
            alertGridPane.add(alertPane, col, row);

            row++;
            if (row >= MAX_ROWS) {
                row = 0;
                col++;
            }
        }

        for (JmaWarning.Warning warning : weather.getWarnings()) {
            StackPane alertPane = createWarningPane(warning);
            alertGridPane.add(alertPane, col, row);

            messageConsumer.accept(warning.getName() + "が発表されています。");

            row++;
            if (row >= MAX_ROWS) {
                row = 0;
                col++;
            }
        }
    }

    private StackPane createAlertPane(String currentMaxSignName) {
        Label typeLabel = new Label(currentMaxSignName);

        Region bg = new Region();
        bg.setPrefSize(150, 50);
        bg.setOpacity(0.5);

        typeLabel.setStyle("-fx-font-size: 24px;");
        typeLabel.setAlignment(Pos.CENTER);

        if (currentMaxSignName.contains("５")) {
            bg.setStyle("-fx-background-color: black;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.WHITE);
        } else if (currentMaxSignName.contains("４")) {
            bg.setStyle("-fx-background-color: purple;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.WHITE);
        } else if (currentMaxSignName.contains("３")) {
            bg.setStyle("-fx-background-color: red;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.WHITE);
        } else {
            bg.setStyle("-fx-background-color: yellow;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.BLACK);
        }

        StackPane alertPane = new StackPane(bg, typeLabel);
        alertPane.setPadding(new Insets(10));
        return alertPane;
    }

    private StackPane createWarningPane(JmaWarning.Warning warning) {
        Label typeLabel = new Label(JmaWarningCode.getName(warning.getCode()));

        Region bg = new Region();
        bg.setPrefSize(150, 50);
        bg.setOpacity(0.5);

        typeLabel.setStyle("-fx-font-size: 24px;");
        typeLabel.setAlignment(Pos.CENTER);

        if (warning.getName().contains("特別警報")) {
            bg.setStyle("-fx-background-color: purple;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.WHITE);
        } else if (warning.getName().contains("警報")) {
            bg.setStyle("-fx-background-color: red;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.WHITE);
        } else if (warning.getName().contains("注意報")) {
            bg.setStyle("-fx-background-color: yellow;-fx-background-radius: 10;-fx-border-radius: 10;");
            typeLabel.setTextFill(Color.BLACK);
        }

        StackPane alertPane = new StackPane(bg, typeLabel);
        alertPane.setPadding(new Insets(10));
        return alertPane;
    }
}