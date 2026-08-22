package com.sparrowwallet.sparrow.net.update;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Picks the release asset that most likely matches how this copy of Ashigaru was installed.
 *
 * <p>The app cannot know for certain: jpackage leaves no marker saying whether it arrived as a .deb,
 * an .rpm, a tarball or an AppImage, and nothing records the installer that ran on Windows. So this
 * makes the best inference available and the UI presents every candidate with that one preselected,
 * leaving the user to correct it. A wrong guess is then a visible dropdown, not a wrong download.
 */
public class ReleaseArtifactSelector {
    /** A downloadable asset from the release, and whether it is the inferred match. */
    public record Candidate(String name, String url, long size, boolean suggested) {}

    public enum Family { WINDOWS, MACOS, LINUX, UNKNOWN }

    /**
     * Filters {@code assets} to those installable on this platform and marks one as suggested.
     * Attestation files are excluded - they are downloaded separately, not offered as the payload.
     */
    public static List<Candidate> select(List<ReleaseAsset> assets) {
        return select(assets, currentFamily(), System.getProperty("os.arch"), new Environment());
    }

    static List<Candidate> select(List<ReleaseAsset> assets, Family family, String osArch, Environment env) {
        List<ReleaseAsset> installable = new ArrayList<>();
        for(ReleaseAsset asset : assets) {
            if(!isAttestation(asset.name()) && matchesFamily(asset.name(), family)) {
                installable.add(asset);
            }
        }

        String suggested = suggestName(installable, family, osArch, env);
        List<Candidate> candidates = new ArrayList<>();
        for(ReleaseAsset asset : installable) {
            candidates.add(new Candidate(asset.name(), asset.url(), asset.size(), asset.name().equals(suggested)));
        }

        return candidates;
    }

    private static boolean isAttestation(String name) {
        return name.equals(ReleaseTrust.SUMS_ASSET)
                || name.equals(ReleaseTrust.MESSAGE_ASSET)
                || name.equals(ReleaseTrust.SIGNATURE_ASSET)
                || name.startsWith("RELEASE-");
    }

    private static boolean matchesFamily(String name, Family family) {
        String lower = name.toLowerCase(Locale.ROOT);
        return switch(family) {
            case WINDOWS -> lower.endsWith(".exe") || lower.endsWith(".msi");
            case MACOS -> lower.endsWith(".dmg") || lower.endsWith("-osx-x86_64.zip") || lower.endsWith("-osx-aarch64.zip");
            case LINUX -> lower.endsWith(".deb") || lower.endsWith(".rpm")
                    || lower.endsWith(".appimage") || lower.endsWith(".tar.gz");
            case UNKNOWN -> true;
        };
    }

    private static String suggestName(List<ReleaseAsset> assets, Family family, String osArch, Environment env) {
        return switch(family) {
            //The .exe is the installer the download table leads with; .msi is the alternative.
            case WINDOWS -> firstEndingWith(assets, ".exe");
            case MACOS -> firstEndingWith(assets, macArchSuffix(osArch) + ".dmg");
            case LINUX -> suggestLinux(assets, env);
            case UNKNOWN -> null;
        };
    }

    /** Apple Silicon builds are published as -aarch64, Intel as -x86_64. */
    private static String macArchSuffix(String osArch) {
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        return arch.contains("aarch64") || arch.contains("arm") ? "-aarch64" : "-x86_64";
    }

    /**
     * An AppImage announces itself through the APPIMAGE variable it sets for the running process.
     * Failing that, os-release distinguishes the Debian family from the RPM ones. Anything else
     * falls back to the portable tarball, which works everywhere.
     */
    private static String suggestLinux(List<ReleaseAsset> assets, Environment env) {
        if(env.getenv("APPIMAGE") != null) {
            String appImage = firstEndingWith(assets, ".AppImage");
            if(appImage != null) {
                return appImage;
            }
        }

        String osRelease = env.readOsRelease();
        if(osRelease != null) {
            String ids = idFields(osRelease);
            if(ids.contains("debian") || ids.contains("ubuntu") || ids.contains("mint")) {
                String deb = firstEndingWith(assets, ".deb");
                if(deb != null) {
                    return deb;
                }
            }
            if(ids.contains("fedora") || ids.contains("rhel") || ids.contains("centos")
                    || ids.contains("suse") || ids.contains("mandriva")) {
                String rpm = firstEndingWith(assets, ".rpm");
                if(rpm != null) {
                    return rpm;
                }
            }
        }

        return firstEndingWith(assets, ".tar.gz");
    }

    /** Collects the ID and ID_LIKE values from an os-release file into one lowercase string. */
    private static String idFields(String osRelease) {
        StringBuilder ids = new StringBuilder();
        for(String line : osRelease.split("\\R")) {
            String trimmed = line.trim();
            if(trimmed.startsWith("ID=") || trimmed.startsWith("ID_LIKE=")) {
                ids.append(trimmed.substring(trimmed.indexOf('=') + 1).replace("\"", "").toLowerCase(Locale.ROOT)).append(' ');
            }
        }
        return ids.toString();
    }

    private static String firstEndingWith(List<ReleaseAsset> assets, String suffix) {
        String lowerSuffix = suffix.toLowerCase(Locale.ROOT);
        for(ReleaseAsset asset : assets) {
            //The desktop build is preferred over the headless server package of the same format
            if(asset.name().toLowerCase(Locale.ROOT).endsWith(lowerSuffix) && !isHeadless(asset.name())) {
                return asset.name();
            }
        }
        return null;
    }

    private static boolean isHeadless(String name) {
        return name.toLowerCase(Locale.ROOT).contains("ashigaru-server");
    }

    static Family currentFamily() {
        String os = System.getProperty("os.name");
        if(os == null) {
            return Family.UNKNOWN;
        }

        String lower = os.toLowerCase(Locale.ROOT);
        if(lower.startsWith("windows")) {
            return Family.WINDOWS;
        }
        if(lower.startsWith("mac") || lower.contains("darwin")) {
            return Family.MACOS;
        }
        if(lower.contains("linux") || lower.contains("unix")) {
            return Family.LINUX;
        }
        return Family.UNKNOWN;
    }

    /** Seam for the two pieces of ambient Linux state, so selection can be tested. */
    static class Environment {
        String getenv(String name) {
            return System.getenv(name);
        }

        String readOsRelease() {
            for(String path : new String[] {"/etc/os-release", "/usr/lib/os-release"}) {
                try {
                    Path file = new File(path).toPath();
                    if(Files.isReadable(file)) {
                        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    }
                } catch(Exception e) {
                    //unreadable, try the next
                }
            }
            return null;
        }
    }

    private ReleaseArtifactSelector() {}
}
