# Learn while running the project

## Day 1: trace one complete request

Start the app, register, and create a resource. Open browser DevTools -> Network. Find the POST request, its JSON, response code and Bearer header. Read `frontend/src/main.jsx`, `auth/AuthController.java`, `auth/LabStore.java`, and `schema.sql` in that order. Identify where a password becomes a BCrypt hash. Explain why the database must enforce unique emails even though the UI validates them.

## Day 2: security and ownership

Read `SecurityConfiguration.java` and `TokenService.java`. Explain signature verification, issuer, audience, expiry and role checks. Use two different users. Prove one cannot read or debit another's private resource. Learn why CSRF is disabled for explicit bearer headers without browser-automatic credential cookies. Add a test for a malformed resource ID.

## Day 3: transactions and invariants

Read the domain service, then its controller. A controller handles HTTP; the service owns business rules and transaction boundaries; JdbcTemplate executes parameterized SQL. Find `FOR UPDATE`, the mutation sequence, and the invariant that must remain true. Cause an invalid request and confirm balances/history stay unchanged.

## Day 4: retries and concurrency

Read the duplicate-request and concurrent-update tests. Explain the difference between a request key and a transaction ID. Repeat the same POST with the same key; change its body and observe the conflict. Explain why a Java synchronized block would not coordinate two application instances. Trace row-lock acquisition order.

## Day 5: SQL and debugging

Inspect schema tables, primary/foreign keys and indexes. Find every owner-scoped query. Add a query-level test. Follow an error response from React to controller advice. Inspect the Maven test report in `backend/target/surefire-reports`.

## Day 6: frontend behavior

Study React state, controlled inputs, forms, Promise.all and the API helper. Explain how data moves from API JSON to cards and tables. Session tokens are kept in memory. Identify the empty, loading and error states. Add one new accessible filter with a label.

## Day 7: deployable packaging and explanation

Run all tests and the production React build. Inspect the Dockerfiles, Compose volume and Nginx proxy; distinguish supplied configuration from an actually tested deployment. Give a five-minute demo, draw UI -> API -> DB, and discuss limitations. Extend one feature and write a meaningful test.

## Interview questions

1. Which operations must commit or roll back together, and why?
2. How does idempotency behave if the client loses the HTTP response after commit?
3. Which checks rely on token claims and which rely on database ownership?
4. What does the row lock protect? What still needs work for a public deployment?
5. Why use DECIMAL/BigDecimal rather than double for financial amounts?
6. How would you add Kafka with a transactional outbox without making money posting depend on broker availability?
7. Which tests prove failure behavior rather than only the happy path?

The app is a modular monolith using Spring JDBC, not microservices or JPA/Hibernate. Do not claim technologies or production deployment that have not been implemented and exercised.
