package backend.crawler.kca.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ParseUtils {
    private ParseUtils() {}

    private static final Pattern PIPE_SPLIT = Pattern.compile("\\|");                // 좌/우 분리
    private static final Pattern DELIMS = Pattern.compile("[,;/·ㆍ，、\\s]+");        // 다양한 구분자
    private static final Pattern ELLIPSIS = Pattern.compile("[.…]+$");               // 끝의 ... 제거
    private static final Pattern EXTRA = Pattern.compile("[\\p{Z}\\u00A0]+");        // 특수공백

    /** "A,B... | C/D/E" -> "C/D/E" 로 정규화 */
    public static String normalizeSpecialty(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return null;

        // 1) 파이프 우측만 사용 (파이프가 없으면 전체를 사용)
        String right = raw;
        String[] parts = PIPE_SPLIT.split(raw, -1);
        if (parts.length >= 2) {
            right = parts[parts.length - 1]; // 마지막 파이프 오른쪽
        }

        // 2) 말줄임표/공백 정리
        right = ELLIPSIS.matcher(right).replaceAll("");
        right = EXTRA.matcher(right).replaceAll(" ").trim();

        if (right.isEmpty()) return null;

        // 3) 구분자 정규화 + 토큰 클린 + 중복 제거(등장 순서 유지)
        LinkedHashSet<String> uniq = Arrays.stream(DELIMS.split(right))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ParseUtils::cleanupToken)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniq.isEmpty()) return null;

        // 4) 최종 표기는 "/"로 연결
        return String.join("/", uniq);
    }

    private static String cleanupToken(String s) {
        // 앞뒤 불필요 문자 제거(괄호/따옴표 등), 전각-반각 공백 정리
        s = s.replace('\u00A0', ' ').trim();
        // 필요 시 표준화 룰 추가 가능
        return s;
    }
}
