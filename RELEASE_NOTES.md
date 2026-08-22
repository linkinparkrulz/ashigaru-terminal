# Ashigaru Desktop 1.3.0

*Released 2026-08-16*

Ashigaru can now tell you when a new version exists, fetch it, and prove it came from the Ashigaru
release key before you install it — so finding an upgrade no longer means going back to a search
engine, where forks of this project are easy to mistake for the real one.

This release also carries everything from 1.2.0, which was prepared but never published: a Logs
tool, and a fix to release verification that had been quietly failing since it was introduced.

## Updates

- **Ashigaru checks for new releases.** A new **Settings → Update** screen shows the version you
  are running and the latest published release, and can check on demand or once a day. Checking is
  off until you say yes: you are asked once, on first run, and the request goes through your proxy
  when one is configured. It reveals only that someone asked for a version number, never anything
  about your wallets.
- **Downloads are verified before you are offered them.** Choosing an update fetches it and then
  checks four things in order, showing each as it passes: the release message was signed by the
  Ashigaru release key, the published checksums match what that signed message commits to, the
  release is the one being offered and is newer than the one you run, and the file you downloaded
  matches its published checksum. These are the same checks the README asks you to perform by hand.
- **A failed check deletes the download.** If any link in that chain does not hold, the file is
  removed rather than left somewhere it could be opened by accident, and the screen says which
  check failed. A release published without its signature is refused outright rather than trusted.
- **Then you install it.** Ashigaru does not update itself. On Windows and macOS it hands the
  verified file to the system installer and steps aside; on Linux it shows you the file, since
  package installs need root and the right format depends on your distribution. The package
  matching how you installed is preselected, with the rest available if the guess is wrong.

## Tools

- **Logs.** A new **Tools → Logs** screen shows recent application activity without leaving the
  app. Filter to warnings or errors, or search the text; either way stack traces stay attached to
  the entry they belong to rather than being cut in half.
- **Safe to share.** Bitcoin addresses, extended public keys, BIP47 payment codes, transaction ids,
  onion hostnames, derivation paths and the names of your open wallets are replaced with
  placeholders before anything is shown. That is the default, so the text you are looking at is
  already the text that is safe to send. A **Show raw log** toggle turns redaction off and tells
  you what it exposes.
- **Built for bug reports.** **Copy** and **Save** emit exactly what is on screen, with a short
  header carrying the version, operating system, Java version, network and whether a proxy is
  active — the things a developer asks for first, and nothing about your wallets. **Open Folder**
  takes you to the log directory if you would rather handle the file yourself.

## Interface

- **The Whirlpool Stats icon appears.** Its sidebar icon was drawn from an older icon set whose
  character position is empty in the font the app actually loads, so the space beside the label was
  blank. It now shows.

## Under the hood

- **The log file no longer grows forever.** `ashigaru.log` was written to indefinitely and could
  reach any size given enough time. It now rolls at 10 MB, keeping three files and 40 MB at most.

## Verifying releases

- **`SHA256SUMS` now lists the release files.** Every release up to and including 1.1.2 carried a
  `SHA256SUMS` containing exactly one line: a hash of nothing, naming the file itself. Running the
  `sha256sum -c SHA256SUMS` check documented in the README could therefore never confirm a
  download. It now lists every published file under the name you download it as, so the documented
  check works as written. Thanks to the user who reported it.
- **Both Linux tarballs ship.** The desktop and headless Linux builds were producing tarballs with
  the same filename, so only one of the two ever reached the release page. The headless build is
  now published as `Ashigaru-server-…`, matching the naming already used for its `.deb` and `.rpm`
  packages.
