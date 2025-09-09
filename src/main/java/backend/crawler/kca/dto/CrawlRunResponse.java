package backend.crawler.kca.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CrawlRunResponse {
    private String task;                // "list", "detail", "all", "one"
    private Integer upsertedFromList;   // 목록 업서트 건수
    private Integer enrichedFromDetail; // 상세 보강 건수
    private String source;              // "KCA"
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;             // 에러/메모
}
