package com.akidukisystems.notify.notifyapp;

import java.io.*;
import java.util.concurrent.CompletableFuture;

/**
 * Bluetoothマウスのバッテリー残量をPowerShell({@code Get-PnpDeviceProperty})経由で取得する(Windows専用)。
 */
public class BatteryManager {

    private static final String BATTERY_LEVEL_KEY_NAME = "{104EA319-6EE2-4701-BD47-8DDBF425BBE5} 2";

    private final String deviceInstanceId;

    /**
     * @param deviceInstanceId 対象デバイスのPnPデバイスインスタンスID(設定画面から変更可能)
     */
    public BatteryManager(String deviceInstanceId) {
        this.deviceInstanceId = deviceInstanceId;
    }

    /**
     * PowerShellを呼び出してバッテリー残量(%)を非同期に取得する。
     *
     * @return バッテリー残量(0〜100)を返すFuture。取得に失敗した場合は例外的に完了する
     */
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

    /**
     * バッテリー残量(0〜100)を5段階のアイコン番号(0,2,4,6,7)にマッピングし、{@code format}に埋め込んで返す。
     *
     * @param battery バッテリー残量(0〜100)
     * @param format  {@code %d}を含むアイコンパスの書式文字列(例: "/icons/svg/battery%d.svg")
     */
    public String getBatteryIcon(int battery, String format) {
        int index = Math.round(battery * 4.0f / 100.0f);

        int[] levels = {0, 2, 4, 6, 7};

        return String.format(format, levels[index]);
    }
}