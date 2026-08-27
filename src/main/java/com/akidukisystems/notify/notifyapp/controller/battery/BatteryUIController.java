package com.akidukisystems.notify.notifyapp.controller.battery;

import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import com.akidukisystems.notify.notifyapp.BatteryManager;
import com.akidukisystems.notify.notifyapp.controller.SVGIcon;
import com.akidukisystems.notify.notifyapp.controller.theme.ThemeManager;

import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Bluetoothマウスのバッテリー残量アイコンを表示・更新するコントローラー(Windows専用)。
 */
public class BatteryUIController implements ThemeManager.ThemeListener {

    private final ThemeManager themeManager;
    private final BatteryManager batteryManager;

    // ctrl.java側はこの入れ物だけを一度buttonsBoxに追加すればよい
    private final StackPane container = new StackPane();

    private SVGIcon currentIcon;

    /**
     * @param themeManager        テーマカラー変更を購読するための{@link ThemeManager}
     * @param mouseBatteryDeviceId バッテリー取得対象のBluetoothデバイスID
     */
    public BatteryUIController(ThemeManager themeManager, String mouseBatteryDeviceId) {
        this.themeManager = themeManager;
        this.batteryManager = new BatteryManager(mouseBatteryDeviceId);
        themeManager.addListener(this);

        setIcon("/icons/svg/batterymissing.svg");
    }

    /** バッテリーアイコンを表示するノードを返す(ボタン列に一度だけ追加すればよい)。 */
    public StackPane getNode() {
        return container;
    }

    /** バッテリー残量を非同期取得し、アイコンを更新する(天気更新のタイミングなどから呼ばれる)。 */
    public void refresh() {
        batteryManager.getMouseBattery().thenAccept(battery -> {
            System.out.println("Battery: " + battery + "%");
            String iconPath = batteryManager.getBatteryIcon(battery, "/icons/svg/battery%d.svg");
            Platform.runLater(() -> setIcon(iconPath));
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    /** 指定パスのSVGアイコンを読み込み、テーマカラーで着色して表示する。 */
    private void setIcon(String path) {
        SVGImage svg = SVGLoader.load(getClass().getResource(path));
        SVGImage scaled = svg.scaleTo(32);

        currentIcon = new SVGIcon(scaled, 32);
        currentIcon.setColor(themeManager.getTextColor());
        currentIcon.setEffect(themeManager.getShadow());

        container.getChildren().setAll(currentIcon);
    }

    /** テーマカラーが変わった際、現在表示中のアイコンの色を追従させる。 */
    @Override
    public void onThemeChanged(Color textColor, Color wbColor, DropShadow shadow) {
        if (currentIcon != null) {
            currentIcon.setColor(textColor);
        }
    }
}