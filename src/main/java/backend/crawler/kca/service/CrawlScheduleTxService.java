package backend.crawler.kca.service;

import backend.crawler.kca.entity.CrawlSchedule;
import backend.crawler.kca.repo.CrawlScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CrawlScheduleTxService {
    private final CrawlScheduleRepository scheduleRepo;

    @Transactional
    public CrawlSchedule fetchOrInitWithLock(String key, Supplier<CrawlSchedule> initSupplier) {
        return scheduleRepo.findByKeyNameForUpdate(key)
                .orElseGet(() -> scheduleRepo.save(initSupplier.get()));
    }

    @Transactional
    public void updateNextRun(CrawlSchedule s, ZonedDateTime lastRunAt, ZonedDateTime nextRunAt) {
        s.setLastRunAt(lastRunAt);
        s.setNextRunAt(nextRunAt);
        scheduleRepo.save(s);
    }
}