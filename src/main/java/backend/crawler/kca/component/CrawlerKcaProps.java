package backend.crawler.kca.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "crawler.kca")
public class CrawlerKcaProps {
    private String baseUrl;     // e.g. https://counselors.or.kr
    private String listPath;    // e.g. /KOR/license/supervisor_6.php
    private int timeoutMs = 8000;
    private int minDelayMs = 800;
    private int maxDelayMs = 1500;
    private int maxPages = 200;
    private String userAgent = "MindNoteCrawler/1.0 (+https://your-service.example) Mozilla/5.0";
}

