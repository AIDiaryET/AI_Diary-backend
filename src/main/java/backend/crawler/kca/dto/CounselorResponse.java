// backend/crawler/kca/dto/CounselorResponse.java
package backend.crawler.kca.dto;

import backend.crawler.kca.entity.CounselorEntity;
import lombok.*;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CounselorResponse {
    private Long id;
    private String uniqueKey;
    private String source;
    private String sourceId;
    private String detailUrl;

    private String name;
    private String gender;
    private String licenseNo;
    private String licenseType;
    private String email;
    private String targets;
    private String specialty;
    private String regions;
    private String fee;

    public static CounselorResponse from(CounselorEntity e){
        return CounselorResponse.builder()
                .id(e.getId())
                .uniqueKey(e.getUniqueKey())
                .source(e.getSource())
                .sourceId(e.getSourceId())
                .detailUrl(e.getDetailUrl())
                .name(e.getName())
                .gender(e.getGender())
                .licenseNo(e.getLicenseNo())
                .licenseType(e.getLicenseType())
                .email(e.getEmail())
                .targets(e.getTargets())
                .specialty(e.getSpecialty())
                .regions(e.getRegions())
                .fee(e.getFee())
                .build();
    }
}
