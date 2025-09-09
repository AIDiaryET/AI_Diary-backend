// src/main/java/com/example/crawler/kca/KcaAdminController.java
package backend.crawler.kca.controller;

import backend.crawler.kca.component.CrawlProgress;
import backend.crawler.kca.service.KcaSupervisorCrawler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/kca")
@RequiredArgsConstructor
public class KcaAdminController {
    private final KcaSupervisorCrawler crawler;
    private final CrawlProgress progress;

    @PostMapping("/crawl")
    public Map<String,Object> run() throws IOException {
        int saved = crawler.crawlAll();
        return Map.of("saved", saved, "ts", OffsetDateTime.now().toString());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "running", progress.isRunning(),
                "startedAt", progress.getStartedAt(),
                "finishedAt", progress.getFinishedAt(),
                "page", progress.getPage().get(),
                "itemsThisPage", progress.getItemsThisPage().get(),
                "savedTotal", progress.getSavedTotal().get(),
                "failedTotal", progress.getFailedTotal().get()
        );
    }
}
