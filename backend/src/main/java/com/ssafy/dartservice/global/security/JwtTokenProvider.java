package com.ssafy.dartservice.global.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dartservice.user.User;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.access-token-validity-ms}")
	private long accessTokenValidityMs;

	private byte[] secretKey;

	public JwtTokenProvider(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void initialize() {
		this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusMillis(accessTokenValidityMs);

		Map<String, Object> header = new LinkedHashMap<>();
		header.put("alg", "HS256");
		header.put("typ", "JWT");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("sub", user.getEmail());
		payload.put("userId", user.getId());
		payload.put("role", user.getRole().name());
		payload.put("iat", now.getEpochSecond());
		payload.put("exp", expiresAt.getEpochSecond());

		String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
		return unsignedToken + "." + sign(unsignedToken);
	}

	public boolean validateToken(String token) {
		try {
			String[] parts = splitToken(token);
			String unsignedToken = parts[0] + "." + parts[1];
			if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
				return false;
			}

			Map<String, Object> claims = parsePayload(parts[1]);
			long expiresAt = ((Number) claims.get("exp")).longValue();
			return Instant.now().getEpochSecond() < expiresAt;
		} catch (Exception exception) {
			return false;
		}
	}

	public String getSubject(String token) {
		try {
			String[] parts = splitToken(token);
			Map<String, Object> claims = parsePayload(parts[1]);
			return (String) claims.get("sub");
		} catch (Exception exception) {
			return null;
		}
	}

	public long getAccessTokenValiditySeconds() {
		return accessTokenValidityMs / 1000;
	}

	private String encodeJson(Map<String, Object> value) {
		try {
			return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to encode JWT JSON", exception);
		}
	}

	private Map<String, Object> parsePayload(String encodedPayload) throws Exception {
		byte[] payloadBytes = BASE64_URL_DECODER.decode(encodedPayload);
		return objectMapper.readValue(payloadBytes, new TypeReference<>() {
		});
	}

	private String sign(String unsignedToken) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to sign JWT", exception);
		}
	}

	private String[] splitToken(String token) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid JWT format");
		}
		return parts;
	}

	private boolean constantTimeEquals(String expected, String actual) {
		return MessageDigestUtil.equals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
	}
}
