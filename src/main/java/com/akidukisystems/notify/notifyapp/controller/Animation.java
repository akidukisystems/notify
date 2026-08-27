package com.akidukisystems.notify.notifyapp.controller;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * ノードのフェードイン/フェードアウトをまとめた小さなアニメーションヘルパー。
 */
public class Animation {

    /**
     * 300msかけてノードをフェードアウトし、完了後に非表示にする。
     *
     * @param node ノード
     * @param isManaging フェードアウト後、レイアウト計算の対象に含め続けるか({@code setManaged}に渡す値)
     */
    public void fadeOut(Node node, boolean isManaging) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        fade.setOnFinished(e -> {
            node.setVisible(false);   // 完全に消す
            node.setManaged(isManaging);
            node.setOpacity(1.0);     // 次回表示のために戻す
        });

        fade.play();
    }

    /**
     * ノードを表示状態にしてから300msかけてフェードインする。
     */
    public void fadeIn(Node node) {
        node.setOpacity(0.0);
        node.setVisible(true);
        node.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        fade.play();
    }
}
