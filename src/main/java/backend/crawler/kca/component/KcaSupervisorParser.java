package backend.crawler.kca.component;

import backend.crawler.kca.dto.CounselorItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class KcaSupervisorParser {

    private final CrawlerKcaProps props;
    private final JsoupFetcher fetcher;

    /** 테스트/리스트용: 페이지에서 row/card를 파싱해 대략적 정보 + detailUrl + idx 반환 */
    public List<CounselorItem> fetchPage(int page) throws IOException {
        String url = String.format("%s%s?page=%d", props.getBaseUrl(), props.getListPath(), page);
        Document doc = fetcher.get(url);

        Elements rows = doc.select("table tbody tr");
        if (!rows.isEmpty()) {
            log.debug("[KCA] Page {}: table layout detected, rows={}", page, rows.size());
            return parseTable(doc, rows);
        }

        Elements cards = doc.select(".result_list li, .card, .listWrap .item");
        if (!cards.isEmpty()) {
            log.debug("[KCA] Page {}: card layout detected, items={}", page, cards.size());
            return parseCards(cards);
        }

        log.info("[KCA] Page {}: no results detected.", page);
        return List.of();
    }

    /** 상세 URL로 직접 호출: 문서 fetch + 상세 파싱 + source/idx/url 세팅 */
    public CounselorItem fetchDetail(String detailUrl) throws IOException {
        Document doc = fetcher.get(detailUrl);
        CounselorItem item = parseDetail(doc);
        item.setSource("KCA");
        item.setSourceId(extractIdx(detailUrl));
        item.setDetailUrl(detailUrl);
        return item;
    }

    /** 상세 페이지 전용 파싱(라벨 기반, 구조 안정) */
    public CounselorItem parseDetail(Document doc) {
        String name   = textOf(doc, "table.counselor_profile th:matchesOwn(^이름$) + td");
        String gender = textOf(doc, "table.counselor_profile th:matchesOwn(^성별$) + td");

        Element licTd = doc.selectFirst("table.counselor_profile th:matchesOwn(^자격증$) + td");
        String licenseType = licTd == null ? "" :
                licTd.html()
                        .replaceAll("(?i)<br[^>]*>", "/")
                        .replaceAll("\\s+", " ")
                        .trim();

        String email     = textOf(doc, "table.counselor_profile th:matchesOwn(^이메일$) + td");
        String targets   = textOf(doc, "table.counselor_info th:matchesOwn(^상담대상$) + td");
        String specialty = textOf(doc, "table.counselor_info th:matchesOwn(^전문분야$) + td");

        Element regionTd = doc.selectFirst("table.counselor_info th:matchesOwn(^상담가능장소$) + td");
        String regions   = regionTd == null ? "" : regionTd.text().replaceAll("\\s+", " ").trim();

        String fee       = textOf(doc, "table.counselor_info th:matchesOwn(^상담비용$) + td");

        name = normalizeName(name);

        return CounselorItem.builder()
                .name(name)
                .gender(gender)
                .licenseType(licenseType)
                .email(email)
                .targets(targets)
                .specialty(specialty)
                .regions(regions)
                .fee(fee)
                .build(); // source/sourceId/detailUrl은 fetchDetail()에서 세팅
    }

    /* ===================== 리스트/카드 파싱 ===================== */

    private List<CounselorItem> parseTable(Document doc, Elements rows) {
        Map<String,Integer> colIdx = mapHeaderIndexes(doc.select("table thead th"));

        List<CounselorItem> out = new ArrayList<>();
        for (Element tr : rows) {
            Elements tds = tr.select("td");
            if (tds.isEmpty()) continue;

            Function<String,String> val = key -> {
                Integer i = colIdx.get(key);
                return (i != null && i < tds.size()) ? tds.get(i).text().trim() : "";
            };
            String detailUrl = extractDetailUrl(tr);
            String idx = extractIdx(detailUrl);

            String name = normalizeName(val.apply("이름"));
            if (name.isBlank()) continue;

            out.add(CounselorItem.builder()
                    .source("KCA")
                    .sourceId(idx)
                    .detailUrl(detailUrl)
                    .name(name)
                    .gender(val.apply("성별"))
                    .specialty(val.apply("전문분야"))
                    .regions(val.apply("상담가능지역"))
                    .licenseNo(val.apply("자격증 번호"))
                    .licenseType(val.apply("자격증 종류"))
                    .build());
        }
        return out;
    }

    private Map<String,Integer> mapHeaderIndexes(Elements ths) {
        Map<String,Integer> map = new HashMap<>();
        for (int i = 0; i < ths.size(); i++) {
            String h = ths.get(i).text().replaceAll("\\s+","").trim();
            if (h.contains("이름") || h.contains("성명")) map.put("이름", i);
            if (h.contains("성별")) map.put("성별", i);
            if (h.contains("전문") || h.contains("분야")) map.put("전문분야", i);
            if (h.contains("지역")) map.put("상담가능지역", i);
            if (h.contains("자격") && h.contains("번호")) map.put("자격증 번호", i);
            if (h.contains("자격") && (h.contains("종류") || h.contains("등급"))) map.put("자격증 종류", i);
            if (h.contains("상세") || h.contains("보기")) map.put("상세", i);
        }
        return map;
    }

    private List<CounselorItem> parseCards(Elements cards) {
        List<CounselorItem> out = new ArrayList<>();
        for (Element c : cards) {
            String name = textOrBlank(c.selectFirst(".name, h3.name, h3.title, .card-header .name"));
            if (name.isBlank()) name = pickNear(c, "이름", "성명");
            name = cleanNameLoose(name);
            if (name.isBlank()) continue;

            String gender = textOrBlank(c.selectFirst(".gender, .info .gender"));
            if (gender.isBlank()) gender = pickNear(c, "성별");

            String specialty = textOrBlank(c.selectFirst(".specialty, .tags.specialty"));
            if (specialty.isBlank()) specialty = pickNear(c, "전문", "전문분야");

            String regions = textOrBlank(c.selectFirst(".regions, .region, .area"));
            if (regions.isBlank()) regions = pickNear(c, "지역", "상담가능지역");

            String licenseNo = textOrBlank(c.selectFirst(".license-no, .lic-no"));
            if (licenseNo.isBlank()) licenseNo = pickNear(c, "자격", "번호");

            String licenseType = textOrBlank(c.selectFirst(".license-type, .lic-type"));
            if (licenseType.isBlank()) licenseType = pickNear(c, "자격", "종류", "등급");

            String detailUrl = extractDetailUrl(c);
            String idx = extractIdx(detailUrl);

            out.add(CounselorItem.builder()
                    .source("KCA")
                    .sourceId(idx)
                    .detailUrl(detailUrl)
                    .name(name)
                    .gender(gender)
                    .specialty(specialty)
                    .regions(regions)
                    .licenseNo(licenseNo)
                    .licenseType(licenseType)
                    .build());
        }
        return out;
    }

    /* ===================== 헬퍼 ===================== */

    private String textOf(Document doc, String css) {
        Element e = doc.selectFirst(css);
        return e == null ? "" : e.text().trim();
    }

    private String textOrBlank(Element e) {
        return e == null ? "" : e.text().trim();
    }

    // 상세 링크 추출 (상대경로 → 절대경로)
    private String extractDetailUrl(Element scope) {
        Element a = scope.selectFirst("a[href*='detail'], a[href*='view'], a.btn, button[data-url]");
        if (a == null) return "";
        if (a.hasAttr("href")) return a.attr("abs:href");
        if (a.hasAttr("data-url")) return a.attr("abs:data-url");
        return "";
    }

    // 상세 URL에서 idx= 추출
    private String extractIdx(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            URI uri = new URI(url);
            String q = uri.getQuery(); // e.g. "idx=1104"
            if (q == null) return "";
            for (String kv : q.split("&")) {
                String[] p = kv.split("=", 2);
                if (p.length == 2 && "idx".equals(p[0])) return p[1];
            }
        } catch (Exception ignored) {}
        return "";
    }

    // 라벨 근처 값 추출 (dt/dd, 라벨:값 등)
    private String pickNear(Element root, String... labels) {
        Elements els = root.select("*");
        for (Element e : els) {
            String t = e.text();
            boolean ok = true;
            for (String lb : labels) {
                if (!t.contains(lb)) { ok = false; break; }
            }
            if (!ok) continue;

            Element next = e.nextElementSibling();
            if (next != null && !next.text().isBlank()) return next.text().trim();

            if ("dt".equalsIgnoreCase(e.tagName())) {
                Element dd = e.parent().selectFirst("dd");
                if (dd != null) return dd.text().trim();
            }
            String[] parts = t.split("[:：]\\s*");
            if (parts.length == 2) return parts[1].trim();
        }
        return "";
    }

    /* ===== 이름 정규화/필터 ===== */

    private static final Set<String> NAME_BLACKLIST =
            Set.of("검색","닫기","상담가능지역","이메일","상담대상","상담비용","수퍼비전","수퍼바이지","내담자","찾아가는 상담","전지역");
    private static final Pattern MANY_REGIONS =
            Pattern.compile("서울|부산|대구|인천|광주|대전|울산|경기|강원|충북|충남|전북|전남|경북|경남|제주|세종");
    private static final Pattern KOR_NAME_2_4 = Pattern.compile("^[가-힣]{2,4}$");

    /** 상세/정확 데이터용: 한글 2~4자만 허용 */
    private String normalizeName(String s) {
        if (s == null) return "";
        String v = s.replaceAll("\\s+","");
        if (KOR_NAME_2_4.matcher(v).matches()) return v;
        v = v.replaceAll("[^가-힣]","");
        return KOR_NAME_2_4.matcher(v).matches() ? v : "";
    }

    /** 리스트/카드용(느슨): 블랙리스트/지역패턴 제거 후 길이 컷 */
    private String cleanNameLoose(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.length() > 80) s = s.substring(0, 80);
        for (String bad : NAME_BLACKLIST) if (s.contains(bad)) return "";
        if (MANY_REGIONS.matcher(s).find()) return "";
        // 가능하면 정규화 이름으로 축소
        String normalized = normalizeName(s);
        return normalized.isBlank() ? s : normalized;
    }
}
