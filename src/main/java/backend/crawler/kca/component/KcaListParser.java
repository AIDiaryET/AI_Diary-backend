package backend.crawler.kca.component;

import backend.crawler.kca.util.CrawlUtil;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class KcaListParser {

    public record Row(
            String idx,        // sourceId
            String detailUrl,  // 절대경로
            String name,
            String genderKo,   // "남성"/"여성"
            String region,     // "서울" 등
            String specialty,  // "개인상담,심리검사,..." (… 포함 가능)
            String acquiredYmd,// "2024-01-01" (참고용)
            String comment     // 비고
    ){}

    public static List<Row> parse(Document doc, String baseUrl) {
        List<Row> out = new ArrayList<>();
        Element tbody = doc.selectFirst("table.counselors_list > tbody");
        if (tbody == null) return out;

        for (Element tr : tbody.select("tr")) {
            Elements tds = tr.select("td");
            if (tds.size() < 8) continue;

            String region   = text(tds.get(0));
            String genderKo = text(tds.get(1)); // 남성/여성
            String name     = text(tds.get(2));
            String spec     = text(tds.get(3)).replace("...", "…");
            String comment  = text(tds.get(4));
            String acquired = text(tds.get(6));

            Element a = tds.get(7).selectFirst("a[href]");
            String href = a != null ? a.attr("href") : "";
            String abs  = absolutize(baseUrl, href);
            String idx  = extractIdx(href);

            out.add(new Row(idx, abs,
                    CrawlUtil.normText(name),
                    CrawlUtil.normText(genderKo),
                    CrawlUtil.normText(region),
                    CrawlUtil.normText(spec),
                    CrawlUtil.normText(acquired),
                    CrawlUtil.normText(comment)));
        }
        return out;
    }

    private static String text(Element td){ return td==null? "": td.text().trim(); }

    private static String extractIdx(String href){
        int i = href.indexOf("idx=");
        if (i<0) return null;
        return href.substring(i+4).replaceAll("[^0-9]","");
    }

    private static String absolutize(String base, String href){
        try { return URI.create(base).resolve(href).toString(); }
        catch (Exception e) { return href; }
    }
}
