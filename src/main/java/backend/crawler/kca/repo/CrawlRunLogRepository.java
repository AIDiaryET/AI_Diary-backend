package backend.crawler.kca.repo;

import backend.crawler.kca.entity.CrawlRunLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// backend/crawler/kca/repo/CrawlRunLogRepository.java
public interface CrawlRunLogRepository extends JpaRepository<CrawlRunLog, Long> {
    Optional<CrawlRunLog> findFirstByKeyNameOrderByFinishedAtDesc(String keyName);
}
