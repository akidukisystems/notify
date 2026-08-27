package com.akidukisystems.notify.notifyapp.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * SVGから読み込んだノードを固定サイズの{@link StackPane}にラップし、後から色・影を差し替えられるようにするアイコン部品。
 */
public class SVGIcon extends StackPane {

    private final Node svgNode;

    /**
     * @param svgNode ラップするSVGノード(スケール済みのもの)
     * @param size    アイコンの一辺のサイズ(正方形)
     */
    public SVGIcon(Node svgNode, double size) {
        this.svgNode = svgNode;

        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        setAlignment(Pos.CENTER);
        getChildren().add(svgNode);
    }

    /**
     * アイコン内の全シェイプの塗り・線の色をまとめて差し替える。
     */
    public void setColor(Color color) {
        applyColor(svgNode, color);
    }

    /**
     * ノードツリーを再帰的にたどり、{@link javafx.scene.shape.Shape}の塗り・線色を{@code color}に上書きする。
     */
    private void applyColor(Node node, Color color) {
        if (node instanceof javafx.scene.shape.Shape shape) {
            shape.setFill(color);
            shape.setStroke(color);
        }

        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyColor(child, color);
            }
        }
    }

    /**
     * アイコンにドロップシャドウ効果を設定する。
     */
    public void setShadow(double radius, Color color) {
        setEffect(new DropShadow(radius, color));
    }
}
