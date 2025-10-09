package backend.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, String>> handlePaymentException(PaymentException e) {
        log.error("Payment error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "PAYMENT_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePaymentNotFoundException(PaymentNotFoundException e) {
        log.error("Payment not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "PAYMENT_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFoundException(OrderNotFoundException e) {
        log.error("Order not found: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "ORDER_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(Map.of("error", "VALIDATION_ERROR", "message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_ERROR", "message", "An unexpected error occurred"));
    }
}
