package backend.crawler.kca.repo;

import backend.crawler.kca.entity.CounselorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CounselorRepository extends JpaRepository<CounselorEntity, Long>, JpaSpecificationExecutor<CounselorEntity> {
    Optional<CounselorEntity> findByUniqueKey(String uniqueKey);
    Optional<CounselorEntity> findBySourceAndSourceId(String kca, String sourceId);

    long countByEmailIsNotNull();
    long countByEmailIsNull();

    long countBySpecialtyIsNotNull();
    long countBySpecialtyIsNull();
    long countByRegionsIsNotNull();

}