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

/**
 * OpenWeather One Call APIから現在の天気・予報・警報情報を取得し、パースして保持するクラス。
 * 気象庁の警報・注意報({@link JmaWarning})も合わせて取得する。
 */
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

    private WeatherConverter wc = new WeatherConverter();
    JmaWarning jma;

    /** 気象庁の警報・注意報取得器を返す。 */
    public JmaWarning getJma() {
        return jma;
    }

    private Core core;

    /** 親となる{@link Core}を設定し、設定済みの地域コードで{@link JmaWarning}を生成する。 */
    public void setCore(Core core) {
        this.core = core;
        this.jma = new JmaWarning(core.configure.getJmaAreaCode());
    }

    /** 降雨量(mm/h)を返す。 */
    public double getRain() {
        return rain;
    }

    /** 降雪量(mm/h)を返す。 */
    public double getSnow() {
        return snow;
    }

    /** 紫外線指数を返す。 */
    public double getUvi() {
        return uvi;
    }

    /** 天気ID(OpenWeather独自のコード)を返す。 */
    public int getWeatherID() {
        return weatherID;
    }

    /** 天候の簡潔な表現(例: "Rain")を返す。 */
    public String getWeatherString() {
        return weatherString;
    }

    /** {@link WeatherConverter}で変換済みの天候詳細文を返す。 */
    public String getWeatherDetail() {
        return weatherDetail;
    }

    /** 天気アイコンのコードを返す。 */
    public String getWeatherIcon() {
        return weatherIcon;
    }

    /** 気温(K)を返す。 */
    public double getTemp() {
        return temp;
    }

    /** 体感気温(K)を返す。 */
    public double getFeelingTemp() {
        return feelingTemp;
    }

    /** 大気圧(hPa)を返す。 */
    public int getPressure() {
        return pressure;
    }

    /** 湿度(%)を返す。 */
    public double getHumidity() {
        return humidity;
    }

    /** 視界(m)を返す。 */
    public int getVisibility() {
        return visibility;
    }

    /** 風速(m/s)を返す。 */
    public double getWindSpeed() {
        return windSpeed;
    }

    /** 風向き(度)を返す。 */
    public int getWindDeg() {
        return windDeg;
    }

    /** 雲量(%)を返す。 */
    public int getClouds() {
        return clouds;
    }

    /** データ取得時刻(UNIX time)を返す。 */
    public long getFetchTime() {
        return fetchTime;
    }

    /** 日の出時刻(UNIX time)を返す。 */
    public long getSunrise() {
        return sunrise;
    }

    /** 日の入時刻(UNIX time)を返す。 */
    public long getSunset() {
        return sunset;
    }

    /** 地域名(タイムゾーン文字列)を返す。 */
    public String getArea() {
        return area;
    }

    /** タイムゾーン(UTCからのオフセット秒)を返す。 */
    public int getTimezone() {
        return timezone;
    }

    /** OpenWeatherが返す気象アラート1件分。 */
    public static class Alert {
        public String sender;
        public String event;
        public long start;
        public long end;
        public String description;
        public List<String> tags = new ArrayList<>();
    }

    /** 1日分の予報データ。 */
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

    /** OpenWeather側のアラート一覧を返す。 */
    public List<Alert> getAlerts() {
        return alerts;
    }

    private List<Daily> daily = new ArrayList<>();

    /** 日別予報一覧を返す(先頭が本日分)。 */
    public List<Daily> getDaily() {
        return daily;
    }

    private List<JmaWarning.Warning> warnings = new ArrayList<>();

    /** 気象庁の警報・注意報一覧を返す。 */
    public List<JmaWarning.Warning> getWarnings() {
        return warnings;
    }

    /** ネットワーク接続無しで動作確認できるよう、固定JSONを使ってパース処理を実行する(デモ用)。 */
    public void fetchDemo() {
        // デモ用の固定JSON
        String demoJson = """
        {"lat":35.6895,"lon":139.692,"timezone":"Asia/Tokyo","timezone_offset":32400,"current":{"dt":1775312005,"sunrise":1775247850,"sunset":1775293453,"temp":288.95,"feels_like":289.04,"pressure":1007,"humidity":94,"dew_point":287.99,"uvi":0,"clouds":75,"visibility":4000,"wind_speed":7.72,"wind_deg":170,"weather":[{"id":521,"main":"Rain","description":"にわか雨","icon":"09n"},{"id":701,"main":"Mist","description":"霧","icon":"50n"}],"rain":{"1h":1.94}},"alerts":[{"sender_name":"JMA","event":"強風注意報","start":1775304001,"end":1775336400,"description":"","tags":["Wind"]}]}
        """;

        parse(demoJson);
        System.out.println("取得完了 demo");
    }

    /**
     * OpenWeather One Call APIから最新の天気情報を非同期取得し、{@link #parse}でこのインスタンスに反映する。
     * あわせて気象庁の警報・注意報({@link JmaWarning#fetch()})も同期的に取得する。
     *
     * @return パース完了時に完了するFuture
     */
    public CompletableFuture<Void> fetch() {
        HttpClient client = HttpClient.newHttpClient();

        double lat = core.configure.getLatitude();
        double lon = core.configure.getLongitude();

         String url = "https://api.openweathermap.org/data/3.0/onecall?lat=" + lat
                + "&lon=" + lon
                + "&lang=ja&exclude=minutely,hourly&appid=" + core.configure.getApiKey();

        LocalDateTime now = LocalDateTime.now();
        System.out.println("fetchしています ("+ now.format(DateTimeFormatter.ofPattern("dd HH:mm:ss")) +")");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            warnings = jma.fetch();

            for (JmaWarning.Warning warning : warnings) {
                System.out.println(warning.getName());
                System.out.println("Code: " + warning.getCode());
                System.out.println("Status: " + warning.getStatus());
                System.out.println("危険度: " + warning.getSignificancyName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync(response -> {
                parse(response.body());
                return null;
            });
    }

    /** OpenWeather One Call APIのレスポンスJSONをパースし、現在の天気・日別予報・アラートをフィールドへ反映する。 */
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
                weatherDetail = weather.has("description") ? wc.formatted(weather.get("description").getAsString()) : "";
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
                    d.weatherDetail = weather.has("description") ? wc.formatted(weather.get("description").getAsString()) : "";
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