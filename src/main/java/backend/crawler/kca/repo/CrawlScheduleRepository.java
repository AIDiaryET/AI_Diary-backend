package backend.crawler.kca.repo;

import backend.crawler.kca.entity.CrawlSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


public interface CrawlScheduleRepository extends JpaRepository<CrawlSchedule, Long> {

    Optional<CrawlSchedule> findByKeyName(String keyName);

    // 동시 실행 방지: 스케줄 행에 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CrawlSchedule s where s.keyName = :key")
    Optional<CrawlSchedule> findByKeyNameForUpdate(@Param("key") String keyName);
}