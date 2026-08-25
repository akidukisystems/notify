package com.akidukisystems.notify.notifyapp.controller.settings;

import java.io.File;

import com.akidukisystems.notify.notifyapp.Configure;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsController {

    private final Configure configure;
    private final Stage stage;

    private TextField wallpaperPathField;
    private TextField wallpaperMetadataPathField;
    private TextField latitudeField;
    private TextField longitudeField;
    private TextField jmaAreaCodeField;
    private TextField jmaAreaNameField;
    private TextField blurField;
    private TextField fontScaleField;
    private TextField wallpaperChangeSecondField;
    private TextField refreshWeatherSecondField;
    private TextField reverseUISecondField;
    private TextField mouseBatteryDeviceIdField;
    private PasswordField apiKeyField;

    private Label statusLabel;

    public SettingsController(Configure configure) {
        this.configure = configure;
        this.stage = new Stage();
        buildUI();
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    private void buildUI() {
        stage.setTitle("設定");
        stage.initModality(Modality.NONE);

        Label pathInfoLabel = new Label("保存先: " + Configure.getSettingsFilePath());
        pathInfoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(170);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        int row = 0;

        wallpaperPathField = new TextField(configure.getWallpaperPath());
        Button browseWallpaperDir = new Button("参照...");
        browseWallpaperDir.setOnAction(e -> chooseDirectory(wallpaperPathField));
        row = addRow(grid, row, "壁紙フォルダのパス", withBrowseButton(wallpaperPathField, browseWallpaperDir));

        wallpaperMetadataPathField = new TextField(configure.getWallpaperMetadataPath());
        Button browseMetadata = new Button("参照...");
        browseMetadata.setOnAction(e -> chooseFile(wallpaperMetadataPathField, "JSONファイル", "*.json"));
        row = addRow(grid, row, "壁紙メタデータのパス", withBrowseButton(wallpaperMetadataPathField, browseMetadata));

        latitudeField = new TextField(String.valueOf(configure.getLatitude()));
        row = addRow(grid, row, "緯度", latitudeField);

        longitudeField = new TextField(String.valueOf(configure.getLongitude()));
        row = addRow(grid, row, "経度", longitudeField);

        jmaAreaCodeField = new TextField(configure.getJmaAreaCode());
        row = addRow(grid, row, "市区町村コード(気象警報用)", jmaAreaCodeField);

        jmaAreaNameField = new TextField(configure.getJmaAreaName());
        row = addRow(grid, row, "地域名(表示用)", jmaAreaNameField);

        blurField = new TextField(String.valueOf(configure.getBlur()));
        row = addRow(grid, row, "背景ぼかし量", blurField);

        fontScaleField = new TextField(String.valueOf(configure.getFontScale()));
        row = addRow(grid, row, "フォントサイズ倍率", fontScaleField);

        wallpaperChangeSecondField = new TextField(String.valueOf(configure.getWallpaperChangeSecond()));
        row = addRow(grid, row, "壁紙更新周期(秒)", wallpaperChangeSecondField);

        refreshWeatherSecondField = new TextField(String.valueOf(configure.getRefreshWeatherSecond()));
        row = addRow(grid, row, "天気予報更新周期(秒)", refreshWeatherSecondField);

        reverseUISecondField = new TextField(String.valueOf(configure.getReverseUISecond()));
        row = addRow(grid, row, "UI反転周期(秒)", reverseUISecondField);

        mouseBatteryDeviceIdField = new TextField(configure.getMouseBatteryDeviceId());
        row = addRow(grid, row, "マウスのBluetoothデバイスID(バッテリー取得用)", mouseBatteryDeviceIdField);

        apiKeyField = new PasswordField();
        apiKeyField.setText(configure.getApiKey());
        row = addRow(grid, row, "APIキー(OpenWeather)", apiKeyField);

        statusLabel = new Label();

        Button saveButton = new Button("保存");
        saveButton.setOnAction(e -> onSave());

        Button cancelButton = new Button("閉じる");
        cancelButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, pathInfoLabel, grid, statusLabel, buttonBox);
        root.setPadding(new Insets(15));

        Scene scene = new Scene(root, 600, 640);
        stage.setScene(scene);
    }

    private int addRow(GridPane grid, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        return row + 1;
    }

    private HBox withBrowseButton(TextField field, Button button) {
        HBox box = new HBox(6, field, button);
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void chooseDirectory(TextField target) {
        DirectoryChooser chooser = new DirectoryChooser();
        File current = new File(target.getText());
        if (current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            target.setText(selected.getAbsolutePath() + File.separator);
        }
    }

    private void chooseFile(TextField target, String description, String extensionPattern) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensionPattern));
        File current = new File(target.getText());
        if (current.getParentFile() != null && current.getParentFile().isDirectory()) {
            chooser.setInitialDirectory(current.getParentFile());
        }
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            target.setText(selected.getAbsolutePath());
        }
    }

    private void onSave() {
        try {
            double latitude = Double.parseDouble(latitudeField.getText().trim());
            double longitude = Double.parseDouble(longitudeField.getText().trim());
            int blur = Integer.parseInt(blurField.getText().trim());
            double fontScale = Double.parseDouble(fontScaleField.getText().trim());
            int wallpaperChangeSecond = Integer.parseInt(wallpaperChangeSecondField.getText().trim());
            int refreshWeatherSecond = Integer.parseInt(refreshWeatherSecondField.getText().trim());
            int reverseUISecond = Integer.parseInt(reverseUISecondField.getText().trim());

            configure.setWallpaperPath(wallpaperPathField.getText().trim());
            configure.setWallpaperMetadataPath(wallpaperMetadataPathField.getText().trim());
            configure.setLatitude(latitude);
            configure.setLongitude(longitude);
            configure.setJmaAreaCode(jmaAreaCodeField.getText().trim());
            configure.setJmaAreaName(jmaAreaNameField.getText().trim());
            configure.setBlur(blur);
            configure.setFontScale(fontScale);
            configure.setWallpaperChangeSecond(wallpaperChangeSecond);
            configure.setRefreshWeatherSecond(refreshWeatherSecond);
            configure.setReverseUISecond(reverseUISecond);
            configure.setMouseBatteryDeviceId(mouseBatteryDeviceIdField.getText().trim());

            configure.saveSettings();
            configure.saveApiKey(apiKeyField.getText());

            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("保存しました。壁紙フォルダ・各種周期・背景ぼかしは次回起動時に反映されます。");

        } catch (NumberFormatException ex) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("数値項目(緯度・経度・ぼかし量・フォントサイズ倍率・各種周期)の入力を確認してください。");
        }
    }
}