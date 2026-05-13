package com.oinkvalley.board_svc.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HS256 서명 검증과 페이로드(숫자 {@code sub}, 선택적 {@code roles},{@code exp}) 문자열 파싱.
 * 외부 JWT 라이브러리 없이 최소 구현입니다.
 */
@Component
public class JwtUtil {

    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");
    /** {@code "roles":["USER","ADMIN"]} 또는 {@code "roles":[]} — 없으면 빈 목록 */
    private static final Pattern ROLES_ARRAY_PATTERN = Pattern.compile("\"roles\"\\s*:\\s*\\[([^\\]]*)]");
    private static final Pattern JSON_STRING_TOKEN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private final byte[] secret;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes (UTF-8)");
        }
        this.secret = bytes;
    }

    /**
     * HS256 JWT를 검증한 뒤 페이로드에서 {@code sub}(사용자 ID), {@code roles}(선택), {@code exp}(선택)를 읽습니다.
     * {@code roles}는 문자열 배열입니다. 스프링 {@code hasRole("USER")}와 맞추려면 JWT에는 {@code USER}처럼 {@code ROLE_} 접두사 없이 넣어도 됩니다(필터에서 접두사를 붙임).
     */
    public ParsedJwt parseToken(String token) {
        String json = verifiedPayloadJson(token);
        Matcher subMatcher = SUB_PATTERN.matcher(json);
        if (!subMatcher.find()) {
            throw new IllegalArgumentException("JWT missing numeric sub claim");
        }
        Matcher expMatcher = EXP_PATTERN.matcher(json);
        if (expMatcher.find()) {
            long exp = Long.parseLong(expMatcher.group(1));
            if (Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException("JWT expired");
            }
        }
        long userId = Long.parseLong(subMatcher.group(1));
        return new ParsedJwt(userId, parseRoles(json));
    }

    private String verifiedPayloadJson(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed JWT");
        }
        byte[] expectedSig = sign(parts[0] + "." + parts[1]);
        byte[] actualSig = base64UrlDecode(parts[2]);
        if (!MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }
        return new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
    }

    private static List<String> parseRoles(String json) {
        Matcher arrayMatcher = ROLES_ARRAY_PATTERN.matcher(json);
        if (!arrayMatcher.find()) {
            return List.of();
        }
        String inner = arrayMatcher.group(1).trim();
        if (inner.isEmpty()) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        Matcher stringMatcher = JSON_STRING_TOKEN.matcher(inner);
        while (stringMatcher.find()) {
            roles.add(unescapeJsonString(stringMatcher.group(1)));
        }
        return List.copyOf(roles);
    }

    private static String unescapeJsonString(String s) {
        return s.replace("\\\\", "\\").replace("\\\"", "\"");
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign JWT", e);
        }
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public record ParsedJwt(long userId, List<String> roles) {}
}
