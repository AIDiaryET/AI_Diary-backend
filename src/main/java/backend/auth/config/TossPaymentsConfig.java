package backend.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "toss.payments")
@Data
public class TossPaymentsConfig {
    private String clientKey;
    private String secretKey;
    private String apiUrl;
    private String successUrl;
    private String failUrl;

    public String getBaseUrl() {
        return apiUrl;
    }
}