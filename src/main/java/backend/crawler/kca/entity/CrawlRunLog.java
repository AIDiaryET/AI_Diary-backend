package backend.crawler.kca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;


@Entity
@Table(name = "kca_crawl_run_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlRunLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String keyName; // "KCA_MONTHLY"

    @CreationTimestamp
    private ZonedDateTime startedAt;

    private ZonedDateTime finishedAt;

    @Column(nullable = false, length = 20)
    private String status; // STARTED, SUCCESS, FAILED

    @Column(length = 1000)
    private String message;

    private Integer upsertedCount;
}