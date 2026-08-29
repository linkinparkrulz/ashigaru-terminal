# Ashigaru Desktop 1.4.5

*Released 2026-08-23*

Everything since 1.3.0. The important one is first: published builds could not make a clearnet HTTPS
connection at all, which is why 1.3.0 cannot find this release on its own.

## Connectivity

- **Published builds could not make an HTTPS connection.** Anything reaching a normal `https://`
  address failed with `Received fatal alert: handshake_failure` — the update check most visibly, but
  the cause was general. The packaged runtime is assembled with `jlink`, and it was being built
  without `jdk.crypto.ec`, the module providing the SunEC security provider. SunEC is a *service*
  provider, so nothing in the module graph `requires` it and `jlink` had no reason to include it.
  Without it the runtime cannot complete the elliptic-curve key exchange that essentially every
  modern server needs.

  Two things hid it. Onion addresses were unaffected, since onion HTTP is not TLS, so mixing, fee
  rates and server connections all kept working. And running from source was unaffected, because that
  uses the full JDK — the fault only ever existed in the builds published here.

  **1.3.0 has to be replaced by hand.** Its update checker cannot reach GitHub, so it will not find
  this release for you. From 1.4.5 onward, in-app updates work.

## Startup

- **A second launch no longer starts a second Ashigaru.** Opening the app while it was already
  running started a whole second process. Both then pointed Tor at the same data directory, which Tor
  refuses — `It looks like another Tor process is running with the same data directory` — and the
  second window died during startup. Launching again now brings the running window to the front and
  exits, which is what it always appeared to do. Opening a `.psbt` or a `bitcoin:` link while
  Ashigaru is running is unchanged.
- **The window opens where you can reach it.** The title bar could land above the top of the screen,
  leaving no way to click minimise or maximise without dragging the window down first. It is now
  pulled inside the usable area of whichever screen it opens on.
- **The splash reports real progress.** It used to show a bar that spun regardless and one of four
  fixed messages. It now follows the startup as it happens — Tor, then the server connection, then
  wallet loading — marking each step as it completes, and moving with Tor's own reported percentage
  during bootstrap. A Tor bootstrap that fails or a server that cannot be reached is marked failed
  with the reason kept on screen, rather than leaving a bar spinning until something times out.

## Creating a wallet

- **The setup dialogs are the right size again.** Every step of **+ New / Restore** had grown far
  taller than its content, in some cases running off the bottom of the screen with the second option
  out of reach. They size to their content properly now.
- **Dice passphrases are joined with dashes.** A rolled passphrase reads
  `abandon-ability-able-…` rather than using spaces. The passphrase has to be copied off the screen by
  hand and typed back in later, possibly into another wallet, and a space is the one separator you
  cannot verify you reproduced: one space or two look identical on paper, and a trailing one is
  invisible. The advisory now says the dashes are part of the passphrase, and explains that the
  master fingerprint is how you check you typed it back correctly.

  Existing wallets are unaffected — you type your own passphrase when restoring, and nothing
  re-derives it.

## Settings

- **The settings file can no longer be left half written.** `config` was saved by truncating it and
  writing over the top, so a crash — or a second process writing at the same moment — could leave it
  cut off mid-value. Ashigaru would then fail to read it at the next start, fall back to defaults, and
  overwrite it, losing every setting. It is now written alongside the real file and moved into place
  in one step.
- **An unreadable settings file is kept** as `config.corrupt-<date>` rather than silently
  overwritten, with the log naming the path, so the settings can still be recovered by hand.

## Tools

- **The BIP47 verifier takes a PayNym handle.** Enter `+linkinparkrulz` and press **Look up** to
  fill in the payment code, instead of pasting 116 characters from somewhere. The lookup goes over
  Tor when it is configured, runs only when you press the button, and shows the name it resolved
  rather than changing the field silently. For checking an Ashigaru release, **Use Release Signing
  Code** remains the button to press — it uses the code compiled into the app and depends on nothing
  external.
- The payment code field is a single line rather than a three-row box.

## Packages

- **`.rpm` packages are published.** The download table has listed `.rpm` for Linux since well before
  this release and it was never actually built — Fedora, RHEL and openSUSE users were quietly left
  with the tarball. Both the desktop and headless builds now produce one.
- **`.msi` installers are published.** Same story on Windows: advertised, never built. The `.exe`
  installer remains the recommended route, with the `.msi` alongside it.
- **The portable Windows build is offered in-app.** The updater only ever offered `.exe` and `.msi`,
  so anyone running the portable `.zip` was shown an installer instead of the build they actually
  use.

## Appearance

- **A typeface of its own.** Ashigaru is set in Nikkyou Sans, a display face drawn from wartime
  propaganda poster lettering. Anything that has to line up or be read character by character —
  amounts, addresses, transaction ids, the log view, typed input — stays monospaced.
- **Bundled fonts are attributed.** Roboto Mono and the two Font Awesome faces had shipped inside
  every release without appearing in `THIRD_PARTY_NOTICES.md`, though both licences ask for their
  notices to travel with the binaries. They are recorded now, along with the new typeface.

## Verifying releases

`SHA256SUMS` lists every published file under the name you download it as:

```bash
sha256sum -c SHA256SUMS --ignore-missing
sha256sum SHA256SUMS   # compare with SHA256(SHA256SUMS) in MESSAGE.txt
```

The signature in `RELEASE-BIP47-SIGNATURE.txt` must recover to `1K8CDoBYWBuaeAhejLAk5hiACAgbbPnDCJ`,
the notification address of the release signing payment code published in the README.
**Tools → Verifier** does this for you, and from this release **Settings → Update** checks the
whole chain automatically.
