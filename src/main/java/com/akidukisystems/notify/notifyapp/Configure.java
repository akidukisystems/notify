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

/**
 * アプリの設定値を保持し、外部JSONファイル({@code settings.json}/{@code apikey.json})との読み書きを担うクラス。
 * 設定画面({@code SettingsController})から変更された値もここを経由して永続化される。
 */
public class Configure {

    /** 壁紙1枚分のテーマカラー(文字色・単色アクセント)とタグを保持するデータクラス。 */
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

    /**
     * 設定保存先ディレクトリを決定する。{@code %APPDATA%}が使えるWindowsではその下、
     * それ以外のOSではユーザーホーム直下の{@code .notifyapp}を使う。
     */
    private static Path resolveConfigDir() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "NotifyApp");
        }
        return Paths.get(System.getProperty("user.home"), ".notifyapp");
    }

    /** 設定ファイルの絶対パスを返す(設定画面での案内表示用)。 */
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
    /** 壁紙の背景ぼかし量を返す(次回起動時に反映)。 */
    public int getBlur() { return blur; }
    /** 壁紙自動切替の周期(秒)を返す。 */
    public int getWallpaperChangeSecond() { return wallpaperChangeSecond; }
    /** 天気更新の周期(秒)を返す。 */
    public int getRefreshWeatherSecond() { return refreshWeatherSecond; }
    /** UI左右反転の周期(秒)を返す。 */
    public int getReverseUISecond() { return reverseUISecond; }
    /** フォントサイズ倍率を返す(次回起動時に反映)。 */
    public double getFontScale() { return fontScale; }
    /** OpenWeather APIキーを返す。 */
    public String getApiKey() { return apiKey; }
    /** 壁紙フォルダのパスを返す。 */
    public String getWallpaperPath() { return wallpaperPath; }
    /** APIキーを保存しているJSONファイルのパスを返す。 */
    public String getApiKeyJson() { return apiKeyJson; }
    /** 壁紙メタデータ(色・タグ)JSONファイルのパスを返す。 */
    public String getWallpaperMetadataPath() { return wallpaperMetadataPath; }
    /** 天気取得に使う緯度を返す。 */
    public double getLatitude() { return latitude; }
    /** 天気取得に使う経度を返す。 */
    public double getLongitude() { return longitude; }
    /** 気象警報取得対象の市区町村コードを返す。 */
    public String getJmaAreaCode() { return jmaAreaCode; }
    /** 表示用の地域名を返す。 */
    public String getJmaAreaName() { return jmaAreaName; }
    /** バッテリー残量取得対象のBluetoothデバイスIDを返す。 */
    public String getMouseBatteryDeviceId() { return mouseBatteryDeviceId; }

    // --- setter(設定画面用に追加) ---
    /** 背景ぼかし量を設定する。 */
    public void setBlur(int v) { blur = v; }
    /** 壁紙自動切替の周期(秒)を設定する。 */
    public void setWallpaperChangeSecond(int v) { wallpaperChangeSecond = v; }
    /** 天気更新の周期(秒)を設定する。 */
    public void setRefreshWeatherSecond(int v) { refreshWeatherSecond = v; }
    /** UI左右反転の周期(秒)を設定する。 */
    public void setReverseUISecond(int v) { reverseUISecond = v; }
    /** フォントサイズ倍率を設定する。 */
    public void setFontScale(double v) { fontScale = v; }
    /** 壁紙フォルダのパスを設定する。 */
    public void setWallpaperPath(String v) { wallpaperPath = v; }
    /** APIキー保存先JSONファイルのパスを設定する。 */
    public void setApiKeyJson(String v) { apiKeyJson = v; }
    /** 壁紙メタデータJSONファイルのパスを設定する。 */
    public void setWallpaperMetadataPath(String v) { wallpaperMetadataPath = v; }
    /** 緯度を設定する。 */
    public void setLatitude(double v) { latitude = v; }
    /** 経度を設定する。 */
    public void setLongitude(double v) { longitude = v; }
    /** 気象警報取得対象の市区町村コードを設定する。 */
    public void setJmaAreaCode(String v) { jmaAreaCode = v; }
    /** 表示用の地域名を設定する。 */
    public void setJmaAreaName(String v) { jmaAreaName = v; }
    /** バッテリー残量取得対象のBluetoothデバイスIDを設定する。 */
    public void setMouseBatteryDeviceId(String v) { mouseBatteryDeviceId = v; }

    /**
     * {@link #wallpaperMetadataPath}のJSONを読み込み、壁紙ファイル名ごとのテーマカラー・タグを{@link #wallpaperColors}に格納する。
     */
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

    /** 壁紙ファイル名をキーとするテーマカラー一覧を返す。 */
    public Map<String, WallpaperColor> getWallpaperColors() {
        return wallpaperColors;
    }

    /** 指定した壁紙ファイルのテーマカラーを返す。未登録の場合は{@code null}。 */
    public WallpaperColor getColors(String wallpaperFileName) {
        return wallpaperColors.get(wallpaperFileName);
    }

    /**
     * 設定ファイルを読み込む。初回起動時はクラスパス同梱の初期設定をブートストラップし、
     * 旧バージョンのクラスパス形式パスが残っていれば実ファイルへ移行した上で保存し直す。
     */
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

    /** 現在の設定値を{@code settings.json}へ書き出す。 */
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

    /** 指定パスのJSONファイルからAPIキーを読み込み、{@link #apiKey}に設定する。 */
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

    /** APIキーを{@link #apiKeyJson}のファイルへ保存する(未設定なら設定フォルダ配下に新規作成)。 */
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

    /** 初回起動時、クラスパス同梱のデフォルト設定を実ファイルへコピーする。 */
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

    /** 旧バージョンのクラスパス形式パスを実ファイルへコピーし、新しい絶対パスを返す。 */
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