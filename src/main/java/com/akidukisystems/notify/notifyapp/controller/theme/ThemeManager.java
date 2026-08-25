package com.akidukisystems.notify.notifyapp.controller.theme;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class ThemeManager {

    public interface ThemeListener {
        void onThemeChanged(Color textColor, Color wbColor, DropShadow shadow);
    }

    private Color fixedTextColor;
    private Color wbColor;

    // 使い回す1個だけのインスタンス。以後は中身(色)だけ書き換える
    private final DropShadow shadow = createBaseShadow();

    private final List<ThemeListener> listeners = new ArrayList<>();

    public void addListener(ThemeListener listener) {
        listeners.add(listener);
    }

    public void initColors(Color textColor, Color wbColor) {
        this.fixedTextColor = textColor;
        this.wbColor = wbColor;
        updateShadowColor();
    }

    public void applyColors(Color textColor, Color wbColor) {
        this.fixedTextColor = textColor;
        this.wbColor = wbColor;
        updateShadowColor();
        listeners.forEach(l -> l.onThemeChanged(fixedTextColor, this.wbColor, shadow));
    }

    private DropShadow createBaseShadow() {
        DropShadow ds = new DropShadow();
        ds.setOffsetX(2);
        ds.setOffsetY(2);
        ds.setRadius(16);
        ds.setSpread(0.4);
        return ds;
    }

    // 色だけ書き換える(インスタンスは差し替えない)
    private void updateShadowColor() {
        shadow.setColor(wbColor.equals(Color.BLACK) ? Color.color(1, 1, 1, 1) : Color.color(0, 0, 0, 0.75));
    }

    public Color getTextColor() {
        return fixedTextColor;
    }

    public Color getWbColor() {
        return wbColor;
    }

    public DropShadow getShadow() {
        return shadow;
    }
}