package backend.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank(message = "결제 키는 필수입니다")
    private String paymentKey;

    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;

    @NotNull(message = "결제 금액은 필수입니다")
    @Min(value = 100, message = "결제 금액은 최소 100원 이상이어야 합니다")
    private Integer amount;
}
