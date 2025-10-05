package backend.auth.service;

import backend.auth.dto.PaymentCancelRequest;
import backend.auth.dto.PaymentConfirmRequest;
import backend.auth.dto.PaymentConfirmResponse;
import backend.auth.dto.TossPaymentResponse;
import backend.auth.entity.Order;
import backend.auth.entity.Payment;
import backend.auth.exception.OrderNotFoundException;
import backend.auth.exception.PaymentException;
import backend.auth.exception.PaymentNotFoundException;
import backend.auth.repository.OrderRepository;
import backend.auth.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentsClient tossClient;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    };

    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        try {
            Order order = orderRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + request.getOrderId()));

            // 금액 검증
            Long requestAmount = Long.valueOf(request.getAmount());
            if (!order.getAmount().equals(requestAmount)) {
                throw new PaymentException("Payment amount mismatch. Expected: " + order.getAmount() + ", Actual: " + requestAmount);
            }

            Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
            if (existingPayment.isPresent() && existingPayment.get().getStatus() == Payment.PaymentStatus.DONE) {
                throw new PaymentException("Payment already completed for order: " + request.getOrderId());
            }

            TossPaymentResponse tossResponse = tossClient.confirmPayment(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    requestAmount
            ).block();

            Payment payment = Payment.builder()
                    .paymentKey(request.getPaymentKey())
                    .orderId(request.getOrderId())
                    .orderName(tossResponse.getOrderName()) // orderName 추가
                    .amount(requestAmount)
                    .status(Payment.PaymentStatus.valueOf(tossResponse.getStatus()))
                    .method(tossResponse.getMethod())
                    .currency(tossResponse.getCurrency())
                    .customerName(tossResponse.getCustomerName())
                    .customerEmail(tossResponse.getCustomerEmail())
                    .paidAt(parseDateTime(tossResponse.getApprovedAt()))
                    .build();

            if (existingPayment.isPresent()) {
                Payment existing = existingPayment.get();
                existing.setStatus(payment.getStatus());
                existing.setMethod(payment.getMethod());
                existing.setPaidAt(payment.getPaidAt());
                existing.setCustomerName(payment.getCustomerName());
                existing.setCustomerEmail(payment.getCustomerEmail());
                existing.setOrderName(payment.getOrderName());
                existing.setCurrency(payment.getCurrency());
                payment = paymentRepository.save(existing);
            } else {
                payment = paymentRepository.save(payment);
            }

            order.setStatus(Order.OrderStatus.PAID);
            orderRepository.save(order);

            log.info("Payment confirmation successful: paymentKey={}, orderId={}",
                    request.getPaymentKey(), request.getOrderId());

            return PaymentConfirmResponse.builder()
                    .success(true)
                    .message("Payment confirmed successfully")
                    .paymentKey(payment.getPaymentKey())
                    .orderId(payment.getOrderId())
                    .orderName(payment.getOrderName())
                    .status(payment.getStatus().name())
                    .method(payment.getMethod())
                    .totalAmount(BigDecimal.valueOf(payment.getAmount()))
                    .currency(payment.getCurrency())
                    .approvedAt(payment.getPaidAt())
                    .build();

        } catch (Exception e) {
            log.error("Payment confirmation failed for order: {}", request.getOrderId(), e);
            saveFailedPayment(request, e.getMessage());

            return PaymentConfirmResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .errorCode("PAYMENT_CONFIRM_FAILED")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Transactional
    public Payment getPaymentByKey(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentKey));

        if (payment.getStatus() == Payment.PaymentStatus.WAITING) {
            try {
                TossPaymentResponse latestInfo = tossClient.getPayment(paymentKey).block();
                if (latestInfo != null) {
                    updatePaymentFromTossResponse(payment, latestInfo);
                    payment = paymentRepository.save(payment);
                    log.info("Payment status updated from Toss: paymentKey={}, status={}",
                            paymentKey, payment.getStatus());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch latest payment status for {}: {}", paymentKey, e.getMessage());
            }
        }

        return payment;
    }

    public Payment cancelPayment(String paymentKey, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentKey));

        if (payment.getStatus() != Payment.PaymentStatus.DONE) {
            throw new PaymentException("Only completed payments can be canceled. Current status: " + payment.getStatus());
        }

        try {
            BigDecimal cancelAmount = request.getCancelAmount();
            if (cancelAmount == null) {
                cancelAmount = BigDecimal.valueOf(payment.getAmount());
            }

            TossPaymentResponse cancelResponse = tossClient.cancelPayment(
                    paymentKey,
                    request.getCancelReason(),
                    cancelAmount
            ).block();

            payment.setStatus(Payment.PaymentStatus.CANCELED);
            payment.setCancelReason(request.getCancelReason());
            payment.setCanceledAt(parseDateTime(cancelResponse.getCanceledAt()));

            paymentRepository.save(payment);

            Order order = orderRepository.findByOrderId(payment.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + payment.getOrderId()));
            order.setStatus(Order.OrderStatus.CANCELED);
            orderRepository.save(order);

            log.info("Payment cancellation successful: paymentKey={}, reason={}",
                    paymentKey, request.getCancelReason());

            return payment;

        } catch (Exception e) {
            log.error("Payment cancellation failed for {}: {}", paymentKey, e.getMessage());
            throw new PaymentException("Payment cancellation failed: " + e.getMessage());
        }
    }

    private void saveFailedPayment(PaymentConfirmRequest request, String failReason) {
        try {
            Payment failedPayment = Payment.builder()
                    .paymentKey(request.getPaymentKey())
                    .orderId(request.getOrderId())
                    .amount(Long.valueOf(request.getAmount()))
                    .status(Payment.PaymentStatus.FAILED)
                    .failReason(failReason)
                    .build();

            paymentRepository.save(failedPayment);
            log.info("Failed payment info saved: paymentKey={}, reason={}",
                    request.getPaymentKey(), failReason);

        } catch (Exception e) {
            log.error("Failed to save failed payment info: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public boolean isPayableOrder(String orderId) {
        Optional<Order> order = orderRepository.findByOrderId(orderId);
        if (order.isEmpty()) {
            return false;
        }

        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        return payment.isEmpty() || payment.get().getStatus() != Payment.PaymentStatus.DONE;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {
            }
        }

        log.warn("Failed to parse datetime: {}", dateTimeStr);
        return null;
    }

    private void updatePaymentFromTossResponse(Payment payment, TossPaymentResponse tossResponse) {
        try {
            payment.setStatus(Payment.PaymentStatus.valueOf(tossResponse.getStatus()));
            payment.setMethod(tossResponse.getMethod());
            payment.setCurrency(tossResponse.getCurrency());

            if (tossResponse.getApprovedAt() != null) {
                payment.setPaidAt(parseDateTime(tossResponse.getApprovedAt()));
            }
            if (tossResponse.getCanceledAt() != null) {
                payment.setCanceledAt(parseDateTime(tossResponse.getCanceledAt()));
            }
            if (tossResponse.getCustomerName() != null) {
                payment.setCustomerName(tossResponse.getCustomerName());
            }
            if (tossResponse.getCustomerEmail() != null) {
                payment.setCustomerEmail(tossResponse.getCustomerEmail());
            }
        } catch (Exception e) {
            log.warn("Failed to update payment from Toss response: {}", e.getMessage());
        }
    }
}
