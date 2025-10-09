package backend.crawler.kca.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupFetcher {
    private final CrawlerKcaProps props;
    private final Random rnd = new Random();

    @Retryable(value = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1500, multiplier = 2.0))
    public Document get(String url) throws IOException {
        sleep();
        log.debug("[KCA] GET {}", url);
        return Jsoup.connect(url)
                .userAgent(props.getUserAgent())
                .timeout(props.getTimeoutMs())
                .header("Accept-Language","ko,en;q=0.8")
                .referrer(props.getBaseUrl())
                .get();
    }

    private void sleep() {
        int span = Math.max(1, props.getMaxDelayMs() - props.getMinDelayMs());
        int d = props.getMinDelayMs() + rnd.nextInt(span);
        try { Thread.sleep(d); } catch (InterruptedException ignored) {}
    }
}
