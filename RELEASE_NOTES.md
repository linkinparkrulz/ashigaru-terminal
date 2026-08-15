# Ashigaru Desktop 1.2.0

*Released 2026-08-15*

A release about seeing what the software is actually doing — a new Logs tool for looking under the
hood and reporting problems, and a fix to release verification that had been quietly failing since
it was introduced.

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

- **`SHA256SUMS` now lists the release files.** Published releases carried a `SHA256SUMS`
  containing exactly one line: a hash of nothing, naming the file itself. Running the
  `sha256sum -c SHA256SUMS` check documented in the README could therefore never confirm a
  download, on this release or any before it. It now lists every published file under the name you
  download it as, so the documented check works as written. Thanks to the user who reported it.
- **Both Linux tarballs ship.** The desktop and headless Linux builds were producing tarballs with
  the same filename, so only one of the two ever reached the release page. The headless build is
  now published as `Ashigaru-server-…`, matching the naming already used for its `.deb` and `.rpm`
  packages.
