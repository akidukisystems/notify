package com.akidukisystems.notify.notifyapp;

public class WeatherConverter {
    
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

    public String formatted(String str) {
        String out = matching(str);
        System.out.println(str +" -> "+ out);
        return out;
    }
}
