package com.akidukisystems.notify.notifyapp;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.stage.Stage;

// ウィンドウを閉じてもアプリを終了させず、タスクトレイに常駐させるための管理クラス
public class TrayManager {

    private TrayIcon trayIcon;

    // onHide: 非表示になった直後、onShow: 表示に復帰した直後に呼ばれる(バックグラウンド更新の一時停止/再開などに利用)
    public void install(Stage stage, Runnable onHide, Runnable onShow) {
        if (!SystemTray.isSupported()) {
            System.out.println("システムトレイはサポートされていません");
            return;
        }

        // トレイに格納している間もJavaFXランタイムを終了させない
        Platform.setImplicitExit(false);

        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("開く");
        openItem.addActionListener(e -> Platform.runLater(() -> {
            showStage(stage);
            if (onShow != null) onShow.run();
        }));

        MenuItem maximizeItem = new MenuItem("最大化");
        maximizeItem.addActionListener(e -> Platform.runLater(() -> {
            maximizeStage(stage);
            if (onShow != null) onShow.run();
        }));

        MenuItem exitItem = new MenuItem("終了");
        exitItem.addActionListener(e -> Platform.runLater(this::exitApp));

        popup.add(openItem);
        popup.add(maximizeItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(createIconImage(), "NotifyApp", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    Platform.runLater(() -> {
                        showStage(stage);
                        if (onShow != null) onShow.run();
                    });
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // ウィンドウの×ボタンなどでの終了要求は、トレイへの格納に読み替える
        stage.setOnCloseRequest(e -> {
            e.consume();
            hideStage(stage);
            if (onHide != null) onHide.run();
        });

        // タイトルバーの最小化(タスクバーへの格納)も、トレイへの格納に統一する
        stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (isIconified) {
                Platform.runLater(() -> {
                    hideStage(stage);
                    if (onHide != null) onHide.run();
                });
            }
        });
    }

    public void hideStage(Stage stage) {
        stage.hide();
    }

    private void showStage(Stage stage) {
        stage.setIconified(false);
        stage.show();
        stage.toFront();
    }

    private void maximizeStage(Stage stage) {
        stage.setIconified(false);
        stage.show();
        stage.toFront();
        stage.setFullScreen(true);
    }

    private void exitApp() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        Platform.exit();
        System.exit(0);
    }

    // 専用の画像アセットを持たないため、シンプルなバッジをその場で描画する
    private Image createIconImage() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x21, 0x96, 0xF3));
        g.fillOval(0, 0, size, size);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String label = "N";
        int textWidth = fm.stringWidth(label);
        int baseline = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(label, (size - textWidth) / 2f, baseline);

        g.dispose();
        return image;
    }
}
