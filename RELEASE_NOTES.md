# Ashigaru Desktop 1.4.0

*Released 2026-08-22*

This release fixes a fault that broke every clearnet HTTPS request in released builds, including the
update check that 1.3.0 introduced. It also makes the startup screen report what it is doing, and
finally builds the packages the download table has long advertised.

## Connectivity

- **Released builds could not make an HTTPS connection.** Anything reaching a normal `https://`
  address failed with `Received fatal alert: handshake_failure` — the update check most visibly, but
  the cause was general. The packaged runtime is assembled with `jlink`, and it was being built
  without `jdk.crypto.ec`, the module providing the SunEC security provider. Without it the runtime
  cannot complete the elliptic-curve handshake that essentially every modern server requires. Onion
  addresses were unaffected, which is why mixing, fee rates and server connections kept working and
  this went unnoticed for so long. Running from source was unaffected too, since that uses the full
  JDK — the fault only ever appeared in the builds published here.

  **If you are on 1.3.0, its update checker cannot reach GitHub, so this release has to be installed
  by hand.** From 1.4.0 onward in-app updates work.

## Startup

- **The splash reports real progress.** It used to show a bar that spun regardless and one of four
  fixed messages. It now follows the startup as it happens — Tor, then the server connection, then
  wallet loading — marking each step as it completes. During Tor bootstrap, the slow part of a cold
  start, the bar moves with Tor's own reported percentage rather than pretending not to know.
- **Failures are shown.** A Tor bootstrap that fails, or a server that cannot be reached, used to
  leave the same spinning bar until something timed out. The step is now marked failed with the
  reason kept on screen, so a stall is distinguishable from a hang.
- **A typeface of its own.** The splash title is set in Nikkyou Sans, a display face drawn from
  wartime propaganda poster lettering, which suits the name better than the system font did.

## Packages

- **`.rpm` packages are published.** The download table has listed `.rpm` for Linux since well
  before this release, and it was never actually built — Fedora, RHEL and openSUSE users were
  quietly left with the tarball. Both the desktop and headless builds now produce one.
- **`.msi` installers are published.** Same story on Windows: advertised, never built. The `.exe`
  installer remains the recommended route, with the `.msi` alongside it.
- **The portable Windows build is offered in-app.** Ashigaru's updater only ever offered `.exe` and
  `.msi`, so anyone running the portable `.zip` was shown an installer instead of the build they
  actually use. It is now listed with the others, with the `.exe` still preselected.

## Project

- **Bundled fonts are attributed.** Roboto Mono and the two Font Awesome faces have shipped inside
  every release without appearing in `THIRD_PARTY_NOTICES.md`, though both licences ask for their
  notices to travel with the binaries. They are recorded now, along with the new splash typeface.

## Verifying releases

Unchanged from 1.3.0, and worth repeating because this is the first release an existing install can
check for itself. `SHA256SUMS` lists every published file under the name you download it as:

```bash
sha256sum -c SHA256SUMS --ignore-missing
sha256sum SHA256SUMS   # compare with SHA256(SHA256SUMS) in MESSAGE.txt
```

The signature in `RELEASE-BIP47-SIGNATURE.txt` must recover to `1K8CDoBYWBuaeAhejLAk5hiACAgbbPnDCJ`,
the notification address of the release signing payment code published in the README. **Tools →
Verify BIP47 Message** does this for you.
