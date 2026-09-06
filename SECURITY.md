# Security Policy

## Supported Versions

Only the latest release receives security updates. Please make sure you are
running an up-to-date build before reporting an issue.

| Version | Supported |
| ------- | --------- |
| 1.4.x   | ✔        |
| < 1.4   | :x:       |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues
or pull requests.** Ashigaru Desktop is Bitcoin wallet software, and public
disclosure of a flaw can put users' funds and privacy at risk before a fix is
available.

Instead, report vulnerabilities privately through either of these channels:

- **Email:** [linkinparkrulz@protonmail.com](mailto:linkinparkrulz@protonmail.com)
- **GitHub:** the private
  [Report a vulnerability](https://github.com/linkinparkrulz/ashigaru-desktop/security/advisories/new)
  advisory form (GitHub Security Advisories)

Please include as much of the following as you can:

- The affected version and your operating system
- A description of the vulnerability and its potential impact
- Step-by-step instructions to reproduce it, and any proof-of-concept code
- Any suggested mitigation, if you have one

## What to Expect

- We aim to acknowledge your report within a few days.
- We will investigate, keep you informed of progress, and work toward a fix.
- We follow coordinated disclosure: please give us a reasonable window to
  release a fix before any public discussion of the issue.

We appreciate responsible disclosure and the effort it takes to report issues
in good faith.

## Verifying Releases

To confirm a release is authentic and untampered, verify its hash and
signature as described in the
[*Verifying a release*](README.md#verifying-a-release) section of the README.
Every release is signed by the maintainer using the Ashigaru release-signing
BIP47 Payment Code.
