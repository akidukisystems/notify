package com.akidukisystems.notify.notifyapp;

/**
 * OpenWeatherの天気説明文(日本語)を、UI表示用の簡潔な表現に変換するクラス。
 */
public class WeatherConverter {

    /**
     * 天気説明文に含まれるキーワードから、UI表示用の簡潔な表現に変換する。
     * 該当するキーワードが無ければ元の文字列をそのまま返す。
     */
    private String matching(String str) {
        if(str.contains("雷雨"))
            return "雷雨";
        if(str.contains("雷"))
            return "雷";

        if(str.contains("小雨"))
            return "曇り 時に 雨";
        if(str.contains("雨"))
            return "雨";

        if(str.contains("曇りがち"))
            return "晴れ 時に 曇";
        if(str.contains("雲"))
            return "曇";

        if(str.contains("晴"))
            return "晴れ";
        
        return str;
    }

    /**
     * 天気説明文を{@link #matching}で変換し、変換前後をログ出力した上で返す。
     */
    public String formatted(String str) {
        String out = matching(str);
        System.out.println(str +" -> "+ out);
        return out;
    }
}
