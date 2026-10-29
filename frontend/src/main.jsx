import React, { useState, useEffect, useRef } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
const money = (n) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(
    n || 0,
  );
const short = (s) => s?.slice(0, 8) || "—";
function App() {
  const [session, setSession] = useState(null),
    [mode, setMode] = useState("login"),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false);
  const request = async (
    path,
    { method = "GET", body, key, raw = false } = {},
  ) => {
    const r = await fetch("/api/v1" + path, {
      method,
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(session ? { Authorization: "Bearer " + session.accessToken } : {}),
        ...(key ? { "Idempotency-Key": key } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!r.ok) {
      let msg = "Request failed (" + r.status + ")";
      try {
        msg = (await r.json()).error || msg;
      } catch {}
      if (r.status === 401 && session) setSession(null);
      throw new Error(msg);
    }
    return raw ? r.text() : r.status === 204 ? null : r.json();
  };
  const run = async (fn) => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await fn();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  async function authenticate(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    await run(async () => {
      const s = await request("/auth/" + mode, {
        method: "POST",
        body: { email: f.get("email"), password: f.get("password") },
      });
      setSession(s);
    });
  }
  async function logout() {
    await run(async () => {
      await request("/auth/logout", { method: "POST" });
      setSession(null);
    });
  }
  async function download(path, name) {
    const text = await request(path, { raw: true });
    const url = URL.createObjectURL(new Blob([text], { type: "text/csv" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
  return (
    <>
      <header>
        <div className="brand">
          <span className="logo">◈</span>
          <div>
            LedgerFlow<small>JAVA · SPRING BOOT · REACT</small>
          </div>
        </div>
        <div className="header-actions">
          <span className="badge">SIMULATION ONLY</span>
          {session && (
            <button disabled={busy} onClick={logout}>
              Log out
            </button>
          )}
        </div>
      </header>
      <main>
        {!session ? (
          <section className="welcome">
            <div>
              <p className="eyebrow">FULL STACK LEARNING LAB</p>
              <h1>Trace every rupee. Verify every transfer.</h1>
              <p className="muted">
                Build confidence in a transaction engine with account ownership,
                atomic updates, and a verifiable ledger.
              </p>
              <div className="feature-list">
                <span>✓ Transfers with duplicate protection</span>
                <span>✓ Double-entry transfer ledger</span>
                <span>✓ Audit and reconciliation</span>
              </div>
              <p className="muted">
                No real money. No external API keys. Your data stays in the
                local SQL database.
              </p>
            </div>
            <form className="card auth" onSubmit={authenticate}>
              <h2>
                {mode === "login" ? "Welcome back" : "Create your account"}
              </h2>
              <label>
                Email
                <input
                  name="email"
                  type="email"
                  maxLength="254"
                  autoComplete="username"
                  required
                />
              </label>
              <label>
                Password
                <input
                  name="password"
                  type="password"
                  minLength="12"
                  maxLength="72"
                  autoComplete={
                    mode === "login" ? "current-password" : "new-password"
                  }
                  required
                />
              </label>
              <small>Use 12–72 characters (maximum 72 UTF-8 bytes).</small>
              <button className="primary" disabled={busy}>
                {busy
                  ? "Please wait…"
                  : mode === "login"
                    ? "Sign in"
                    : "Register"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode(mode === "login" ? "register" : "login");
                  setError("");
                }}
              >
                {mode === "login"
                  ? "New here? Create an account"
                  : "Already registered? Sign in"}
              </button>
            </form>
          </section>
        ) : (
          <Workspace
            session={session}
            request={request}
            run={run}
            busy={busy}
            notice={setNotice}
            download={download}
          />
        )}
        {error && (
          <div className="alert error" role="alert">
            {error}
          </div>
        )}
        {notice && (
          <div className="alert success" role="status">
            {notice}
          </div>
        )}
      </main>
      <footer>
        Local development demo · Money values stored with SQL DECIMAL and Java
        BigDecimal · Access tokens expire after 15 minutes
      </footer>
    </>
  );
}
function Workspace({ session, request, run, busy, notice, download }) {
  const [accounts, setAccounts] = useState([]),
    [selected, setSelected] = useState(""),
    [history, setHistory] = useState([]),
    [entries, setEntries] = useState([]),
    [recon, setRecon] = useState(null),
    [events, setEvents] = useState([]),
    [filter, setFilter] = useState("");
  const pending = useRef(null);
  async function refresh(id = selected) {
    const [as, ts, es] = await Promise.all([
      request("/accounts"),
      request("/transfers"),
      request("/audit"),
    ]);
    setAccounts(as);
    setHistory(ts);
    setEvents(es);
    const chosen = id || as[0]?.id || "";
    setSelected(chosen);
    if (chosen) {
      const [ls, rs] = await Promise.all([
        request("/accounts/" + chosen + "/ledger"),
        request("/accounts/" + chosen + "/reconciliation"),
      ]);
      setEntries(ls);
      setRecon(rs);
    } else {
      setEntries([]);
      setRecon(null);
    }
  }
  useEffect(() => {
    run(() => refresh());
  }, []);
  async function create(e) {
    e.preventDefault();
    const f = e.currentTarget,
      name = new FormData(f).get("name");
    await run(async () => {
      const a = await request("/accounts", { method: "POST", body: { name } });
      await refresh(a.id);
      f.reset();
      notice(
        "Account created with ₹10,000 demo opening funds and an opening ledger entry.",
      );
    });
  }
  async function transfer(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const body = {
      sourceId: f.get("source"),
      targetId: f.get("target").trim(),
      amount: Number(f.get("amount")),
      note: f.get("note"),
    };
    const fingerprint = JSON.stringify(body);
    if (pending.current?.fingerprint !== fingerprint)
      pending.current = { fingerprint, key: crypto.randomUUID() };
    await run(async () => {
      const t = await request("/transfers", {
        method: "POST",
        body,
        key: pending.current.key,
      });
      pending.current = null;
      await refresh();
      notice(
        "Transfer posted: " +
          t.id +
          ". Both balances and ledger entries committed together.",
      );
    });
  }
  const total = accounts.reduce((s, a) => s + Number(a.balance), 0);
  const volume = history.reduce((s, t) => s + Number(t.amount), 0);
  const rows = history.filter((t) =>
    (t.id + " " + t.note + " " + t.sourceId + " " + t.targetId)
      .toLowerCase()
      .includes(filter.toLowerCase()),
  );
  return (
    <>
      <div className="toolbar">
        <div>
          <p className="eyebrow">PAYMENTS ENGINEERING LAB</p>
          <h1>Transaction operations</h1>
          <small>Atomic transfers · Persistent ledger · Reconciliation</small>
        </div>
        <button disabled={busy} onClick={() => run(() => refresh())}>
          Refresh records
        </button>
      </div>
      <form className="inline" onSubmit={create}>
        <label>
          New demo account
          <input
            name="name"
            placeholder="Primary wallet"
            maxLength="80"
            required
          />
        </label>
        <button className="primary" disabled={busy}>
          Create account
        </button>
      </form>
      <div className="stats">
        <Stat label="TOTAL OWNED BALANCE" value={money(total)} />
        <Stat label="ACCOUNTS" value={accounts.length} />
        <Stat label="VISIBLE TRANSFERS" value={history.length} />
        <Stat label="TRANSFER VOLUME" value={money(volume)} />
      </div>
      <div className="accounts">
        {accounts.map((a) => (
          <button
            disabled={busy}
            key={a.id}
            className={"card account " + (selected === a.id ? "active" : "")}
            onClick={() => run(() => refresh(a.id))}
          >
            <h3>{a.name}</h3>
            <strong>{money(a.balance)}</strong>
            <div className="id">{a.id}</div>
            <small>Click to inspect ledger</small>
          </button>
        ))}
      </div>
      <div className="grid">
        <section className="card">
          <div className="row">
            <h2>Transfer history</h2>
            <button
              disabled={busy}
              onClick={() =>
                run(() => download("/transfers/export", "transfers.csv"))
              }
            >
              Export CSV
            </button>
          </div>
          <label>
            Search transfers
            <input
              className="search"
              placeholder="ID or note"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          </label>
          <Table
            heads={["Time", "Reference", "From → To", "Amount", "Note"]}
            rows={rows.map((t) => [
              new Date(t.createdAt).toLocaleString(),
              <span title={t.id}>{short(t.id)}</span>,
              <span title={t.sourceId + " → " + t.targetId}>
                {short(t.sourceId)} → {short(t.targetId)}
              </span>,
              money(t.amount),
              t.note,
            ])}
          />
        </section>
        <section className="card">
          <h2>Send demo funds</h2>
          <form className="stack" onSubmit={transfer}>
            <label>
              Source account
              <select name="source" aria-label="Source account" required>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} · {money(a.balance)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Destination account ID
              <input
                name="target"
                  aria-label="Destination account ID"
                placeholder="Paste the full account UUID"
                required
                list="destinations"
              />
              <datalist id="destinations">
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </datalist>
            </label>
            <div className="inline">
              <label>
                Amount (INR)
                <input
                  name="amount"
                  type="number"
                  min="0.01"
                  max="1000000"
                  step="0.01"
                  defaultValue="100"
                  required
                />
              </label>
            </div>
            <label>
              Note
              <input
                name="note"
                maxLength="140"
                placeholder="Optional reference"
              />
            </label>
            <button className="primary" disabled={busy || !accounts.length}>
              Post transfer
            </button>
          </form>
          <p className="hint">
            Transfers can go to your own accounts or another registered user's
            account. To test two users, use a private browser window and share
            the destination ID. Only the source owner can authorize a transfer.
          </p>
        </section>
      </div>
      {selected && (
        <div className="grid">
          <section className="card">
            <h2>
              Account ledger · {accounts.find((a) => a.id === selected)?.name}
            </h2>
            <p className="id">{selected}</p>
            <Table
              heads={["Time", "Type", "Amount", "Transfer reference"]}
              rows={entries.map((e) => [
                new Date(e.createdAt).toLocaleString(),
                <span className="pill">{e.kind}</span>,
                <span className={e.amount >= 0 ? "positive" : "negative"}>
                  {money(e.amount)}
                </span>,
                <span title={e.transferId}>{short(e.transferId)}</span>,
              ])}
            />
          </section>
          <section className="card">
            <h2>Reconciliation</h2>
            {recon && (
              <div className="stack">
                <div className="row">
                  <span>Stored balance</span>
                  <strong>{money(recon.balance)}</strong>
                </div>
                <div className="row">
                  <span>Ledger sum</span>
                  <strong>{money(recon.ledgerTotal)}</strong>
                </div>
                <div className="subtle">
                  Balance check:{" "}
                  <span className={recon.matches ? "positive" : "negative"}>
                    {recon.matches ? "MATCHED" : "MISMATCH"}
                  </span>
                </div>
                <div className="subtle">
                  Transfer pairs:{" "}
                  <span
                    className={
                      recon.transferPairsBalanced ? "positive" : "negative"
                    }
                  >
                    {recon.transferPairsBalanced ? "BALANCED" : "MISMATCH"}
                  </span>
                </div>
                <p className="hint">
                  Every posted transfer must have two entries whose signed
                  amounts sum to zero. Demo opening credits are separate funding
                  events.
                </p>
              </div>
            )}
          </section>
        </div>
      )}
      <section className="card">
        <h2>Your audit trail</h2>
        <Table
          heads={["Time", "Action", "Resource ID"]}
          rows={events.map((e) => [
            new Date(e.createdAt).toLocaleString(),
            <span className="pill">{e.action}</span>,
            <span className="id">{e.resourceId}</span>,
          ])}
        />
      </section>
    </>
  );
}
function Stat({ label, value, positive }) {
  return (
    <div className="card stat">
      <small>{label}</small>
      <strong
        className={
          positive === undefined ? "" : positive ? "positive" : "negative"
        }
      >
        {value}
      </strong>
    </div>
  );
}
function Table({ heads, rows, empty = "No records yet." }) {
  const [page, setPage] = useState(0);
  useEffect(() => setPage(0), [rows.length]);
  const pages = Math.ceil(rows.length / 8);
  return (
    <>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              {heads.map((h) => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.slice(page * 8, page * 8 + 8).map((r, i) => (
              <tr key={i}>
                {r.map((c, j) => (
                  <td key={j}>{c}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        {!rows.length && <div className="empty">{empty}</div>}
      </div>
      {pages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          {page + 1} / {pages}
          <button
            disabled={page >= pages - 1}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </>
  );
}
createRoot(document.getElementById("root")).render(<App />);
