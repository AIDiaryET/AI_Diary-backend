package backend.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String paymentKey;

    @Column(nullable = false)
    private String orderId;

    private String orderName;

    @Column(nullable = false)
    private Long amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String method;

    // 고객 정보
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // 결제 승인 시간
    private LocalDateTime paidAt;

    // 취소 관련
    private LocalDateTime canceledAt;
    private String cancelReason;

    // 실패 관련
    private String failReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // PaymentStatus enum
    public enum PaymentStatus {
        WAITING,    // 결제 대기
        IN_PROGRESS, // 결제 진행중
        DONE,       // 결제 완료
        CANCELED,   // 결제 취소
        PARTIAL_CANCELED, // 부분 취소
        ABORTED,    // 결제 중단
        EXPIRED,    // 결제 만료
        FAILED      // 결제 실패
    }

    // 비즈니스 메서드
    public boolean isCancelable() {
        return this.status == PaymentStatus.DONE && this.canceledAt == null;
    }

    public boolean isPaid() {
        return this.status == PaymentStatus.DONE;
    }

    public boolean isCanceled() {
        return this.status == PaymentStatus.CANCELED || this.status == PaymentStatus.PARTIAL_CANCELED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED || this.status == PaymentStatus.ABORTED;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.DONE ||
                this.status == PaymentStatus.CANCELED ||
                this.status == PaymentStatus.FAILED;
    }
}
