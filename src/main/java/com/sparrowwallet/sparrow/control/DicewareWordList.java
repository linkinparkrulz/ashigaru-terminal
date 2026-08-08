package com.sparrowwallet.sparrow.control;

import com.sparrowwallet.drongo.wallet.Bip39MnemonicCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the EFF "large" diceware wordlist (7776 = 6^5 words) bundled at
 * {@code eff-large.txt} (co-located with this class), verifying its integrity with a hardcoded SHA-256 digest so a
 * tampered list fails fast. Modelled on {@link com.sparrowwallet.drongo.wallet.Bip39MnemonicCode}.
 *
 * <p>The wordlist is the EFF Long Wordlist by the Electronic Frontier Foundation, made available
 * under CC BY 3.0 US (https://creativecommons.org/licenses/by/3.0/us/). Source: https://www.eff.org/dice
 *
 * <p>Each line is {@code <five-digit roll>\t<word>}, where the roll is five digits each 1-6 (the
 * result of rolling five physical dice), e.g. {@code 11111\tabacus} ... {@code 66666\tzoom}. No
 * random generation happens here: entropy comes solely from the user's physical dice.
 */
public class DicewareWordList {
    private static final Logger log = LoggerFactory.getLogger(DicewareWordList.class);

    // Resolved relative to this class's package (co-located resource) to avoid a JPMS split package
    // with drongo's /wordlist package.
    private static final String EFF_LARGE_RESOURCE_NAME = "eff-large.txt";
    // SHA-256 of the concatenated word column (no separators), matching the read loop below.
    private static final String EFF_LARGE_SHA256 = "68bf9af582305329a9b30476ff584c7077aecefa4e4644e54b8ab422b0c66872";

    /** Number of words in the EFF large list: 6^5, so each word contributes exactly 5 dice of entropy. */
    public static final int WORD_COUNT = 7776;
    /** Exact bits of entropy contributed by each independently-rolled word: log2(7776) ~= 12.9248. */
    public static final double BITS_PER_WORD = Math.log(WORD_COUNT) / Math.log(2);

    public static DicewareWordList INSTANCE;

    static {
        try {
            INSTANCE = new DicewareWordList();
        } catch(RuntimeException e) {
            log.error("Failed to load EFF diceware wordlist", e);
        }
    }

    private final List<String> words;         // index 0..7775, in canonical (11111..66666) order
    private final Map<String, String> byRoll; // "11111" -> "abacus"
    private final Set<String> wordSet;        // for O(1) membership checks

    private DicewareWordList() {
        InputStream stream = DicewareWordList.class.getResourceAsStream(EFF_LARGE_RESOURCE_NAME);
        if(stream == null) {
            throw new IllegalStateException(new FileNotFoundException(EFF_LARGE_RESOURCE_NAME));
        }

        List<String> wordList = new ArrayList<>(WORD_COUNT);
        Map<String, String> rollMap = new HashMap<>(WORD_COUNT * 2);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String line;
            while((line = br.readLine()) != null) {
                if(line.isEmpty()) {
                    continue;
                }
                int tab = line.indexOf('\t');
                String roll = tab >= 0 ? line.substring(0, tab) : null;
                String word = tab >= 0 ? line.substring(tab + 1) : line;
                md.update(word.getBytes(StandardCharsets.UTF_8));
                wordList.add(word);
                if(roll != null) {
                    rollMap.put(roll, word);
                }
            }

            if(wordList.size() != WORD_COUNT) {
                throw new IllegalStateException("EFF wordlist did not contain " + WORD_COUNT + " words (found " + wordList.size() + ")");
            }

            String hexDigest = HexFormat.of().formatHex(md.digest());
            if(!hexDigest.equals(EFF_LARGE_SHA256)) {
                throw new IllegalStateException("EFF wordlist digest mismatch");
            }
        } catch(IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error loading EFF wordlist", e);
        }

        this.words = Collections.unmodifiableList(wordList);
        this.byRoll = Collections.unmodifiableMap(rollMap);
        this.wordSet = Collections.unmodifiableSet(new HashSet<>(wordList));
    }

    /**
     * The wordlist in canonical order (index 0 == roll 11111 == "abacus").
     */
    public List<String> getWords() {
        return words;
    }

    /**
     * Maps five physical die rolls to the corresponding EFF word.
     *
     * @param fiveDigits exactly five characters, each '1'..'6', in the order the dice were read
     * @return the mapped word, or empty if the input is not five valid die digits
     */
    public Optional<String> wordForRoll(String fiveDigits) {
        if(fiveDigits == null || fiveDigits.length() != 5) {
            return Optional.empty();
        }
        for(int i = 0; i < fiveDigits.length(); i++) {
            char c = fiveDigits.charAt(i);
            if(c < '1' || c > '6') {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(byRoll.get(fiveDigits));
    }

    /**
     * Whether the given (already lower-cased) token is one of the EFF diceware words.
     */
    public boolean contains(String word) {
        return word != null && wordSet.contains(word);
    }

    /**
     * A light, honest weakness check for a hand-typed passphrase. It flags only the unambiguous
     * failures and is purely advisory — callers must never block on it. A blank passphrase is a valid
     * choice (no passphrase) and returns no warning. Strong passphrases (including diceware phrases)
     * return empty. Uses the already-bundled BIP39 and EFF wordlists for the single-dictionary-word
     * check, so it adds no dependency.
     */
    public static Optional<String> passphraseWeakness(String passphrase) {
        if(passphrase == null) {
            return Optional.empty();
        }
        String trimmed = passphrase.strip();
        if(trimmed.isEmpty()) {
            return Optional.empty();
        }
        if(trimmed.length() < 8) {
            return Optional.of("Short passphrase — consider rolling a diceware passphrase.");
        }
        if(trimmed.chars().distinct().count() == 1) {
            return Optional.of("Repeated characters — weak. Consider rolling a diceware passphrase.");
        }
        if(!trimmed.contains(" ")) {
            String lower = trimmed.toLowerCase();
            boolean effWord = INSTANCE != null && INSTANCE.contains(lower);
            boolean bip39Word = Bip39MnemonicCode.INSTANCE != null && Bip39MnemonicCode.INSTANCE.getWordList().contains(lower);
            if(effWord || bip39Word) {
                return Optional.of("Single dictionary word — weak. Consider rolling a diceware passphrase.");
            }
        }
        return Optional.empty();
    }
}
