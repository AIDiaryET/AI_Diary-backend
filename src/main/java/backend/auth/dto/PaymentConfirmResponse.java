package backend.auth.dto;

import com.fasterxml.classmate.AnnotationOverrides;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmResponse {

    public static AnnotationOverrides PaymentData;
    private boolean success;
    private String message;

    // 응답 데이터
    private String paymentKey;
    private String orderId;
    private String orderName;
    private String status;
    private String method;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    // 카드 정보
    private Map<String, Object> card;

    // 가상계좌 정보
    private Map<String, Object> virtualAccount;

    // 계좌이체 정보
    private Map<String, Object> transfer;

    // 휴대폰 결제 정보
    private Map<String, Object> mobilePhone;

    // 영수증 정보
    private Map<String, Object> receipt;

    // 에러 정보
    private String errorCode;
    private String errorMessage;

    // 편의 메서드
    public static PaymentConfirmResponse success(Map<String, Object> tossResponse) {
        return PaymentConfirmResponse.builder()
                .success(true)
                .message("결제 승인 완료")
                .paymentKey((String) tossResponse.get("paymentKey"))
                .orderId((String) tossResponse.get("orderId"))
                .orderName((String) tossResponse.get("orderName"))
                .status((String) tossResponse.get("status"))
                .method((String) tossResponse.get("method"))
                .totalAmount(new BigDecimal(tossResponse.get("totalAmount").toString()))
                .currency((String) tossResponse.get("currency"))
                .card((Map<String, Object>) tossResponse.get("card"))
                .virtualAccount((Map<String, Object>) tossResponse.get("virtualAccount"))
                .transfer((Map<String, Object>) tossResponse.get("transfer"))
                .mobilePhone((Map<String, Object>) tossResponse.get("mobilePhone"))
                .receipt((Map<String, Object>) tossResponse.get("receipt"))
                .build();
    }

    public static PaymentConfirmResponse failure(String message) {
        return PaymentConfirmResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static PaymentConfirmResponse failure(String errorCode, String errorMessage) {
        return PaymentConfirmResponse.builder()
                .success(false)
                .message(errorMessage)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
