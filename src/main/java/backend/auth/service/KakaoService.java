package backend.auth.service;

import backend.auth.dto.KakaoTokenResponse;
import backend.auth.dto.KakaoUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.annotation.PostConstruct;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class KakaoService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-url}")
    private String redirectUrl;

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String AUTH_URL = "https://kauth.kakao.com/oauth/authorize";

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        log.info("KakaoService 초기화 완료 - clientId: {}", clientId);
    }

    public String getAuthUrl() {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
            String authUrl = AUTH_URL +
                    "?client_id=" + clientId +
                    "&redirect_uri=" + encodedRedirectUri +
                    "&response_type=code" +
                    "&scope=profile_nickname,profile_image";

            log.info("카카오 인증 URL 생성: {}", authUrl);
            return authUrl;
        } catch (Exception e) {
            log.error("URL 인코딩 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 인증 URL 생성 실패", e);
        }
    }

    public String getAccessToken(String code) {
        log.info("카카오 액세스 토큰 요청: {}", code);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUrl);
        params.add("code", code);

        try {
            KakaoTokenResponse response = webClient.post()
                    .uri(TOKEN_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();

            String accessToken = response != null ? response.getAccessToken() : null;
            log.info("카카오 액세스 토큰 수신: {}", accessToken != null ? "성공" : "실패");

            return accessToken;
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 요청 실패: {}", e.getMessage());
            return null;
        }
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 요청");

        try {
            KakaoUserInfo userInfo = webClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block();

            if (userInfo != null) {
                log.info("카카오 사용자 정보 수신: ID={}, 닉네임={}",
                        userInfo.getId(), userInfo.getNickname());
            }

            return userInfo;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 실패: {}", e.getMessage());
            return null;
        }
    }

    public KakaoUserInfo loginWithCode(String code) {
        log.info("카카오 로그인 처리 시작: {}", code);

        String accessToken = getAccessToken(code);
        if (accessToken == null) {
            throw new RuntimeException("액세스 토큰을 받을 수 없습니다.");
        }

        return getUserInfo(accessToken);
    }
}
