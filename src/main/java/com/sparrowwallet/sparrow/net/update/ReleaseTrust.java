package com.sparrowwallet.sparrow.net.update;

/**
 * The trust anchors for in-app updates, pinned into the binary at build time.
 *
 * <p>These are deliberately compiled constants rather than configuration. The point of the update
 * feature is that a user who obtains a genuine build once never has to find the download site
 * again: the repository it asks and the key it trusts travel with the binary, so neither a hostile
 * network nor a convincing fork of the project can redirect an existing install.
 *
 * <p>Note what that does and does not buy. Pinning protects <em>continuity</em>, not
 * <em>acquisition</em> - a build that was already a fork pins the attacker's repository and key.
 * It makes the first download the only one that has to be got right.
 */
public class ReleaseTrust {
    /**
     * The BIP47 payment code whose notification address signs every release. Releases are accepted
     * only if the signature over MESSAGE.txt resolves to this identity; if the signing key is ever
     * rotated, builds pinned to the old code fail closed and their users must re-download manually.
     *
     * <p>This code derives to notification address
     * {@code 1K8CDoBYWBuaeAhejLAk5hiACAgbbPnDCJ}, which is the address that signed the v1.1.2
     * release. Recorded here so the pin can be audited by eye: the address is <em>not</em> used as a
     * second trust anchor, since it is derived from the code above at verification time. If you
     * change this constant, confirm the new code derives to the address you actually sign with -
     * Tools -&gt; Verifier against a published RELEASE-BIP47-SIGNATURE.txt does it.
     */
    public static final String RELEASE_SIGNING_PAYMENT_CODE =
            "PM8TJM51x2mDd85CzEgVc2y7vdyB3eBj93JVjVtCt6PZtmfzhFzYPMXYBXh28zthWhVKGjVQZPT1MKxGxEtfenLYEkuc5GhoWtMzQCF8c8mrckYFM7r1";

    public static final String GITHUB_OWNER = "linkinparkrulz";
    public static final String GITHUB_REPO = "ashigaru-desktop";

    public static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";

    public static final String RELEASES_PAGE_URL =
            "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases";

    /** Asset names published alongside the binaries that carry the attestation, not the payload. */
    public static final String SUMS_ASSET = "SHA256SUMS";
    public static final String MESSAGE_ASSET = "MESSAGE.txt";
    public static final String SIGNATURE_ASSET = "RELEASE-BIP47-SIGNATURE.txt";

    private ReleaseTrust() {}
}
