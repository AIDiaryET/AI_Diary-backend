package backend.crawler.kca.util;

import backend.crawler.kca.dto.CounselorItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class UniqueKeyUtil {
    public static String of(CounselorItem it) {
        // idx가 있으면 그걸로 고정
        if (hasText(it.getSourceId())) {
            return sha256Hex("KCA|" + it.getSourceId().trim());
        }
        // fallback: licenseNo, name|email, name|gender
        if (hasText(it.getLicenseNo())) {
            return sha256Hex(it.getLicenseNo().trim());
        }
        if (hasText(it.getEmail())) {
            return sha256Hex(safe(it.getName()) + "|" + it.getEmail().trim());
        }
        return sha256Hex(safe(it.getName()) + "|" + safe(it.getGender()));
    }

    private static boolean hasText(String s){ return s != null && !s.trim().isEmpty(); }
    private static String safe(String s){ return s == null ? "" : s.trim(); }

    private static String sha256Hex(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(b.length*2);
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}


