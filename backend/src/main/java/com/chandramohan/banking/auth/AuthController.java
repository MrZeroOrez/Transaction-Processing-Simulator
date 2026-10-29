package com.chandramohan.banking.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  public record Credentials(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record AuthResponse(String accessToken, String tokenType, int expiresIn, String userId) {}

  public record Me(String userId, String role) {}

  private final LabStore store;
  private final PasswordEncoder passwords;
  private final TokenService tokens;
  private final String dummyHash;

  public AuthController(LabStore store, PasswordEncoder passwords, TokenService tokens) {
    this.store = store;
    this.passwords = passwords;
    this.tokens = tokens;
    this.dummyHash = passwords.encode("unmatchable-dummy-password");
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody Credentials body) {
    checkBytes(body.password());
    var user =
        store
            .register(body.email(), passwords.encode(body.password()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered"));
    return response(user);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody Credentials body) {
    checkBytes(body.password());
    var user = store.find(body.email());
    boolean matches =
        passwords.matches(body.password(), user.map(LabStore.User::passwordHash).orElse(dummyHash));
    if (user.isEmpty() || !matches)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    return response(user.get());
  }

  @GetMapping("/me")
  public Me me(@AuthenticationPrincipal Jwt jwt) {
    return new Me(jwt.getSubject(), "CUSTOMER");
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@AuthenticationPrincipal Jwt jwt) {
    tokens.revoke(jwt);
  }

  private AuthResponse response(LabStore.User user) {
    return new AuthResponse(tokens.issue(user), "Bearer", 900, user.id().toString());
  }

  private static void checkBytes(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
  }
}
