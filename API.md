# API guide

Base path: `/api/v1`. JSON content type. Private operations require `Authorization: Bearer <accessToken>`. POST trade/transfer/import endpoints additionally require `Idempotency-Key: <8-80 letters, digits, underscore or hyphen>`. Use a unique UUID per intended operation. For network/409 concurrency retries, keep the SAME body and key. A new key represents a new operation. If a key/body is already committed it returns the original result; changing the body with that key returns 409. Account/portfolio creation is not idempotent.

## Auth

- `POST /auth/register`: `{"email":"you@example.com","password":"A-long-password-123"}` -> 201, accessToken/tokenType/expiresIn/userId.
- `POST /auth/login`: same body -> 200.
- `GET /auth/me` -> userId and CUSTOMER role.
- `POST /auth/logout` -> 204; current token revoked.
- `GET /actuator/health` is outside `/api/v1` and is public.

Error JSON normally has an `error` message. 400 invalid request, 401 missing/expired/tampered token or failed login, 404 missing or other-owner private resource, 409 duplicate email/idempotency conflict/insufficient funds/concurrent update. The local app returns 201 for both a newly posted trade/transfer and its successful idempotent replay; compare returned IDs.

## Transfer endpoints

- `GET /accounts`: only the caller's accounts.
- `POST /accounts`: `{"name":"Primary"}` -> 201, account with INR 10,000 demo opening funds.
- `POST /transfers`: `{"sourceId":"SOURCE-UUID","targetId":"DESTINATION-UUID","amount":100.25,"note":"Demo"}` plus key -> 201.
- `GET /transfers`: all transfers involving a caller-owned account.
- `GET /accounts/{id}/ledger`: signed entries for an owned account.
- `GET /accounts/{id}/reconciliation`: balance vs ledger sum, and transfer-pair integrity.
- `GET /audit`: events performed by the caller (recipient audit is not a second actor event).
- `GET /transfers/export`: caller-visible transfers as CSV. Notes are quoted; common spreadsheet formula prefixes are neutralized.

Amount: 0.01 to 1,000,000.00, at most two decimal places. Negative balances and self-transfers rejected. Transferring to another user's known account is allowed; reading their private ledger is not. Transfers are final simulated postings; there is no reversal endpoint. Opening credits are external demo funding, so only transfers themselves have a debit/credit pair.
