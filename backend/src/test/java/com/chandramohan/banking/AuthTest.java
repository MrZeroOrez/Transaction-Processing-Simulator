package com.chandramohan.banking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthTest extends HttpSupport {
  @Autowired PasswordEncoder encoder;

  @Test
  void signupHashesPasswordAndLoginIgnoresEmailCase() throws Exception {
    String e = email(), t = register(e);
    assertTrue(encoder.matches("Test-password-123", users.find(e).orElseThrow().passwordHash()));
    assertEquals(
        200,
        call(
                "POST",
                "/auth/login",
                "{\"email\":\"" + e.toUpperCase() + "\",\"password\":\"Test-password-123\"}",
                null,
                null)
            .statusCode());
    assertEquals(200, call("GET", "/auth/me", null, t, null).statusCode());
  }

  @Test
  void duplicateRejected() throws Exception {
    String e = email();
    register(e);
    assertEquals(
        409,
        call(
                "POST",
                "/auth/register",
                "{\"email\":\"" + e + "\",\"password\":\"Test-password-123\"}",
                null,
                null)
            .statusCode());
  }

  @Test
  void unauthorizedCannotRead() throws Exception {
    assertEquals(401, call("GET", "/auth/me", null, null, null).statusCode());
  }

  @Test
  void logoutRevokesToken() throws Exception {
    String t = register(email());
    assertEquals(204, call("POST", "/auth/logout", null, t, null).statusCode());
    assertEquals(401, call("GET", "/auth/me", null, t, null).statusCode());
  }

  @Test
  void invalidCredentialsAreRejected() throws Exception {
    assertEquals(
        400,
        call("POST", "/auth/register", "{\"email\":\"bad\",\"password\":\"short\"}", null, null)
            .statusCode());
    assertEquals(
        401,
        call(
                "POST",
                "/auth/login",
                "{\"email\":\"missing@example.test\",\"password\":\"Test-password-123\"}",
                null,
                null)
            .statusCode());
  }

  @Test
  void modifiedSignatureRejected() throws Exception {
    String t = register(email());
    var a = t.split("\\.");
    a[2] = (a[2].charAt(0) == 'A' ? "B" : "A") + a[2].substring(1);
    assertEquals(401, call("GET", "/auth/me", null, String.join(".", a), null).statusCode());
  }
}
