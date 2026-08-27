package com.akidukisystems.notify.notifyapp.controller.clock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.akidukisystems.notify.notifyapp.controller.theme.ThemeManager;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 日付・時刻(HH:mm:ss)をフェードアニメーション付きで1秒ごとに更新するコントローラー。
 */
public class ClockController implements ThemeManager.ThemeListener {

    private final ThemeManager themeManager;

    private final Label[] hourMinLabels = new Label[5]; // "HH:mm"
    private final Label[] secLabels = new Label[2];     // "ss"
    private final Label dateLabel;

    private final HBox timeBox;
    private final HBox secBox;

    /** 時計用ラベル群を初期値付きで構築する({@link #start()}を呼ぶまで時刻更新は始まらない)。 */
    public ClockController(ThemeManager themeManager) {
        this.themeManager = themeManager;
        themeManager.addListener(this);

        timeBox = new HBox(15);
        timeBox.setAlignment(Pos.CENTER);

        HBox hourMinBox = new HBox(5);
        hourMinBox.setAlignment(Pos.CENTER);

        String initialTime = "12:34";
        for (int i = 0; i < initialTime.length(); i++) {
            Label l = createLabel("" + initialTime.charAt(i), 156);
            hourMinLabels[i] = l;
            hourMinBox.getChildren().add(l);
        }

        secBox = new HBox(0);
        secBox.setAlignment(Pos.CENTER);
        String initialSec = "56";
        for (int i = 0; i < initialSec.length(); i++) {
            Label l = createLabel("" + initialSec.charAt(i), 112);
            secLabels[i] = l;
            secBox.getChildren().add(l);
        }

        timeBox.getChildren().addAll(hourMinBox, secBox);

        dateLabel = createLabel("", 72);
    }

    // --- ctrl.java側でレイアウトに組み込むためのアクセサ ---
    /** 時・分・秒をまとめたボックスを返す。 */
    public HBox getTimeBox() { return timeBox; }
    /** 秒表示部分のボックスを返す(夜間モードのfadeOut対象として引き続き必要)。 */
    public HBox getSecBox() { return secBox; }
    /** 日付ラベルを返す。 */
    public Label getDateLabel() { return dateLabel; }

    private boolean running = false;
    private Timeline tickTimeline;

    /** 1秒ごとの時刻更新を開始する。 */
    public void start() {
        running = true;
        scheduleNextTick();
    }

    /** 時刻更新を停止する(トレイ格納中などに使用)。 */
    public void pause() {
        running = false;
        if (tickTimeline != null) {
            tickTimeline.stop();
        }
    }

    /** 停止していた時刻更新を再開する。 */
    public void resume() {
        if (running) return;
        running = true;
        scheduleNextTick();
    }

    /** 次の秒の切り替わりタイミングに合わせて{@link #updateClock()}を1回だけ予約する(自己再スケジュール方式)。 */
    private void scheduleNextTick() {
        LocalDateTime now = LocalDateTime.now();
        long delay = 1000 - now.getNano() / 1_000_000;

        tickTimeline = new Timeline(new KeyFrame(Duration.millis(delay), e -> {
            updateClock();
            if (running) scheduleNextTick();
        }));
        tickTimeline.setCycleCount(1);
        tickTimeline.play();
    }

    /** 現在時刻を計算し、変化した桁だけ{@link #fadeLabel}でフェード切り替えする。 */
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();

        String nextHourMin = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String nextSec = now.format(DateTimeFormatter.ofPattern("ss"));
        String nextDate = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (EEE)", Locale.ENGLISH));

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

    /** ラベルをフェードアウトし、テキストを差し替えてからフェードインする。 */
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

    /** 現在のテーマカラー・フォントサイズ倍率を反映したラベルを生成する。 */
    private Label createLabel(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + (size * themeManager.getFontScale()) + "px;");
        l.setTextFill(themeManager.getTextColor());
        l.setEffect(themeManager.getShadow());
        return l;
    }

    /** テーマカラーが変わった際、全ラベルの文字色をフェードで追従させる。 */
    @Override
    public void onThemeChanged(Color textColor, Color wbColor, DropShadow shadow) {
        fadeTextColor(dateLabel, textColor);
        for (Label l : hourMinLabels) fadeTextColor(l, textColor);
        for (Label l : secLabels) fadeTextColor(l, textColor);
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
}