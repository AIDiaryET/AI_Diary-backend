package backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPaymentResponse {

    private String paymentKey;
    private String orderId;
    private String orderName;
    private String status;
    private String method;
    private BigDecimal totalAmount;
    private String currency;

    // 고객 정보
    private String customerName;
    private String customerEmail;
    private String customerMobilePhone;

    // 시간 정보
    private String requestedAt;
    private String approvedAt;
    private String canceledAt;

    // 결제 수단별 상세 정보
    private Map<String, Object> card;
    private Map<String, Object> virtualAccount;
    private Map<String, Object> transfer;
    private Map<String, Object> mobilePhone;
    private Map<String, Object> giftCertificate;
    private Map<String, Object> cashReceipt;
    private Map<String, Object> discount;

    // 취소 정보
    private TossCancelDetails[] cancels;

    // 영수증 정보
    private Map<String, Object> receipt;

    // 결제 실패 정보
    private TossFailureDetails failure;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossCancelDetails {
        private String cancelReason;
        private BigDecimal cancelAmount;
        private BigDecimal canceledAt;
        private BigDecimal taxFreeAmount;
        private BigDecimal taxExemptionAmount;
        private BigDecimal refundableAmount;
        private BigDecimal easyPayDiscountAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossFailureDetails {
        private String code;
        private String message;
    }
}
