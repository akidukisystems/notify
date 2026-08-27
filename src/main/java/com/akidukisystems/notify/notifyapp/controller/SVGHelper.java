package com.akidukisystems.notify.notifyapp.controller;

import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * SVGアイコンの読み込み・拡縮・色付け・影付けをまとめる共通ヘルパー。
 */
public class SVGHelper {

    /**
     * クラスパス上のSVGリソースを読み込み、指定サイズに拡縮した上で色と影を適用した{@link SVGIcon}を返す。
     *
     * @param resourcePath クラスパス上のSVGファイルパス(例: "/icons/svg/refresh.svg")
     * @param size         アイコンの一辺のサイズ
     * @param color        塗り・線の色
     * @param shadow       適用するドロップシャドウ
     */
    public SVGIcon createIcon(String resourcePath, double size, Color color, DropShadow shadow) {
        SVGImage svg = SVGLoader.load(getClass().getResource(resourcePath));
        SVGImage scaled = svg.scaleTo(size);

        SVGIcon icon = new SVGIcon(scaled, size);
        icon.setColor(color);
        icon.setEffect(shadow);

        return icon;
    }
}
