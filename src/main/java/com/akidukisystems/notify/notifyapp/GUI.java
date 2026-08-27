package com.akidukisystems.notify.notifyapp;

import java.io.File;

import com.akidukisystems.notify.notifyapp.controller.ctrl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * JavaFXアプリケーションのエントリーポイント。FXMLからメイン画面を構築し、
 * 壁紙読み込み中はスプラッシュ画面({@link SplashScreen})を、常駐用にタスクトレイ({@link TrayManager})を用意する。
 */
public class GUI extends Application {

    private static Weather staticWeather;
    private static Configure staticConfigure;

    public static Stage stage;
    public static TrayManager trayManager;

    /** {@link Core}から{@link Weather}/{@link Configure}を受け取り、後続の{@link #start}で使えるようstaticに保持する。 */
    public static void setClass(Weather weather, Configure configure) {
        staticWeather = weather;
        staticConfigure = configure;
    }

    /**
     * メイン画面(FXML)を読み込み、スプラッシュ画面を表示してから壁紙読み込みを開始する。
     * 壁紙が半数読み込まれた時点で本体ウィンドウに切り替わる。
     */
    @Override
    public void start(Stage stage) throws Exception {

        // ★ここで初期化する
        Core core = new Core();
        core.init();

        if (GUI.staticConfigure.getWallpaperPath().isBlank()) {
            promptForWallpaperFolder(GUI.staticConfigure);
        }

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml")
        );

        Parent root = loader.load();

        ctrl controller = loader.getController();

        Scene scene = new Scene(root, 1920, 1080);
        scene.getStylesheets().add(
            getClass().getResource("/fxml/style.css").toExternalForm()
        );

        // GUI.stageはcontroller.setClass()が同期的に壁紙0枚ケースのコールバックを呼ぶ可能性があるため、
        // それより前に必ず代入しておく(でないとコールバック内のGUI.stage.show()でNPEになる)
        GUI.stage = stage;

        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setTitle("Chronoscape");

        // 壁紙読み込み中は枠無しのスプラッシュ画面を表示し、半数読み込めたら本体ウィンドウに切り替える
        SplashScreen splash = new SplashScreen();
        splash.show();

        controller.setWallpaperLoadListener(splash::updateProgress, () -> {
            splash.close();

            GUI.stage.show();
            GUI.trayManager = new TrayManager();
            GUI.trayManager.install(GUI.stage, controller::pauseBackgroundUpdates, controller::resumeBackgroundUpdates);
        });

        controller.setClass(GUI.staticWeather, GUI.staticConfigure);
    }

    /** JavaFXアプリケーションを起動する。 */
    public static void launchApp(String[] args) {
        launch(args);
    }

    /**
     * 壁紙フォルダのパスが未設定のとき、フォルダ選択ダイアログを表示して選択結果を{@link Configure}に保存する。
     * キャンセルされた場合は空のまま進める(壁紙0枚時の案内メッセージが後段で表示される)。
     */
    private static void promptForWallpaperFolder(Configure configure) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("壁紙フォルダを選択してください");

        File selected = chooser.showDialog(null);
        if (selected != null) {
            configure.setWallpaperPath(selected.getAbsolutePath() + File.separator);
            configure.saveSettings();
        }
    }
}