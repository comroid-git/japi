package org.comroid.api;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.With;
import org.comroid.annotations.Ignore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

@With
@Value
@Builder
@Ignore
public class MimeType {
    public static final Pattern PATTERN = Pattern.compile(
            "(?<type>[\\w-_]+)/((?<tree>[\\w-_.]+)\\.)?(?<subtype>[\\w-_]+)(\\+(?<suffix>[\\w-_]+))?(?<args>;[\\w-_]+)*");

    public static final MimeType GRAPHQL = parse("application/graphql");
    public static final MimeType JAVASCRIPT = parse("application/javascript");
    public static final MimeType JSON = parse("application/json");
    public static final MimeType MSWORD = parse("application/msword");
    public static final MimeType PDF = parse("application/pdf");
    public static final MimeType SQL = parse("application/sql");
    public static final MimeType URLENCODED = parse("application/x-www-form-urlencoded");
    public static final MimeType XML = parse("application/xml");
    public static final MimeType ZIP = parse("application/zip");
    public static final MimeType ZSTD = parse("application/zstd");
    public static final MimeType MPEG = parse("audio/mpeg");
    public static final MimeType OGG = parse("audio/ogg");
    public static final MimeType APNG = parse("image/apng");
    public static final MimeType GIF = parse("image/gif");
    public static final MimeType JPEG = parse("image/jpeg");
    public static final MimeType PNG = parse("image/png");
    public static final MimeType SVG = parse("image/svg+xml");
    public static final MimeType DATA = parse("multipart/form-data");
    public static final MimeType CSS = parse("text/css");
    public static final MimeType CSV = parse("text/csv");
    public static final MimeType HTML = parse("text/html");
    public static final MimeType PHP = parse("text/php");
    public static final MimeType PLAIN = parse("text/plain");

    @SneakyThrows
    public static MimeType parse(String str) {
        var matcher = PATTERN.matcher(str);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid MimeType format: " + str);
        return new MimeType(
                matcher.group("type"),
                matcher.group("tree"),
                matcher.group("subtype"),
                matcher.group("suffix"),
                Optional.ofNullable(matcher.group("args"))
                        .map(x -> x.split(";")).orElse(null));
    }

    public MimeType(@NotNull String type, @NotNull String subtype) {
        this(type, null, subtype, null, null);
    }

    public MimeType(@NotNull String type, @Nullable String tree, @NotNull String subtype, @Nullable String suffix, @Nullable String[] args) {
        this.type = type;
        this.tree = tree;
        this.subtype = subtype;
        this.suffix = suffix;
        this.args = args;
    }

    @NotNull String type;
    @Nullable String tree;
    @NotNull String subtype;
    @Nullable String suffix;
    String @Nullable [] args;

    @Override
    public String toString() {
        var sb = new StringBuilder(type).append("/");
        if (tree != null)
            sb.append(tree).append('.');
        sb.append(subtype);
        if (suffix != null)
            sb.append('+').append(suffix);
        if (args != null)
            sb.append(String.join(";", args));
        return sb.toString();
    }

    @Ignore
    public interface Container {
        default MimeType getMimeType() {
            return getMimeTypes()[0];
        }

        default MimeType[] getMimeTypes() {
            return new MimeType[]{getMimeType()};
        }
    }
}
