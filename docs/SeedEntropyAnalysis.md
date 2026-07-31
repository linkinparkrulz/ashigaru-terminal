# Seed Entropy and Key Storage Analysis

An assessment of how Ashigaru Desktop generates wallet seed entropy and protects it at rest,
written in response to the disclosure of a predictable-RNG fallback and 32-bit reseed in Coldcard
firmware ([Block engineering write-up](https://engineering.block.xyz/blog/predictable-rng-fallback-and-32-bit-reseed-in-coldcard-firmware)).

*Reviewed against Ashigaru Desktop v1.1.2 (`0592608`), with cross-checks against Ashigaru Terminal
and Ashigaru Mobile as noted in [§7](#7-cross-check-terminal-and-mobile). Line references are
accurate as of that revision and may drift as the code changes.*

---

## TL;DR

**The Coldcard failure modes do not apply to Ashigaru.** This was verified across all three
products — **Desktop, Terminal, and Mobile** — each of which generates a 12-word seed carrying a
full 128 bits of entropy and requires a BIP39 passphrase. No defect was found that calls for a
code change, and no existing wallet is weakened by anything described here.

| Question | Finding |
|---|---|
| Could a weak RNG produce a guessable seed? | **No.** Entropy comes straight from the OS CSPRNG. The application implements no RNG of its own, so there is no custom TRNG/PRNG to fail. |
| Is there a silent fallback to a predictable generator? | **No.** The one fallback that exists moves between two cryptographically secure sources, and triggers on a JVM configuration condition — never on entropy failure. |
| Is entropy ever narrowed to 32 bits? | **No.** There is no reseed step at all. A 12-word seed carries a full **128 bits**; a 24-word seed carries 256. Seeds under 128 bits are rejected outright. |
| Is the seed protected on disk? | **Strongly — when a wallet password is set.** Argon2id at 256 MiB / 10 iterations / p=4, comfortably above RFC 9106 and OWASP guidance. The password is optional; see [§6.3](#63-what-a-stolen-wallet-file-actually-exposes) for what an unencrypted file does and does not expose. |
| Does the required BIP39 passphrase actually help? | **Yes, materially.** It is never written to disk, and the master private key is not stored either — so a stolen wallet file does not yield spendable keys without it. |

Two minor hygiene items are noted in [§8](#8-observations-and-hygiene-items); neither is
exploitable in this application's threat model.

**One thing is worth your attention, though it is not a defect:** your passphrase, not the seed,
is what protects you if your seed words are ever discovered — and BIP39 stretches passphrases
weakly by modern standards. A self-invented memorable phrase is not sufficient for that job.
See [§5.1](#51-how-much-does-the-passphrase-actually-add).

---

## 1. Scope

**In scope:** generation of BIP39 seed entropy for a newly created wallet — the entropy source,
its width, and every fallback path reachable from wallet creation — plus the key-derivation and
encryption applied to that seed at rest.

**Out of scope** (not examined for this document): key material lifetime in process memory, the
coinjoin client, and imported/restored wallets, where entropy originates on another device and is
outside this application's control.

---

## 2. The reported Coldcard issue

The disclosure describes two distinct defects that compound:

1. **Predictable RNG fallback.** When the hardware TRNG failed to produce output, the firmware
   fell back to a deterministic PRNG rather than refusing to proceed. The failure was silent.
2. **32-bit reseed.** A reseed path narrowed effective entropy to 32 bits, collapsing the search
   space to roughly 4.3 billion candidates — brute-forceable on commodity hardware.

Assessing Ashigaru Desktop means answering three questions: *where does entropy come from, how
wide is it, and what happens when the preferred source is unavailable?*

---

## 3. How Ashigaru Desktop generates a seed

The "Generate New" path in wallet creation:

```
WalletCreationFlow
  └─ SeedEntryDialog                          (src/main/java/.../control/SeedEntryDialog.java:77)
       └─ MnemonicKeystoreEntryPane.generateNew()
                                              (src/main/java/.../control/MnemonicKeystoreEntryPane.java:57)
            └─ new DeterministicSeed(secureRandom, entropyLength, "")
                 └─ DeterministicSeed.getEntropy()
                                              (drongo/src/main/java/.../wallet/DeterministicSeed.java:112)
```

The entropy draw itself (`DeterministicSeed.java:112-120`):

```java
private static byte[] getEntropy(SecureRandom random, int bits) {
    if(bits > MAX_SEED_ENTROPY_BITS) {
        throw new IllegalArgumentException("Requested entropy size too large");
    }

    byte[] seed = new byte[bits / 8];
    random.nextBytes(seed);
    return seed;
}
```

And the source selection (`MnemonicKeystoreEntryPane.java:57-68`):

```java
int mnemonicSeedLength = wordEntriesProperty.get().size() * 11;
int entropyLength = mnemonicSeedLength - (mnemonicSeedLength/33);

SecureRandom secureRandom;
try {
    secureRandom = SecureRandom.getInstanceStrong();
} catch(NoSuchAlgorithmException e) {
    secureRandom = new SecureRandom();
}
```

Those bytes go straight into the BIP39 mnemonic. There is no intermediate reseed, no mixing
step, and no truncation between `nextBytes()` and the resulting words.

---

## 4. Assessment against each failure mode

### 4.1 Predictable RNG fallback — not applicable

There *is* a fallback in this code, and it is worth being explicit about why it is not the same
class of problem:

- It triggers only on `NoSuchAlgorithmException`, meaning the JVM has no algorithm configured
  under `securerandom.strongAlgorithms` in `java.security`. That is a configuration condition,
  **not** an entropy-exhaustion or hardware-failure condition.
- Both branches are cryptographically secure. `SecureRandom.getInstanceStrong()` and
  `new SecureRandom()` are both CSPRNGs seeded from the operating system entropy pool
  (`/dev/urandom` on Linux/macOS, `BCryptGenRandom`/`CryptGenRandom` on Windows). The fallback
  moves between two secure sources.

The Coldcard defect was a fallback from a hardware TRNG to a *deterministic* PRNG. No branch in
this code path reaches a non-cryptographic generator.

More structurally: this application implements no RNG of its own. Entropy generation is delegated
entirely to the platform, so the class of bug that requires a custom TRNG/PRNG implementation to
get wrong has no surface on which to occur.

### 4.2 32-bit reseed — not applicable

There is no reseed step at all. `nextBytes()` fills the full entropy buffer in a single call and
that buffer becomes the mnemonic. Width is determined solely by the requested word count:

| Words | `entropyLength` | Checksum | Total |
|---|---|---|---|
| 12 | 128 bits | 4 bits | 132 bits (12 × 11) |
| 24 | 256 bits | 8 bits | 264 bits (24 × 11) |

The `mnemonicSeedLength - (mnemonicSeedLength/33)` expression is the standard BIP39 checksum
subtraction (132 − 4 = 128; 264 − 8 = 256).

Two guards additionally prevent an under-width seed from being constructed at all
(`DeterministicSeed.java:67-73`): entropy must be a multiple of 32 bits, and must be at least
`DEFAULT_SEED_ENTROPY_BITS` (128). A 32-bit seed cannot be represented by this type.

### 4.3 Related checks

- **No `SecureRandom.setSeed()` anywhere in the codebase.** On some JVM configurations, seeding a
  `SecureRandom` with a caller-supplied value can constrain its output; no such call exists.
  (Occurrences of `setSeed` in the tree are `Keystore.setSeed(DeterministicSeed)`, an unrelated
  field setter.)
- **No user-supplied entropy mixing.** There is no dice-roll or manual-entropy feature whose
  combination logic could weaken the result.
- **No timestamp or PID seeding.** `System.currentTimeMillis()` appears in `DeterministicSeed`
  only as the wallet creation date recorded for rescan purposes — it never contributes to entropy.

---

## 5. The Ashigaru standard: 12 words + passphrase

Ashigaru's standard configuration is a 12-word seed with a BIP39 passphrase. Ashigaru Desktop's
creation flow **requires a non-empty passphrase** — the "Create Wallet" button stays disabled
until one is entered and confirmed (`WalletCreationFlow.java:356`):

```java
boolean valid = !passField.getText().isEmpty()
        && passField.getText().equals(passConfirmField.getText());
```

The seed itself carries **128 bits** of entropy. For context against the disclosed issue:

| | Search space |
|---|---|
| Coldcard 32-bit reseed | 2³² ≈ 4.3 × 10⁹ |
| Ashigaru 12-word seed | 2¹²⁸ ≈ 3.4 × 10³⁸ |

The passphrase is applied as BIP39 specifies — as PBKDF2 salt (`"mnemonic" + passphrase`),
HMAC-SHA512, 2048 rounds, 512-bit output (`Bip39MnemonicCode.java:28`).

It is worth stating plainly that **the passphrase is not what makes seed generation safe.** 128
bits is already far beyond brute force; the passphrase earns its value elsewhere — against
compromise of the seed words themselves, whether from a photographed backup or a stolen wallet
file (see [§6.3](#63-what-a-stolen-wallet-file-actually-exposes)). Because user-chosen
passphrases typically carry far less entropy than 128 bits, a passphrase should not be treated as
compensating for weak generation — and here it does not need to.

### 5.1 How much does the passphrase actually add?

The intuitive answer is that a 12-word seed (128 bits) plus, say, a 28-bit passphrase gives 156
bits of protection. **That addition is arithmetically correct but practically misleading, and it
is the single most important thing to understand about passphrase strength.**

The two secrets are not layers that stack into one pool. They are relevant in *mutually
exclusive* scenarios, and you never experience their sum:

| Scenario | Seed contributes | Passphrase contributes | Your real security |
|---|---|---|---|
| Attacker has nothing | 128 bits | + its own entropy | **128 bits** — the addition is real but unobservable; 2¹²⁸ was already unreachable |
| Attacker has your seed words | **0 bits** — no longer secret | its own entropy | **the passphrase alone** |

In the first row the passphrase is redundant padding on something already impossible to brute
force. In the second row the seed's 128 bits evaporate — a photographed backup, a discovered
steel plate, or an unencrypted wallet file makes them public — and the passphrase becomes the
*entirety* of what stands between an attacker and the funds.

An analogy: a lock worth 128 and an alarm worth 28 do not combine into "156 of security." If
someone has your key, the lock is worth nothing and only the alarm remains. The alarm never
inherits the lock's strength.

**So the number that matters is not the sum — it is the minimum across scenarios you consider
realistic.** For a self-invented passphrase that minimum is roughly 28 bits, which is not
meaningful protection at all.

This second scenario is where BIP39 shows its age. The passphrase is stretched with
PBKDF2-HMAC-SHA512 at only **2048 iterations** — fast to attack, and orders of magnitude weaker
than the Argon2id used for the wallet file ([§6.1](#61-key-derivation--argon2id)). As an order of
magnitude, a single high-end GPU can test on the order of 10⁶ BIP39 candidates per second, so a
modest multi-GPU rig searches very roughly 2⁴⁸ candidates per year. Exact figures vary with
hardware, but the shape of the guidance does not:

| Passphrase | Approx. entropy | Blind attacker (irrelevant) | **If your seed leaks** |
|---|---|---|---|
| Self-invented memorable phrase | ~28 bits | 2¹⁵⁶ | **Falls in seconds** |
| 4 random diceware words | ~52 bits | 2¹⁸⁰ | Reachable by a well-funded attacker |
| 5 random diceware words | ~65 bits | 2¹⁹³ | Comfortably out of reach |
| 6 random diceware words | ~78 bits | 2²⁰⁶ | Far out of reach |

Note how the third column is uniformly meaningless — every row is unbreakable — while the fourth
column spans "seconds" to "never." All of the real decision lives in the last column.

**Practical guidance:** choose the passphrase assuming your seed words will one day be found. A
phrase you invented yourself is not sufficient for that job; five or six randomly chosen words is
the realistic bar. This is not a shortcoming of Ashigaru — the 2048-round PBKDF2 is fixed by the
BIP39 specification and is identical in every compliant wallet — but it does mean the seed phrase
and the passphrase are not equally hardened, which is a good reason to set a wallet-file password
as well rather than relying on the passphrase alone.

---

## 6. Protection at rest

Generating a strong seed matters little if it is poorly protected on disk. For a desktop wallet
this is the more realistic attack surface: an adversary with file access attacking the password
offline, rather than attacking 128-bit entropy directly.

**This section describes Ashigaru Desktop (and Terminal, which shares the same storage layer)
only.** Ashigaru Mobile protects wallet data through the Android platform — PIN and OS-level
keystore facilities — which is a different mechanism and was not examined here. The
entropy findings in [§3](#3-how-ashigaru-desktop-generates-a-seed)–[§4](#4-assessment-against-each-failure-mode)
do apply to all three.

### 6.1 Key derivation — Argon2id

The wallet password is stretched with Argon2id (`drongo/.../crypto/Argon2KeyDeriver.java:11`):

```java
public static final Argon2Parameters SPRW1_PARAMETERS =
        new Argon2Parameters(16, 32, 10, 256 * 1024, 4);
//                            │   │   │        │     └─ parallelism = 4
//                            │   │   │        └─ memory = 256 MiB
//                            │   │   └─ iterations = 10
//                            │   └─ hash length = 32 bytes
//                            └─ salt length = 16 bytes
```

These parameters are strong — materially above published guidance rather than merely meeting it:

| Source | Recommendation | Ashigaru |
|---|---|---|
| RFC 9106 (second recommended option) | Argon2id, 64 MiB, t=3, p=4 | **256 MiB, t=10, p=4** |
| OWASP Password Storage Cheat Sheet (minimum) | Argon2id, 19 MiB, t=2, p=1 | **256 MiB, t=10, p=4** |

Argon2**id** is the correct variant choice — the hybrid mode resistant to both side-channel
attacks on data-dependent addressing and to GPU/time-memory trade-off attacks.

The 16-byte salt is drawn per wallet from `new SecureRandom()` and stored in the wallet file
header (`DbPersistence.java:520-530`).

### 6.2 Encryption — ECIES and H2

Current-format wallets are H2 databases opened with `CIPHER=AES` (`DbPersistence.java:700`),
keyed by the Argon2id-derived key. The ECIES crypter used for wallet payload encryption
(`drongo/.../crypto/ECIESKeyCrypter.java`) is sound on the points that most often go wrong:

- **Fresh ephemeral key per encryption** (`ECIESKeyCrypter.java:61`), so the derived IV is never
  reused across encryptions.
- **Encrypt-then-MAC** — HMAC-SHA256 over `magic ‖ ephemeralPubKey ‖ ciphertext`, the correct
  composition order.
- **MAC verified before decryption** (`ECIESKeyCrypter.java:47-49`), which is what neutralises
  padding-oracle attacks against the underlying AES-CBC/PKCS7 layer.

### 6.3 What a stolen wallet file actually exposes

The wallet-file password is **optional** in the creation flow ("leave blank for no password",
`WalletCreationFlow.java:571`), so the Argon2id protection above only applies when a user sets
one. Two design properties limit the consequences when they do not:

- **The BIP39 passphrase is never persisted.** The `seed` table stores only a `needsPassphrase`
  boolean alongside the mnemonic — there is no passphrase column anywhere in the schema
  (`V1__Initial.sql`), and the persistence mappers read back only that flag
  (`KeystoreMapper.java:45,53`).
- **The master private key is not stored for seed-based wallets.** `Keystore.fromSeed()` leaves
  `masterPrivateExtendedKey` unset (`Keystore.java:350-356`); `getMasterPrivateKey()` re-derives
  it from the seed at runtime (`Keystore.java:164-175`), which requires the passphrase supplied by
  the user each time the wallet is opened.

The practical consequence: an attacker who obtains an unencrypted Ashigaru wallet file recovers
the 12 mnemonic words, but **not** spendable keys — the required passphrase is a genuine second
factor at rest, not merely a formality.

That attacker does, however, recover the extended public key (`keystore.extendedPublicKey`), which
discloses the wallet's full transaction history and future addresses. **An unencrypted wallet file
is therefore a privacy compromise even though it is not a loss-of-funds compromise** — setting a
wallet password remains worthwhile.

---

## 7. Cross-check: Terminal and Mobile

The same question was checked across the other Ashigaru codebases so the conclusion is not
specific to one entry point:

- **Ashigaru Terminal** — identical code. Same `drongo` `DeterministicSeed.getEntropy()`
  implementation and the same `getInstanceStrong()`/`new SecureRandom()` selection in
  `MnemonicKeystoreEntryPane`, `MnemonicKeystoreImportPane`, and `Bip39Dialog`.
- **Ashigaru Mobile** — a separate implementation reaching the same result.
  `HD_WalletFactoryGeneric.newWallet()` uses `new SecureRandom()` and `random.nextBytes()` into a
  16-byte (12-word) or 32-byte (24-word) buffer. Every call site passes 12 words explicitly, so
  the standard configuration is enforced at the call site rather than offered as a choice.

**A non-empty BIP39 passphrase is enforced in all three**, each at its own entry point:

| Product | Enforcement |
|---|---|
| Desktop | `WalletCreationFlow.java:356` — "Create Wallet" stays disabled until entered and confirmed |
| Terminal | `Bip39Dialog.java:108` — rejects with *"Passphrase can not be empty."* |
| Mobile | `CreateWalletActivity.java:150` — blocks the creation wizard from advancing |

Note one difference in surface, not in security: Mobile fixes the seed at 12 words, while Desktop
offers 12 or 24 in the creation dialog. Both produce full-width entropy for the length selected.

---

## 8. Observations and hygiene items

None of the following are exploitable in this application's threat model. They are recorded for
completeness.

1. **MAC comparison is not constant-time.** `ECIESKeyCrypter.java:47` uses `Arrays.equals()` to
   compare the HMAC, which short-circuits on first mismatch. Exploiting this would require an
   oracle the attacker can query repeatedly while measuring timing; an attacker who possesses the
   wallet file computes candidate MACs offline instead, so no such oracle exists here.
   `MessageDigest.isEqual()` would remove the question entirely.
2. **No KDF parameter agility.** Argon2 parameters are compiled in and recomputed at open time
   rather than stored in the wallet file, so raising them in future requires a migration path for
   existing wallets (the `SPRW1` prefix suggests this versioning was anticipated). The same
   property is also a security benefit: because parameters are never read from the file, a
   tampered file cannot force a downgrade to weaker parameters.
3. **Test parameters exist in the same class.** `TEST_PARAMETERS` (1 iteration, 1 MiB, p=1) is
   selected when the JVM system property `org.gradle.test.worker` is set. Setting that property
   requires control of the launch command — i.e. code execution the attacker already has — and
   because parameters are not stored, a wallet written under test parameters simply fails to open
   normally rather than silently weakening.

Separately, two uses of non-cryptographic `java.util.Random` exist in `drongo`. Neither touches
key material: `StonewallUtxoSelector` (`StonewallUtxoSelector.java:13`) uses `new Random(42)` with
a comment stating deterministic UTXO selection is intended, and
`Wallet.applySequenceAntiFeeSniping()` uses `new Random()` for locktime randomisation. Both sit on
spend paths, which Ashigaru Desktop does not expose.

---

## 9. Conclusion

Ashigaru Desktop, Terminal, and Mobile all draw seed entropy directly from the platform CSPRNG at
the full width implied by the chosen mnemonic length, with no custom RNG, no reseed, no narrowing,
and no path to a non-cryptographic source. The Coldcard failure modes — a silent fallback to a
predictable generator, and a reseed collapsing the keyspace to 32 bits — have no analogue in this
code. A standard Ashigaru wallet is a 12-word seed carrying a full 128 bits of entropy, with a
mandatory BIP39 passphrase as an independent second secret.

Key storage is likewise sound: Argon2id parameters exceed current published guidance, the ECIES
construction gets composition order and IV handling right, and the required BIP39 passphrase is
never written to disk — meaning even an unencrypted wallet file does not surrender spendable keys.
