package backend.crawler.kca.spec;

import backend.crawler.kca.entity.CounselorEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.stream.Collectors;

public final class CounselorSpecs {
    private CounselorSpecs() {}

    private static List<String> cleanTokens(Collection<String> tokens) {
        if (tokens == null) return List.of();
        return tokens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private static String normTokenTight(String t) {
        return t.toLowerCase()
                .replace(" ", "")
                .replace("|", "")
                .replace(";", "")
                .replace(":", "")
                .replace("-", "")
                .replace("·", "")
                .replace("ㆍ", "")
                .replace(",", "");
    }

    /** regions: 기호/공백 제거 후 부분일치 OR */
    public static Specification<CounselorEntity> regionAny(Collection<String> raw) {
        var tokens = cleanTokens(raw);
        if (tokens.isEmpty()) return null;

        return Specification.anyOf(
                tokens.stream().map(tok -> (Specification<CounselorEntity>) (root, q, cb) -> {
                    var col = cb.lower(root.get("regions"));
                    var noSpace = cb.function("REPLACE", String.class, col, cb.literal(" "), cb.literal(""));
                    var noPipe  = cb.function("REPLACE", String.class, noSpace, cb.literal("|"), cb.literal(""));
                    var noSemi  = cb.function("REPLACE", String.class, noPipe,  cb.literal(";"), cb.literal(""));
                    var noColon = cb.function("REPLACE", String.class, noSemi,  cb.literal(":"), cb.literal(""));
                    var noHyphen= cb.function("REPLACE", String.class, noColon, cb.literal("-"), cb.literal(""));
                    var noDot1  = cb.function("REPLACE", String.class, noHyphen, cb.literal("·"), cb.literal(""));
                    var noDot2  = cb.function("REPLACE", String.class, noDot1,   cb.literal("ㆍ"), cb.literal(""));
                    var noComma = cb.function("REPLACE", String.class, noDot2,   cb.literal(","), cb.literal(""));
                    return cb.like(noComma, "%" + normTokenTight(tok) + "%");
                }).toList()
        );
    }

    /** specialty: '/' 경계 매칭 OR */
    public static Specification<CounselorEntity> specialtyAny(Collection<String> raw) {
        var tokens = cleanTokens(raw);
        if (tokens.isEmpty()) return null;

        return Specification.anyOf(
                tokens.stream().map(tok -> (Specification<CounselorEntity>) (root, q, cb) -> {
                    var lowered = cb.lower(root.get("specialty"));
                    var wrapped = cb.concat(cb.literal("/"), cb.concat(lowered, cb.literal("/")));
                    return cb.like(wrapped, "%/" + tok.toLowerCase().trim() + "/%");
                }).toList()
        );
    }

    /** targets: 다양한 구분자(|,/,·,ㆍ,;,,)를 '/'로 정규화 후 경계 매칭 OR */
    public static Specification<CounselorEntity> targetsAny(Collection<String> raw) {
        var tokens = cleanTokens(raw);
        if (tokens.isEmpty()) return null;

        return Specification.anyOf(
                tokens.stream().map(tok -> (Specification<CounselorEntity>) (root, q, cb) -> {
                    var colLower = cb.lower(root.get("targets"));
                    // REPLACE 체인으로 구분자를 '/'로 통일
                    var r1 = cb.function("REPLACE", String.class, colLower, cb.literal("|"), cb.literal("/"));
                    var r2 = cb.function("REPLACE", String.class, r1,       cb.literal("·"), cb.literal("/"));
                    var r3 = cb.function("REPLACE", String.class, r2,       cb.literal("ㆍ"), cb.literal("/"));
                    var r4 = cb.function("REPLACE", String.class, r3,       cb.literal(";"), cb.literal("/"));
                    var r5 = cb.function("REPLACE", String.class, r4,       cb.literal(","), cb.literal("/"));
                    var r6 = cb.function("REPLACE", String.class, r5,       cb.literal(" "), cb.literal("")); // 토큰 내부 공백 제거
                    // 양쪽 경계 추가
                    var wrapped = cb.concat(cb.literal("/"), cb.concat(r6, cb.literal("/")));
                    return cb.like(wrapped, "%/" + tok.toLowerCase().trim().replace(" ", "") + "/%");
                }).toList()
        );
    }
}
