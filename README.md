# Transaction Processing Simulator

A complete local learning application: **React UI -> Spring Security/JWT -> Spring Boot REST API -> JDBC/SQL -> persistent H2 file**. All business operations go through the backend. There is no hosted service, paid account, API key, or external database required for local use.

## Start on Windows

1. Extract the ZIP fully. Use a writable folder, for example `C:\Projects\Java_Fullstack_Projects`.
2. Install a JDK (21 recommended; Java 17+ target), and Node.js 20.19+ (22 LTS recommended). Check `java -version`, `node --version`, `npm --version`. If `JAVA_HOME` is set, it must point to the JDK folder, not its `bin` directory.
3. Open this project folder and double-click **start.cmd**, or run `.\start.cmd` in PowerShell. Two terminal windows open. The first start needs internet to download Maven/npm dependencies.
4. Wait until the backend says Started and the frontend prints its URL. Open **http://127.0.0.1:5174**.
5. Choose **Create an account**. Use your own email and a 12-72 character password. No demo password is shipped.
6. Stop both terminals with Ctrl+C. Close the two windows. Do not run two copies of the same backend against the same H2 file.

If the launcher has trouble, use two terminals from this project folder:

```powershell
# Terminal 1
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 2
cd frontend
npm ci
npm run dev
```

Backend: http://127.0.0.1:8081/actuator/health. Frontend port: 5174. The other project uses different ports, so they can run together, though one at a time saves RAM. Close the earlier Banking-Investment-Platform backend if port 8080 is already occupied.

Linux/macOS: `./start.sh`, or use the same two-terminal commands with `./mvnw`.

## Implemented features

- Register/login/logout; BCrypt hashes, signed 15-minute JWTs, CUSTOMER role and server-side ownership checks.
- Multiple accounts, each with INR 10,000 demo opening funds and a separate opening ledger entry.
- Transfer to your own or another registered user's account ID; only the source owner can debit it.
- SQL transactions update both balances, transfer record, two signed ledger entries and audit event atomically.
- Sorted row locks handle concurrent transfers; insufficient funds and invalid amounts are rejected.
- Request-key idempotency survives application restarts: same key/body replays; changed body conflicts.
- Persistent account ledger, actor audit trail, searchable/paginated transfer history and CSV export.
- Reconciliation compares stored balance with signed ledger sum and checks each transfer has two zero-sum entries.

## Data, auth and money rules

- Default database: `backend/data/lab.mv.db`. It persists users and business records after restart. Back up the entire `data` folder **only after stopping the backend**. To reset, stop it and delete that folder. Never commit it to Git.
- A fresh RSA signing key is generated on startup. Data persists; old JWTs become invalid after restart. Sign in again. JWTs live only in React memory; refreshing the browser signs you out locally. Logout revokes the current token for the life of that server process.
- Every account has only CUSTOMER privileges. There is no admin role promotion endpoint or admin console. Ownership is checked on the server; hiding a UI control is not authorization.
- Money uses Java BigDecimal and SQL DECIMAL. No real funds, trading exchange, payments, market data feed, brokerage, tax, or advice is involved.
- Opening funds are deliberately created demo funding. They are not a bank integration. Market scenarios, where present, are test controls.
- H2 is the verified default. A PostgreSQL JDBC driver and environment-variable configuration are included as an optional extension, but PostgreSQL has **not** been tested here. Use a `jdbc:postgresql://...` URL, not a `postgres://...` URL. Review schema/migrations before changing databases.

## Checks

```powershell
cd backend
.\mvnw.cmd test
# or .\mvnw.cmd verify
cd ..\frontend
npm ci
npm run build
```

Read `VERIFICATION.md` for checks actually executed during delivery. CI configuration is included; it has not run on your GitHub account. There is no dependency vulnerability audit claim.

## Optional local Docker

If Docker Desktop is already working, run `docker compose up --build` from this project folder. Open http://127.0.0.1:4174. Nginx serves the React build and proxies API calls to the backend. Data uses a named volume. `docker compose down` preserves it; `docker compose down -v` deletes it. Docker image builds are supplied as an optional path and are not verified in this execution environment. You can use the native setup without Docker and with less RAM.

## Learning and review

See `LEARNING_GUIDE.md`, `API.md`, `MANUAL_TESTS.md`, and `VERIFICATION.md`. Read the source, run the tests, change one business rule, and add its test before describing this work in an interview.

## Scope of this delivery

End-to-end means the listed local UI, API, persistence, validation and tests are implemented. It does not mean production banking certification or every future feature. AWS/Render deployment, Kafka/outbox consumers, refresh-token rotation, password recovery/email verification, public-facing rate limits, MFA, admin roles, live data, distributed token revocation, monitoring dashboards and production disaster recovery are future work. No cloud resources are created and no paid subscriptions are configured. Database result sets are loaded for the local demo; pagination and filtering happen in React, not at SQL level.

## References

- https://spring.io/guides/gs/managing-transactions/
- https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html
- https://h2database.com/html/features.html
- https://react.dev/learn

## Browser acceptance test

Keep the native backend and Vite frontend running. In a third terminal:

```powershell
cd frontend
npx playwright install chromium
npm run test:e2e
```

The test creates a new test user and demo records in your local database. It exercises the real UI/API/database flow and checks error handling, CSV download, refresh/login, logout and a narrow mobile layout. It does not clear existing users or records. The script takes a local screenshot in the OS temporary directory for diagnosis; no screenshot is uploaded. Optional environment variables: E2E_BASE_URL, PLAYWRIGHT_EXECUTABLE_PATH, E2E_SCREENSHOT. Browser download needs internet. The ZIP includes source and lockfiles; node_modules, compiled JARs and user databases are excluded.
