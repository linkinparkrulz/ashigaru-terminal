package com.sparrowwallet.sparrow.net.update;

import com.sparrowwallet.drongo.address.Address;
import com.sparrowwallet.drongo.bip47.PaymentCode;
import com.sparrowwallet.sparrow.net.Version;
import com.sparrowwallet.sparrow.net.dojo.SignedMessageVerifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies a downloaded release against the attestation the release workflow publishes alongside it.
 *
 * <p>The chain has four links, and all four must hold:
 *
 * <ol>
 *   <li><b>Signature</b> - MESSAGE.txt was signed by the notification address of the pinned
 *       release-signing payment code. Everything downstream is then read out of that signed text,
 *       so MESSAGE.txt needs no independent trust: tampering with it changes the address recovered
 *       from the signature.</li>
 *   <li><b>Commitment</b> - the SHA256SUMS actually downloaded hashes to the value committed to
 *       inside the signed message.</li>
 *   <li><b>Version</b> - the signed message names the release being offered, and that release is
 *       newer than the running one. An old release is still validly signed, so without this an
 *       attacker could serve a genuine but vulnerable earlier version.</li>
 *   <li><b>Artifact</b> - the downloaded file hashes to its entry in that SHA256SUMS.</li>
 * </ol>
 *
 * <p>This class is deliberately free of JavaFX and of any I/O beyond reading the file it is asked
 * to hash, so the whole chain can be exercised in tests.
 */
public class ReleaseVerifier {
    /** Which link of the chain broke, for both the UI checklist and the failure message. */
    public enum Step {
        SIGNATURE("Signed by the Ashigaru release key"),
        COMMITMENT("Checksums match the signed message"),
        VERSION("Release is the one offered, and is newer"),
        ARTIFACT("Download matches its published checksum");

        private final String description;

