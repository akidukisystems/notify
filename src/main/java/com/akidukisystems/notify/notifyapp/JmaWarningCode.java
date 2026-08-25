package com.akidukisystems.notify.notifyapp;

import java.util.Map;

public class JmaWarningCode {

    private static final Map<String, String> WARNING_NAMES = Map.ofEntries(
        Map.entry("02", "暴風雪警報"),
        Map.entry("03", "大雨警報"),
        Map.entry("04", "洪水警報"),
        Map.entry("05", "暴風警報"),
        Map.entry("06", "大雪警報"),
        Map.entry("07", "波浪警報"),
        Map.entry("08", "高潮警報"),
        Map.entry("09", "土砂災害警報"),
        Map.entry("10", "大雨注意報"),
        Map.entry("12", "大雪注意報"),
        Map.entry("13", "風雪注意報"),
        Map.entry("14", "雷注意報"),
        Map.entry("15", "強風注意報"),
        Map.entry("16", "波浪注意報"),
        Map.entry("17", "融雪注意報"),
        Map.entry("18", "洪水注意報"),
        Map.entry("19", "高潮注意報"),
        Map.entry("20", "濃霧注意報"),
        Map.entry("21", "乾燥注意報"),
        Map.entry("22", "なだれ注意報"),
        Map.entry("23", "低温注意報"),
        Map.entry("24", "霜注意報"),
        Map.entry("25", "着氷注意報"),
        Map.entry("26", "着雪注意報"),
        Map.entry("27", "その他の注意報"),
        Map.entry("29", "土砂災害注意報"),

        Map.entry("32", "暴風雪特別警報"),
        Map.entry("33", "大雨特別警報"),
        Map.entry("35", "暴風特別警報"),
        Map.entry("36", "大雪特別警報"),
        Map.entry("37", "波浪特別警報"),
        Map.entry("38", "高潮特別警報"),
        Map.entry("39", "土砂災害特別警報"),

        Map.entry("43", "大雨危険警報"),
        Map.entry("48", "高潮危険警報"),
        Map.entry("49", "土砂災害危険警報")
    );

    public static String getName(String code) {
        return WARNING_NAMES.getOrDefault(code, "不明な警報・注意報");
    }
}