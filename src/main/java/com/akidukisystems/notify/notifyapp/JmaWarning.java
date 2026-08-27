package com.akidukisystems.notify.notifyapp;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * 気象庁の防災情報XMLフィード(regular.xml → VPWS50 個別データ)から、
 * 指定した市区町村の警報・注意報一覧を取得・パースするクラス。
 */
public class JmaWarning {

    private static final String FEED_URL =
            "https://www.data.jma.go.jp/developer/xml/feed/regular.xml";

    private static final String WARNING_TYPE =
            "気象警報・注意報（市町村等）";

    private static final String VPWS50_SUFFIX =
            "VPWS50_010000.xml";

    private final String areaCode;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    private String maxSignificancyName;
    private String maxSignificancyCode;

    /**
     * @param areaCode 対象とする市区町村コード(気象庁XML中の{@code Area/Code}と一致するもの)
     */
    public JmaWarning(String areaCode) {
        this.areaCode = areaCode;
    }

    /** 直近の{@link #fetch()}で見つかった最大警戒レベルの警報・注意報名を返す(無ければ{@code null})。 */
    public String getMaxSignificancyName() {
        return maxSignificancyName;
    }

    /** 直近の{@link #fetch()}で見つかった最大警戒レベルの重大度コードを返す(無ければ{@code null})。 */
    public String getMaxSignificancyCode() {
        return maxSignificancyCode;
    }

    /** 1件の警報・注意報を表すデータクラス。 */
    public static class Warning {

        private String name;
        private String code;
        private String status;

        private OffsetDateTime reportDateTime;

        private String propertyType;
        private String significancyName;
        private String significancyCode;

        private final List<String> notes = new ArrayList<>();

        /** 警報・注意報名(例: "大雨警報")を返す。 */
        public String getName() {
            return name;
        }

        /** 警報・注意報の種別コードを返す。 */
        public String getCode() {
            return code;
        }

        /** 発表状況(例: "発表"/"継続"/"解除")を返す。 */
        public String getStatus() {
            return status;
        }

        /** 発表時刻を返す。 */
        public OffsetDateTime getReportDateTime() {
            return reportDateTime;
        }

        /** プロパティ種別を返す。 */
        public String getPropertyType() {
            return propertyType;
        }

        /** 重大度(危険度)の名称を返す。 */
        public String getSignificancyName() {
            return significancyName;
        }

        /** 重大度(危険度)のコードを返す。 */
        public String getSignificancyCode() {
            return significancyCode;
        }

        /** 付随する特記事項(Note)一覧を返す。 */
        public List<String> getNotes() {
            return notes;
        }

        @Override
        public String toString() {
            return name
                    + " [code=" + code
                    + ", status=" + status
                    + ", risk=" + significancyName
                    + "]";
        }
    }

    /**
     * regular.xmlフィードから最新のVPWS50(警報・注意報)データを見つけて取得し、
     * {@link #areaCode}に該当する警報・注意報一覧を返す。あわせて{@link #maxSignificancyName}/
     * {@link #maxSignificancyCode}を最大警戒レベルで更新する。
     */
    public List<Warning> fetch() throws Exception {

        maxSignificancyName = null;
        maxSignificancyCode = null;

        // 1. regular.xml を取得
        String feedXml = fetchText(FEED_URL);

        // 2. 最新の VPWS50_010000.xml を取得
        String latestUrl = findLatestVpws50(feedXml);

        if (latestUrl == null) {
            throw new IllegalStateException(
                    "VPWS50_010000.xml が見つかりません");
        }

        System.out.println("JMA XML: " + latestUrl);

        // 3. VPWS50 本体を取得
        String warningXml = fetchText(latestUrl);

        // 4. 対象市町村の警報・注意報を取得
        return parseWarnings(warningXml);
    }

