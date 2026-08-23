# Ashigaru Desktop 1.4.1

*Released 2026-08-23*

Everything since 1.3.0 in one release. Most importantly it fixes a fault that broke every clearnet
HTTPS request in published builds — including the update check 1.3.0 introduced, which is why 1.3.0
cannot find this release on its own.

## Connectivity

- **Published builds could not make an HTTPS connection.** Anything reaching a normal `https://`
  address failed with `Received fatal alert: handshake_failure` — the update check most visibly, but
  the cause was general. The packaged runtime is assembled with `jlink`, and it was being built
  without `jdk.crypto.ec`, the module providing the SunEC security provider. SunEC is a *service*
  provider, so nothing in the module graph `requires` it and `jlink` had no reason to include it.
  Without it the runtime cannot complete the elliptic-curve key exchange that essentially every
  modern server needs.

  Two things hid it. Onion addresses were unaffected, since onion HTTP is not TLS, so mixing, fee
  rates and server connections all kept working. And running from source was unaffected, because
  that uses the full JDK — the fault only ever existed in the builds published here.

  **1.3.0 has to be replaced by hand.** Its update checker cannot reach GitHub, so it will not find
  this release for you. From 1.4.1 onward, in-app updates work.

## Startup

- **The splash reports real progress.** It used to show a bar that spun regardless and one of four
  fixed messages. It now follows the startup as it happens — Tor, then the server connection, then
  wallet loading — marking each step as it completes. During Tor bootstrap, the slow part of a cold
  start, the bar moves with Tor's own reported percentage rather than pretending not to know.
- **Failures are shown.** A Tor bootstrap that fails, or a server that cannot be reached, used to
  leave the same spinning bar until something timed out. The step is now marked failed with the
  reason kept on screen, so a stall is distinguishable from a hang.
- **A second launch no longer starts a second Ashigaru.** Opening the app while it was already
  running started a whole second process. Both then pointed Tor at the same data directory, which
  Tor refuses — `It looks like another Tor process is running with the same data directory` — and
  the second window died during startup. Launching again now brings the running window to the front
  and exits, which is what it always appeared to do. Opening a `.psbt` or a `bitcoin:` link while
  Ashigaru is running is unchanged.
- **A typeface of its own.** The splash title is set in Nikkyou Sans, a display face drawn from
  wartime propaganda poster lettering, which suits the name better than the system font did.

## Settings

- **The settings file can no longer be left half written.** `config` was saved by truncating it and
  writing over the top, so a crash — or a second process writing at the same moment — could leave it
  cut off mid-value. Ashigaru would then fail to read it at the next start, fall back to defaults,
  and overwrite it, losing every setting. It is now written alongside the real file and moved into
  place in one step, so what is on disk is always either the previous version entire or the new one
  entire.
- **An unreadable settings file is kept.** If `config` cannot be parsed it is moved aside to
  `config.corrupt-<date>` before Ashigaru starts from defaults, and the log says where it went.
  Previously the damaged file was silently overwritten, leaving nothing to recover from.

## Packages

- **`.rpm` packages are published.** The download table has listed `.rpm` for Linux since well
  before this release, and it was never actually built — Fedora, RHEL and openSUSE users were
  quietly left with the tarball. Both the desktop and headless builds now produce one.
- **`.msi` installers are published.** Same story on Windows: advertised, never built. The `.exe`
  installer remains the recommended route, with the `.msi` alongside it.
- **The portable Windows build is offered in-app.** The updater only ever offered `.exe` and `.msi`,
  so anyone running the portable `.zip` was shown an installer instead of the build they actually
  use. It is now listed with the others, with the `.exe` still preselected.

## Project

- **Bundled fonts are attributed.** Roboto Mono and the two Font Awesome faces have shipped inside
  every release without appearing in `THIRD_PARTY_NOTICES.md`, though both licences ask for their
  notices to travel with the binaries. They are recorded now, along with the new splash typeface.

## Verifying releases

`SHA256SUMS` lists every published file under the name you download it as:

```bash
sha256sum -c SHA256SUMS --ignore-missing
sha256sum SHA256SUMS   # compare with SHA256(SHA256SUMS) in MESSAGE.txt
```

The signature in `RELEASE-BIP47-SIGNATURE.txt` must recover to `1K8CDoBYWBuaeAhejLAk5hiACAgbbPnDCJ`,
the notification address of the release signing payment code published in the README. **Tools →
Verify BIP47 Message** does this for you, and from this release **Settings → Update** checks the
whole chain automatically.
