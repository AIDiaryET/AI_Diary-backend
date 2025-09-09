// backend/crawler/kca/controller/KcaCrawlerController.java
package backend.crawler.kca.controller;

import backend.crawler.kca.dto.*;
import backend.crawler.kca.entity.CounselorEntity;
import backend.crawler.kca.repo.CounselorRepository;
import backend.crawler.kca.service.KcaDetailCrawler;
import backend.crawler.kca.service.KcaListCrawler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/crawler/kca")
@RequiredArgsConstructor
public class KcaCrawlerController {

    private final KcaListCrawler listCrawler;
    private final KcaDetailCrawler detailCrawler;
    private final CounselorRepository repo;

    @PostMapping("/list")
    public ResponseEntity<CrawlRunResponse> runList() {
        LocalDateTime s = LocalDateTime.now();
        Integer upserted = null;
        String msg = null;

        try {
            upserted = listCrawler.crawlAllPages();
        } catch (Exception e) {
            msg = e.toString();
        }

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("list")
                .source("KCA")
                .upsertedFromList(upserted)
                .enrichedFromDetail(null)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    @PostMapping("/detail")
    public ResponseEntity<CrawlRunResponse> runDetail() {
        LocalDateTime s = LocalDateTime.now();
        Integer enriched = null;
        String msg = null;

        try {
            enriched = detailCrawler.crawlAndEnrichAll();
        } catch (Exception e) {
            msg = e.toString();
        }

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("detail")
                .source("KCA")
                .upsertedFromList(null)
                .enrichedFromDetail(enriched)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    @PostMapping("/all")
    public ResponseEntity<CrawlRunResponse> runAll() {
        LocalDateTime s = LocalDateTime.now();
        Integer upserted = 0, enriched = 0;
        String msg = null;

        try {
            upserted = listCrawler.crawlAllPages();
            enriched = detailCrawler.crawlAndEnrichAll();
        } catch (Exception e) {
            msg = e.toString();
        }

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("all")
                .source("KCA")
                .upsertedFromList(upserted)
                .enrichedFromDetail(enriched)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    /** 단건 재크롤 (특정 idx) */
    @PostMapping("/one")
    public ResponseEntity<CrawlRunResponse> runOne(@Valid @RequestBody CrawlOneRequest req) {
        LocalDateTime s = LocalDateTime.now();
        boolean ok = detailCrawler.crawlOne(req.getSourceId());
        String msg = ok ? "OK" : "NOT_FOUND_OR_FAILED";

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("one")
                .source("KCA")
                .upsertedFromList(null)
                .enrichedFromDetail(ok ? 1 : 0)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    /** (옵션) 최신 저장분 프리뷰 */
    @GetMapping("/preview")
    public ResponseEntity<Page<CounselorResponse>> preview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<CounselorEntity> data = repo.findAll(pageable);
        Page<CounselorResponse> res = data.map(CounselorResponse::from);
        return ResponseEntity.ok(res);
    }
}
