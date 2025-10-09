package backend.crawler.kca.component;

import backend.crawler.kca.util.CrawlUtil;
import org.jsoup.nodes.*;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KcaDetailParser {

    public static class Detail {
        public String name;
        public String genderKo;
        public String email;
        public String licenseNo;     // 자격번호(있으면)
        public String licenseType;   // 자격증 목록(HTML → 텍스트)
        public String targets;       // 상담대상
        public String specialty;     // 전문분야
        public String regions;       // 상담가능장소(요약 텍스트)
        public String fee;           // 상담비용(원문)
        public String profileImage;  // 필요 시 별도 저장
    }

    public static Detail parse(Document doc, String baseUrl){
        Detail d = new Detail();

// 프로필 이미지
        Element img = doc.selectFirst(".counselor_profile_wrap .counselor_img img[src]");
        if (img != null) d.profileImage = absolutize(baseUrl, img.attr("src"));

// 프로필 테이블: 이름/성별/자격증/이메일
        Element prof = doc.selectFirst("table.counselor_profile");
        if (prof != null) {
            for (Element tr : prof.select("tr")) {
                Element thEl = tr.selectFirst("th");
                Element tdEl = tr.selectFirst("td");
                if (thEl == null || tdEl == null) continue;

                String thRaw = thEl.text();                 // 원문 라벨
                String key = thRaw.replaceAll("\\s+", "")   // 공백/개행 제거
                        .toLowerCase();           // 소문자 정규화

                // 기본 값: 줄바꿈을 보존해야 하는 케이스(자격증) 전에는 td.text() 사용 금지
                // -> 자격증 분기에서만 <br> -> \n 삽입 후 텍스트 추출
                switch (key) {
                    case "이름" -> {
                        String val = CrawlUtil.normText(tdEl.text());
                        if (val != null) d.name = val;
                    }
                    case "성별" -> {
                        String val = CrawlUtil.normText(tdEl.text()); // 남성/여성
                        if (val != null) d.genderKo = val;
                    }
                    case "자격증" -> {
                        // 줄바꿈 보존 후 텍스트 추출
                        tdEl.select("br").forEach(br -> br.after("\n"));
                        String val = CrawlUtil.normText(tdEl.text());
                        if (val != null) d.licenseType = val; // 다중 항목 문자열 그대로
                    }
                    // 이메일 라벨 동의어 대응: "이메일", "email", "e-mail"
                    default -> {
                        if (key.contains("이메일") || key.contains("email")) {
                            String val = CrawlUtil.normText(tdEl.text());
                            if (val != null) d.email = val;
                        }
                    }
                }
            }
        }

// 페이지 전체에서 이메일 패턴 fallback (프로필 표에 없을 경우 대비)
        if (d.email == null) {
            String whole = doc.text();
            Matcher m = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
                    .matcher(whole);
            if (m.find()) d.email = m.group();
        }


        // 상세 테이블
        Element info = doc.selectFirst("table.counselor_info");
        if (info != null) {
            for (Element tr : info.select("tr")) {
                String th = safeText(tr.selectFirst("th")).replace("\n","").replace(" ","");
                Element td = tr.selectFirst("td"); if (td==null) continue;

                if (th.contains("상담대상")) {
                    d.targets = CrawlUtil.normText(td.text());
                } else if (th.contains("전문분야")) {
                    d.specialty = CrawlUtil.normText(td.text());
                } else if (th.contains("상담가능장소")) {
                    d.regions = CrawlUtil.normText(extractPlaces(td));
                } else if (th.contains("상담비용")) {
                    d.fee = CrawlUtil.normText(td.text());
                } else if (th.contains("자격번호")) {
                    d.licenseNo = CrawlUtil.normText(td.text());
                }
            }
        }
        return d;
    }

    private static String extractPlaces(Element td){
        StringBuilder sb = new StringBuilder();
        for (Element dl : td.select("dl")) {
            String dt = safeText(dl.selectFirst("dt"));
            String dd = safeText(dl.selectFirst("dd"));
            String line = (dt.isBlank()? dd: dt+" : "+dd);
            if (!line.isBlank()) {
                if (sb.length()>0) sb.append("; ");
                sb.append(line);
            }
        }
        return (sb.length()>0)? sb.toString(): td.text();
    }

    private static String safeText(Element el){ return el==null? "": el.text().trim(); }

    private static String absolutize(String base, String href){
        try { return URI.create(base).resolve(href).toString(); }
        catch (Exception e) { return href; }
    }


}
