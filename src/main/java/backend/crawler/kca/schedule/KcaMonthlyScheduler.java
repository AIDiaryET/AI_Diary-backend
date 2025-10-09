package backend.crawler.kca.schedule;

import backend.crawler.kca.entity.CrawlSchedule;
import backend.crawler.kca.repo.CrawlScheduleRepository;
import backend.crawler.kca.service.CrawlScheduleTxService;
import backend.crawler.kca.service.KcaCrawlOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;


@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class KcaMonthlyScheduler implements SchedulingConfigurer {

    public static final String KEY = "KCA_MONTHLY";
    private final TaskScheduler taskScheduler;
    private final CrawlScheduleRepository scheduleRepo;
    private final CrawlScheduleTxService txService;     // ✅ 주입
    private final KcaCrawlOrchestrator orchestrator;


    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(taskScheduler);
        registrar.addTriggerTask(this::executeIfDue, dynamicTrigger());
    }

    private Runnable executeIfDue = this::executeIfDue;

    private void executeIfDue() {
        try {

            log.debug("[KCA][SCHED] wakeup");
            // 🔒 트랜잭션 안에서 FOR UPDATE 수행
            var schedule = txService.fetchOrInitWithLock(KEY, this::initDefault);

            if (!schedule.isEnabled()) return;

            var now = ZonedDateTime.now(schedule.zoneId());
            if (now.isBefore(schedule.getNextRunAt())) return;

            if (now.isBefore(schedule.getNextRunAt())) {
                log.debug("[KCA][SCHED] not due yet. nextRunAt={} now={}",
                        schedule.getNextRunAt(), now);
                return;
            }

            log.info("[KCA] Trigger run. nextRunAt={} now={}", schedule.getNextRunAt(), now);

            int upserted = orchestrator.runOnce(KEY);

            var newNext = schedule.getNextRunAt().plusMonths(1);
            // 🔄 트랜잭션 안에서 갱신
            txService.updateNextRun(schedule, now, newNext);

            log.info("[KCA] Rescheduled nextRunAt={}", newNext);
        } catch (Exception e) {
            log.error("[KCA] executeIfDue error", e);
        }
    }

    private Trigger dynamicTrigger() {
        return triggerContext -> {
            try {
                var schedule = scheduleRepo.findByKeyName(KEY).orElse(null);
                if (schedule == null)
                    return ZonedDateTime.now().plusSeconds(10).toInstant();
                return ZonedDateTime.now(schedule.zoneId()).plusMinutes(1).toInstant();
            } catch (Exception e) {
                log.error("[KCA] trigger error", e);
                return ZonedDateTime.now().plusMinutes(1).toInstant();
            }
        };
    }

    @Transactional
    protected CrawlSchedule fetchOrInitScheduleWithLock() {
        return scheduleRepo.findByKeyNameForUpdate(KEY)
                .orElseGet(() -> initDefault());
    }


    private CrawlSchedule initDefault() {
        var zone = "Asia/Seoul";
        var now = ZonedDateTime.now(ZoneId.of(zone));
        return CrawlSchedule.builder()
                .keyName(KEY).timezone(zone).enabled(true)
                .nextRunAt(now).lastRunAt(null)
                .build();
    }
}