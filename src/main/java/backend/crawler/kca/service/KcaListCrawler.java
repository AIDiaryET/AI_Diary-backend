package backend.crawler.kca.service;

import backend.crawler.kca.component.KcaListParser;
import backend.crawler.kca.entity.CounselorEntity;
import backend.crawler.kca.repo.CounselorRepository;
import backend.crawler.kca.util.CrawlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcaListCrawler {

    private final CounselorRepository repo;

    private static final String SOURCE = "KCA";
    private static final String LIST_URL = "https://www.counselors.or.kr/KOR/user/find_counselors.php";
    private static final int TIMEOUT_MS = 15000;

    @Transactional
    public int crawlAllPages() throws Exception {
        int savedOrUpdated = 0;

        Document first = fetch(1);
        int last = findLastPage(first);
        savedOrUpdated += upsert(first);

        for (int p=2; p<=last; p++){
            Thread.sleep(400);
            Document doc = fetch(p);
            savedOrUpdated += upsert(doc);
        }
        log.info("[KCA][list] upsert={}", savedOrUpdated);
        return savedOrUpdated;
    }

    private Document fetch(int page) throws Exception {
        return Jsoup.connect(LIST_URL)
                .timeout(TIMEOUT_MS)
                .userAgent("Mozilla/5.0 (compatible; KCA-ListCrawler/1.0)")
                .referrer("https://www.counselors.or.kr/")
                .data("page", String.valueOf(page))
                .get();
    }

    private int upsert(Document doc){
        List<KcaListParser.Row> rows = KcaListParser.parse(doc, LIST_URL);
        int cnt = 0;
        for (var r : rows){
            if (r.idx()==null) continue;
            String unique = CrawlUtil.sha256(SOURCE, r.idx());
            CounselorEntity e = repo.findBySourceAndSourceId(SOURCE, r.idx())
                    .orElseGet(CounselorEntity::new);

            boolean isNew = (e.getId() == null);

            e.setUniqueKey(unique);
            e.setSource(SOURCE);
            e.setSourceId(r.idx());
            e.setDetailUrl(r.detailUrl());

            // 목록 기반으로 채울 수 있는 값
            if (r.name()!=null)   e.setName(r.name());
            if (r.genderKo()!=null) e.setGender(r.genderKo());   // 남성/여성
            // 전문분야(콤마→슬래시 형태 혼재) → specialty
            if (r.specialty()!=null)
                e.setSpecialty(CrawlUtil.joinDistinct(e.getSpecialty(), r.specialty(), " | "));
            // 지역(단건) → regions
            if (r.region()!=null)
                e.setRegions(CrawlUtil.joinDistinct(e.getRegions(), r.region(), " | "));
            // fee/targets/email/license는 상세에서 채움

            if (isNew) repo.save(e);
            cnt++;
        }
        return cnt;
    }

    private int findLastPage(Document doc){
        int max = 1;
        for (var a : doc.select(".paging a[href*=page=]")){
            try {
                String s = a.attr("href").split("page=")[1].replaceAll("[^0-9]","");
                max = Math.max(max, Integer.parseInt(s));
            } catch (Exception ignored){}
        }
        return max;
    }
}
