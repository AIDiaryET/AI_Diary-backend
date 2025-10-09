package backend.crawler.kca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "kca_crawl_schedule")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // 스케줄 키(여러 크롤러 확장 대비)
    @Column(nullable = false, unique = true, length = 50)
    private String keyName; // 예: "KCA_MONTHLY"


    // 다음 실행 예정 시각(타임존 포함)
    @Column(nullable = false)
    private ZonedDateTime nextRunAt;


    // 마지막 실행 완료 시각
    private ZonedDateTime lastRunAt;


    @Column(nullable = false)
    private String timezone; // 예: "Asia/Seoul"


    @Column(nullable = false)
    private boolean enabled;


    @Version
    private Long lockVersion; // 낙관적 잠금도 보조


    @CreationTimestamp
    private ZonedDateTime createdAt;


    @UpdateTimestamp
    private ZonedDateTime updatedAt;


    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }
}