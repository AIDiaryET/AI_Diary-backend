package backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfo {
    private Long id;
    private Map<String, Object> properties;

    @JsonProperty("kakao_account")
    private Map<String, Object> kakaoAccount;

    public String getNickname() {
        return properties != null ? (String) properties.get("nickname") : null;
    }

    public String getProfileImage() {
        return properties != null ? (String) properties.get("profile_image") : null;
    }
}

