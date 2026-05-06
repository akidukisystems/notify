package com.akidukisystems.notify.notifyapp.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class SVGIcon extends StackPane {

    private final Node svgNode;

    public SVGIcon(Node svgNode, double size) {
        this.svgNode = svgNode;

        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        setAlignment(Pos.CENTER);
        getChildren().add(svgNode);
    }

    public void setColor(Color color) {
        applyColor(svgNode, color);
    }

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

    public void setShadow(double radius, Color color) {
        setEffect(new DropShadow(radius, color));
    }
}
