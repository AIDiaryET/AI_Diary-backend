package backend.auth.entity;

import backend.auth.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_nickname", columnList = "nickname"),
        @Index(name = "idx_kakao_id", columnList = "kakao_id")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "kakao_id", unique = true)
    private String kakaoId;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "nickname", unique = true, length = 50)
    private String nickname;

    @Column(name = "name")
    private String name; // 실명

    @Column(name = "phone")
    private String phone; // 전화번호

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "address")
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    public enum Role {
        USER, ADMIN
    }


    public String getNickName() {
        return this.nickname;
    }

    public void setNickName(String nickName) {
        this.nickname = nickName;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isKakaoUser() {
        return kakaoId != null && !kakaoId.isEmpty();
    }

    public boolean isEmailUser() {
        return email != null && !email.isEmpty() && password != null;
    }


    public void updateProfile(String name, String nickname, String phone) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateAddress(String zipCode, String address, String detailAddress) {
        this.zipCode = zipCode;
        this.address = address;
        this.detailAddress = detailAddress;
    }

    public String getDisplayName() {
        return name != null ? name : nickname;
    }

    public boolean hasAddress() {
        return zipCode != null && address != null;
    }
}