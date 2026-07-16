# Ashigaru Desktop 1.1.1

*Released 2026-07-16*

A bug-fix release focused on repairing the **Mix To** flow and fixing Linux desktop
integration.

## Fixes

### Mix To
- **"Mix To…" now actually works.** Choosing a destination wallet and minimum mix count
  and clicking **Apply** previously did nothing — the dialog silently discarded every
  setting, so postmix outputs were never redirected. Apply now applies and persists the
  configuration (and survives a restart).
- **Clear a stale destination.** When the configured mix-to wallet isn't currently open,
  the dialog now enables **Apply** so you can clear the stale target, instead of leaving
  it stuck.
- **Cleaner destination list.** BIP47/PayNym contact accounts (which appeared as
  `wallet — PM8T…` and were never valid mix destinations) are no longer listed — only real
  spendable accounts are shown.

### Linux
- **No longer grouped with Sparrow Wallet.** Ashigaru's windows reported the same
  `WM_CLASS` as Sparrow, so desktop environments merged the two under one taskbar/dock
  icon and you couldn't run both at once. Ashigaru now identifies as its own application,
  matching its `.desktop` launcher. (Fixes #20)

---

# Ashigaru Desktop 1.1.0

*Released 2026-07-11*

Ashigaru Desktop is a privacy-first Bitcoin wallet with first-class Whirlpool coinjoin
support, built on the Sparrow Wallet foundation.

This release brings a cohesive Ashigaru redesign, a dedicated Whirlpool mixing
experience, private Electrum server discovery over Tor, and privacy tooling for inspecting
your transactions — plus a batch of UX and packaging fixes.

---

## Highlights

- **Ashigaru interface** — a custom main shell, wallet dashboard, and dark theme applied
  consistently across every screen and dialog.
- **Whirlpool Stats tool** — track mix progress, remixes, and anonymity-set metrics at a
  glance, with Tx0 broadcast feedback.
- **Dojo Electrum server discovery** — automatically find and connect to the public
  Electrum servers of Dojos listed at **dojobay.pw**, verified over Tor.
- **Privacy links on every transaction** — jump straight to a block explorer or an
  "Am I Exposed" privacy check for any transaction.
- **Card-based wallet views** — responsive UTXO and transaction cards replace the old
  tables, with PayNym/BIP47 identity shown inline.

---

## New features

### Whirlpool / mixing
- New **Whirlpool Stats** tool showing sync status, chart metrics, and actionable
  transactions, with a bithypha explorer and a balanced transaction-card layout.
- Whirlpool Stats data is fetched over the **Tor onion** endpoint.
- **Tx0 broadcast animation** — a centered checklist overlay with clear progress while a
  Tx0 is broadcast.
- A clear **warning when the Whirlpool coordinator is unreachable** instead of a silent
  failure.

### Server & connectivity
- **Dojo Electrum server discovery**: fetches the Dojo directory from dojobay.pw over Tor
  and adds the Electrum (Fulcrum) server of each reachable Dojo (v1.28+) to your server
  list. Runs automatically in the background at startup.
- Discovered servers are **cryptographically verified** (the Dojo operator's signed
  pairing) and surfaced in the **Public Server** section, with a clear notice crediting
  dojobay.pw.
- Parallel probing for faster discovery, and the **block-explorer list is populated from
  the same call**.

### Privacy tooling
- Configurable **"Am I Exposed"** privacy-check service (like the block explorer setting),
  with a "None" option to disable it.
- **Explorer** and **exposure** links added to transaction cards — open a transaction in
  your chosen block explorer or privacy checker in one click.

### Wallet views
- UTXO and transaction tables replaced with **responsive card lists**.
- BIP47 receives show the **PayNym name, abbreviated payment code, and avatar** inline.

---

## Improvements

- **Cohesive create / restore flow** with numbered seed-word entry, all theme-compliant.
- **Redesigned BIP47 message verifier** with a friendlier, more inviting layout. (This
  verifier also backs Dojo pairing verification.)
- Every pop-up dialog (errors, confirmations, password/passphrase, seed display, Tx0,
  Mix To) now matches the dark identity.
- **BIP39 passphrase safety**: rather than trying to reject a "wrong" passphrase (which is
  cryptographically impossible — any passphrase opens a different valid wallet), Ashigaru
  now warns when a wallet's entire history unexpectedly changes ("this may be caused by an
  incorrect passphrase") and offers to **Reopen** (re-enter the passphrase) or **Refresh**.
- **Lock button** for passphrase and encrypted wallets — lock a wallet back to its
  protected state without quitting.
- **Server settings are responsive** — warning and discovery text wrap to fit the window
  instead of truncating, and the settings body scrolls on short windows.
- **Window sizing**: minimum height raised so the bottom status bar (network, block
  height, connection) is always visible; opens at a comfortable default size.

---

## Fixes

- Fixed a crash (NullPointerException) when opening a passphrase wallet while Tor was still
  connecting (Whirlpool starting against a mid-load wallet).
- Fixed transaction-card avatar/placeholder misalignment and restored the master
  fingerprint on the unlock dialog.
- Fixed clipping of the master fingerprint and various avatar sizing issues.
- Fixed an ineffective logo and remaining unthemed dialogs.
- Removed a stray `ashigaru-terminal` submodule gitlink.

## Packaging

- **App icons no longer render oversized** in the Dock/taskbar. The runtime and packaged
  icons (macOS `.icns`, Windows `.ico`, Linux PNG) now carry the platform-standard
  transparent safe-area margin (~82% content), and a 1024px master is included for future
  asset work.

---

## Notes & known limitations

- Ashigaru is **Tor-first** — most functionality (Stats, discovery, verified servers)
  assumes Tor is available.
- The passphrase-history warning and server discovery are best exercised on **mainnet**
  with a wallet that already has on-chain history.
- Testing for this release was **manual** (see `TESTING.txt`); no automated test suite was
  added.

## Credits

- Built on **[Sparrow Wallet](https://sparrowwallet.com)**.
- Dojo directory courtesy of **dojobay.pw**.
