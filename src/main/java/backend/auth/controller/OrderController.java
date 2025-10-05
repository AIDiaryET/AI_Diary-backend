package backend.auth.controller;

import backend.auth.entity.Order;
import backend.auth.entity.User;
import backend.auth.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("인증되지 않은 주문 생성 요청");
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다"));
        }

        try {
            // 주문 생성
            Order order = Order.builder()
                    .orderId((String) request.get("orderId"))
                    .amount(Long.valueOf(request.get("amount").toString()))
                    .customerName((String) request.get("customerName"))
                    .productName((String) request.get("productName"))
                    .user(user) // 사용자 연결
                    .status(Order.OrderStatus.PENDING)
                    .build();

            Order savedOrder = orderRepository.save(order);
            log.info("주문 생성 완료: {}", savedOrder.getOrderId());

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            log.error("주문 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "주문 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다"));
        }

        try {
            List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("주문 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "주문 조회에 실패했습니다"));
        }
    }
}
