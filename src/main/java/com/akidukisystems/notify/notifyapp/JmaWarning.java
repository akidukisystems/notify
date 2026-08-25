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

    public JmaWarning(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getMaxSignificancyName() {
        return maxSignificancyName;
    }

    public String getMaxSignificancyCode() {
        return maxSignificancyCode;
    }

    public static class Warning {

        private String name;
        private String code;
        private String status;

        private OffsetDateTime reportDateTime;

        private String propertyType;
        private String significancyName;
        private String significancyCode;

        private final List<String> notes = new ArrayList<>();

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getStatus() {
            return status;
        }

        public OffsetDateTime getReportDateTime() {
            return reportDateTime;
        }

        public String getPropertyType() {
            return propertyType;
        }

        public String getSignificancyName() {
            return significancyName;
        }

        public String getSignificancyCode() {
            return significancyCode;
        }

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