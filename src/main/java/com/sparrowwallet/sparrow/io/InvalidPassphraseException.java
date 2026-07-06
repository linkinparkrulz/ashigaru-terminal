package com.sparrowwallet.sparrow.io;

/**
 * Thrown when a BIP39 passphrase entered while opening an existing wallet does not reproduce that
 * wallet's stored extended public key — i.e. the wrong passphrase was entered. Unchecked so it can
 * be thrown from {@code Storage.restorePublicKeysFromSeed} without widening its checked signature.
 */
public class InvalidPassphraseException extends RuntimeException {
    public InvalidPassphraseException(String message) {
        super(message);
    }
}
