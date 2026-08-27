package com.akidukisystems.notify.notifyapp;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * 画像を間引きサンプリングして代表色を求め、テーマ用のテキスト色・アクセント色を計算するヘルパー。
 */
public class ColorHelper {

    /**
     * 画像を{@code step}間隔でサンプリングして平均色を求め、そこから
     * 文字色(白/黒)・補色ベースのアクセント色・強調したアクセント色の3色を導出する。
     *
     * @param image 解析対象の画像(壁紙のスナップショットなど)
     * @return {文字色, アクセント色, 強調アクセント色}の3要素配列
     */
    public Color[] calculateColors(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int step = 10;

        double rSum = 0, gSum = 0, bSum = 0;
        int count = 0;

        var reader = image.getPixelReader();
        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                Color c = reader.getColor(x, y);
                rSum += c.getRed();
                gSum += c.getGreen();
                bSum += c.getBlue();
                count++;
            }
        }

        // 0サイズなど異常なスナップショットではNaNになるため、無難な既定色にフォールバックする
        if (count == 0) {
            return new Color[]{Color.WHITE, Color.GRAY, Color.WHITE};
        }

        double rAvg = rSum / count;
        double gAvg = gSum / count;
        double bAvg = bSum / count;

        double[] hsv = rgbToHsv(rAvg, gAvg, bAvg);
        hsv[0] = (hsv[0] + 180) % 360;
        hsv[1] = Math.min(hsv[1] * 1.5, 1.0);
        Color accentColor = hsvToColor(hsv[0], hsv[1], hsv[2]);

        double luminance = 0.2126 * rAvg + 0.7152 * gAvg + 0.0722 * bAvg;
        Color textBase = luminance > 0.5 ? Color.BLACK : Color.WHITE;
        double factor = textBase.equals(Color.BLACK) ? 0.5 : 1.7;
        Color finalAccent = Color.color(
            Math.min(accentColor.getRed() * factor, 1.0),
            Math.min(accentColor.getGreen() * factor, 1.0),
            Math.min(accentColor.getBlue() * factor, 1.0)
        );

        return new Color[]{textBase, accentColor, finalAccent};
    }

    /**
     * RGB(各0〜1)をHSV({@code h}は0〜360度、{@code s}/{@code v}は0〜1)に変換する。
     */
    private double[] rgbToHsv(double r, double g, double b) {
        double cMax = Math.max(r, Math.max(g, b));
        double cMin = Math.min(r, Math.min(g, b));
        double delta = cMax - cMin;

        double h = 0;
        if (delta != 0) {
            if (cMax == r) h = 60 * (((g - b) / delta) % 6);
            else if (cMax == g) h = 60 * (((b - r) / delta) + 2);
            else h = 60 * (((r - g) / delta) + 4);
        }
        if (h < 0) h += 360;

        double s = cMax == 0 ? 0 : delta / cMax;
        double v = cMax;

        return new double[]{h, s, v};
    }

    /**
     * HSVをJavaFXの{@link Color}に変換する。
     */
    private Color hsvToColor(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs((h / 60) % 2 - 1));
        double m = v - c;

        double r1 = 0, g1 = 0, b1 = 0;
        if (h < 60) { r1 = c; g1 = x; b1 = 0; }
        else if (h < 120) { r1 = x; g1 = c; b1 = 0; }
        else if (h < 180) { r1 = 0; g1 = c; b1 = x; }
        else if (h < 240) { r1 = 0; g1 = x; b1 = c; }
        else if (h < 300) { r1 = x; g1 = 0; b1 = c; }
        else { r1 = c; g1 = 0; b1 = x; }

        return Color.color(r1 + m, g1 + m, b1 + m);
    }
}
