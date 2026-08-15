# Ashigaru Desktop 1.1.3

*Released 2026-08-15*

A release built around getting started safely: a guided tour for new users, a dice-first way to
create a strong passphrase, and hardening of how Ashigaru verifies the Dojo nodes it discovers.

## Getting started

- **Guided tour.** A new in-app tour walks you through the app by pointing at the real controls —
  the wallet selector, opening and creating wallets, your balance and accounts, receiving, mixing,
  and the Settings screens. It appears on first launch and is available any time from
  **Tools → Guided Tour**, so you can replay it whenever you like.
- **The tour waits for you.** Start it before you have a wallet and it covers what it can, then
  picks up automatically with the wallet-specific steps as soon as you open your first wallet.

## Wallet creation & passphrases

- **Dice-first passphrase creation.** When adding a passphrase to a new wallet, Ashigaru can now
  walk you through generating one with physical dice, using the EFF large wordlist. Rolling real
  dice takes the entropy out of the computer's hands entirely — useful if you would rather not
  trust software randomness for the one secret that is never written to your wallet file.
- **Weak-passphrase warning.** The passphrase step now tells you when what you have typed is weak,
  at the point where you can still do something about it.

## Dojo discovery & security

- **Signatures are verified before Ashigaru authenticates.** Discovery previously authenticated to
  every reachable Dojo — spending its apikey and building an onion circuit — and only checked the
  signed block afterwards. Verification is pure local cryptography with no network I/O, so it now
  happens first: only listings whose signature verifies are contacted at all.
- **Signed identity is bound to the advertised payment code.** A valid signature on its own only
  proves a block is self-consistent. Without binding, a hostile directory could display an honest
  operator's identity while embedding a block validly signed by the attacker's own payment code.
  Verification now requires the advertised payment code to resolve to the same notification address
  as the embedded one.
- **Simpler, quieter discovery.** Per-Dojo `/auth/login` and `/support/services` authentication is
  gone, along with the thread pool, per-node timeouts, version gate and reachability probe. The only
  network request is the single directory fetch; the indexer endpoint is taken from the directory
  listing directly.
- **Discovered servers are ephemeral.** Dojo Electrum servers and explorers are now cleared at
  shutdown and again at startup, and repopulated by a fresh, verified discovery once you connect.
  Stale or removed nodes no longer linger, and their onion addresses are not left on disk while the
  app is closed.

## Mixing & connectivity

- **Accurate retry logging.** During a coordinator outage, mix sessions reported their retry delay
  in milliseconds while labelling it seconds — "retrying in 90000s". The delay itself was always
  correct; now the log says so too.
- **Fewer Tor circuit rotations.** A sustained coordinator outage used to force a new Tor circuit on
  every single failed connection. Ashigaru now changes identity on the first failure and then every
  third, resetting once a connection succeeds.

## Interface

- **Text wraps instead of disappearing.** Wizard subtitles, body copy and hints, along with the
  public-server warning in Server Settings, were clipping to an ellipsis rather than wrapping. They
  now show in full.

## Project

- **Published security policy.** The repository now carries a `SECURITY.md` setting out coordinated
  disclosure: report vulnerabilities privately by email or through GitHub's Security Advisory form —
  **not** through public issues or pull requests, since this is Bitcoin wallet software and early
  disclosure puts funds and privacy at risk. It also documents which versions receive security
  updates, what to include in a report, what response to expect, and how to verify releases.
