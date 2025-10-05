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
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String orderName;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // 상품 정보 필드 추가
    private String productName;    // 상품명
    private String productImage;   // 상품 이미지 URL
    private Integer quantity;      // 수량
    private Long unitPrice;       // 단가

    // User 연관관계 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 고객 정보 (User가 없는 경우 사용)
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // 배송 정보
    private String shippingAddress;
    private String shippingPhone;
    private String deliveryMemo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // OrderStatus enum
    public enum OrderStatus {
        PENDING,     // 주문 대기
        CONFIRMED,   // 주문 확인
        PAID,        // 결제 완료
        PREPARING,   // 상품 준비중
        SHIPPED,     // 배송중
        DELIVERED,   // 배송 완료
        CANCELED,    // 주문 취소
        REFUNDED     // 환불 완료
    }

    // 비즈니스 메서드
    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    public boolean isCancelable() {
        return this.status == OrderStatus.PENDING ||
                this.status == OrderStatus.CONFIRMED ||
                this.status == OrderStatus.PAID;
    }

    public boolean isShippable() {
        return this.status == OrderStatus.PAID ||
                this.status == OrderStatus.PREPARING;
    }

    public boolean isDelivered() {
        return this.status == OrderStatus.DELIVERED;
    }

    public String getActualCustomerName() {
        if (user != null) {
            // User의 getDisplayName() 메서드 사용 (name이 있으면 name, 없으면 nickname 반환)
            return user.getDisplayName();
        }
        return customerName;
    }

    public String getActualCustomerPhone() {
        if (user != null && user.getPhone() != null) {
            return user.getPhone();
        }
        return customerPhone;
    }

    // 상품 정보 관련 메서드
    public String getDisplayProductName() {
        return productName != null ? productName : orderName;
    }

    public Long getTotalAmount() {
        if (quantity != null && unitPrice != null) {
            return quantity * unitPrice;
        }
        return amount;
    }

    // 주문 요약 정보
    public String getOrderSummary() {
        if (quantity != null && quantity > 1) {
            return getDisplayProductName() + " 외 " + (quantity - 1) + "건";
        }
        return getDisplayProductName();
    }
}