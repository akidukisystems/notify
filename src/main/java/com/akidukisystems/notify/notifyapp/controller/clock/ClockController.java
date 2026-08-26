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

public class ClockController implements ThemeManager.ThemeListener {

    private final ThemeManager themeManager;

    private final Label[] hourMinLabels = new Label[5]; // "HH:mm"
    private final Label[] secLabels = new Label[2];     // "ss"
    private final Label dateLabel;

    private final HBox timeBox;
    private final HBox secBox;

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
    public HBox getTimeBox() { return timeBox; }
    public HBox getSecBox() { return secBox; } // 夜間モードのfadeOut対象として引き続き必要
    public Label getDateLabel() { return dateLabel; }

    private boolean running = false;
    private Timeline tickTimeline;

    public void start() {
        running = true;
        scheduleNextTick();
    }

    // トレイ格納中などに時刻更新を止める
    public void pause() {
        running = false;
        if (tickTimeline != null) {
            tickTimeline.stop();
        }
    }

    public void resume() {
        if (running) return;
        running = true;
        scheduleNextTick();
    }

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

    private Label createLabel(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + (size * themeManager.getFontScale()) + "px;");
        l.setTextFill(themeManager.getTextColor());
        l.setEffect(themeManager.getShadow());
        return l;
    }

    @Override
    public void onThemeChanged(Color textColor, Color wbColor, DropShadow shadow) {
        fadeTextColor(dateLabel, textColor);
        for (Label l : hourMinLabels) fadeTextColor(l, textColor);
        for (Label l : secLabels) fadeTextColor(l, textColor);
    }

    private void fadeTextColor(Label label, Color newColor) {
        final Color oldColor = (Color) label.getTextFill();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e -> label.setTextFill(oldColor)),
            new KeyFrame(Duration.millis(300), new KeyValue(label.textFillProperty(), newColor))
        );
        timeline.play();
    }
}