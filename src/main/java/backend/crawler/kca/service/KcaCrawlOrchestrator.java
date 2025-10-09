package backend.crawler.kca.service;

import backend.crawler.kca.dto.CounselorItem;
import backend.crawler.kca.entity.CrawlRunLog;
import backend.crawler.kca.repo.CrawlRunLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcaCrawlOrchestrator {
    private final KcaListCrawler listCrawler;
    private final KcaDetailCrawler detailCrawler;
    private final CrawlRunLogRepository runLogRepo;

    @Transactional
    public int runOnce(String keyName) throws Exception {
        var run = runLogRepo.save(CrawlRunLog.builder()
                .keyName(keyName)
                .status("STARTED")
                .build());

        int upserted = 0;
        int enriched  = 0;

        try {
            // 1) 목록 전 페이지 업서트
            upserted = listCrawler.crawlAllPages();

            // 2) 상세 크롤로 이메일/부가필드 보강
            //    - 변경감지/미완 데이터만 처리하도록 내부에서 필터링하면 성능 유리
            enriched = detailCrawler.crawlAndEnrichAll();

            run.setStatus("SUCCESS");
            run.setUpsertedCount(upserted);
            run.setMessage("OK (detail+" + enriched + ")");
            run.setFinishedAt(ZonedDateTime.now());
            runLogRepo.save(run);

            log.info("[KCA] Monthly crawl finished. listUpserted={}, detailEnriched={}", upserted, enriched);
            return upserted + enriched; // 필요 시 합/또는 list만 반환 등 정책 선택
        } catch (Exception e) {
            log.error("[KCA] Monthly crawl failed", e);
            run.setStatus("FAILED");
            run.setMessage(e.getMessage());
            run.setFinishedAt(ZonedDateTime.now());
            runLogRepo.save(run);
            throw e;
        }
    }
}