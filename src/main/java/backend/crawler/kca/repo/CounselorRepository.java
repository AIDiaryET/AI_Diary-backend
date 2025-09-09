package backend.crawler.kca.repo;

import backend.crawler.kca.entity.CounselorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CounselorRepository extends JpaRepository<CounselorEntity, Long> {
    Optional<CounselorEntity> findByUniqueKey(String uniqueKey);
    Optional<CounselorEntity> findBySourceAndSourceId(String kca, String sourceId);
}