package com.akidukisystems.notify.notifyapp.controller.alert;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import com.akidukisystems.notify.notifyapp.JmaWarning;
import com.akidukisystems.notify.notifyapp.JmaWarningCode;
import com.akidukisystems.notify.notifyapp.Weather;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

/**
 * 気象庁の最大警戒レベル表示と個別の警報・注意報バッジを、グリッド状に並べて表示するコントローラー。
 * バッジをクリックすると発表時刻・危険度などの詳細をオーバーレイ表示する。
 */
public class AlertUIController {

    private final Weather weather;
    private final Consumer<String> messageConsumer;
    private final double fontScale;

    private final GridPane alertGridPane;

    private static final int MAX_ROWS = 3;
    private static final DateTimeFormatter REPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private Popup warningDetailPopup;
    private StackPane warningDetailSource;

    /**
     * @param weather         警報・注意報の取得元
     * @param messageConsumer 警報発表時に流すスクロールメッセージの送り先
     * @param fontScale       フォントサイズ倍率
     */
    public AlertUIController(Weather weather, Consumer<String> messageConsumer, double fontScale) {
        this.weather = weather;
        this.messageConsumer = messageConsumer;
        this.fontScale = fontScale;

        alertGridPane = new GridPane();
        alertGridPane.setHgap(10);
        alertGridPane.setVgap(10);
        alertGridPane.setPadding(new Insets(10));
    }

    /** 警報・注意報バッジを並べるグリッドノードを返す。 */
    public GridPane getNode() {
        return alertGridPane;
    }

    /** 最大警戒レベル表示と個別の警報・注意報バッジを最新の{@link Weather}の内容で再描画する。 */
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

    /** 気象庁の最大警戒レベルを示すバッジ(危険度に応じた色分け)を生成する。 */
    private StackPane createAlertPane(String currentMaxSignName) {
        Label typeLabel = new Label(currentMaxSignName);

        Region bg = new Region();
        bg.setPrefSize(150, 50);
        bg.setOpacity(0.5);

        typeLabel.setStyle("-fx-font-size: " + (24 * fontScale) + "px;");
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

    /** 個別の警報・注意報バッジをクリックすると詳細をオーバーレイ表示する(もう一度押すと閉じる)。 */
    private void toggleWarningDetail(StackPane source, JmaWarning.Warning warning) {
        if (warningDetailPopup != null && warningDetailPopup.isShowing()) {
            boolean samePane = warningDetailSource == source;
            warningDetailPopup.hide();
            warningDetailPopup = null;
            warningDetailSource = null;
            if (samePane) return;
        }

        String reportTime = warning.getReportDateTime() != null
                ? warning.getReportDateTime().format(REPORT_TIME_FORMAT)
                : "不明";

        VBox content = new VBox(6,
            createDetailLine("発表時刻", reportTime),
            createDetailLine("ステータス", nullToDash(warning.getStatus())),
            createDetailLine("危険度", nullToDash(warning.getSignificancyName())));

        if (!warning.getNotes().isEmpty()) {
            content.getChildren().add(createNotesBlock(warning.getNotes()));
        }

        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: rgba(20,20,20,0.85); -fx-background-radius: 10;");

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(content);

        Point2D anchor = source.localToScreen(0, source.getBoundsInLocal().getHeight());
        popup.show(source, anchor.getX(), anchor.getY() + 8);

        warningDetailPopup = popup;
        warningDetailSource = source;
    }

    /** 「特記事項」見出しと各ノートを、最初のノートの位置に揃えて表示するブロックを生成する。 */
    private HBox createNotesBlock(List<String> notes) {
        Label header = new Label("特記事項");
        header.setTextFill(Color.WHITE);
        header.setStyle("-fx-font-size: " + (18 * fontScale) + "px;");

        VBox notesList = new VBox(2);
        for (String note : notes) {
            Label noteLabel = new Label(note);
            noteLabel.setTextFill(Color.WHITE);
            noteLabel.setStyle("-fx-font-size: " + (18 * fontScale) + "px;");
            notesList.getChildren().add(noteLabel);
        }

        return new HBox(6, header, notesList);
    }

    /** "ラベル  値" の形式のオーバーレイ用テキスト行を生成する。 */
    private Label createDetailLine(String name, String value) {
        Label l = new Label(name + "  " + value);
        l.setTextFill(Color.WHITE);
        l.setStyle("-fx-font-size: " + (18 * fontScale) + "px;");
        return l;
    }

    /** {@code null}または空文字を"-"に置き換える。 */
    private String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    /** 個別の警報・注意報1件分のバッジ(危険度に応じた色分け、クリックで詳細表示)を生成する。 */
    private StackPane createWarningPane(JmaWarning.Warning warning) {
        Label typeLabel = new Label(JmaWarningCode.getName(warning.getCode()));

        Region bg = new Region();
        bg.setPrefSize(150, 50);
        bg.setOpacity(0.5);

        typeLabel.setStyle("-fx-font-size: " + (24 * fontScale) + "px;");
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
        alertPane.setCursor(Cursor.HAND);
        alertPane.setOnMouseClicked(e -> toggleWarningDetail(alertPane, warning));
        return alertPane;
    }
}