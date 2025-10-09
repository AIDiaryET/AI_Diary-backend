package backend.auth.repository;

import backend.auth.entity.Order;
import backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.kakaoId = :kakaoId ORDER BY o.createdAt DESC")
    List<Order> findByUserKakaoIdOrderByCreatedAtDesc(@Param("kakaoId") String kakaoId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByUserAndStatusOrderByCreatedAtDesc(User user, Order.OrderStatus status);

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByUserAndDateRange(@Param("user") User user,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    List<Order> findByStatusInOrderByCreatedAtDesc(List<Order.OrderStatus> statuses);

    boolean existsByOrderId(String orderId);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED', 'PAID') ORDER BY o.createdAt DESC")
    List<Order> findCancelableOrders();

    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.status IN ('PENDING', 'CONFIRMED', 'PAID') ORDER BY o.createdAt DESC")
    List<Order> findCancelableOrdersByUser(@Param("user") User user);
}