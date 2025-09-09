package backend.crawler.kca.service;

import backend.crawler.kca.component.CrawlProgress;
import backend.crawler.kca.component.CrawlerKcaProps;
import backend.crawler.kca.component.KcaSupervisorParser;
import backend.crawler.kca.dto.CounselorItem;
import backend.crawler.kca.entity.CounselorEntity;
import backend.crawler.kca.repo.CounselorRepository;
import backend.crawler.kca.util.UniqueKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcaSaver {

    private final CounselorRepository repo;
    private final KcaSupervisorParser parser;
    private final CrawlProgress progress;
    /** 상세 페이지까지 파서 호출 후 저장 (페이지 단위 트랜잭션) */
    @Transactional
    protected int savePageWithDetails(List<CounselorItem> listItems) throws IOException {
        int saved = 0;

        for (CounselorItem li : listItems) {
            try {
                // detailUrl/idx 없는 항목은 스킵
                if (isBlank(li.getDetailUrl()) || isBlank(li.getSourceId())) {
                    log.debug("[KCA] skip: missing detailUrl/idx (name='{}')", safe(li.getName()));
                    continue;
                }

                // 상세 문서 파싱
                CounselorItem detail = parser.fetchDetail(li.getDetailUrl());
                // 소스 식별자/URL 유지
                detail.setSource("KCA");
                detail.setSourceId(li.getSourceId());
                detail.setDetailUrl(li.getDetailUrl());

                saveOne(detail);
                saved++;
                progress.incItemsThisPage();
                progress.incSaved();

                log.debug("[KCA] saved: idx={} name='{}' gender='{}' email='{}'",
                        detail.getSourceId(), safe(detail.getName()),
                        safe(detail.getGender()), safe(detail.getEmail()));

            } catch (Exception ex) {
                progress.incFailed();
                log.warn("[KCA] save failed (idx={} name='{}'): {}",
                        safe(li.getSourceId()), safe(li.getName()), ex.toString());
                log.debug("stacktrace", ex);
            }
        }
        return saved;
    }

    private void saveOne(CounselorItem it) {
        String uniqueKey = UniqueKeyUtil.of(it);

        // (source, sourceId) 우선 → uniqueKey 보조 조회
        CounselorEntity e = repo.findBySourceAndSourceId("KCA", it.getSourceId())
                .orElseGet(() -> repo.findByUniqueKey(uniqueKey).orElseGet(CounselorEntity::new));

        e.setUniqueKey(uniqueKey);
        e.setSource("KCA");
        e.setSourceId(it.getSourceId());
        e.setDetailUrl(it.getDetailUrl());

        e.setName(cut(safe(it.getName()), 150));
        e.setGender(cut(safe(it.getGender()), 10));
        e.setLicenseNo(cut(safe(it.getLicenseNo()), 128));
        e.setLicenseType(safe(it.getLicenseType()));
        e.setEmail(cut(safe(it.getEmail()), 160));
        e.setTargets(safe(it.getTargets()));
        e.setSpecialty(safe(it.getSpecialty()));
        e.setRegions(safe(it.getRegions()));
        e.setFee(cut(safe(it.getFee()), 64));

        repo.save(e);
    }

    /* ================= util ================= */

    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String cut(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }

}
