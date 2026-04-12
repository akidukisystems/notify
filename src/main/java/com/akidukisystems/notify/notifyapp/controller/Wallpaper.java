package com.akidukisystems.notify.notifyapp.controller;

import java.util.List;

public class Wallpaper {
    public String fileName;
    public List<String> tags; // "morning", "day", "night" など

    public Wallpaper(String fileName, List<String> tags) {
        this.fileName = fileName;
        this.tags = tags;
    }
}