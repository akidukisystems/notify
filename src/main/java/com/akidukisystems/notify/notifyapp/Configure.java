package com.akidukisystems.notify.notifyapp;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        public Color getMainColor() {
            return mainColor;
        }

        public Color getMonoColor() {
            return monoColor;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    private Map<String, WallpaperColor> wallpaperColors = new HashMap<>();

    private int blur = 0;
    private int wallpaperChangeSecond = 0;
    private int refreshWeatherSecond = 0;
    private int reverseUISecond = 0;
    private String apiKey;
    private String wallpaperPath;
    private String apiKeyJson;


    public int getBlur() {
        return blur;
    }

    public int getWallpaperChangeSecond() {
        return wallpaperChangeSecond;
    }

    public int getRefreshWeatherSecond() {
        return refreshWeatherSecond;
    }

    public int getReverseUISecond() {
        return reverseUISecond;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getWallpaperPath() {
        return wallpaperPath;
    }

    public String getApiKeyJson() {
        return apiKeyJson;
    }

    public void loadWallpaperColors() {
        try (InputStream is = getClass().getResourceAsStream("/config/wallpaper_colors.json")) {
            if (is != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();

                for (String key : json.keySet()) {
                    JsonObject obj = json.getAsJsonObject(key);

                    Color main = null;
                    Color mono = null;

                    // nullチェック付きで取得
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
            } else {
                System.out.println("/config/wallpaper_colors.json が見つかりませんでした");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, WallpaperColor> getWallpaperColors() {
        return wallpaperColors;
    }

    public WallpaperColor getColors(String wallpaperFileName) {
        return wallpaperColors.get(wallpaperFileName); // なければ null が返る
    }

    public void loadSettings() {
        try (InputStream is = getClass().getResourceAsStream("/config/settings.json")) {
            if (is != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                blur = json.get("blur").getAsInt();
                wallpaperChangeSecond = json.get("wallpaperChangeTime").getAsInt();
                refreshWeatherSecond = json.get("refreshWeatherTime").getAsInt();
                reverseUISecond = json.get("reverseUITime").getAsInt();
                apiKeyJson = json.get("apiKeyJson").getAsString();
                wallpaperPath = json.get("wallpaperPath").getAsString();
            } else {
                System.out.println("/config/settings.json が見つかりませんでした");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadApiKey(apiKeyJson);
    }

    private void loadApiKey(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                apiKey = json.get("key").getAsString();
            } else {
                System.out.println(path +" が見つかりませんでした");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}