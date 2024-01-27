package org.comroid.api.net;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Streams;
import org.comroid.api.io.FileTransfer;
import org.comroid.api.java.InnerClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Value
public class GitHub {
    public static final GitHub Default = new GitHub(REST.Default, null);

    @NotNull REST rest;
    @Nullable String token;

    public CompletableFuture<List<Release>> fetchReleases(String owner, String repo) {
        var request = rest.newRequest(REST.Method.GET,
                "https://api.github.com/repos/%s/%s/releases".formatted(owner, repo));
        if (token != null)
            request.addHeader("Authorization", "Bearer " + token);
        return request.execute()
                .thenApply(REST.Response::validate2xxOK)
                .thenApply(rsp -> rsp.getBody().asArray().stream()
                        .flatMap(Streams.cast(DataNode.class))
                        .map(DataNode::asObject)
                        .map(obj -> obj.<Release>convert(Release.class))
                        .toList());
    }

    @Value
    @NonFinal
    public static abstract class Entity implements Named {
        long id;
        URI htmlUrl;
    }

    @Value
    public class User extends Entity implements InnerClass<GitHub> {
        String login;

        public User(long id, URI htmlUrl, String login) {
            super(id, htmlUrl);
            this.login = login;
        }

        @Override
        public String getPrimaryName() {
            return login;
        }

        @Override
        public GitHub outer() {
            return GitHub.this;
        }}

    @Value
    public class Release extends Entity implements InnerClass<GitHub> {
        String name;
        String tagName;
        List<Asset> assets;

        public Release(long id, URI htmlUrl, String name, String tagName, List<Asset> assets) {
            super(id, htmlUrl);
            this.name = name;
            this.tagName = tagName;
            this.assets = assets;
        }

        @Override
        public GitHub outer() {
            return GitHub.this;
        }

        @Value
        public class Asset extends Entity implements InnerClass<Release> {
            String name;
            URI browserDownloadUrl;

            public Asset(long id, URI htmlUrl, String name, URI browserDownloadUrl) {
                super(id, htmlUrl);
                this.name = name;
                this.browserDownloadUrl = browserDownloadUrl;
            }

            public FileTransfer download(File destination) {
                return new FileTransfer(browserDownloadUrl, destination.toURI());
            }

            @Override
            public Release outer() {
                return Release.this;
            }
        }
    }
}
