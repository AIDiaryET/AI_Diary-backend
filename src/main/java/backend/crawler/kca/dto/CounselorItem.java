package backend.crawler.kca.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CounselorItem {
    private String source;     // 항상 "KCA"
    private String sourceId;   // idx 값
    private String detailUrl;  // 절대 URL

    private String name;
    private String gender;
    private String licenseNo;
    private String licenseType;
    private String email;
    private String targets;
    private String specialty;
    private String regions;
    private String fee;
}


