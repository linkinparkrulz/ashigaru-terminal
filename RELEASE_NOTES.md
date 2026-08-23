# Ashigaru Desktop 1.4.1

*Released 2026-08-23*

Three fixes for one failure: starting Ashigaru while it was already running left you with a second
process that could not start Tor, and a settings file corrupted by both processes writing it at once.

## Startup

- **A second launch no longer starts a second Ashigaru.** Opening the app while it was already
  running started a whole second process. Both then pointed Tor at the same data directory, which
  Tor refuses — `It looks like another Tor process is running with the same data directory` —
  and the second window died during startup with a config-reading error. Launching again now brings
  the running window to the front and exits, which is what it always looked like it was doing.
  Opening a `.psbt` or a `bitcoin:` link while Ashigaru is running is unchanged: it still opens in
  the window you already have.

## Settings

- **The settings file can no longer be left half written.** `config` was saved by truncating it and
  writing over the top, so a crash or a second process writing at the same moment could leave it cut
  off mid-value. Ashigaru would then fail to read it at the next start, fall back to defaults, and
  overwrite it — losing every setting. It is now written alongside the real file and moved into
  place in one step, so what is on disk is always either the previous version entire or the new one
  entire.
- **An unreadable settings file is kept.** If `config` cannot be parsed it is moved to
  `config.corrupt-<date>` before Ashigaru starts from defaults, and the log says where it went.
  Previously the damaged file was silently overwritten, so there was nothing left to recover from.

## Verifying releases

Unchanged. `SHA256SUMS` lists every published file under the name you download it as:

```bash
sha256sum -c SHA256SUMS --ignore-missing
sha256sum SHA256SUMS   # compare with SHA256(SHA256SUMS) in MESSAGE.txt
```

The signature in `RELEASE-BIP47-SIGNATURE.txt` must recover to `1K8CDoBYWBuaeAhejLAk5hiACAgbbPnDCJ`,
the notification address of the release signing payment code published in the README. **Tools →
Verify BIP47 Message** does this for you, and **Settings → Update** checks the whole chain
automatically.
