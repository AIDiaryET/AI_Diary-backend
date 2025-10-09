// backend/crawler/kca/dto/CrawlOneRequest.java
package backend.crawler.kca.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CrawlOneRequest {
    @NotBlank
    private String sourceId;  // KCA 상세 idx (예: "1104")
}
