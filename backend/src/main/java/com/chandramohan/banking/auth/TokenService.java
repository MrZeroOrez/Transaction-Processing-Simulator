package com.chandramohan.banking.auth;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  public static final String ISSUER = "banking-investment-lab";
  public static final String AUDIENCE = "banking-api";
  private final JwtEncoder encoder;
  private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

  public TokenService(JwtEncoder encoder) {
    this.encoder = encoder;
  }

  public String issue(LabStore.User user) {
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(user.id().toString())
            .audience(List.of(AUDIENCE))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .id(UUID.randomUUID().toString())
            .claim("roles", List.of("CUSTOMER"))
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  public void revoke(Jwt token) {
    prune();
    revoked.put(token.getId(), token.getExpiresAt());
  }

  public boolean isRevoked(String id) {
    prune();
    return id != null && revoked.containsKey(id);
  }

  private void prune() {
    var now = Instant.now();
    revoked.entrySet().removeIf(e -> e.getValue().isBefore(now));
  }
}
