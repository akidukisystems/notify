package com.akidukisystems.notify.notifyapp;

import java.io.*;
import java.util.concurrent.CompletableFuture;

public class BatteryManager {

    private static final String BATTERY_LEVEL_KEY_NAME = "{104EA319-6EE2-4701-BD47-8DDBF425BBE5} 2";

    private final String deviceInstanceId;

    public BatteryManager(String deviceInstanceId) {
        this.deviceInstanceId = deviceInstanceId;
    }

    public CompletableFuture<Integer> getMouseBattery() {
        return CompletableFuture.supplyAsync(() -> {
            String command = "(Get-PnpDeviceProperty -InstanceId '" + deviceInstanceId
                    + "' -KeyName '" + BATTERY_LEVEL_KEY_NAME + "').Data";

            try {
                Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-Command",
                    command
                ).redirectErrorStream(true).start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {

                    String line = reader.readLine();
                    int exitCode = process.waitFor();

                    if (exitCode != 0 || line == null || line.isBlank()) {
                        throw new IOException("バッテリー残量を取得できませんでした。");
                    }

                    return Integer.parseInt(line.trim());
                }

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String getBatteryIcon(int battery, String format) {
        int index = Math.round(battery * 4.0f / 100.0f);

        int[] levels = {0, 2, 4, 6, 7};

        return String.format(format, levels[index]);
    }
}