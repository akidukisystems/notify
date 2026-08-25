package com.akidukisystems.notify.notifyapp;

public class Core {
    protected Configure configure;

    public void init() {
        configure = new Configure();
        configure.loadSettings(); 
        Weather weather = new Weather();
        weather.setCore(this);

        // GUIは触らない
        GUI.setClass(weather, configure);
    }
}
