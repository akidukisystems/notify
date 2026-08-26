package com.akidukisystems.notify.notifyapp;

import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// 壁紙の読み込み中だけ表示する、タイトルバー無しの中サイズのスプラッシュウィンドウ
public class SplashScreen {

    private static final Color ACCENT_COLOR = Color.web("#333333");

    private final Stage stage = new Stage(StageStyle.UNDECORATED);
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("(0/0)");

    public SplashScreen() {
        progressLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 20px;");
        progressLabel.setMouseTransparent(true);

        progressBar.setPrefWidth(320);

        StackPane barStack = new StackPane(progressBar, progressLabel);

        VBox box = new VBox(32, createLogo(), barStack);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: #f5f5f5;");

        Scene scene = new Scene(box, 420, 260);

        stage.setScene(scene);
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }

    public void close() {
        stage.close();
    }

    public void updateProgress(int loaded, int total) {
        double fraction = total == 0 ? 1.0 : (double) loaded / total;
        progressBar.setProgress(fraction);
        progressLabel.setText("(" + loaded + "/" + total + ")");
    }

    private Node createLogo() {
        SVGImage svg = SVGLoader.load(getClass().getResource("/icons/svg/logo.svg"));
        SVGImage scaled = svg.scaleTo(110);
        applyStrokeColor(scaled, ACCENT_COLOR);
        return scaled;
    }

    // fill="none"の線画なので、塗りつぶしには触れずstrokeだけ色を当てる
    private void applyStrokeColor(Node node, Color color) {
        if (node instanceof Shape shape) {
            shape.setStroke(color);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyStrokeColor(child, color);
            }
        }
    }
}