        Step(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public record Attestation(String version, String tag, String commit, String sumsHash) {}

    public record Result(boolean verified, Step failedAt, String detail, Attestation attestation) {
        public static Result ok(Attestation attestation) {
            return new Result(true, null, null, attestation);
        }

        public static Result failed(Step step, String detail) {
            return new Result(false, step, detail, null);
        }
    }

    private static final Pattern VERSION_LINE = Pattern.compile("(?m)^\\s*Version:\\s*(\\S+)\\s*$");
    private static final Pattern TAG_LINE = Pattern.compile("(?m)^\\s*Tag:\\s*(\\S+)\\s*$");
    private static final Pattern COMMIT_LINE = Pattern.compile("(?m)^\\s*Commit:\\s*(\\S+)\\s*$");
    private static final Pattern SUMS_HASH_LINE = Pattern.compile("(?m)^\\s*SHA256\\(SHA256SUMS\\):\\s*([0-9a-fA-F]{64})\\s*$");
    private static final Pattern SIGNATURE_LABEL = Pattern.compile("(?m)^\\s*Signature:\\s*$\\s*(\\S+)");
    private static final Pattern ARMOUR_MARKER = Pattern.compile("-----BEGIN BITCOIN SIGNED MESSAGE-----", Pattern.CASE_INSENSITIVE);

    /**
     * Runs the whole chain. {@code sumsBytes} is the SHA256SUMS exactly as downloaded - it is hashed,
     * so it must not be round-tripped through any normalising read.
     */
    public static Result verify(String messageText, String signatureText, byte[] sumsBytes,
                                File artifact, String artifactName,
                                String offeredTag, String currentVersion) {
        //1. Signature. Establish the signed text first; everything after this is read out of it.
        String signedMessage;
        try {
            signedMessage = verifySignature(messageText, signatureText);
        } catch(VerificationException e) {
            return Result.failed(Step.SIGNATURE, e.getMessage());
        }

        Attestation attestation = parseMessage(signedMessage);
        if(attestation == null) {
            return Result.failed(Step.SIGNATURE, "The signed message is not a release attestation");
        }

        //2. Commitment.
        String actualSumsHash = sha256(sumsBytes);
        if(!actualSumsHash.equalsIgnoreCase(attestation.sumsHash())) {
            return Result.failed(Step.COMMITMENT,
                    "SHA256SUMS hashes to " + actualSumsHash + " but the signed message commits to " + attestation.sumsHash());
        }

        //3. Version, including downgrade protection.
        if(!normaliseTag(attestation.tag()).equals(normaliseTag(offeredTag))) {
            return Result.failed(Step.VERSION,
                    "The signed message is for " + attestation.tag() + ", not the offered " + offeredTag);
        }

        try {
            Version offered = new Version(stripLeadingV(attestation.version()));
            Version running = new Version(stripLeadingV(currentVersion));
            if(offered.compareTo(running) <= 0) {
                return Result.failed(Step.VERSION,
                        attestation.version() + " is not newer than the running " + currentVersion);
            }
        } catch(IllegalArgumentException e) {
            return Result.failed(Step.VERSION, "Could not compare versions: " + e.getMessage());
        }

        //4. Artifact.
        String expected = findSumsEntry(new String(sumsBytes, StandardCharsets.UTF_8), artifactName);
        if(expected == null) {
            return Result.failed(Step.ARTIFACT, artifactName + " is not listed in SHA256SUMS");
        }

        String actual;
        try {
            actual = sha256(artifact);
        } catch(IOException e) {
            return Result.failed(Step.ARTIFACT, "Could not read the download: " + e.getMessage());
        }

        if(!actual.equalsIgnoreCase(expected)) {
            return Result.failed(Step.ARTIFACT,
                    "The download hashes to " + actual + " but SHA256SUMS lists " + expected);
        }

        return Result.ok(attestation);
    }

    private static class VerificationException extends Exception {
        VerificationException(String message) {
            super(message);
        }
    }

    /**
     * Returns the exact text the signature covers, having confirmed it was signed by the pinned
     * release key. An armoured block carries its own message and is preferred over the separately
     * downloaded MESSAGE.txt, so the verified text and the acted-on text cannot differ.
     */
    private static String verifySignature(String messageText, String signatureText) throws VerificationException {
        if(signatureText == null || signatureText.isBlank()) {
            return failSignature("No release signature was published with this release");
        }

        String signature;
        String message = messageText;
        if(ARMOUR_MARKER.matcher(signatureText).find()) {
            try {
                SignedMessageVerifier.ParsedBlock parsed = SignedMessageVerifier.parse(signatureText);
                message = parsed.message();
                signature = parsed.signature();
            } catch(Exception e) {
                return failSignature("The release signature could not be parsed: " + e.getMessage());
            }
        } else {
            Matcher matcher = SIGNATURE_LABEL.matcher(signatureText);
            if(!matcher.find()) {
                return failSignature("The release signature file has no Signature: entry");
            }
            signature = matcher.group(1);
        }

        if(message == null || message.isBlank()) {
            return failSignature("No signed message was published with this release");
        }

        Address expected;
        try {
            expected = new PaymentCode(ReleaseTrust.RELEASE_SIGNING_PAYMENT_CODE).getNotificationAddress();
        } catch(Exception e) {
            return failSignature("The pinned release signing payment code is unusable: " + e.getMessage());
        }

        //Signing tools disagree about a trailing newline. Trying both encodings of the same content
        //resolves that without accepting any different content.
        for(String candidate : new String[] {message, message.stripTrailing()}) {
            try {
                if(expected.equals(SignedMessageVerifier.recoverAddress(candidate, signature))) {
                    return candidate;
                }
            } catch(Exception e) {
                //try the next encoding, then fall through to the failure below
            }
        }

        return failSignature("The signature does not match the release signing key this build trusts."
                + " If the signing key was rotated, download the new version manually from "
                + ReleaseTrust.RELEASES_PAGE_URL);
    }

    private static String failSignature(String detail) throws VerificationException {
        throw new VerificationException(detail);
    }

    /** Reads the attestation fields out of a MESSAGE.txt body. Returns null if it is not one. */
    public static Attestation parseMessage(String text) {
        if(text == null) {
            return null;
        }

        Matcher version = VERSION_LINE.matcher(text);
        Matcher tag = TAG_LINE.matcher(text);
        Matcher sumsHash = SUMS_HASH_LINE.matcher(text);
        if(!version.find() || !tag.find() || !sumsHash.find()) {
            return null;
        }

        Matcher commit = COMMIT_LINE.matcher(text);
        return new Attestation(version.group(1), tag.group(1),
                commit.find() ? commit.group(1) : null, sumsHash.group(1));
    }

    /**
     * Returns the hash SHA256SUMS lists for the given file name, or null. Entries are matched on the
     * exact name, since release assets are served flat and that is the name the file was downloaded
     * under.
     */
    public static String findSumsEntry(String sumsText, String name) {
        if(sumsText == null || name == null) {
            return null;
        }

        for(String line : sumsText.split("\\R")) {
            String trimmed = line.trim();
            if(trimmed.isEmpty()) {
                continue;
            }

            int split = trimmed.indexOf(' ');
            if(split < 0) {
                continue;
            }

            String hash = trimmed.substring(0, split);
            String entry = trimmed.substring(split).trim();
            //sha256sum marks binary mode with a leading * on the name
            if(entry.startsWith("*")) {
                entry = entry.substring(1);
            }

            if(entry.equals(name) && hash.matches("[0-9a-fA-F]{64}")) {
                return hash;
            }
        }

        return null;
    }

    public static String sha256(byte[] bytes) {
        return toHex(newDigest().digest(bytes));
    }

    public static String sha256(File file) throws IOException {
        MessageDigest digest = newDigest();
        try(InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for(byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return builder.toString();
    }

    private static String normaliseTag(String tag) {
        return tag == null ? "" : stripLeadingV(tag.trim()).toLowerCase(Locale.ROOT);
    }

    public static String stripLeadingV(String value) {
        if(value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.startsWith("v") || trimmed.startsWith("V") ? trimmed.substring(1) : trimmed;
    }

    private ReleaseVerifier() {}
}
