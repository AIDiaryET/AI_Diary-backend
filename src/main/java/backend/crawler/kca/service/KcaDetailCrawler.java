package backend.crawler.kca.service;

import backend.crawler.kca.component.FrameAwareFetcher;
import backend.crawler.kca.component.KcaDetailParser;
import backend.crawler.kca.entity.CounselorEntity;
import backend.crawler.kca.repo.CounselorRepository;
import backend.crawler.kca.util.CrawlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static backend.crawler.kca.util.ParseUtils.normalizeSpecialty;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcaDetailCrawler {

    private final CounselorRepository repo;

    private static final String DETAIL_BASE = "https://www.counselors.or.kr/KOR/user/find_counselors_detail.php";
    private static final String UA = "Mozilla/5.0 (compatible; KCA-DetailCrawler/1.0)";
    private static final String REF = "https://www.counselors.or.kr/";

    /** detailUrl/idx 있는 레코드 대상으로 상세 크롤링하여 필드 보강 */
    @Transactional
    public int crawlAndEnrichAll() {
        int updated = 0;
        int page = 0, size = 100;

        while (true){
            var pageable = PageRequest.of(page, size);
            List<CounselorEntity> batch = repo.findAll(pageable).getContent();
            if (batch.isEmpty()) break;

            for (CounselorEntity e : batch){
                if (e.getSourceId()==null || e.getSourceId().isBlank()) continue;
                try {
                    Document doc = FrameAwareFetcher.fetchFollowingFrames(
                            DETAIL_BASE + "?idx=" + e.getSourceId(), REF, UA);

                    var d = KcaDetailParser.parse(doc, DETAIL_BASE);

                    // 이름/성별(상세가 더 신뢰도 높음)
                    e.setName(CrawlUtil.normText(firstNonBlank(d.name, e.getName())));
                    e.setGender(CrawlUtil.normText(firstNonBlank(d.genderKo, e.getGender())));

                    // 이메일
                    if (d.email != null) e.setEmail(d.email);

                    // 라이선스: 번호/종류(텍스트)
                    if (d.licenseNo != null)   e.setLicenseNo(d.licenseNo);
                    if (d.licenseType != null) e.setLicenseType(d.licenseType);

                    // 대상/전문분야
                    if (d.targets != null)   e.setTargets(CrawlUtil.joinDistinct(e.getTargets(), d.targets, " | "));
                    if (d.specialty != null) {
                        String norm = normalizeSpecialty(d.specialty); // 예: "A,B... | C/D/E" -> "C/D/E"
                        if (norm != null) e.setSpecialty(norm);
                    }

                    // 지역(상담가능장소 요약)
                    if (d.regions != null)   e.setRegions(CrawlUtil.joinDistinct(e.getRegions(), d.regions, " | "));

                    // 비용
                    if (d.fee != null)       e.setFee(d.fee);

                    updated++;
                } catch (Exception ex) {
                    log.warn("[KCA][detail] idx={} err={}", e.getSourceId(), ex.toString());
                }
            }
            page++;
            try { Thread.sleep(500); } catch (InterruptedException ie){ Thread.currentThread().interrupt(); }
        }
        log.info("[KCA][detail] enriched={}", updated);
        return updated;
    }

    // backend/crawler/kca/service/KcaDetailCrawler.java (추가 메서드)
    @Transactional
    public boolean crawlOne(String sourceId) {
        return repo.findBySourceAndSourceId("KCA", sourceId)
                .map(e -> {
                    try {
                        var doc = FrameAwareFetcher.fetchFollowingFrames(
                                DETAIL_BASE + "?idx=" + sourceId, REF, UA);
                        var d = KcaDetailParser.parse(doc, DETAIL_BASE);

                        // 덮어쓰기/보강
                        if (d.name != null)      e.setName(d.name);
                        if (d.genderKo != null)  e.setGender(d.genderKo);
                        if (d.email != null)     e.setEmail(d.email);
                        if (d.licenseNo != null) e.setLicenseNo(d.licenseNo);
                        if (d.licenseType != null) e.setLicenseType(d.licenseType);
                        if (d.targets != null)   e.setTargets(CrawlUtil.joinDistinct(e.getTargets(), d.targets, " | "));
                        if (d.specialty != null) {
                            String norm = normalizeSpecialty(d.specialty);
                            if (norm != null) e.setSpecialty(norm);
                        }
                        if (d.regions != null)   e.setRegions(CrawlUtil.joinDistinct(e.getRegions(), d.regions, " | "));
                        if (d.fee != null)       e.setFee(d.fee);
                        return true;
                    } catch (Exception ex){
                        return false;
                    }
                })
                .orElse(false);
    }


    private static String firstNonBlank(String a, String b){
        return (a!=null && !a.isBlank()) ? a : b;
    }
}
