package com.akidukisystems.notify.notifyapp;

/**
 * アプリ起動時の初期化処理をまとめるクラス。設定の読み込みと{@link Weather}・{@link GUI}への橋渡しを行う。
 */
public class Core {
    protected Configure configure;

    /**
     * 設定を読み込み、{@link Weather}を生成して{@link GUI}に渡す。
     */
    public void init() {
        configure = new Configure();
        configure.loadSettings(); 
        Weather weather = new Weather();
        weather.setCore(this);

        // GUIは触らない
        GUI.setClass(weather, configure);
    }
}
