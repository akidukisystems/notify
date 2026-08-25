package com.akidukisystems.notify.notifyapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.scene.paint.Color;

public class Configure {

    public static class WallpaperColor {
        public final Color mainColor;
        public final Color monoColor;
        public final List<String> tags;

        public WallpaperColor(Color mainColor, Color monoColor, List<String> tags) {
            this.mainColor = mainColor;
            this.monoColor = monoColor;
            this.tags = tags;
        }

        public Color getMainColor() { return mainColor; }
        public Color getMonoColor() { return monoColor; }
        public List<String> getTags() { return tags; }
    }

    // 設定の保存先。クラスパス埋め込みではなく実ファイルにして、設定画面から保存できるようにする
    private static final Path CONFIG_DIR = resolveConfigDir();
    private static final Path SETTINGS_FILE = CONFIG_DIR.resolve("settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path resolveConfigDir() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "NotifyApp");
        }
        return Paths.get(System.getProperty("user.home"), ".notifyapp");
    }

    public static String getSettingsFilePath() {
        return SETTINGS_FILE.toString();
    }

    private Map<String, WallpaperColor> wallpaperColors = new HashMap<>();

    private int blur = 0;
    private int wallpaperChangeSecond = 0;
    private int refreshWeatherSecond = 0;
    private int reverseUISecond = 0;
    private double fontScale = 1.0;
    private String apiKey = "";
    private String wallpaperPath = "";
    private String apiKeyJson = "";
    private String wallpaperMetadataPath = "";
    private double latitude;
    private double longitude;
    private String jmaAreaCode = "";
    private String jmaAreaName = "";
    private String mouseBatteryDeviceId = "";

    // --- getter ---
    public int getBlur() { return blur; }
    public int getWallpaperChangeSecond() { return wallpaperChangeSecond; }
    public int getRefreshWeatherSecond() { return refreshWeatherSecond; }
    public int getReverseUISecond() { return reverseUISecond; }
    public double getFontScale() { return fontScale; }
    public String getApiKey() { return apiKey; }
    public String getWallpaperPath() { return wallpaperPath; }
    public String getApiKeyJson() { return apiKeyJson; }
    public String getWallpaperMetadataPath() { return wallpaperMetadataPath; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getJmaAreaCode() { return jmaAreaCode; }
    public String getJmaAreaName() { return jmaAreaName; }
    public String getMouseBatteryDeviceId() { return mouseBatteryDeviceId; }

    // --- setter(設定画面用に追加) ---
    public void setBlur(int v) { blur = v; }
    public void setWallpaperChangeSecond(int v) { wallpaperChangeSecond = v; }
    public void setRefreshWeatherSecond(int v) { refreshWeatherSecond = v; }
    public void setReverseUISecond(int v) { reverseUISecond = v; }
    public void setFontScale(double v) { fontScale = v; }
    public void setWallpaperPath(String v) { wallpaperPath = v; }
    public void setApiKeyJson(String v) { apiKeyJson = v; }
    public void setWallpaperMetadataPath(String v) { wallpaperMetadataPath = v; }
    public void setLatitude(double v) { latitude = v; }
    public void setLongitude(double v) { longitude = v; }
    public void setJmaAreaCode(String v) { jmaAreaCode = v; }
    public void setJmaAreaName(String v) { jmaAreaName = v; }
    public void setMouseBatteryDeviceId(String v) { mouseBatteryDeviceId = v; }

    public void loadWallpaperColors() {
        Path path = Paths.get(wallpaperMetadataPath);

        if (wallpaperMetadataPath.isBlank() || !Files.exists(path)) {
            System.out.println(wallpaperMetadataPath + " が見つかりませんでした");
            return;
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            for (String key : json.keySet()) {
                JsonObject obj = json.getAsJsonObject(key);

                Color main = null;
                Color mono = null;

                if (obj.has("mainColor") && !obj.get("mainColor").isJsonNull()) {
                    main = Color.web(obj.get("mainColor").getAsString());
                }

                if (obj.has("monoColor") && !obj.get("monoColor").isJsonNull()) {
                    mono = Color.web(obj.get("monoColor").getAsString());
                }

                List<String> tags = new ArrayList<>();
                if (obj.has("tags") && obj.get("tags").isJsonArray()) {
                    obj.getAsJsonArray("tags").forEach(e -> tags.add(e.getAsString()));
                }

                wallpaperColors.put(key, new WallpaperColor(main, mono, tags));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, WallpaperColor> getWallpaperColors() {
        return wallpaperColors;
    }

    public WallpaperColor getColors(String wallpaperFileName) {
        return wallpaperColors.get(wallpaperFileName);
    }

    public void loadSettings() {
        ensureExternalConfigBootstrapped();

        if (!Files.exists(SETTINGS_FILE)) {
            System.out.println(SETTINGS_FILE + " が見つかりませんでした。初期設定で起動します。");
            return;
        }

        try (InputStream is = Files.newInputStream(SETTINGS_FILE)) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            blur = json.has("blur") ? json.get("blur").getAsInt() : 0;
            wallpaperChangeSecond = json.has("wallpaperChangeTime") ? json.get("wallpaperChangeTime").getAsInt() : 300;
            refreshWeatherSecond = json.has("refreshWeatherTime") ? json.get("refreshWeatherTime").getAsInt() : 1200;
            reverseUISecond = json.has("reverseUITime") ? json.get("reverseUITime").getAsInt() : 600;
            fontScale = json.has("fontScale") ? json.get("fontScale").getAsDouble() : 1.0;
            apiKeyJson = json.has("apiKeyJson") ? json.get("apiKeyJson").getAsString() : "";
            wallpaperPath = json.has("wallpaperPath") ? json.get("wallpaperPath").getAsString() : "";
            wallpaperMetadataPath = json.has("wallpaperMetadataPath") ? json.get("wallpaperMetadataPath").getAsString() : "";
            latitude = json.has("latitude") ? json.get("latitude").getAsDouble() : 0;
            longitude = json.has("longitude") ? json.get("longitude").getAsDouble() : 0;
            jmaAreaCode = json.has("jmaAreaCode") ? json.get("jmaAreaCode").getAsString() : "";
            jmaAreaName = json.has("jmaAreaName") ? json.get("jmaAreaName").getAsString() : "";
            mouseBatteryDeviceId = json.has("mouseBatteryDeviceId") ? json.get("mouseBatteryDeviceId").getAsString()
                    : "BTHLE\\DEV_DCEA530D8015\\A&CBCF98&0&DCEA530D8015";
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 旧バージョンのクラスパス形式("/config/...")のパスが残っていたら実ファイルへ移行する
        boolean needsResave = false;

        if (apiKeyJson.startsWith("/")) {
            apiKeyJson = migrateClasspathResourceToExternalFile(apiKeyJson, "apikey.json");
            needsResave = true;
        }

        if (wallpaperMetadataPath.startsWith("/")) {
            wallpaperMetadataPath = migrateClasspathResourceToExternalFile(wallpaperMetadataPath, "wallpaper_colors.json");
            needsResave = true;
        }

        if (needsResave) {
            saveSettings();
        }

        loadApiKey(apiKeyJson);
    }

    public void saveSettings() {
        try {
            Files.createDirectories(CONFIG_DIR);

            JsonObject json = new JsonObject();
            json.addProperty("blur", blur);
            json.addProperty("wallpaperChangeTime", wallpaperChangeSecond);
            json.addProperty("refreshWeatherTime", refreshWeatherSecond);
            json.addProperty("reverseUITime", reverseUISecond);
            json.addProperty("fontScale", fontScale);
            json.addProperty("apiKeyJson", apiKeyJson);
            json.addProperty("wallpaperPath", wallpaperPath);
            json.addProperty("wallpaperMetadataPath", wallpaperMetadataPath);
            json.addProperty("latitude", latitude);
            json.addProperty("longitude", longitude);
            json.addProperty("jmaAreaCode", jmaAreaCode);
            json.addProperty("jmaAreaName", jmaAreaName);
            json.addProperty("mouseBatteryDeviceId", mouseBatteryDeviceId);

            Files.writeString(SETTINGS_FILE, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadApiKey(String path) {
        if (path == null || path.isBlank()) {
            apiKey = "";
            return;
        }

        Path keyPath = Paths.get(path);

        if (!Files.exists(keyPath)) {
            System.out.println(path + " が見つかりませんでした");
            apiKey = "";
            return;
        }

        try (InputStream is = Files.newInputStream(keyPath)) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            apiKey = json.has("key") ? json.get("key").getAsString() : "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveApiKey(String newKey) {
        this.apiKey = newKey;

        if (apiKeyJson == null || apiKeyJson.isBlank()) {
            apiKeyJson = CONFIG_DIR.resolve("apikey.json").toString();
        }

        try {
            Path keyPath = Paths.get(apiKeyJson);
            Path parent = keyPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            JsonObject json = new JsonObject();
            json.addProperty("key", newKey);

            Files.writeString(keyPath, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- 初回起動時: クラスパス同梱のデフォルト設定を実ファイルへコピーする ---
    private void ensureExternalConfigBootstrapped() {
        if (Files.exists(SETTINGS_FILE)) {
            return;
        }

        try (InputStream is = getClass().getResourceAsStream("/config/settings.json")) {
            if (is == null) {
                return;
            }
            Files.createDirectories(CONFIG_DIR);
            Files.copy(is, SETTINGS_FILE);
            System.out.println("初期設定を " + SETTINGS_FILE + " に作成しました");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- 旧バージョンのクラスパス形式パスを実ファイルへコピーし、新しい絶対パスを返す ---
    private String migrateClasspathResourceToExternalFile(String classpathStylePath, String targetFileName) {
        Path target = CONFIG_DIR.resolve(targetFileName);

        if (!Files.exists(target)) {
            try (InputStream is = getClass().getResourceAsStream(classpathStylePath)) {
                if (is != null) {
                    Files.createDirectories(CONFIG_DIR);
                    Files.copy(is, target);
                    System.out.println(classpathStylePath + " を " + target + " へ移行しました");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return target.toString();
    }
}