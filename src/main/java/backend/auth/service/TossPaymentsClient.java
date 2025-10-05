package backend.auth.service;

import backend.auth.config.TossPaymentsConfig;
import backend.auth.dto.TossPaymentResponse;
import backend.auth.exception.PaymentException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsClient {

    private final TossPaymentsConfig config;
    private final WebClient webClient;

    @PostConstruct
    public void init() {
        if (config.getSecretKey() == null || config.getSecretKey().isEmpty()) {
            throw new IllegalStateException("Toss Payments secret key is not configured");
        }
    }

    private String getAuthorizationHeader() {
        String credentials = config.getSecretKey() + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    public Mono<TossPaymentResponse> confirmPayment(String paymentKey, String orderId, Long amount) {

        Map<String, Object> requestBody = Map.of(
                "orderId", orderId,
                "amount", amount
        );

        return webClient.post()
                .uri(config.getBaseUrl() + "/payments/" + paymentKey)
                .header("Authorization", getAuthorizationHeader())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Toss API Error: {}", errorBody);
                                return Mono.error(new PaymentException("Payment confirmation failed: " + errorBody));
                            });
                })
                .bodyToMono(TossPaymentResponse.class);
    }

    public Mono<TossPaymentResponse> getPayment(String paymentKey) {
        return webClient.get()
                .uri(config.getBaseUrl() + "/payments/" + paymentKey)
                .header("Authorization", getAuthorizationHeader())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Toss API Error: {}", errorBody);
                                return Mono.error(new PaymentException("Payment inquiry failed: " + errorBody));
                            });
                })
                .bodyToMono(TossPaymentResponse.class);
    }

    public Mono<TossPaymentResponse> cancelPayment(String paymentKey, String cancelReason, BigDecimal cancelAmount) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cancelReason", cancelReason);
        if (cancelAmount != null) {
            requestBody.put("cancelAmount", cancelAmount);
        }

        return webClient.post()
                .uri(config.getBaseUrl() + "/payments/" + paymentKey + "/cancel")
                .header("Authorization", getAuthorizationHeader())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Toss API Error: {}", errorBody);
                                return Mono.error(new PaymentException("Payment cancellation failed: " + errorBody));
                            });
                })
                .bodyToMono(TossPaymentResponse.class);
    }
}