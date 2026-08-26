package com.akidukisystems.notify.notifyapp;

import com.akidukisystems.notify.notifyapp.controller.ctrl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUI extends Application {

    private static Weather staticWeather;
    private static Configure staticConfigure;

    public static Stage stage;
    public static TrayManager trayManager;

    public static void setClass(Weather weather, Configure configure) {
        staticWeather = weather;
        staticConfigure = configure;
    }

    @Override
    public void start(Stage stage) throws Exception {

        // ★ここで初期化する
        Core core = new Core();
        core.init();

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml")
        );

        Parent root = loader.load();

        ctrl controller = loader.getController();

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

        Scene scene = new Scene(root, 1920, 1080);
        scene.getStylesheets().add(
            getClass().getResource("/fxml/style.css").toExternalForm()
        );

        GUI.stage = stage;

        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setTitle("壁紙プレビュー");
    }

    public static void launchApp(String[] args) {
        launch(args);
    }
}