package com.akidukisystems.notify.notifyapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Weather {
    int weatherID;          // 天気ID
    String weatherString;   // 天候（完結）
    String weatherDetail;   // 天候（詳細）
    String weatherIcon;     // アイコン

    double temp;            // 気温　K
    double feelingTemp;     // 体感気温 K

    int pressure;           // 大気圧 hpa
    double humidity;        // 湿度　RH%
    int visibility;         // 視界 m

    double windSpeed;       // 風速 m/s
    int windDeg;            // 風向き degree

    int clouds;             // 雲量 %
    double rain;            // 降雨量 mm/h
    double snow;            // 降雪量 mm/h

    double uvi;             // 紫外線指数

    long fetchTime;         // 取得時刻 UNIXtime
    long sunrise;           // 日の出時刻 同上
    long sunset;            // 日の入時刻 同上

    String area;            // 地域
    int timezone;           // タイムゾーン（秒単位）

    private Core core;

    public void setCore(Core core) {
        this.core = core;
    }

    public double getRain() {
        return rain;
    }

    public double getSnow() {
        return snow;
    }

    public double getUvi() {
        return uvi;
    }

    public int getWeatherID() {
        return weatherID;
    }

    public String getWeatherString() {
        return weatherString;
    }

    public String getWeatherDetail() {
        return weatherDetail;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public double getTemp() {
        return temp;
    }

    public double getFeelingTemp() {
        return feelingTemp;
    }

    public int getPressure() {
        return pressure;
    }

    public double getHumidity() {
        return humidity;
    }

    public int getVisibility() {
        return visibility;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public int getWindDeg() {
        return windDeg;
    }

    public int getClouds() {
        return clouds;
    }

    public long getFetchTime() {
        return fetchTime;
    }

    public long getSunrise() {
        return sunrise;
    }

    public long getSunset() {
        return sunset;
    }

    public String getArea() {
        return area;
    }

    public int getTimezone() {
        return timezone;
    }

    public static class Alert {
        public String sender;
        public String event;
        public long start;
        public long end;
        public String description;
        public List<String> tags = new ArrayList<>();
    }

    public static class Daily {
        public double temp_day;
        public double temp_min;
        public double temp_max;
        public int weatherID;
        public String weatherString;
        public String weatherDetail;
        public String weatherIcon;
        public double moon_phase;
    }

    private List<Alert> alerts = new ArrayList<>();

    public List<Alert> getAlerts() {
        return alerts;
    }

    private List<Daily> daily = new ArrayList<>();

    public List<Daily> getDaily() {
        return daily;
    }

    public void fetchDemo() {
        // デモ用の固定JSON
        String demoJson = """
        {"lat":35.6895,"lon":139.692,"timezone":"Asia/Tokyo","timezone_offset":32400,"current":{"dt":1775312005,"sunrise":1775247850,"sunset":1775293453,"temp":288.95,"feels_like":289.04,"pressure":1007,"humidity":94,"dew_point":287.99,"uvi":0,"clouds":75,"visibility":4000,"wind_speed":7.72,"wind_deg":170,"weather":[{"id":521,"main":"Rain","description":"にわか雨","icon":"09n"},{"id":701,"main":"Mist","description":"霧","icon":"50n"}],"rain":{"1h":1.94}},"alerts":[{"sender_name":"JMA","event":"強風注意報","start":1775304001,"end":1775336400,"description":"","tags":["Wind"]}]}
        """;

        parse(demoJson);
        System.out.println("取得完了 demo");
    }

    public CompletableFuture<Void> fetch() {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://api.openweathermap.org/data/3.0/onecall?lat=35.921474&lon=140.029464&lang=ja&exclude=minutely,hourly&appid=" + core.configure.getApiKey();

        LocalDateTime now = LocalDateTime.now();
        System.out.println("fetchしています ("+ now.format(DateTimeFormatter.ofPattern("dd HH:mm:ss")) +")");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync(response -> {
                parse(response.body());
                return null;
            });
    }

    private void parse(String data) {
        JsonObject json = JsonParser.parseString(data).getAsJsonObject();

        // --- main
        JsonObject main = json.has("current") ? json.getAsJsonObject("current") : null;
        if (main != null) {
            temp = main.has("temp") ? main.get("temp").getAsDouble() : 0;
            feelingTemp = main.has("feels_like") ? main.get("feels_like").getAsDouble() : 0;
            pressure = main.has("pressure") ? main.get("pressure").getAsInt() : 0;
            humidity = main.has("humidity") ? main.get("humidity").getAsDouble() : 0;

            // --- 日の出,日の入
            sunrise = main.has("sunrise") ? main.get("sunrise").getAsLong() : 0;
            sunset = main.has("sunset") ? main.get("sunset").getAsLong() : 0;

            // --- visibility
            visibility = main.has("visibility") ? main.get("visibility").getAsInt() : 0;

            // --- wind
            windSpeed = main.has("wind_speed") ? main.get("wind_speed").getAsDouble() : 0;
            windDeg = main.has("wind_deg") ? main.get("wind_deg").getAsInt() : 0;

            // --- clouds
            clouds = main.has("clouds") ? main.get("clouds").getAsInt() : 0;

            // --- uvi
            uvi = main.has("uvi") ? main.get("uvi").getAsDouble() : 0;

            // --- 取得時刻
            fetchTime = main.has("dt") ? main.get("dt").getAsLong() : 0;

            // --- 天気情報（配列）
            JsonArray weatherArr = main.has("weather") ? main.getAsJsonArray("weather") : null;
            if (weatherArr != null && weatherArr.size() > 0) {
                JsonObject weather = weatherArr.get(0).getAsJsonObject();
                weatherID = weather.has("id") ? weather.get("id").getAsInt() : 0;
                weatherString = weather.has("main") ? weather.get("main").getAsString() : "";
                weatherDetail = weather.has("description") ? weather.get("description").getAsString() : "";
                weatherIcon = weather.has("icon") ? weather.get("icon").getAsString() : "";
            }
        }

        daily.clear();
        if (json.has("daily")) {
            JsonArray dailyArr = json.getAsJsonArray("daily");
            for (JsonElement elem : dailyArr) {
                JsonObject obj = elem.getAsJsonObject();
                Daily d = new Daily();

                d.moon_phase = obj.has("moon_phase") ? obj.get("moon_phase").getAsDouble() : 0;

                JsonObject temp = obj.has("temp") ? obj.getAsJsonObject("temp") : null;
                if (temp != null) {
                    d.temp_day = temp.has("day") ? temp.get("day").getAsDouble() : 0;
                    d.temp_min = temp.has("min") ? temp.get("min").getAsDouble() : 0;
                    d.temp_max = temp.has("max") ? temp.get("max").getAsDouble() : 0;
                }

                JsonArray weatherArr = obj.has("weather") ? obj.getAsJsonArray("weather") : null;
                if (weatherArr != null && weatherArr.size() > 0) {
                    JsonObject weather = weatherArr.get(0).getAsJsonObject();
                    d.weatherID = weather.has("id") ? weather.get("id").getAsInt() : 0;
                    d.weatherString = weather.has("main") ? weather.get("main").getAsString() : "";
                    d.weatherDetail = weather.has("description") ? weather.get("description").getAsString() : "";
                    d.weatherIcon = weather.has("icon") ? weather.get("icon").getAsString() : "";
                }

                daily.add(d);
            }
        }

        // --- 注意報・警報
        alerts.clear();
        if (json.has("alerts")) {
            JsonArray alertArr = json.getAsJsonArray("alerts");
            for (JsonElement elem : alertArr) {
                JsonObject obj = elem.getAsJsonObject();
                Alert a = new Alert();
                a.sender = obj.has("sender_name") ? obj.get("sender_name").getAsString() : "";
                a.event = obj.has("event") ? obj.get("event").getAsString() : "";
                a.start = obj.has("start") ? obj.get("start").getAsLong() : 0;
                a.end = obj.has("end") ? obj.get("end").getAsLong() : 0;
                a.description = obj.has("description") ? obj.get("description").getAsString() : "";
                if (obj.has("tags")) {
                    JsonArray tagsArr = obj.getAsJsonArray("tags");
                    for (JsonElement tag : tagsArr) {
                        a.tags.add(tag.getAsString());
                    }
                }
                alerts.add(a);
            }
        }

        // --- その他
        area = json.has("timezone") ? json.get("timezone").getAsString() : "";
        timezone = json.has("timezone_offset") ? json.get("timezone_offset").getAsInt() : 0;
    }
}