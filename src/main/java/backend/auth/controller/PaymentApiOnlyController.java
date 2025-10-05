package backend.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentApiOnlyController {

    @Value("${toss.payments.test.client-key:test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm}")
    private String clientKey;

    @Value("${toss.payments.test.secret-key:test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6}")
    private String secretKey;

    /**
     * 기본 API 테스트
     */
    @GetMapping(value = "/simple-test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> simpleTest() {
        log.info("=== 간단한 API 테스트 호출 ===");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "API 서버가 정상 작동 중입니다!");
        response.put("timestamp", System.currentTimeMillis());
        response.put("endpoint", "/api/simple-test");
        response.put("server_time", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * 토스 페이먼츠 설정 확인
     */
    @GetMapping(value = "/toss-config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> tossConfig() {
        log.info("=== 토스 설정 확인 ===");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "토스 페이먼츠 설정 확인");
        response.put("client_key", clientKey);
        response.put("secret_key_length", secretKey != null ? secretKey.length() : 0);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 결제 준비 API
     */
    @PostMapping(value = "/payment-prepare",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> paymentPrepare(@RequestBody Map<String, Object> request) {
        log.info("=== 결제 준비 API 호출 ===");
        log.info("요청 데이터: {}", request);

        try {
            // 필수 파라미터 추출 및 검증
            String orderId = extractString(request, "orderId");
            Integer amount = extractInteger(request, "amount");
            String orderName = extractString(request, "orderName");
            String method = extractString(request, "method", "CARD");
            String customerName = extractString(request, "customerName", "고객");
            String customerEmail = extractString(request, "customerEmail", "test@example.com");
            String customerPhone = extractString(request, "customerPhone", "01012345678");

            // 필수 파라미터 검증
            if (orderId == null || amount == null || orderName == null) {
                log.warn("필수 파라미터 누락: orderId={}, amount={}, orderName={}",
                        orderId, amount, orderName);
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "필수 파라미터 누락 (orderId, amount, orderName)", "MISSING_PARAMETER"));
            }

            // 금액 유효성 검증
            if (amount < 100) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "결제 금액은 최소 100원 이상이어야 합니다", "INVALID_AMOUNT"));
            }

            if (amount > 10000000) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "결제 금액은 최대 10,000,000원을 초과할 수 없습니다", "AMOUNT_EXCEEDED"));
            }

            log.info("결제 준비 처리 완료: orderId={}, amount={}, orderName={}",
                    orderId, amount, orderName);

            // 성공 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "결제 준비 완료");
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("orderName", orderName);
            response.put("method", method);
            response.put("customerName", customerName);
            response.put("customerEmail", customerEmail);
            response.put("customerPhone", customerPhone);
            response.put("timestamp", System.currentTimeMillis());
            response.put("server_time", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결제 준비 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("결제 준비 실패: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 결제 승인 API (시뮬레이션)
     */
    @PostMapping(value = "/payment-confirm",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> paymentConfirm(@RequestBody Map<String, Object> request) {
        log.info("=== 결제 승인 API 호출 ===");
        log.info("요청 데이터: {}", request);

        try {
            String paymentKey = extractString(request, "paymentKey");
            String orderId = extractString(request, "orderId");
            Integer amount = extractInteger(request, "amount");

            // 필수 파라미터 검증
            if (paymentKey == null || orderId == null || amount == null) {
                log.warn("필수 파라미터 누락: paymentKey={}, orderId={}, amount={}",
                        paymentKey, orderId, amount);
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "필수 파라미터 누락 (paymentKey, orderId, amount)", "MISSING_PARAMETER"));
            }

            log.info("결제 승인 시뮬레이션 처리: paymentKey={}, orderId={}, amount={}",
                    paymentKey, orderId, amount);

            // 토스 페이먼츠 API 응답 형태로 성공 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "결제 승인 완료 (시뮬레이션)");
            response.put("paymentKey", paymentKey);
            response.put("orderId", orderId);
            response.put("orderName", "테스트 상품");
            response.put("status", "DONE");
            response.put("totalAmount", amount);
            response.put("currency", "KRW");
            response.put("method", "카드");
            response.put("approvedAt", LocalDateTime.now().toString());
            response.put("requestedAt", LocalDateTime.now().minusMinutes(1).toString());

            // 카드 정보 (시뮬레이션)
            Map<String, Object> cardInfo = new HashMap<>();
            cardInfo.put("company", "신한");
            cardInfo.put("number", "433012******0000");
            cardInfo.put("installmentPlanMonths", 0);
            cardInfo.put("isInterestFree", false);
            cardInfo.put("approveNo", "00000000");
            response.put("card", cardInfo);

            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결제 승인 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("결제 승인 실패: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 결제 조회 API (시뮬레이션)
     */
    @GetMapping(value = "/payment/{paymentKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable String paymentKey) {
        log.info("=== 결제 조회 API 호출: paymentKey={} ===", paymentKey);

        try {
            if (paymentKey == null || paymentKey.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "paymentKey가 필요합니다", "MISSING_PAYMENT_KEY"));
            }

            // 시뮬레이션 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "결제 조회 완료");
            response.put("paymentKey", paymentKey);
            response.put("orderId", "ORDER_SIMULATION");
            response.put("status", "DONE");
            response.put("totalAmount", 10000);
            response.put("method", "카드");
            response.put("approvedAt", LocalDateTime.now().minusHours(1).toString());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결제 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("결제 조회 실패: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 결제 취소 API (시뮬레이션)
     */
    @PostMapping(value = "/payment/{paymentKey}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @PathVariable String paymentKey,
            @RequestBody Map<String, Object> request) {

        log.info("=== 결제 취소 API 호출: paymentKey={} ===", paymentKey);
        log.info("취소 요청 데이터: {}", request);

        try {
            String cancelReason = extractString(request, "cancelReason", "테스트 취소");

            // 시뮬레이션 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "결제 취소 완료 (시뮬레이션)");
            response.put("paymentKey", paymentKey);
            response.put("status", "CANCELED");
            response.put("cancelReason", cancelReason);
            response.put("canceledAt", LocalDateTime.now().toString());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결제 취소 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("결제 취소 실패: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 헬스 체크
     */
    @GetMapping(value = "/payment-health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> paymentHealth() {
        log.info("=== 헬스 체크 호출 ===");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Payment API 정상 작동");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server_time", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        response.put("toss_client_key", clientKey);

        return ResponseEntity.ok(response);
    }

    /**
     * 웹훅 시뮬레이션
     */
    @PostMapping(value = "/payment-webhook",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> paymentWebhook(@RequestBody Map<String, Object> webhookData) {
        log.info("=== 웹훅 수신 시뮬레이션 ===");
        log.info("웹훅 데이터: {}", webhookData);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "웹훅 처리 완료");
        response.put("received_data", webhookData);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    // === 유틸리티 메서드들 ===

    private String extractString(Map<String, Object> map, String key) {
        return extractString(map, key, null);
    }

    private String extractString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Double) {
                return ((Double) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            }
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: key={}, value={}, type={}", key, value, value.getClass());
        }

        return null;
    }

    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("errorCode", errorCode);
        response.put("timestamp", System.currentTimeMillis());
        response.put("server_time", LocalDateTime.now().toString());
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        return createErrorResponse(message, "UNKNOWN_ERROR");
    }
}

