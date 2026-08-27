package com.akidukisystems.notify.notifyapp.controller.theme;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * 壁紙から抽出したテーマカラー(文字色・影色)を一括管理し、登録済みリスナーに変更を通知するハブ。
 * Clock/Weather/Battery各UIコントローラーはこれを購読して色を追従させる。
 */
public class ThemeManager {

    /** テーマカラーの変更通知を受け取るリスナー。 */
    public interface ThemeListener {
        /** テーマカラーが変わった際に呼ばれる。 */
        void onThemeChanged(Color textColor, Color wbColor, DropShadow shadow);
    }

    // 壁紙からの色抽出は非同期で後から届くため、それまでの間もLabel生成時にnullにならないよう既定色を持たせる
    private Color fixedTextColor = Color.WHITE;
    private Color wbColor = Color.BLACK;
    private double fontScale = 1.0;

    // 使い回す1個だけのインスタンス。以後は中身(色)だけ書き換える
    private final DropShadow shadow = createBaseShadow();

    private final List<ThemeListener> listeners = new ArrayList<>();

    /** 既定色(白文字/黒背景相当)でシャドウを初期化する。 */
    public ThemeManager() {
        updateShadowColor();
    }

    /** テーマカラー変更の通知先リスナーを登録する。 */
    public void addListener(ThemeListener listener) {
        listeners.add(listener);
    }

    /** テーマカラーを更新し、登録済みの全リスナーに変更を通知する。 */
    public void applyColors(Color textColor, Color wbColor) {
        this.fixedTextColor = textColor;
        this.wbColor = wbColor;
        updateShadowColor();
        listeners.forEach(l -> l.onThemeChanged(fixedTextColor, this.wbColor, shadow));
    }

    /** 使い回す基本のドロップシャドウインスタンスを生成する(色は後から{@link #updateShadowColor}で書き換える)。 */
    private DropShadow createBaseShadow() {
        DropShadow ds = new DropShadow();
        ds.setOffsetX(2);
        ds.setOffsetY(2);
        ds.setRadius(16);
        ds.setSpread(0.4);
        return ds;
    }

    /** 背景色(白黒どちらに近いか)に応じてシャドウの色だけ書き換える(インスタンスは差し替えない)。 */
    private void updateShadowColor() {
        shadow.setColor(wbColor.equals(Color.BLACK) ? Color.color(1, 1, 1, 1) : Color.color(0, 0, 0, 0.75));
    }

    /** 現在のテキスト色を返す。 */
    public Color getTextColor() {
        return fixedTextColor;
    }

    /** 現在の背景系(白/黒)の代表色を返す。 */
    public Color getWbColor() {
        return wbColor;
    }

    /** 使い回しの{@link DropShadow}インスタンスを返す。 */
    public DropShadow getShadow() {
        return shadow;
    }

    /** フォントサイズ倍率を設定する。 */
    public void setFontScale(double fontScale) {
        this.fontScale = fontScale;
    }

    /** フォントサイズ倍率を返す。 */
    public double getFontScale() {
        return fontScale;
    }
}