    /** 指定URLへHTTP GETし、レスポンスボディを文字列で返す。2xx以外は例外を投げる。 */
    private String fetchText(String url) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/xml, text/xml")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "JMA XML取得失敗 HTTP "
                            + response.statusCode()
                            + ": " + url);
        }

        return response.body();
    }

    /** regular.xmlフィードのエントリから、更新日時が最も新しいVPWS50データのURLを探す。 */
    private String findLatestVpws50(String xml) throws Exception {

        Document doc = parseXml(xml);

        NodeList entries = doc.getElementsByTagName("entry");

        String latestUrl = null;
        OffsetDateTime latestTime = null;

        for (int i = 0; i < entries.getLength(); i++) {

            Element entry = (Element) entries.item(i);

            String updatedText = getDirectChildText(
                    entry,
                    "updated");

            if (updatedText == null || updatedText.isBlank()) {
                continue;
            }

            OffsetDateTime updated;

            try {
                updated = OffsetDateTime.parse(updatedText);
            } catch (Exception e) {
                continue;
            }

            NodeList links = entry.getElementsByTagName("link");

            for (int j = 0; j < links.getLength(); j++) {

                Element link = (Element) links.item(j);

                String href = link.getAttribute("href");

                if (href == null || href.isBlank()) {
                    continue;
                }

                if (!href.endsWith(VPWS50_SUFFIX)) {
                    continue;
                }

                if (latestTime == null
                        || updated.isAfter(latestTime)) {

                    latestTime = updated;
                    latestUrl = href;
                }
            }
        }

        return latestUrl;
    }

    /**
     * VPWS50 XML本体から、{@link #areaCode}に一致する市区町村の警報・注意報を抽出する。
     * 対象市区町村のItemが1件見つかった時点でそこまでの結果を返す(以降は探索しない)。
     */
    private List<Warning> parseWarnings(String xml) throws Exception {

        List<Warning> result = new ArrayList<>();

        Document doc = parseXml(xml);

        NodeList warningNodes =
                doc.getElementsByTagName("Warning");

        for (int i = 0; i < warningNodes.getLength(); i++) {

            Element warning = (Element) warningNodes.item(i);

            if (!WARNING_TYPE.equals(
                    warning.getAttribute("type"))) {
                continue;
            }

            // Warning直下のItemのみを見る
            NodeList children = warning.getChildNodes();

            for (int j = 0; j < children.getLength(); j++) {

                Node node = children.item(j);

                if (!(node instanceof Element item)) {
                    continue;
                }

                if (!"Item".equals(item.getTagName())) {
                    continue;
                }

                // 対象市町村か確認
                if (!isTargetArea(item)) {
                    continue;
                }

                // Item直下のKindを取得
                NodeList itemChildren =
                        item.getChildNodes();

                int maxLevel = -1;
                Warning maxWarning = null;

                for (int k = 0;
                    k < itemChildren.getLength();
                    k++) {

                    Node node2 = itemChildren.item(k);

                    if (!(node2 instanceof Element kind)) {
                        continue;
                    }

                    if (!"Kind".equals(kind.getTagName())) {
                        continue;
                    }

                    Warning parsed = parseKind(kind);

                    if (parsed != null) {
                        result.add(parsed);

                        String significancyCode =
                                parsed.getSignificancyCode();

                        if (significancyCode != null
                                && significancyCode.matches("[1-5]1")) {

                            int level =
                                    Integer.parseInt(
                                            significancyCode.substring(0, 1));

                            if (level > maxLevel) {
                                maxLevel = level;
                                maxWarning = parsed;
                            }
                        }
                    }
                }

                if (maxWarning != null) {
                    maxSignificancyName =
                            maxWarning.getSignificancyName();

                    maxSignificancyCode =
                            maxWarning.getSignificancyCode();
                }

                // 見たい市町村のItemが見つかったので終了
                return result;
            }
        }

        return result;
    }

    /** {@code Item}要素の中に{@link #areaCode}と一致する{@code Area/Code}があるかを判定する。 */
    private boolean isTargetArea(Element item) {
        NodeList children = item.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (!(node instanceof Element area)) {
                continue;
            }

            if (!"Area".equals(area.getTagName())) {
                continue;
            }

            String code = getDirectChildText(area, "Code");

            if (areaCode.equals(code)) {
                return true;
            }
        }

        return false;
    }

    /** {@code Kind}要素1件を{@link Warning}にパースする。{@code Name}が無ければ{@code null}を返す。 */
    private Warning parseKind(Element kind) {

        String name = getDirectChildText(
                kind,
                "Name");

        if (name == null || name.isBlank()) {
            return null;
        }

        Warning warning = new Warning();

        warning.name = name;

        warning.code = getDirectChildText(
                kind,
                "Code");

        warning.status = getDirectChildText(
                kind,
                "Status");

        String dateTimeText = null;

        NodeList children = kind.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (!(node instanceof Element element)) {
                continue;
            }

            if ("DateTime".equals(element.getTagName())
                    && "発表時刻".equals(
                    element.getAttribute("type"))) {

                dateTimeText =
                        element.getTextContent().trim();

                break;
            }
        }

        if (dateTimeText != null
                && !dateTimeText.isBlank()) {

            try {
                warning.reportDateTime =
                        OffsetDateTime.parse(dateTimeText);
            } catch (Exception ignored) {
            }
        }

        parseProperty(kind, warning);
        parseAddition(kind, warning);

        return warning;
    }

    /** {@code Kind}内の{@code Property/Significancy}から重大度の名称・コードを読み取り、{@code warning}に設定する。 */
    private void parseProperty(
            Element kind,
            Warning warning) {

        NodeList properties =
                kind.getElementsByTagName("Property");

        if (properties.getLength() == 0) {
            return;
        }

        Element property =
                (Element) properties.item(0);

        warning.propertyType =
                getDirectChildText(
                        property,
                        "Type");

        NodeList significancies =
                property.getElementsByTagName(
                        "Significancy");

        if (significancies.getLength() == 0) {
            return;
        }

        Element significancy =
                (Element) significancies.item(0);

        warning.significancyName =
                getDirectChildText(
                        significancy,
                        "Name");

        warning.significancyCode =
                getDirectChildText(
                        significancy,
                        "Code");
    }

    /** {@code Kind}内の{@code Addition/Note}を読み取り、特記事項として{@code warning}に追加する。 */
    private void parseAddition(
            Element kind,
            Warning warning) {

        NodeList additions =
                kind.getElementsByTagName("Addition");

        if (additions.getLength() == 0) {
            return;
        }

        Element addition =
                (Element) additions.item(0);

        NodeList notes =
                addition.getElementsByTagName("Note");

        for (int i = 0; i < notes.getLength(); i++) {

            String note =
                    notes.item(i)
                            .getTextContent()
                            .trim();

            if (!note.isBlank()) {
                warning.notes.add(note);
            }
        }
    }

    /** {@code parent}の直接の子要素から{@code tagName}に一致するものを探し、そのテキスト内容を返す(無ければ{@code null})。 */
    private String getDirectChildText(
            Element parent,
            String tagName) {

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (!(node instanceof Element element)) {
                continue;
            }

            if (tagName.equals(element.getTagName())) {
                return element.getTextContent().trim();
            }
        }

        return null;
    }

    /** XML外部エンティティ等を無効化した安全な設定で、XML文字列を{@link Document}にパースする。 */
    private Document parseXml(String xml) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        // XML外部エンティティ等を無効化
        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true);

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false);

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false);

        factory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);

        DocumentBuilder builder =
                factory.newDocumentBuilder();

        return builder.parse(
                new InputSource(
                        new StringReader(xml)));
    }
}