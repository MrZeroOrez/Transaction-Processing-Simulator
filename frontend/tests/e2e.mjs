import { chromium } from "playwright";
import assert from "node:assert/strict";
import { tmpdir } from "node:os";
import { join } from "node:path";
const browser = await chromium.launch({
  headless: true,
  ...(process.env.PLAYWRIGHT_EXECUTABLE_PATH
    ? {
        executablePath: process.env.PLAYWRIGHT_EXECUTABLE_PATH,
        args: ["--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"],
      }
    : {}),
});
const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
const errors = [];
page.on("pageerror", (e) => errors.push(e.message));
const email = "e2e-" + Date.now() + "@example.test",
  password = "Test-password-123";
const url = process.env.E2E_BASE_URL || "http://127.0.0.1:5174";
async function waitIdle() {
  await page.waitForFunction(
    () =>
      ![...document.querySelectorAll("button")].some((b) =>
        b.textContent.includes("Please wait"),
      ),
  );
}
async function register() {
  await page.goto(url);
  await page
    .getByRole("button", { name: "New here? Create an account" })
    .click();
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Register", exact: true }).click();
}
async function login() {
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}
async function status(text) {
  await page.getByRole("status").filter({ hasText: text }).waitFor();
}
async function stat(index, expected) {
  await page.waitForFunction(
    ({ index, expected }) =>
      document.querySelectorAll(".stat strong")[index]?.textContent ===
      expected,
    { index, expected },
  );
}
try {
  await register();
  await page.getByRole("heading", { name: "Transaction operations" }).waitFor();
  for (const name of ["Primary", "Savings"]) {
    await page.getByLabel("New demo account", { exact: true }).fill(name);
    await page
      .getByRole("button", { name: "Create account", exact: true })
      .click();
    await status("Account created");
  }
  await stat(0, "₹20,000.00");
  const ids = await page.locator(".account .id").allTextContents();
  assert.equal(ids.length, 2);
  await page.getByLabel("Source account", { exact: true }).selectOption(ids[0]);
  await page.getByLabel("Destination account ID", { exact: true }).fill(ids[1]);
  await page.getByLabel("Amount (INR)", { exact: true }).fill("123.45");
  await page.getByLabel("Note", { exact: true }).fill("E2E transfer");
  await page.getByRole("button", { name: "Post transfer" }).click();
  await status("Transfer posted");
  await stat(2, "1");
  await page.locator(".account").nth(0).click();
  await page.waitForFunction(() =>
    document.querySelectorAll(".account")[0]?.classList.contains("active"),
  );
  assert.ok(
    (await page.locator(".account").nth(0).innerText()).includes("₹9,876.55"),
  );
  await page.getByText("MATCHED", { exact: true }).waitFor();
  await page.getByText("BALANCED", { exact: true }).waitFor();
  await page.getByLabel("Amount (INR)", { exact: true }).fill("99999");
  await page.getByRole("button", { name: "Post transfer" }).click();
  await page
    .getByRole("alert")
    .filter({ hasText: "Insufficient funds" })
    .waitFor();
  await stat(2, "1");
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export CSV" }).click();
  const file = await download;
  assert.equal(file.suggestedFilename(), "transfers.csv");
  await page.getByLabel("Search transfers").fill("E2E");
  assert.equal(
    await page.locator("table").nth(0).locator("tbody tr").count(),
    1,
  );
  await page.screenshot({
    path: process.env.E2E_SCREENSHOT || join(tmpdir(), "transfer-e2e.png"),
    fullPage: true,
  });
  await page.reload();
  await login();
  await page.getByRole("heading", { name: "Transaction operations" }).waitFor();
  await stat(2, "1");
  await stat(0, "₹20,000.00");
  await page.setViewportSize({ width: 390, height: 844 });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth + 1,
    ),
    "Mobile page overflow",
  );
  await page.getByRole("button", { name: "Log out" }).click();
  await page.getByRole("heading", { name: "Welcome back" }).waitFor();
  console.log(
    "PASS: transfer browser -> API -> SQL, two accounts, transfer, ledger/reconciliation, insufficient funds, CSV export, filter, reload/login, logout, mobile layout.",
  );
  assert.deepEqual(errors, [], "No uncaught browser errors");
} catch (e) {
  await page.screenshot({
    path: join(tmpdir(), "failed-" + url.split(":").pop() + ".png"),
    fullPage: true,
  });
  console.error("Page text:", await page.locator("body").innerText());
  throw e;
} finally {
  await browser.close();
}
