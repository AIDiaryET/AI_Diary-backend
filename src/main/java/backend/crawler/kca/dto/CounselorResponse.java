// backend/crawler/kca/dto/CounselorResponse.java
package backend.crawler.kca.dto;

import backend.crawler.kca.entity.CounselorEntity;
import lombok.*;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CounselorResponse {
    private String source;

    private String name;
    private String gender;
    private String licenseType;
    private String email;
    private String targets;
    private String specialty;
    private String regions;
    private String fee;

    public static CounselorResponse from(CounselorEntity e){
        return CounselorResponse.builder()
                .source(e.getSource())
                .name(e.getName())
                .gender(e.getGender())
                .licenseType(e.getLicenseType())
                .email(e.getEmail())
                .targets(e.getTargets())
                .specialty(e.getSpecialty())
                .regions(e.getRegions())
                .fee(e.getFee())
                .build();
    }
}
