# Local acceptance checklist

## Common

1. Register a new user. Duplicate email must fail; wrong password must fail.
2. Refresh the page, sign in again, and verify your data persists.
3. Log out. Sign in with another user and verify private records are isolated.
4. Stop and restart the backend; sign in again and verify stored data remains.
5. Inspect Network: no private call should succeed without a bearer token.
6. Check desktop and narrow mobile viewport; table sections should scroll horizontally rather than force the entire page wide.

## Transfer scenario

1. Create "Primary" and "Savings". Each begins at INR 10,000.
2. Copy Savings' full UUID and send INR 123.45 from Primary.
3. Balances must be 9,876.55 and 10,123.45. Primary has a -123.45 entry; Savings has +123.45. Reconciliation must match.
4. Send more than the source balance; it must fail with no new transfer or ledger row.
5. Try 0, a negative amount, a self-transfer and an unknown destination.
6. Use another browser/private window to create a second user/account. Transfer into it. The recipient sees the incoming transfer but cannot read the sender's private ledger.
7. Filter history, export CSV, and inspect the audit event.
8. Use API.md to repeat an identical request/key; balances must move only once. Change its amount with that key; it must conflict.
