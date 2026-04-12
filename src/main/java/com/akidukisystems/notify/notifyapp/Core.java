package com.akidukisystems.notify.notifyapp;

public class Core {
    protected Configure configure;

    public void init() {
        configure = new Configure();
        Weather weather = new Weather();
        weather.setCore(this);

        // GUIは触らない
        GUI.setClass(weather, configure);
    }
}
