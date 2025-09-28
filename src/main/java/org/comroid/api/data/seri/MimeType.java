package org.comroid.api.data.seri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.With;
import org.comroid.annotations.Ignore;
import org.comroid.api.data.seri.adp.FormData;
import org.comroid.api.data.seri.adp.Jackson;
import org.comroid.api.java.SoftDepend;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@With
@Value
@Builder
@Ignore
public class MimeType {
    public static final Pattern PATTERN = Pattern.compile(
            "(?<type>[\\w-_]+)/((?<tree>[\\w-_.]+)\\.)?(?<subtype>[\\w-_]+)(\\+(?<suffix>[\\w-_]+))?(?<args>;[\\w-_]+)*");

    public static final MimeType GRAPHQL    = parse("application/graphql");
    public static final MimeType JAVASCRIPT = parse("application/javascript");
    public static final MimeType JSON       = parse("application/json",
            DataNode::json,
            SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                    .ifPresentMapOrElseGet($ -> Jackson.JSON, () -> org.comroid.api.data.seri.adp.JSON.Parser));
    public static final MimeType MSWORD     = parse("application/msword");
    public static final MimeType PDF        = parse("application/pdf");
    public static final MimeType SQL        = parse("application/sql");
    public static final MimeType URLENCODED = parse("application/x-www-form-urlencoded",
            DataNode::form,
            FormData.Parser);
    public static final MimeType XML        = parse("application/xml");
    public static final MimeType ZIP        = parse("application/zip");
    public static final MimeType ZSTD       = parse("application/zstd");
    public static final MimeType MPEG       = parse("audio/mpeg");
    public static final MimeType OGG        = parse("audio/ogg");
    public static final MimeType APNG       = parse("image/apng");
    public static final MimeType GIF        = parse("image/gif");
    public static final MimeType JPEG       = parse("image/jpeg");
    public static final MimeType PNG        = parse("image/png");
    public static final MimeType SVG        = parse("image/svg+xml");
    public static final MimeType DATA       = parse("multipart/form-data");
    public static final MimeType CSS        = parse("text/css");
    public static final MimeType CSV        = parse("text/csv");
    public static final MimeType HTML       = parse("text/html");
    public static final MimeType PHP        = parse("text/php");
    public static final MimeType PLAIN      = parse("text/plain");

    public static MimeType parse(
            String str
    ) {
        return parse(str, null, null);
    }

    @SneakyThrows
    public static MimeType parse(
            String str, @Nullable Function<DataNode, DataNode> serializerPrefix,
            @Nullable Serializer<? extends DataNode> deserializer
    ) {
        var matcher = PATTERN.matcher(str);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid MimeType format: " + str);
        return new MimeType(matcher.group("type"),
                matcher.group("tree"),
                matcher.group("subtype"),
                matcher.group("suffix"),
                Optional.ofNullable(matcher.group("args")).map(x -> x.split(";")).orElse(null),
                serializerPrefix,
                deserializer);
    }

    public static Optional<MimeType> forExtension(String extension) {
        return Stream.of(JSON, XML, HTML, PHP, CSV, GIF, JPEG, PNG, SVG, APNG)
                .filter(mimeType -> Objects.equals(mimeType.tree, extension))
                .findAny();
    }

    @NotNull  String type;
    @Nullable String tree;
    @NotNull  String subtype;
    @Nullable String suffix;
    String @Nullable [] args;
    @Nullable Function<DataNode, DataNode>   serializerPrefix;
    @Nullable Serializer<? extends DataNode> deserializer;

    public MimeType(
            @NotNull String type, @NotNull String subtype, @Nullable Function<DataNode, DataNode> serializerPrefix,
            @Nullable Serializer<?> deserializer
    ) {
        this(type, null, subtype, null, null, null, null);
    }

    public MimeType(
            @NotNull String type, @Nullable String tree, @NotNull String subtype, @Nullable String suffix,
            @Nullable String[] args, @Nullable Function<DataNode, DataNode> serializerPrefix,
            @Nullable Serializer<? extends DataNode> deserializer
    ) {
        this.type             = type;
        this.tree             = tree;
        this.subtype          = subtype;
        this.suffix           = suffix;
        this.args             = args;
        this.serializerPrefix = serializerPrefix;
        this.deserializer     = deserializer;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(type).append("/");
        if (tree != null) sb.append(tree).append('.');
        sb.append(subtype);
        if (suffix != null) sb.append('+').append(suffix);
        if (args != null) sb.append(String.join(";", args));
        return sb.toString();
    }

    @Ignore
    public interface Container {
        @Ignore
        @JsonIgnore
        default MimeType getMimeType() {
            return getMimeTypes()[0];
        }

        @Ignore
        @JsonIgnore
        default MimeType[] getMimeTypes() {
            return new MimeType[]{ getMimeType() };
        }
    }
}
