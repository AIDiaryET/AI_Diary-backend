package backend.crawler.kca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "counselor_kca",
        indexes = {
                @Index(name="ux_counselor_source", columnList="source,sourceId", unique = true),
                @Index(name="ux_counselor_kca_unique_key", columnList="uniqueKey", unique = true)
        })
@Getter @Setter
public class CounselorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내부 하위호환/빠른 조회용 (sha256)
    @Column(nullable=false, length=64, unique = true)
    private String uniqueKey;

    @Column(nullable=false, length=16)
    private String source;            // "KCA"

    @Column(length=32)
    private String sourceId;          // idx

    @Column(columnDefinition="text")
    private String detailUrl;

    @Column(length=150) private String name;
    @Column(length=10)  private String gender;

    @Column(length=128) private String licenseNo;
    @Column(columnDefinition="text") private String licenseType;

    @Column(length=160) private String email;
    @Column(columnDefinition="text") private String targets;
    @Column(columnDefinition="text") private String specialty;
    @Column(columnDefinition="text") private String regions;
    @Column(length=64)  private String fee;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist void prePersist(){ createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void preUpdate(){  updatedAt = LocalDateTime.now(); }
}
