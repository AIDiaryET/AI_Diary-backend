package backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {

    @NotBlank(message = "취소 사유는 필수입니다")
    private String cancelReason;

    // 부분 취소시 사용
    private BigDecimal cancelAmount;

    // 취소 가능 금액
    private BigDecimal refundableAmount;

    // 세금 관련 정보
    private BigDecimal taxFreeAmount;
    private BigDecimal taxExemptionAmount;
}