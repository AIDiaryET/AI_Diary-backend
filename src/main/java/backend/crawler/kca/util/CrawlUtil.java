package backend.crawler.kca.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;

public class CrawlUtil {
    public static String sha256(String... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String joined = String.join("|", parts);
            byte[] hash = md.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String normText(String s) {
        if (s == null) return null;
        // EUC-KR 페이지에서도 안전하게 공백/제어문자 정리
        String t = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+"," ")
                .trim();
        return t.isEmpty()? null : t;
    }

    public static String joinDistinct(String base, String add, String sep) {
        if (add == null || add.isBlank()) return base;
        if (base == null || base.isBlank()) return add;
        if (base.contains(add)) return base;
        return base + sep + add;
    }


}
