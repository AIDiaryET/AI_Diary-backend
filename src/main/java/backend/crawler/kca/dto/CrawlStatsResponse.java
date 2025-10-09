package backend.crawler.kca.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CrawlStatsResponse {
    private long total;
    private long emailFilled;
    private long emailMissing;
    private double emailFilledRate; // 0~100
    private long specialtyFilled;
    private long specialtyMissing;
    private long regionsFilled;
}