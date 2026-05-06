package com.akidukisystems.notify.notifyapp.controller;

import org.girod.javafx.svgimage.SVGLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

public class SVGHelper {

    public Node createSVG(String resourcePath, double size, Color color) {
        try {
            Node node = SVGLoader.load(getClass().getResource(resourcePath));

            StackPane wrapper = new StackPane(node);

            wrapper.setPrefSize(size, size);
            wrapper.setMinSize(size, size);
            wrapper.setMaxSize(size, size);

            applyColor(node, color);

            StackPane.setAlignment(node, Pos.CENTER);

            return wrapper;

        } catch (Exception e) {
            e.printStackTrace();
            return new StackPane();
        }
    }

    private void applyColor(Node node, Color color) {
        if (node instanceof javafx.scene.shape.Shape shape) {
            shape.setFill(color);
        }

        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyColor(child, color);
            }
        }
    }
}