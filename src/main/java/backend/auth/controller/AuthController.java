package backend.auth.controller;

import backend.auth.entity.User;
import backend.auth.dto.KakaoUserInfo;
import backend.auth.repository.UserRepository;
import backend.auth.service.KakaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KakaoService kakaoService;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        return "redirect:/auth/kakao/login";
    }

    @GetMapping("/kakao/login")
    public String kakaoLogin(HttpSession session) {
        try {
            // 기존 세션 초기화
            session.invalidate();

            String authUrl = kakaoService.getAuthUrl();
            log.info("=== 카카오 로그인 시작 ===");
            log.info("생성된 카카오 URL: {}", authUrl);

            return "redirect:" + authUrl;
        } catch (Exception e) {
            log.error("카카오 로그인 URL 생성 실패", e);
            return "redirect:/auth/error?message=URL_생성_실패";
        }
    }

    @GetMapping("/kakao/callback")
    public String kakaoCallback(@RequestParam String code, HttpSession session) {
        try {
            log.info("=== 카카오 콜백 수신 ===");
            log.info("받은 코드: {}", code);

            KakaoUserInfo userInfo = kakaoService.loginWithCode(code);
            if (userInfo == null) {
                log.error("카카오 사용자 정보가 null입니다");
                return "redirect:/auth/error?message=사용자_정보_조회_실패";
            }

            log.info("카카오 사용자 정보 수신: ID={}, 닉네임={}",
                    userInfo.getId(), userInfo.getNickname());

            User user = userRepository.findByKakaoId(userInfo.getId().toString())
                    .orElseGet(() -> {
                        log.info("새 사용자 생성 중...");
                        return createNewUser(userInfo);
                    });

            // 로그인 시간 업데이트
            user.updateLastLoginAt();
            userRepository.save(user);

            session.setAttribute("user", user);
            log.info("=== 카카오 로그인 성공 ===");
            log.info("사용자: {}, ID: {}", user.getNickName(), user.getId());

            return "redirect:/auth/success";

        } catch (Exception e) {
            log.error("=== 카카오 로그인 실패 ===", e);
            return "redirect:/auth/error?message=로그인_처리_실패";
        }
    }

    @GetMapping("/success")
    @ResponseBody
    public String success(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return String.format("""
                <h1>카카오 로그인 성공!</h1>
                <h2>사용자 정보:</h2>
                <ul>
                    <li>사용자명: %s</li>
                    <li>사용자 ID: %d</li>
                    <li>카카오 ID: %s</li>
                    <li>프로필 이미지: <img src='%s' width='100' style='border-radius: 50px;'></li>
                    <li>가입일: %s</li>
                    <li>마지막 로그인: %s</li>
                </ul>
                <br>
                <a href="/auth/logout">로그아웃</a> | 
                <a href="/auth/kakao/login">다시 로그인</a>
                """,
                    user.getNickName(),
                    user.getId(),
                    user.getKakaoId(),
                    user.getProfileImage() != null ? user.getProfileImage() : "",
                    user.getCreatedAt(),
                    user.getLastLoginAt()
            );
        }
        return "<h1>로그인되지 않았습니다.</h1><a href='/auth/kakao/login'>카카오 로그인</a>";
    }

    @GetMapping("/error")
    @ResponseBody
    public String error(@RequestParam(required = false) String message) {
        String errorMessage = message != null ? message.replace("_", " ") : "알 수 없는 오류";
        return String.format("""
            <h1>카카오 로그인 실패</h1>
            <p>오류: %s</p>
            <a href="/auth/kakao/login">다시 시도</a>
            """, errorMessage);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        log.info("로그아웃 완료");
        return "redirect:/auth/login-page";
    }

    @GetMapping("/login-page")
    @ResponseBody
    public String loginPage() {
        return """
            <h1>카카오 로그인 데모</h1>
            <h2>로그인이 필요합니다</h2>
            <a href="/auth/kakao/login" style="
                display: inline-block;
                background: #FEE500;
                color: #000;
                padding: 15px 30px;
                text-decoration: none;
                border-radius: 5px;
                font-weight: bold;
            ">카카오 로그인</a>
            """;
    }

    private User createNewUser(KakaoUserInfo userInfo) {
        try {
            User newUser = User.builder()
                    .kakaoId(userInfo.getId().toString())
                    .nickname(userInfo.getNickname())
                    .profileImage(userInfo.getProfileImage())
                    .role(User.Role.USER)
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("새 사용자 저장 완료: ID={}, 닉네임={}", savedUser.getId(), savedUser.getNickName());

            return savedUser;
        } catch (Exception e) {
            log.error("사용자 생성 실패", e);
            throw new RuntimeException("사용자 생성 실패", e);
        }
    }
}
