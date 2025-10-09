package backend.crawler.kca.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CrawlStatusResponse {
    private String key;
    private String timezone;
    private boolean enabled;
    private ZonedDateTime nextRunAt;
    private ZonedDateTime lastRunAt;

    // 최근 실행 로그 요약
    private String lastStatus;      // SUCCESS/FAILED/STARTED
    private Integer lastUpserted;   // list upsert
    private ZonedDateTime lastFinishedAt;
    private String lastMessage;
}

