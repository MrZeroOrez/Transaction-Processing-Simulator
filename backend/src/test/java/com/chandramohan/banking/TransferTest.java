package com.chandramohan.banking;

import static org.junit.jupiter.api.Assertions.*;

import com.chandramohan.banking.domain.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferTest extends HttpSupport {
  @Autowired TransferService s;

  @Test
  void transferCreatesBalancedLedger() {
    String u = owner(), a = s.create(u, "A").id(), b = s.create(u, "B").id();
    s.transfer(u, a, b, new BigDecimal("123.45"), "demo", "transfer-01");
    var ac = s.accounts(u);
    eq("9876.55", ac.stream().filter(x -> x.id().equals(a)).findFirst().orElseThrow().balance());
    eq("10123.45", ac.stream().filter(x -> x.id().equals(b)).findFirst().orElseThrow().balance());
    assertTrue(s.reconcile(u, a).matches());
    assertTrue(s.reconcile(u, b).transferPairsBalanced());
    assertEquals(2, s.ledger(u, a).size());
    assertEquals(3, s.audit(u).size());
  }

  @Test
  void duplicateRequestDoesNotTransferTwice() {
    String u = owner(), a = s.create(u, "A").id(), b = s.create(u, "B").id();
    var one = s.transfer(u, a, b, BigDecimal.TEN, "", "duplicate-01");
    var two = s.transfer(u, a, b, BigDecimal.TEN, "", "duplicate-01");
    assertEquals(one.id(), two.id());
    assertEquals(1, s.history(u).size());
    assertThrows(Exception.class, () -> s.transfer(u, a, b, BigDecimal.ONE, "", "duplicate-01"));
  }

  @Test
  void insufficientFundsAndInvalidAmountsDoNotMutate() {
    String u = owner(), a = s.create(u, "A").id(), b = s.create(u, "B").id();
    for (String amount : List.of("-1", "0", "0.001", "10001"))
      assertThrows(
          Exception.class,
          () -> s.transfer(u, a, b, new BigDecimal(amount), "", "invalid-key-" + amount));
    assertTrue(s.history(u).isEmpty());
    eq("10000", s.reconcile(u, a).balance());
    assertThrows(Exception.class, () -> s.transfer(u, a, a, BigDecimal.ONE, "", "self-key-01"));
  }

  @Test
  void crossUserTransfersAllowedButDebitRequiresOwnership() {
    String u = owner(), v = owner(), a = s.create(u, "A").id(), b = s.create(v, "B").id();
    s.transfer(u, a, b, BigDecimal.TEN, "", "cross-key-1");
    assertEquals(1, s.history(v).size());
    assertThrows(Exception.class, () -> s.transfer(v, a, b, BigDecimal.ONE, "", "stolen-key-1"));
    assertThrows(Exception.class, () -> s.ledger(v, a));
    assertTrue(s.reconcile(v, b).matches());
  }

  @Test
  void concurrentTransfersPreserveFunds() throws Exception {
    String u = owner(), a = s.create(u, "A").id(), b = s.create(u, "B").id();
    var pool = Executors.newFixedThreadPool(5);
    try {
      var tasks = new ArrayList<Callable<Boolean>>();
      for (int i = 0; i < 15; i++) {
        String key = "concurrent-" + i;
        tasks.add(
            () -> {
              try {
                s.transfer(u, a, b, new BigDecimal("1000"), "", key);
                return true;
              } catch (Exception e) {
                return false;
              }
            });
      }
      int ok = 0;
      for (var f : pool.invokeAll(tasks)) if (f.get()) ok++;
      assertEquals(10, ok);
      eq("0", s.reconcile(u, a).balance());
      eq("20000", s.reconcile(u, b).balance());
      assertTrue(s.reconcile(u, a).transferPairsBalanced());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void oppositeDirectionTransfersAvoidDeadlock() throws Exception {
    String u = owner(), v = owner(), a = s.create(u, "A").id(), b = s.create(v, "B").id();
    var pool = Executors.newFixedThreadPool(2);
    try {
      var fs =
          pool.invokeAll(
              List.<Callable<Boolean>>of(
                  () -> {
                    s.transfer(u, a, b, BigDecimal.TEN, "", "opposite-01");
                    return true;
                  },
                  () -> {
                    s.transfer(v, b, a, BigDecimal.TEN, "", "opposite-02");
                    return true;
                  }));
      for (var f : fs) assertTrue(f.get(10, TimeUnit.SECONDS));
      eq("10000", s.reconcile(u, a).balance());
      eq("10000", s.reconcile(v, b).balance());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void lateDatabaseFailureRollsBackBothBalances() {
    String u = owner(), a = s.create(u, "A").id(), b = s.create(u, "B").id();
    db.execute("ALTER TABLE transfers ADD CONSTRAINT reject_test_note CHECK(note<>'force-fail')");
    try {
      assertThrows(
          Exception.class, () -> s.transfer(u, a, b, BigDecimal.TEN, "force-fail", "rollback-01"));
      eq("10000", s.reconcile(u, a).balance());
      eq("10000", s.reconcile(u, b).balance());
      assertTrue(s.history(u).isEmpty());
      assertEquals(1, s.ledger(u, a).size());
    } finally {
      db.execute("ALTER TABLE transfers DROP CONSTRAINT reject_test_note");
    }
  }

  @Test
  void apiFlowOwnershipAndCsv() throws Exception {
    String a = register(email()), b = register(email());
    var ca = call("POST", "/accounts", "{\"name\":\"A\"}", a, null);
    var cb = call("POST", "/accounts", "{\"name\":\"B\"}", a, null);
    assertEquals(201, ca.statusCode());
    String id = field(ca.body(), "id"), to = field(cb.body(), "id");
    assertEquals(404, call("GET", "/accounts/" + id + "/ledger", null, b, null).statusCode());
    var t =
        call(
            "POST",
            "/transfers",
            "{\"sourceId\":\""
                + id
                + "\",\"targetId\":\""
                + to
                + "\",\"amount\":12.34,\"note\":\"demo\"}",
            a,
            "http-transfer-01");
    assertEquals(201, t.statusCode(), t.body());
    assertTrue(call("GET", "/transfers/export", null, a, null).body().contains("12.34"));
    assertTrue(
        call("GET", "/accounts/" + id + "/reconciliation", null, a, null)
            .body()
            .contains("\"matches\":true"));
  }
}
