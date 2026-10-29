# Delivery verification — 29 October 2026

Actually run in the build environment:

- Maven package: PASS; **14 JUnit tests, zero failures/errors/skips**. Tests include auth and real HTTP integration plus service-level database transactions.
- React production build: PASS.
- Chromium browser acceptance against the running executable Spring Boot JAR and Vite proxy: PASS. No uncaught browser JavaScript errors. The UI made real authenticated API requests to SQL-backed services.
- Browser reload -> sign in -> business records retained: PASS.
- Backend process stopped and started against the same file-based H2 database -> login and resource identity retained: PASS.
- Narrow 390px viewport: no page-wide horizontal overflow after layout fixes; tables scroll inside their cards.
- Desktop screenshots reviewed for layout.

Default versions: Spring Boot 4.1.2; Java release target 17 (tested on the environment's Java runtime); React 19.3.1; Vite 6.3.6; Playwright 1.62.1. Lockfiles included.

Not executed here: Windows CMD launchers, Docker builds/Compose, PostgreSQL runtime, GitHub-hosted Actions, AWS/Render deployments, penetration tests, stress/soak tests, or a dependency vulnerability audit. Docker/CI files are supplied configuration, not evidence of a deployment. Native servers were launched directly during verification.

Re-run the included tests on your machine and follow MANUAL_TESTS.md.
