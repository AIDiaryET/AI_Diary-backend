package backend.crawler.kca.component;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Getter
public class CrawlProgress {
    private volatile boolean running = false;
    private volatile OffsetDateTime startedAt;
    private volatile OffsetDateTime finishedAt;
    private final AtomicInteger page = new AtomicInteger(0);
    private final AtomicInteger itemsThisPage = new AtomicInteger(0);
    private final AtomicInteger savedTotal = new AtomicInteger(0);
    private final AtomicInteger failedTotal = new AtomicInteger(0);

    public void start() {
        running = true;
        startedAt = OffsetDateTime.now();
        finishedAt = null;
        page.set(0); itemsThisPage.set(0);
        savedTotal.set(0); failedTotal.set(0);
    }
    public void pageStart(int p) {
        page.set(p);
        itemsThisPage.set(0);
    }
    public void incItemsThisPage() { itemsThisPage.incrementAndGet(); }
    public void incSaved() { savedTotal.incrementAndGet(); }
    public void incFailed() { failedTotal.incrementAndGet(); }
    public void finish() {
        running = false;
        finishedAt = OffsetDateTime.now();
    }
}

