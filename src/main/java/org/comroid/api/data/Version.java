package org.comroid.api.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Integer.*;
import static org.comroid.api.Polyfill.*;

public final class Version implements Comparable<Version> {
    public static final Pattern     PATTERN = Pattern.compile(
            "(?<major>\\d+)\\.(?<minor>\\d+)\\.?(?<patch>\\d+)?[.\\-_]?(?<candidate>(s(nap" +
            "(shot)?)?|a(lpha)?|b(eta)?|(r(elease)?|c?))+)?[.\\-_]?(?<hotfix>\\d+)?");
    private final       int         major;
    private final       int         minor;
    private final       int         patch;
    private final       ReleaseType releaseType;
    private final       int         hotfix;

    public Version(int major, int minor, int patch, ReleaseType releaseType, int hotfix) {
        this.major  = major;
        this.minor  = minor;
        this.patch  = patch;
        this.releaseType = releaseType;
        this.hotfix = hotfix;
    }

    public Version(String version) {
        final Matcher matcher = PATTERN.matcher(version.toLowerCase());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Version \"" + version + "\" does not match pattern: " + PATTERN.pattern());
        }

        this.major = parseInt(regexGroupOrDefault(matcher, "major", null));
        this.minor = parseInt(regexGroupOrDefault(matcher, "minor", null));

        this.patch  = parseInt(regexGroupOrDefault(matcher, "patch", "0"));
        this.releaseType = ReleaseType.parse(regexGroupOrDefault(matcher, "candidate", "rc"));
        this.hotfix = parseInt(regexGroupOrDefault(matcher, "hotfix", "0"));
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public int getHotfix() {
        return hotfix;
    }

    @Override
    @Contract(pure = true)
    public int compareTo(@NotNull Version other) {
        if (major != other.major) {
            return major - other.major;
        }

        if (minor != other.minor) {
            return minor - other.minor;
        }

        if (patch != other.patch) {
            return patch - other.patch;
        }

        if (releaseType != other.releaseType) {
            return releaseType.ordinal() - other.releaseType.ordinal();
        }

        if (hotfix != other.hotfix) {
            return hotfix - other.hotfix;
        }

        return 0;
    }

    @Override
    public String toString() {
        if (patch == 0 && hotfix == 0) {
            return String.format("%d.%d-%s", major, minor, releaseType);
        }

        if (hotfix == 0) {
            return String.format("%d.%d.%d-%s", major, minor, patch, releaseType);
        }
        if (patch == 0) {
            return String.format("%d.%d-%s_%d", major, minor, releaseType, hotfix);
        }

        return String.format("%d.%d.%d-%s_%d", major, minor, patch, releaseType, hotfix);
    }

    public String toSimpleString() {
        return String.format("%d.%d", major, minor);
    }

    public enum ReleaseType {
        SNAPSHOT("s", "snap", "snapshot"),

        ALPHA("a", "alpha"),

        BETA("b", "beta"),

        RELEASE("r", "rc", "release");

        private final String[] idents;

        ReleaseType(String... idents) {
            this.idents = idents;
        }

        private static ReleaseType parse(String candidate) {
            if (candidate.isEmpty()) {
                return RELEASE;
            }

            return Stream.of(values())
                    .filter(rt -> Arrays.asList(rt.idents)
                            .contains(candidate))
                    .findAny()
                    .orElse(RELEASE);
        }
    }

    public interface Container {
        Version getVersion();
    }
}
