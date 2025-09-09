package backend.crawler.kca.component;

import backend.crawler.kca.service.KcaSupervisorCrawler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KcaCrawlerScheduler {
    private final KcaSupervisorCrawler crawler;

    // 매일 새벽 3시
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runNightly() {
        try {
            int n = crawler.crawlAll();
            System.out.println("[KCA] nightly crawl saved=" + n);
        } catch (Exception e) {
            System.err.println("[KCA] nightly crawl failed: " + e.getMessage());
        }
    }
}

