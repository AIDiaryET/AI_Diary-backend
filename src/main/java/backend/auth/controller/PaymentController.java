package backend.auth.controller;

import backend.auth.dto.PaymentConfirmRequest;
import backend.auth.dto.PaymentConfirmResponse;
import backend.auth.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // ⭐ 누락된 REST API 엔드포인트 추가
    @PostMapping("/api/payments/confirm")
    @ResponseBody
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @RequestBody PaymentConfirmRequest request) {
        try {
            log.info("결제 승인 API 호출 - paymentKey: {}, orderId: {}, amount: {}",
                    request.getPaymentKey(), request.getOrderId(), request.getAmount());

            PaymentConfirmResponse response = paymentService.confirmPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("결제 승인 중 오류 발생", e);
            PaymentConfirmResponse errorResponse = new PaymentConfirmResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("결제 승인 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ⭐ 결제 상태 조회 API 추가 (프론트엔드에서 호출할 수 있음)
    @GetMapping("/api/payments/status/{paymentKey}")
    @ResponseBody
    public ResponseEntity<PaymentConfirmResponse> getPaymentStatus(@PathVariable String paymentKey) {
        try {
            log.info("결제 상태 조회 API 호출 - paymentKey: {}", paymentKey);

            // PaymentService에서 결제 정보 조회하는 메서드가 있다면 사용
            // 없다면 간단한 응답 반환
            PaymentConfirmResponse response = new PaymentConfirmResponse();
            response.setSuccess(true);
            response.setPaymentKey(paymentKey);
            response.setMessage("결제 상태 조회 성공");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("결제 상태 조회 중 오류 발생", e);
            PaymentConfirmResponse errorResponse = new PaymentConfirmResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("결제 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 결제 성공 페이지 (토스페이먼츠에서 리다이렉트)
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam String paymentKey,
                                 @RequestParam String orderId,
                                 @RequestParam Long amount,
                                 Model model) {
        log.info("결제 성공 페이지 접근 - paymentKey: {}, orderId: {}, amount: {}", paymentKey, orderId, amount);

        try {
            // PaymentConfirmRequest 객체 생성
            PaymentConfirmRequest confirmRequest = new PaymentConfirmRequest();
            confirmRequest.setPaymentKey(paymentKey);
            confirmRequest.setOrderId(orderId);
            confirmRequest.setAmount(Integer.valueOf(amount.toString()));

            // 기존 PaymentService의 confirmPayment 메소드 호출
            PaymentConfirmResponse confirmResponse = paymentService.confirmPayment(confirmRequest);

            if (confirmResponse.isSuccess()) {
                // 성공 시 결제 정보를 모델에 추가
                model.addAttribute("paymentKey", confirmResponse.getPaymentKey());
                model.addAttribute("orderId", confirmResponse.getOrderId());
                model.addAttribute("amount", confirmResponse.getTotalAmount());

                // paymentData 객체로 묶어서 전달 (기존 HTML 템플릿에 맞춤)
                PaymentData paymentData = new PaymentData();
                paymentData.setMethod(confirmResponse.getMethod());
                paymentData.setPaidAt(String.valueOf(confirmResponse.getApprovedAt()));

                model.addAttribute("paymentData", paymentData);

                log.info("결제 승인 완료 - orderId: {}, paymentKey: {}", orderId, paymentKey);
            } else {
                // 승인 실패 시에도 기본 정보는 전달
                model.addAttribute("paymentKey", paymentKey);
                model.addAttribute("orderId", orderId);
                model.addAttribute("amount", amount);
                model.addAttribute("paymentData", null);

                log.error("결제 승인 실패 - orderId: {}, error: {}", orderId, confirmResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("결제 승인 처리 중 예외 발생 - orderId: {}, paymentKey: {}", orderId, paymentKey, e);

            // 예외 발생 시에도 기본 정보는 전달
            model.addAttribute("paymentKey", paymentKey);
            model.addAttribute("orderId", orderId);
            model.addAttribute("amount", amount);
            model.addAttribute("paymentData", null);
        }

        return "payment-success";
    }

    // 결제 실패 페이지
    @GetMapping("/payment/fail")
    public String paymentFail(@RequestParam String code,
                              @RequestParam String message,
                              @RequestParam(required = false) String orderId,
                              Model model) {
        log.warn("결제 실패 페이지 접근 - code: {}, message: {}, orderId: {}", code, message, orderId);

        model.addAttribute("errorCode", code);
        model.addAttribute("errorMessage", message);
        model.addAttribute("orderId", orderId);

        return "payment-fail";
    }

    // 테스트 결제 페이지
    @GetMapping("/payment/test")
    public String testPaymentPage() {
        return "test-payment";
    }

    // 홈페이지
//    @GetMapping("/")
//    public String home() {
//        return "redirect:/payment/test";
//    }

    // PaymentData 내부 클래스 (기존 HTML 템플릿의 paymentData 객체용)
    public static class PaymentData {
        private String method;
        private String paidAt;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPaidAt() {
            return paidAt;
        }

        public void setPaidAt(String paidAt) {
            this.paidAt = paidAt;
        }
    }
}