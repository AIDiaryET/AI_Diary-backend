package backend.crawler.kca.controller;

import backend.crawler.kca.dto.*;
import backend.crawler.kca.entity.CounselorEntity;
import backend.crawler.kca.repo.CounselorRepository;
import backend.crawler.kca.repo.CrawlRunLogRepository;
import backend.crawler.kca.repo.CrawlScheduleRepository;
import backend.crawler.kca.schedule.KcaMonthlyScheduler;
import backend.crawler.kca.service.KcaDetailCrawler;
import backend.crawler.kca.service.KcaListCrawler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import backend.crawler.kca.spec.CounselorSpecs;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/crawler/kca")
@RequiredArgsConstructor
@Tag(name = "KCA 크롤러", description = "한국상담심리학회(KCA) 크롤링 및 월간 스케줄 관리 API")
public class KcaCrawlerController {

    private final KcaListCrawler listCrawler;
    private final KcaDetailCrawler detailCrawler;
    private final CounselorRepository repo;
    private final CrawlScheduleRepository scheduleRepo;
    private final CrawlRunLogRepository runLogRepo;

    // ========================== 상태/통계 ==========================

    @Operation(
            summary = "크롤링 스케줄 상태 조회",
            description = "현재 KCA 월간 스케줄의 다음 실행 시각, 마지막 실행 결과, 상태 등을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "스케줄 상태 조회 성공",
                            content = @Content(schema = @Schema(implementation = CrawlStatusResponse.class)))
            }
    )
    @GetMapping("/status")
    public ResponseEntity<CrawlStatusResponse> status() {
        var key = KcaMonthlyScheduler.KEY;
        var sOpt = scheduleRepo.findByKeyName(key);
        if (sOpt.isEmpty()) {
            return ResponseEntity.ok(CrawlStatusResponse.builder()
                    .key(key).enabled(false).build());
        }
        var s = sOpt.get();
        var lastLog = runLogRepo.findFirstByKeyNameOrderByFinishedAtDesc(key).orElse(null);

        var res = CrawlStatusResponse.builder()
                .key(s.getKeyName())
                .timezone(s.getTimezone())
                .enabled(s.isEnabled())
                .nextRunAt(s.getNextRunAt())
                .lastRunAt(s.getLastRunAt())
                .lastStatus(lastLog != null ? lastLog.getStatus() : null)
                .lastUpserted(lastLog != null ? lastLog.getUpsertedCount() : null)
                .lastFinishedAt(lastLog != null ? lastLog.getFinishedAt() : null)
                .lastMessage(lastLog != null ? lastLog.getMessage() : null)
                .build();
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "데이터 통계 조회",
            description = "현재 DB에 저장된 상담사 데이터 중 이메일·전문분야·지역 등의 채워진 비율을 집계하여 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "통계 조회 성공",
                            content = @Content(schema = @Schema(implementation = CrawlStatsResponse.class)))
            }
    )
    @GetMapping("/stats")
    public ResponseEntity<CrawlStatsResponse> stats() {
        long total = repo.count();
        long emailFilled = repo.countByEmailIsNotNull();
        long emailMissing = repo.countByEmailIsNull();
        long specialtyFilled = repo.countBySpecialtyIsNotNull();
        long specialtyMissing = repo.countBySpecialtyIsNull();
        long regionsFilled = repo.countByRegionsIsNotNull();

        double rate = (total == 0) ? 0.0 : (emailFilled * 100.0 / total);

        var res = CrawlStatsResponse.builder()
                .total(total)
                .emailFilled(emailFilled)
                .emailMissing(emailMissing)
                .emailFilledRate(Math.round(rate * 10.0) / 10.0)
                .specialtyFilled(specialtyFilled)
                .specialtyMissing(specialtyMissing)
                .regionsFilled(regionsFilled)
                .build();
        return ResponseEntity.ok(res);
    }


    @Operation(
            summary = "목록 크롤링",
            description = "KCA 목록 페이지 전체를 크롤링하여 상담사 기본정보를 갱신합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "크롤 성공",
                            content = @Content(schema = @Schema(implementation = CrawlRunResponse.class)))
            }
    )
    @PostMapping("/list")
    public ResponseEntity<CrawlRunResponse> runList() {
        LocalDateTime s = LocalDateTime.now();
        Integer upserted = null;
        String msg;
        try {
            upserted = listCrawler.crawlAllPages();
            msg = "OK";
        } catch (Exception e) {
            msg = e.toString();
        }

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("list")
                .source("KCA")
                .upsertedFromList(upserted)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    @Operation(
            summary = "상세 크롤링",
            description = "상세 페이지를 순회하며 이메일, 전문분야, 상담대상 등 세부 정보를 보강합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "상세 크롤링 완료",
                            content = @Content(schema = @Schema(implementation = CrawlRunResponse.class)))
            }
    )
    @PostMapping("/detail")
    public ResponseEntity<CrawlRunResponse> runDetail() {
        LocalDateTime s = LocalDateTime.now();
        Integer enriched = null;
        String msg;
        try {
            enriched = detailCrawler.crawlAndEnrichAll();
            msg = "OK";
        } catch (Exception e) {
            msg = e.toString();
        }

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("detail")
                .source("KCA")
                .enrichedFromDetail(enriched)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    @Operation(
            summary = "전체 크롤링 (목록 + 상세)",
            description = "목록과 상세를 순차적으로 실행하여 전체 상담사 정보를 최신화합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "전체 크롤링 완료",
                            content = @Content(schema = @Schema(implementation = CrawlRunResponse.class)))
            }
    )
    @PostMapping("/all")
    public ResponseEntity<CrawlRunResponse> runAll() {
        LocalDateTime s = LocalDateTime.now();
        Integer upserted = 0, enriched = 0;
        String msg;
        try {
            upserted = listCrawler.crawlAllPages();
            enriched = detailCrawler.crawlAndEnrichAll();
            msg = "OK";
        } catch (Exception e) {
            msg = e.toString();
        }

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("all")
                .source("KCA")
                .upsertedFromList(upserted)
                .enrichedFromDetail(enriched)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    @Operation(
            summary = "단건 재크롤",
            description = "특정 상담사(`sourceId`)에 대해 상세 페이지를 다시 크롤링합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "단건 크롤링 완료",
                            content = @Content(schema = @Schema(implementation = CrawlRunResponse.class)))
            }
    )
    @PostMapping("/one")
    public ResponseEntity<CrawlRunResponse> runOne(@Valid @RequestBody CrawlOneRequest req) {
        LocalDateTime s = LocalDateTime.now();
        boolean ok = detailCrawler.crawlOne(req.getSourceId());
        String msg = ok ? "OK" : "NOT_FOUND_OR_FAILED";

        return ResponseEntity.ok(CrawlRunResponse.builder()
                .task("one")
                .source("KCA")
                .enrichedFromDetail(ok ? 1 : 0)
                .startedAt(s)
                .finishedAt(LocalDateTime.now())
                .message(msg)
                .build());
    }

    @Operation(
            summary = "최신 상담사 데이터 프리뷰",
            description = "DB에 저장된 상담사 데이터를 최근 수정일 기준으로 페이지네이션하여 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "프리뷰 성공",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CounselorResponse.class))))
            }
    )
    @GetMapping("/preview")
    public ResponseEntity<Page<CounselorResponse>> preview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<CounselorEntity> data = repo.findAll(pageable);
        Page<CounselorResponse> res = data.map(CounselorResponse::from);
        return ResponseEntity.ok(res);
    }



    @Operation(
            summary = "상담사 검색(지역+특화+타겟)",
            description = """
    지역(regions), 특화(specialty), 타겟(targets)으로 검색합니다.
    - region: CSV (부분일치 OR)  예) '대구,동구,서울'
    - specialty: CSV ('/' 경계 매칭 OR)  예) '개인상담,심리검사,위기사례개입'
    - targets: CSV (다양한 구분자 정규화 후 '/' 경계 매칭 OR)  예) '아동,청소년,성인,노인'
    각 항목 그룹은 OR로 묶이고, 그룹 간에는 AND로 결합됩니다.
    정렬 기본: updatedAt,DESC (sort=컬럼,방향)
    """
    )
    @GetMapping("/search")
    public ResponseEntity<Page<CounselorResponse>> search(
            @Parameter(description = "지역 CSV (부분일치 OR)", example = "대구,동구,서울")
            @RequestParam(required = false) String region,
            @Parameter(description = "특화(전문분야) CSV ('/' 경계 매칭 OR)", example = "개인상담,심리검사,위기사례개입")
            @RequestParam(required = false) String specialty,
            @Parameter(description = "타겟(상담대상) CSV ('/' 경계 매칭 OR)", example = "아동,청소년,성인,노인")
            @RequestParam(required = false) String targets,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,DESC") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        var regions = splitCsv(region);
        var specs   = splitCsv(specialty);
        var tgs     = splitCsv(targets);

        Specification<CounselorEntity> spec = Specification.allOf(
                CounselorSpecs.regionAny(regions),
                CounselorSpecs.specialtyAny(specs),
                CounselorSpecs.targetsAny(tgs)
        );

        Page<CounselorEntity> data = repo.findAll(spec, pageable);
        Page<CounselorResponse> res = data.map(CounselorResponse::from);
        return ResponseEntity.ok(res);
    }

    private static Pageable toPageable(int page, int size, String sort) {
        String[] parts = sort.split(",", 2);
        String col = parts.length > 0 ? parts[0].trim() : "updatedAt";
        Sort.Direction dir = (parts.length == 2 && "ASC".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(dir, col));
    }

    private static java.util.List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
