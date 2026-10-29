package com.chandramohan.banking.auth;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.*;
import java.security.interfaces.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Ephemeral RSA key is for this local demo only. Restart invalidates all tokens.
  @Bean
  KeyPair signingKeys() throws NoSuchAlgorithmException {
    var g = KeyPairGenerator.getInstance("RSA");
    g.initialize(2048);
    return g.generateKeyPair();
  }

  @Bean
  JwtEncoder jwtEncoder(KeyPair keys) {
    var key =
        new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) keys.getPublic())
            .privateKey((RSAPrivateKey) keys.getPrivate())
            .keyID("local-demo")
            .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  JwtDecoder jwtDecoder(KeyPair keys, TokenService tokens, LabStore store) {
    var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
    OAuth2TokenValidator<Jwt> appClaims =
        jwt -> {
          if (!jwt.getAudience().contains(TokenService.AUDIENCE)
              || jwt.getId() == null
              || !store.hasUser(jwt.getSubject())
              || tokens.isRevoked(jwt.getId()))
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token is not valid for this session", null));
          return OAuth2TokenValidatorResult.success();
        };
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(TokenService.ISSUER), appClaims));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // No cookies or browser-automatic credentials; only explicit Authorization: Bearer tokens.
        .csrf(csrf -> csrf.disable())
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())
        .logout(l -> l.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/api/v1/platform",
                        "/api/v1/demo/portfolio")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .hasRole("CUSTOMER")
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter()
                              .write("{\"error\":\"Authentication required or token invalid\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, e) -> {
                          res.setStatus(403);
                          res.setContentType("application/json");
                          res.getWriter().write("{\"error\":\"Access denied\"}");
                        }));
    return http.build();
  }
}
