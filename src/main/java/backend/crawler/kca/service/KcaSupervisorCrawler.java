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
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcaSupervisorCrawler {

    private final CrawlerKcaProps props;
    private final KcaSupervisorParser parser;
    private final CounselorRepository repo;
    private final CrawlProgress progress;

    /** 전체 크롤(운영용) */
    public int crawlAll() throws IOException {
        return crawlRange(1, 5);
//        props.getMaxPages()
    }

    /** 테스트·부분 수행: start~end 페이지만 */
    public int crawlRange(int startPage, int endPage) throws IOException {
        int start = Math.max(1, startPage);
        int end   = Math.min(Math.max(start, endPage), props.getMaxPages());

        log.info("[KCA] Crawl range start: {}..{} (ts={})", start, end, OffsetDateTime.now());
        progress.start();
        int totalSaved = 0;

        try {
            for (int page = start; page <= end; page++) {
                progress.pageStart(page);
                log.info("[KCA] Page {} fetching ...", page);

                List<CounselorItem> listItems = parser.fetchPage(page);
                if (listItems.isEmpty()) {
                    log.info("[KCA] Page {} empty → stop early.", page);
                    break;
                }
                log.info("[KCA] Page {} parsed(list): {} items", page, listItems.size());

                int savedThisPage = 0;

                // 페이지 단위 트랜잭션 (격리)
                savedThisPage = savePageWithDetails(listItems);

                totalSaved += savedThisPage;
                log.info("[KCA] Page {} done: saved={}, failed(acc)={}",
                        page, savedThisPage, progress.getFailedTotal().get());
            }

            log.info("[KCA] Crawl finished: savedTotal={}, failedTotal={}",
                    progress.getSavedTotal().get(), progress.getFailedTotal().get());
            return totalSaved;

        } finally {
            progress.finish();
        }
    }

    /** 상세 페이지까지 파서 호출 후 저장 (페이지 단위 트랜잭션) */
    /** 상세 페이지까지 파서 호출 후 저장 (페이지 단위 트랜잭션) */
    @Transactional
    protected int savePageWithDetails(List<CounselorItem> listItems) throws IOException {
        log.info("[KCA] savePageWithDetails ENTER: items={}", listItems.size());

        int saved = 0, skippedMissingDetail = 0, failed = 0;

        for (CounselorItem li : listItems) {
            try {
                // detailUrl/idx 보정: idx가 비면 detailUrl에서 뽑기
                String detailUrl = safe(li.getDetailUrl());
                String sourceId  = safe(li.getSourceId());
                if (isBlank(sourceId) && !isBlank(detailUrl)) {
                    sourceId = parseIdxFromUrl(detailUrl);   // 아래 함수 추가
                    li.setSourceId(sourceId);
                }

                if (isBlank(detailUrl) || isBlank(sourceId)) {
                    skippedMissingDetail++;
                    log.info("[KCA] skip(missing detail): name='{}', url='{}', idx='{}'",
                            safe(li.getName()), detailUrl, sourceId);
                    continue;
                }

                // 상세 문서 파싱
                CounselorItem detail = parser.fetchDetail(detailUrl);
                if (detail == null) {
                    failed++;
                    log.warn("[KCA] detail parse returned null: idx={} url={}", sourceId, detailUrl);
                    continue;
                }

                // 소스 식별자/URL 유지
                detail.setSource("KCA");
                detail.setSourceId(sourceId);
                detail.setDetailUrl(detailUrl);

                saveOne(detail);
                saved++;
                progress.incItemsThisPage();
                progress.incSaved();

                log.info("[KCA] saved: idx={} name='{}' email='{}'",
                        sourceId, safe(detail.getName()), safe(detail.getEmail()));

            } catch (Exception ex) {
                failed++;
                progress.incFailed();
                log.warn("[KCA] save failed (idx={} name='{}'): {}", safe(li.getSourceId()), safe(li.getName()), ex.toString());
                log.debug("stacktrace", ex);
            }
        }

        log.info("[KCA] savePageWithDetails EXIT: parsed={}, saved={}, skippedMissingDetail={}, failed={}",
                listItems.size(), saved, skippedMissingDetail, failed);
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

    private static String parseIdxFromUrl(String url) {
        if (isBlank(url)) return "";
        // 예: ...?idx=12345 또는 .../detail/12345
        var m = java.util.regex.Pattern.compile("[?&]idx=(\\w+)").matcher(url);
        if (m.find()) return m.group(1);
        m = java.util.regex.Pattern.compile("/(\\d+)(?:\\D|$)").matcher(url);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String cut(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
}
